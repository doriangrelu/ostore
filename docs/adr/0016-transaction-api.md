# ADR-0016 — API des transactions

- Statut : **Accepté** (design validé par l'utilisateur le 2026-09-30). Précise ADR-0006 et ADR-0009.
  Révisé le 2026-11-27 (décisions de l'utilisateur après la revue indépendante) : un objet `PENDING` ne se
  remplace, ne se supprime ni ne se copie hors de sa transaction ; l'identifiant de transaction n'est plus
  cité dans les erreurs ; la ligne de l'objet visé est verrouillée.

## Contexte
Un fichier peut être déposé « en attente » et n'est conservé que si un autre micro-service valide la
transaction avant son échéance ; sinon il est entièrement supprimé (ADR-0006). Les objets sont désignés par
leur identifiant (ADR-0015).

## Décision

### Modèle
- **Transaction** : `id` (UUID v7), `status`, `reference` (texte libre optionnel, ex. numéro de dossier),
  `createdAt`, `expiresAt`, `closedAt`.
  `OPEN → COMMITTED` (commit), `OPEN → ROLLED_BACK` (rollback), `OPEN → EXPIRED` (échéance dépassée).
  Une transaction dont l'échéance est passée est **expirée**, même si le job ne l'a pas encore traitée.
- **Objet** : `status` (`PENDING` / `ACTIVE`) et `transactionId` (optionnel), renvoyés dans toutes les réponses.

### Endpoints (`/api/v1/transactions`)
| Opération | Requête | Effet |
|---|---|---|
| Ouvrir | `POST /` `{ttl?, reference?}` | `201`, transaction `OPEN` |
| Consulter | `GET /{id}` | statut, échéance, nombre d'objets |
| Objets | `GET /{id}/objects?limit=&continuationToken=` | objets de la transaction (tous buckets), paginés |
| Valider | `POST /{id}/commit` | objets `PENDING` → `ACTIVE`, atomiquement |
| Annuler | `POST /{id}/rollback` | objets supprimés, blobs purgés |
| Prolonger | `POST /{id}/extend` `{ttl}` | nouvelle échéance = maintenant + ttl |

Idempotence : commit d'une transaction validée → `200` ; rollback d'une transaction annulée ou expirée →
`200` (l'état voulu est atteint). Commit d'une transaction annulée/expirée, rollback d'une transaction validée,
dépôt ou prolongation d'une transaction fermée → `409 TRANSACTION_CLOSED` (statut réel dans la propriété
`transactionStatus` du `ProblemDetail`).

### Écritures en transaction (création, copie **et remplacement**)
Les trois endpoints d'écriture d'objets acceptent les mêmes en-têtes :
- `X-OStore-Transaction-Id: <id>` : l'écriture rejoint une transaction ouverte (mode explicite, plusieurs
  fichiers) ;
- `X-OStore-Pending-Ttl: PT15M` : une transaction est créée pour cette seule écriture (mode implicite) ;
- les deux à la fois → `400 CONFLICTING_TRANSACTION_HEADERS`.

**Remplacement transactionnel d'un objet validé** (révision du 2026-09-30, à la demande de l'utilisateur,
qui remplace la première décision « interdit en v1 ») :
- la nouvelle version est une **ligne interne `PENDING`** de `OST_OBJECT` qui référence l'objet visé
  (`REPLACES_OBJECT_ID`, unique : **un seul remplacement en attente par objet**). Son identifiant n'est jamais
  exposé : les réponses portent l'identifiant de l'objet visé, avec `status = PENDING` ;
