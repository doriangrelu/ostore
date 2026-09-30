# ADR-0015 — API des objets et flux binaires

- Statut : **Accepté** (jalon M2, validé par l'utilisateur le 2026-09-30). Révisé le 2026-09-30 : les
  objets sont désignés par leur **identifiant**, la clé devient un **nom libre** (demande utilisateur).

## Contexte
Une API JSON unique (ADR-0014), des fichiers de plusieurs Go à mémoire bornée (ADR-0003), un contrat
utilisable par des clients tiers (Feign, générateurs).

Une première version désignait les objets par une **clé** porteuse de chemin (`?key=invoices/2026/a.pdf`).
L'utilisateur l'a refusée : un objet doit être désigné par un **identifiant unique, sans `/`**.
L'ETag ne convient pas : c'est l'empreinte MD5 du *contenu*, identique pour deux fichiers identiques et
modifiée à chaque remplacement.

## Décision

### 1. Adressage par identifiant, nom libre
- Un objet est désigné **uniquement** par son `id` (UUID v7), renvoyé au dépôt. Toutes les opérations sur un
  objet passent par `/api/v1/objects/{id}`.
- Le **nom** est libre et optionnel (1 à 1024 octets UTF-8, sans caractère de contrôle), **non unique**. Il
  sert au filtrage des listes et devient le nom de fichier proposé au téléchargement
  (`Content-Disposition`).

| Opération | Requête | Réponse |
|---|---|---|
| Déposer (crée toujours un nouvel objet) | `POST /buckets/{bucket}/objects?name=` — corps binaire en flux | `201` + JSON `ObjectResponse` (`id`) |
| Lister | `GET /buckets/{bucket}/objects?namePrefix=&limit=&continuationToken=` | JSON `ObjectListResponse` |
| Lire les métadonnées | `GET /objects/{id}` | JSON `ObjectResponse` |
| Lire le contenu | `GET /objects/{id}/content` — `Range` optionnel | `200` / `206` binaire en flux |
| Remplacer le contenu (même `id`) | `PUT /objects/{id}/content?name=` — corps binaire en flux | `200` + JSON |
| Copier | `POST /objects/{id}/copy` — JSON (bucket cible, nom), optionnels | `201` + JSON (nouvel `id`) |
| Supprimer | `DELETE /objects/{id}` | `204` |

### 2. Dépôt en flux
- Corps `@RequestBody InputStreamResource` : Spring le lit en flux. **Piège** : déclaré `Resource`, le
  corps serait lu entièrement en mémoire par `ResourceHttpMessageConverter` (détecté par le test de 3 Gio).
  Le `Content-Type` de la requête devient celui de l'objet.
- **`Content-Length` obligatoire**, vérifié à l'octet près. Sinon : erreur `CONTENT_LENGTH_MISMATCH`, et le
  blob partiel est purgé. Un envoi de taille inconnue ou repris passera par le multipart (ADR-0010).
- **Métadonnées utilisateur** : en-tête répétable `X-OStore-Meta: nom=valeur` (valeur percent-encodée
  UTF-8), 2 Kio au total. En JSON, elles apparaissent sous forme de dictionnaire.
- **ETag** = MD5 hexadécimal du contenu, calculé par le serveur pendant le flux, indépendamment du driver.

### 3. Lecture en flux
- La réponse porte `Content-Length`, `Content-Type`, `ETag`, `Last-Modified`, `Accept-Ranges: bytes`,
  `X-OStore-Resource-Id`, `Content-Disposition` (si l'objet a un nom) et les `X-OStore-Meta`.
- **`Range` (RFC 9110)** : une seule plage (`a-b`, `a-`, `-n`) → `206` + `Content-Range`, lue directement
  dans le driver. Plusieurs plages → contenu complet en `200` (autorisé par la RFC). Plage hors limites →
  `416`.

### 4. Liste
Pagination par curseur (*keyset*) sur l'identifiant : un UUID v7 étant chronologique, l'ordre est celui des
dépôts. La comparaison est faite octet par octet, identique sur PostgreSQL (`UUID`) et Oracle (`RAW(16)`).
Le jeton de continuation est le dernier identifiant de la page. Le filtre `namePrefix` s'appuie sur
`LIKE` avec échappement de `%` et `_` ; sans filtre, les objets sans nom sont listés aussi.

### 5. Remplacement, suppression et purge (ADR-0006)
- Chaque dépôt ou remplacement écrit un **nouveau blob** (voir §7 pour son chemin). Un remplacement met à
  jour l'objet en une transaction SQL (verrouillage optimiste), et l'ancien blob est planifié pour purge
  (`OST_BLOB_PURGE`). Deux remplacements simultanés : l'un reçoit `409 CONCURRENT_UPDATE`.
- Un job planifié supprime les blobs par lots, sûr en cluster (`FOR UPDATE SKIP LOCKED`), avec retry et
  backoff exponentiel.
- Supprimer un bucket non vide → `409 BUCKET_NOT_EMPTY`.

### 6. Drivers (ADR-0007)
- SPI : `write(BlobPath, InputStream, long size)`, `read(BlobPath, Optional<ByteRange>)`, `delete`
  idempotent, `exists`. Un driver ne connaît ni les buckets, ni les objets, ni les transactions.
- Des **instances** nommées sont configurées, chacune avec son type ; chaque bucket référence une instance :
  ```yaml
  ostore.storage:
    default-driver: local
    drivers:
      local: { type: filesystem, layout: date, properties: { root: ./data/blobs } }
      archive: { type: s3, properties: { bucket: ostore-blobs, endpoint: https://…, region: eu-west-3 } }
  ```
- Le **kit de conformité** (`test-jar` de la SPI) est exécuté par chaque driver : sur un répertoire
  temporaire pour `filesystem`, sur **Adobe S3Mock** (Testcontainers) pour `s3`. L'image MinIO n'est plus
  publiée.

### 7. Organisation des chemins de stockage (demande utilisateur, 2026-09-25)
- Une **stratégie de chemins** (`BlobPathLayout`, SPI) calcule le chemin relatif d'un nouveau blob à partir
  de son identifiant et de sa date de dépôt (UTC) :
  - `date` (**défaut**) : `2026/09/25/<uuid>` ;
  - `hashed` : `98/50/<uuid>`, répartition uniforme ;
  - `flat` : `<uuid>` à plat.
  D'autres stratégies peuvent être ajoutées par `ServiceLoader`.
- **Configurable ou surchargeable** : `ostore.storage.drivers.<id>.layout` l'emporte ; à défaut, le driver
  impose la sienne via `StorageDriver.defaultLayout()` (`date` par défaut, redéfinissable).
- **Le chemin est stocké en base** (`OST_OBJECT.BLOB_PATH`, `OST_BLOB_PURGE.BLOB_PATH`) : lecture et purge
  utilisent le chemin enregistré, donc changer de stratégie ne touche que les nouveaux blobs.
- Le driver range le blob **exactement** à ce chemin sous sa racine, sans réorganisation propre. Le chemin
  est validé (segments `[A-Za-z0-9._-]`, ni `.` ni `..`), ce qui interdit toute sortie de la racine.

### 8. Schéma
Migration `V1_2_0` (sans modifier `V1_1_0`, déjà publiée) : suppression de l'unicité `(BUCKET_ID,
OBJECT_KEY)`, `OBJECT_KEY` renommée `OBJECT_NAME` et rendue optionnelle, ajout de `UPDATED_AT`, index
`IX_OST_OBJECT_BUCKET_ID (BUCKET_ID, ID)` (FK + pagination) et `IX_OST_OBJECT_BUCKET_NAME` (filtre).

## Conséquences
- Les transactions (M3) ajouteront le statut `PENDING` et `TRANSACTION_ID` par une nouvelle migration ; la
  purge, les versions physiques et l'accès par identifiant (ADR-0009) sont déjà en place.
- Hors v1 : conditions `If-Match` / `If-None-Match`.
