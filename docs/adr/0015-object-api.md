# ADR-0015 — API des objets et flux binaires

- Statut : **Proposé** (jalon M2, 2026-09-25). Implémenté, en attente de validation de l'utilisateur.

## Contexte
Une API JSON unique (ADR-0014), des fichiers de plusieurs Go à mémoire bornée (ADR-0003), un contrat
utilisable par des clients tiers (Feign, générateurs), et des clés d'objet libres (avec des `/`,
jusqu'à 1024 octets UTF-8).

## Décision

### 1. Adressage : la clé passe en paramètre de requête
Un objet est désigné par `/api/v1/buckets/{bucket}/objects…?key=<clé>`. Une clé contient des `/` :
- en segment de chemin, elle imposerait `%2F`, rejeté par défaut par Tomcat et encodé différemment
  selon les clients ;
- un motif « reste du chemin » (`{*key}`) n'est compris ni par Feign ni par OpenAPI.
Un paramètre de requête fonctionne avec tous les clients et générateurs.

| Opération | Requête | Réponse |
|---|---|---|
| Déposer (création ou remplacement) | `PUT /buckets/{bucket}/objects?key=` — corps binaire en flux | `201` + JSON `ObjectResponse` |
| Lire le contenu | `GET /buckets/{bucket}/objects/content?key=` — `Range` optionnel | `200` / `206` binaire en flux |
| Lire les métadonnées | `GET /buckets/{bucket}/objects/metadata?key=` | JSON `ObjectResponse` |
| Lister | `GET /buckets/{bucket}/objects?prefix=&maxKeys=&continuationToken=` | JSON `ObjectListResponse` |
| Copier | `POST /buckets/{bucket}/objects/copy` — JSON (clé source, bucket et clé cible) | `201` + JSON |
| Supprimer | `DELETE /buckets/{bucket}/objects?key=` | `204` |

### 2. Dépôt en flux
- Corps `@RequestBody InputStreamResource` : Spring le lit en flux. **Piège** : déclaré `Resource`, le
  corps serait lu entièrement en mémoire par `ResourceHttpMessageConverter` (détecté par le test de 3 Gio). Le `Content-Type` de la requête devient celui de l'objet.
- **`Content-Length` obligatoire.** C'est la sémantique S3 : le backend S3 l'exige, et la taille
  reçue est vérifiée à l'octet près. Sinon : erreur `CONTENT_LENGTH_MISMATCH`, et le blob partiel
  est purgé. Un envoi de taille inconnue ou repris passera par le multipart (ADR-0010).
- **Métadonnées utilisateur** : en-tête répétable `X-OStore-Meta: nom=valeur` (valeur
  percent-encodée UTF-8), 2 Kio au total. En JSON, elles apparaissent sous forme de dictionnaire.
- **ETag** = MD5 hexadécimal du contenu, calculé par le serveur pendant le flux, indépendamment du
  driver.

### 3. Lecture en flux
- La réponse porte `Content-Length`, `Content-Type`, `ETag`, `Last-Modified`, `Accept-Ranges: bytes`,
  `X-OStore-Resource-Id` et les `X-OStore-Meta`.
- **`Range` (RFC 9110)** : une seule plage (`a-b`, `a-`, `-n`) → `206` + `Content-Range`, lue
  directement dans le driver. Plusieurs plages → contenu complet en `200` (autorisé par la RFC).
  Plage hors limites → `416`.

### 4. Liste
Pagination par curseur (*keyset*) sur la clé, en **ordre binaire UTF-8** (comme S3), identique sur
PostgreSQL (colonne `COLLATE "C"`) et Oracle (session `NLS_SORT=BINARY`, `NLS_COMP=BINARY`, car le
driver JDBC hérite sinon de la langue de la JVM). Le jeton de continuation est la dernière clé,
encodée en base64url.

### 5. Remplacement, suppression et purge (ADR-0006)
- Chaque dépôt écrit un **nouveau blob** (identifiant UUID v7, voir §7 pour son chemin). En une transaction SQL, l'ancienne
  version est remplacée et son blob planifié pour purge (`OST_BLOB_PURGE`).
- Un job planifié supprime les blobs par lots, sûr en cluster (`FOR UPDATE SKIP LOCKED`), avec
  retry et backoff exponentiel.
- Supprimer un bucket non vide → `409 BUCKET_NOT_EMPTY`.

### 6. Drivers (ADR-0007)
- SPI : `write(BlobPath, InputStream, long size)`, `read(BlobPath, Optional<ByteRange>)`,
  `delete` idempotent, `exists`. Un driver ne connaît ni les buckets, ni les clés, ni les transactions.
- Des **instances** nommées sont configurées, chacune avec son type ; chaque bucket référence une
  instance :
  ```yaml
  ostore.storage:
    default-driver: local
    drivers:
      local: { type: filesystem, layout: date, properties: { root: ./data/blobs } }
      archive: { type: s3, properties: { bucket: ostore-blobs, endpoint: https://…, region: eu-west-3 } }
  ```
- Le **kit de conformité** (`test-jar` de la SPI) est exécuté par chaque driver : sur un répertoire
  temporaire pour `filesystem`, sur **Adobe S3Mock** (Testcontainers) pour `s3`. L'image MinIO n'est
  plus publiée.

### 7. Organisation des chemins de stockage (demande utilisateur, 2026-09-25)
- Une **stratégie de chemins** (`BlobPathLayout`, SPI) calcule le chemin relatif d'un nouveau blob à
  partir de son identifiant et de sa date de dépôt (UTC) :
  - `date` (**défaut**) : `2026/09/25/<uuid>` ;
  - `hashed` : `98/50/<uuid>`, répartition uniforme ;
  - `flat` : `<uuid>` à plat.
  D'autres stratégies peuvent être ajoutées par `ServiceLoader`.
- **Configurable ou surchargeable** : `ostore.storage.drivers.<id>.layout` l'emporte ; à défaut, le driver
  impose la sienne via `StorageDriver.defaultLayout()` (`date` par défaut, redéfinissable).
- **Le chemin est stocké en base** (`OST_OBJECT.BLOB_PATH`, `OST_BLOB_PURGE.BLOB_PATH`) : lecture et purge
  utilisent le chemin enregistré, donc changer de stratégie ne touche que les nouveaux blobs.
- Le driver range le blob **exactement** à ce chemin sous sa racine, sans réorganisation propre. Le
  chemin est validé (segments `[A-Za-z0-9._-]`, ni `.` ni `..`), ce qui interdit toute sortie de la racine.

## Conséquences
- Les transactions (M3) ajouteront le statut `PENDING` et `TRANSACTION_ID` par une nouvelle migration ;
  la purge et les versions physiques sont déjà en place.
- Hors v1 : accès par `resourceId` (arrive en M3 avec la visibilité des objets en attente),
  conditions `If-Match` / `If-None-Match`.