- lecture sans en-tête = version validée ; avec `X-OStore-Transaction-Id` = version en attente de cette
  transaction (le validateur peut l'inspecter) ;
- **commit** : le contenu en attente (blob, taille, ETag, type, nom, métadonnées) bascule sur l'objet visé,
  même identifiant ; l'ancien blob est purgé ;
- **rollback / expiration** : la version en attente est supprimée et son blob purgé ; l'objet validé est
  intact ;
- tant qu'un remplacement est en attente, l'objet visé ne peut être ni remplacé hors transaction ni supprimé
  (`409 OBJECT_LOCKED`) ; un nouveau remplacement dans **la même** transaction met à jour la version en attente.

### Objets `PENDING` : réservés à leur transaction (révision du 2026-11-27)
Un objet `PENDING` (nouvel objet d'une transaction) n'est modifiable que **dans sa transaction** : le
validateur doit pouvoir valider exactement ce qu'il a inspecté.
- **Remplacer** : `X-OStore-Transaction-Id` égal à sa transaction, qui doit être ouverte ; il est alors corrigé
  sur place et reste dans sa transaction. Sans en-tête, avec une autre transaction ou avec
  `X-OStore-Pending-Ttl` → `409 OBJECT_LOCKED`.
- **Supprimer** : même règle ; sans l'en-tête de sa transaction → `409 OBJECT_LOCKED`.
- **Copier** une source `PENDING` : refusé (`409 OBJECT_LOCKED`), sauf si l'appelant rejoint la transaction de
  la source (`X-OStore-Transaction-Id`) ; la copie entre alors dans cette même transaction.
- Plus généralement, une copie faite avec `X-OStore-Transaction-Id` lit la source comme une lecture avec cet
  en-tête : la **version en attente** de cette transaction si elle existe (objet `PENDING` ou remplacement en
  attente), la version validée sinon.
- Les conflits connus avant l'envoi (objet `PENDING` d'une autre transaction, remplacement en attente) sont
  vérifiés **avant** d'écrire le blob, pour ne pas recevoir un gros fichier pour rien.
- Le `ProblemDetail` de `OBJECT_LOCKED` ne cite **pas** l'identifiant de la transaction qui détient l'objet :
  connaître cet identifiant suffit à valider ou annuler la transaction tant que l'authentification n'est pas
  en place (ADR-0008).

### Durées de vie
`ostore.transactions.default-ttl` = **PT15M**, `max-ttl` = **P1D**, prolongations comprises
(`expiresAt - createdAt ≤ max-ttl`). Durée nulle, négative ou trop longue → `400 INVALID_TTL`.

### Cohérence
- Le blob est écrit **avant** toute écriture en base (ADR-0006).
- Dépôt en transaction, commit, rollback et expiration **verrouillent la ligne de la transaction**
  (`FOR UPDATE`) : aucun objet ne peut rejoindre une transaction pendant sa clôture.
- Remplacement et suppression **verrouillent aussi la ligne de l'objet visé** (`FOR UPDATE`) avant de
  vérifier les conflits (révision du 2026-11-27) : sans ce verrou, un remplacement hors transaction concurrent
  d'une mise en attente passerait, puis serait écrasé au commit. Verrous toujours pris dans l'ordre du commit
  (transaction, puis objet), donc sans interblocage. Filet de sécurité : un second remplacement en attente du
  même objet (unicité de `REPLACES_OBJECT_ID`) est traduit en `409 OBJECT_LOCKED`, jamais en `500`. Le port
  `UnitOfWork` expose la transaction SQL au cœur applicatif, sans dépendance à Spring.
- Job d'expiration : sélection des transactions échues, puis verrouillage de chacune en
  `FOR UPDATE SKIP LOCKED` (sûr en cluster) ; les objets en attente sont supprimés et leurs blobs purgés.

### Visibilité (ADR-0009)
Les listes d'un bucket ne montrent que les objets `ACTIVE`. Un objet `PENDING` reste lisible par son
identifiant ; il n'est supprimable que dans sa transaction (voir plus haut). Un bucket qui en contient n'est
pas vide.

## Conséquences
- Migration `V1_3_0` : table `OST_TRANSACTION`, colonnes `STATUS`, `TRANSACTION_ID` et
  `REPLACES_OBJECT_ID` sur `OST_OBJECT`.
- Codes d'erreur : `TRANSACTION_NOT_FOUND` (404), `TRANSACTION_CLOSED` (409), `OBJECT_LOCKED` (409),
  `INVALID_TTL` (400), `CONFLICTING_TRANSACTION_HEADERS` (400).
