# Revue indépendante du 27/11/2026 : code, performances, sécurité

Revue statique de `main` (3222223), menée par trois relecteurs indépendants : code et architecture,
performances, sécurité. Les constats retenus ci-dessous ont été vérifiés dans le code ; ceux marqués « à
confirmer » le seront par un test avant correction.

## Ce qui est solide

- Pas de traversée de chemin : les chemins de blob sont produits par le serveur (`BlobPath` en liste blanche,
  garde `normalize()` du driver FS), jamais à partir du nom de l'objet ou du bucket.
- SQL entièrement paramétré, `LIKE` échappé, placeholders Flyway validés.
- Streaming réel de bout en bout ; aucune I/O de blob sous transaction de base.
- Machine à états des transactions nette, commit et rollback idempotents, expiration sûre en cluster.
- Pagination keyset indexée, clés étrangères indexées sur les deux SGBD, ArchUnit sur les couches.
- CI durcie : actions épinglées par SHA, permissions minimales, pas de `pull_request_target`.

## À trancher (conception)

| # | Sujet | Constat | Source |
|---|---|---|---|
| D1 | Copie d'un objet `PENDING` | `copy` sans en-tête crée une copie `ACTIVE` d'un contenu non validé, qui survit au rollback ; la copie ignore aussi la version en attente de la transaction de l'appelant (`ObjectService.java:124`). ADR-0016 muet. | sécurité 1, code 10 |
| D2 | Remplacement et suppression d'un `PENDING` sans sa transaction | `PUT /content` sans en-tête corrige sur place l'objet en attente d'une autre transaction (`ObjectService.java:192-200`) ; `DELETE` l'est sans contrôle (permis par ADR-0016 § Visibilité). Le validateur peut valider un contenu qu'il n'a pas inspecté. | sécurité 2 |
| D3 | Identifiant de transaction divulgué | Le 409 `OBJECT_LOCKED` cite l'id de la transaction (`ObjectLockedException.java:24`), aussi renvoyé dans l'en-tête de contenu. Connaître l'id suffit pour commit ou rollback. | sécurité 3 |
| D4 | Contenu servi inline | `Content-Disposition: attachment` seulement si l'objet a un nom, `Content-Type` choisi par le client, pas de `nosniff` ni de CSP : XSS stockée sur l'origine de l'API, donc de la future console (`ObjectDtoMapper.java:101-110`). | sécurité 4 |
| D5 | Taille d'upload non bornée | Ni taille maximale, ni quota ; un seul envoi peut remplir le volume. | sécurité 5 |
| D6 | Objets de plus de 5 Gio sur S3 | Un seul `PutObject` (limite S3 de 5 Gio) ; la copie relit et réécrit tout via le serveur. | perf 4, code 5 |

### Décisions de l'utilisateur (27/11/2026)

- **D1** : copie d'un `PENDING` refusée (`409 OBJECT_LOCKED`), sauf si l'appelant rejoint la transaction de la
  source ; la copie entre alors dans cette transaction, et c'est la version en attente qui est copiée.
- **D2** : remplacer ou supprimer un `PENDING` exige `X-OStore-Transaction-Id` égal à sa transaction, sinon
  `409 OBJECT_LOCKED` (révision de l'ADR-0016).
- **D3** : l'identifiant de transaction est retiré du message `OBJECT_LOCKED`, dès maintenant.
- **D4** : `/content` toujours servi en `attachment` (nom par défaut = id), avec `nosniff` et CSP `sandbox`, dès
  maintenant.
- **D5 / D6** : taille maximale configurable (`413` avant écriture) ; multipart S3 natif au-delà d'un seuil et
  copie native optionnelle dans la SPI (ADR à écrire).

## Défauts à corriger

### Prioritaires

| # | Défaut | Où | Correctif proposé |
|---|---|---|---|
| C1 | ~~`Range` ignoré : Spring lirait tout le flux pour en mesurer la taille.~~ **Faux positif** (T3b.1) : Spring exclut `InputStreamResource` de sa gestion des plages ; un TI le garde. | `ObjectController.java:99-114` | Rejeter ou servir sans `Resource` lisible une seule fois ; TI multi-plages |
| C2 | Verrou de l'objet en « vérifier puis agir » : remplacement hors transaction concurrent d'une mise en attente → écrasé au commit ; violations UK/FK servies en 500. | `ObjectService.java:172-215` | `SELECT … FOR UPDATE` sur la ligne visée, `delete` en `UnitOfWork` |
| C3 | Téléchargement S3 interrompu : `close()` vide tout le reste de l'objet. | `S3StorageDriver.java:70-81` | `abort()` si le flux n'est pas épuisé |
| C4 | Valeur de métadonnée vide : 201 sur PostgreSQL, 500 sur Oracle (`''` = `NULL`, colonne `NOT NULL`). | `ObjectMetadata.java:79` | Règle unique dans le domaine ; TI sur les deux SGBD |
| C5 | Une transaction en échec bloque toute l'expiration (pas d'isolation, même tête de liste à chaque passage). | `TransactionService.java:114-124` | try/catch par transaction, log `warn` |

### Ensuite

- Purge immédiate d'un blob encore lu (500 en lecture concurrente) : délai de grâce `ostore.purge.delay`.
- Blobs orphelins si la base est saturée ou en cas de crash entre écriture et insertion : enregistrer
  l'intention de purge avant d'écrire, exception d'origine conservée (`addSuppressed`).
- Pool S3 (50) et pool JDBC (10) non dimensionnés face aux virtual threads : propriétés et borne de concurrence.
- Commit, rollback et expiration en N+1 sous le verrou de la transaction : SQL ensembliste.
- Purge : `ORDER BY` sans limite SQL, suppressions de blobs sous transaction.
- Listage par préfixe en O(taille du bucket) (index `(BUCKET_ID, STATUS, ID)`).
- Logique de bascule du remplacement logée dans l'adaptateur JDBC (directive 3) : la ramener au domaine.
- Erreurs : jeton de continuation invalide en 500, `DataIntegrityViolationException` non traduite,
  `DomainException` non `sealed`, `StorageDriverNotFoundException` servie en 500 avec son message.
- Conflits d'un remplacement détectés après l'upload complet.
- `Content-Type` de plus de 255 caractères : blob écrit puis 500.
- CI : `SONAR_TOKEN` au niveau du job, `persist-credentials` par défaut, wrapper sans somme SHA-256.
- Écoute sur `0.0.0.0` par défaut alors que l'API n'est pas exposable avant M5.
- `OST_TRANSACTION` sans rétention.

### Tests manquants

Expiration d'une transaction portant un remplacement ; copie en transaction ; remplacement sur place d'un
`PENDING` ; métadonnée vide sur Oracle ; `Range` multi-plages sur un gros objet.

## Pour le jalon M5 (sécurité)

Authentification de l'API et de la console ; autorisation par bucket ; transaction rattachée à un principal
(l'id ne doit plus suffire), séparation déposant / validateur ; commit lié aux ETag inspectés ; droits sur la
source et la cible d'une copie ; CSRF si la console utilise des cookies ; en-têtes de sécurité et origine
distincte pour `/content` ; quotas et limitation de débit ; journal d'audit ; secrets S3 hors des propriétés ;
exposition de `/v3/api-docs`.
