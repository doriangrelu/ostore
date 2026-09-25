# Plan directeur — OStore

## 1. Vision

OStore expose une API **compatible S3** (utilisable avec les SDK AWS, `aws cli`, rclone…) et une
**API REST** (JSON) qui ajoute la notion de **transaction** :

- un client dépose un ou plusieurs fichiers dans une transaction ouverte ;
- les fichiers sont **en attente** (`PENDING`) tant que la transaction n'est pas validée ;
- un autre micro-service **valide** (`commit`) ou **annule** (`rollback`) la transaction ;
- sans validation avant l'échéance (`expiresAt`), la transaction **expire** : métadonnées et
  contenu physique sont **entièrement supprimés**.

Chaque réponse d'écriture renvoie : **identifiant de ressource** (`resourceId`), **clé**
(`bucket` + `key`), **ETag**, et **identifiant de transaction** s'il existe.

## 2. Périmètre v1

| Inclus | Exclu (v1) |
|---|---|
| Buckets : create, delete, head, list | Versioning S3, lifecycle, policies, ACL |
| Objets : put, get (+ Range), head, delete, list (V2), copy | — |
| Multipart upload : **proposé en v1**, voir ADR-0010 | |
| Métadonnées utilisateur `x-amz-meta-*` | Chiffrement serveur, réplication |
| Transactions : open, commit, rollback, expiration, consultation | IHM, multi-tenant avancé |
| Drivers : FileSystem, S3 | Autres drivers (Azure, GCS…) — via SPI |
| PostgreSQL, Oracle | Autres SGBD |

## 3. Modèle fonctionnel des transactions

```
            open (ttl)                 commit
   ┌──────────────────────► OPEN ─────────────────► COMMITTED
   │                          │  \
 client                       │   \ rollback
                              │    └─────────────► ROLLED_BACK ─┐
                              │ expiresAt dépassé               ├─► purge métadonnées + blobs
                              └──────────────────► EXPIRED ─────┘
```

Deux modes d'usage :

1. **Explicite** : `POST /api/v1/transactions` → `transactionId`, puis `PUT` d'objets avec l'en-tête
   `x-ostore-transaction-id`. Permet de regrouper plusieurs fichiers.
2. **Implicite** (fichier unitaire) : `PUT` avec `x-ostore-pending-ttl: PT15M` → la transaction est
   créée à la volée et son id renvoyé dans `x-ostore-transaction-id`.

En-têtes de réponse ajoutés (compatibles SDK S3, ignorés par les clients standards) :
`x-ostore-resource-id`, `x-ostore-transaction-id`, `x-ostore-object-status`.

**Remplacement d'un objet existant dans une transaction** : chaque écriture crée une nouvelle
*version physique* (clé de stockage interne UUID, jamais la clé utilisateur). L'objet actif reste
servi jusqu'au commit, qui bascule atomiquement le pointeur. Rollback / expiration = suppression de
la seule version en attente. → Aucune perte de donnée en cas d'échec.

## 4. Architecture

### 4.1 Modules

Découpage **minimal** (directive utilisateur) : un module n'existe que s'il est publié ou
remplaçable séparément. Tout l'interne du backend vit dans **un seul module**, structuré par packages.

```
ostore/
├── ostore-contract                  # Publié seul : interfaces de ressources + DTO validés → clients, OpenAPI.
├── ostore-drivers/                  # Agrégateur des drivers de stockage (pom).
│   ├── ostore-driver-spi            # SPI StorageDriver + kit de conformité (test-jar).
│   ├── ostore-driver-filesystem     # Driver FileSystem (NIO, écriture atomique).
│   └── ostore-driver-s3             # Driver S3 (AWS SDK v2).
└── ostore-server                    # Backend Spring Boot : domaine, cas d'usage, API, persistance, migrations.
```

Packages de `ostore-server` (racine `io.github.doriangrelu.ostore`) :

```
├── OStoreApplication
├── domain/                     # Modèle métier pur (records, value objects, règles)
├── application/                # Cas d'usage
│   ├── port/in/                #   interfaces des cas d'usage
│   └── port/out/               #   interfaces vers l'infrastructure
├── api/                        # Adaptateurs entrants
│   ├── rest/                   #   contrôleurs = implements des interfaces du contrat
│   └── s3/                     #   protocole S3 (XML, erreurs, aws-chunked)
└── infrastructure/             # Adaptateurs sortants + configuration Spring
    ├── persistence/            #   Spring Data JDBC
    ├── storage/                #   chargement des drivers, adaptation SPI → ports
    └── migration/              #   Flyway + rendu SQL DBA
resources/db/migration/{postgresql,oracle}/
```

Les tests d'intégration (Testcontainers : PostgreSQL, Oracle Free, MinIO) vivent dans
`ostore-server` (suffixe `*IT`, exécutés par failsafe).

### 4.2 Règles de dépendances

```
 api ──────► application ──► domain
   │             │
   ▼             ▼
 contract     driver-spi ◄── driver-filesystem / driver-s3 (scope runtime pour le serveur)
 infrastructure ──► application, domain, driver-spi
```
- **Entre modules** : garanties par Maven. Le serveur ne voit les drivers fournis qu'en scope
  `runtime`, donc il ne peut coder que contre la SPI.
- **Entre packages** : garanties par **ArchUnit** (`ArchitectureTest`), qui fait échouer le build.

### 4.3 SPI de stockage (esquisse)

```java
/** Contrat d'un support physique de stockage. Implémentations découvertes par ServiceLoader / Spring. */
public interface StorageDriver {
    String id();                                                   // "filesystem", "s3", ...
    StoredBlob write(BlobLocation location, InputStream content, long expectedSize);
    InputStream read(BlobLocation location, Optional<ByteRange> range);
    void delete(BlobLocation location);                           // idempotent
    boolean exists(BlobLocation location);
}
```
Le driver ne connaît **ni** les transactions **ni** les clés utilisateur : il manipule des
`BlobLocation` opaques (UUID). Toute la logique métier reste dans `application`.

### 4.4 Cohérence base / stockage

1. Écriture : blob écrit **d'abord** (clé UUID), puis métadonnées insérées en transaction SQL.
   Échec SQL → blob enregistré dans la file de purge.
2. Suppression / rollback / expiration : métadonnées marquées + entrée dans `OST_BLOB_PURGE`
   dans la **même** transaction SQL ; un job supprime les blobs (idempotent, retry avec backoff).
3. Jobs planifiés (expiration, purge) sûrs en cluster via `SELECT … FOR UPDATE SKIP LOCKED`
   (supporté par PostgreSQL et Oracle), par lots.

### 4.5 Modèle de données (esquisse, préfixe `OST_`)

| Table | Colonnes clés |
|---|---|
| `OST_BUCKET` | `ID`, `NAME` (UK), `DRIVER_ID`, `CREATED_AT`, `VERSION` |
| `OST_TRANSACTION` | `ID`, `STATUS`, `CREATED_AT`, `EXPIRES_AT`, `CLOSED_AT`, `CLIENT_REFERENCE`, `VERSION` |
| `OST_OBJECT` | `ID` (= resourceId), `BUCKET_ID`, `OBJECT_KEY`, `STATUS` (PENDING/ACTIVE/SUPERSEDED/DELETED), `TRANSACTION_ID`, `STORAGE_KEY`, `SIZE_BYTES`, `ETAG`, `CONTENT_TYPE`, `CREATED_AT`, `ACTIVATED_AT` |
| `OST_OBJECT_METADATA` | `OBJECT_ID`, `NAME`, `VALUE` |
| `OST_BLOB_PURGE` | `ID`, `DRIVER_ID`, `STORAGE_KEY`, `NOT_BEFORE`, `ATTEMPTS`, `LAST_ERROR` |

Unicité « un seul objet ACTIVE par (bucket, clé) » : index unique partiel (PostgreSQL) /
index unique fonctionnel `CASE WHEN STATUS='ACTIVE' …` (Oracle).

Identifiants : **UUID v7** (triables dans le temps → index B-tree efficaces), stockés en `UUID`
(PostgreSQL) / `RAW(16)` (Oracle).

## 5. Roadmap par jalons

| Jalon | Contenu | Livrable vérifiable |
|---|---|---|
| **M0 — Fondations** | Méthode, ADR, build multi-module, CI, qualité | `./mvnw verify` vert, ArchUnit en place |
| **M1 — Domaine & contrat** | Modèle, cas d'usage, DTO/interfaces | Tests unitaires domaine ≥ 90 % |
| **M2 — Persistance** | Migrations PG + Oracle, adaptateur JDBC | Migrations testées sur les 2 SGBD (Testcontainers) |
| **M3 — Drivers** | SPI, FileSystem, S3 | Suite de tests de conformité commune aux drivers |
| **M4 — API REST** | Buckets, objets, transactions (JSON) | Tests MockMvc + OpenAPI publié |
| **M5 — API S3** | Sous-ensemble S3, XML, erreurs | Tests avec **AWS SDK v2** et `aws cli` réels |
| **M6 — Transactions bout en bout** | Expiration, purge, cluster | Test IT : dépôt → expiration → blob supprimé |
| **M7 — Release OSS** | README, CONTRIBUTING, NOTICE, image Docker, publication contrat | Tag `v0.1.0` |

Le détail des tâches est dans [TASKS.md](TASKS.md).

## 6. Risques identifiés

| Risque | Mitigation |
|---|---|
| SDK AWS récents : `aws-chunked` + checksums CRC32 en trailer par défaut | Décodeur `aws-chunked` dédié, tests avec SDK réel dès M5 |
| Authentification SigV4 attendue par les SDK | Décision reportée à M5 (ADR-0008) ; API S3 non exposable avant |
| Clés S3 jusqu'à 1024 octets → limites de taille d'index (Oracle ~6,4 Ko, PG ~2,7 Ko) | `VARCHAR2(1024 CHAR)` OK en composite ; test explicite sur clés longues multi-octets |
| Divergence des scripts PG / Oracle | Tests de migration automatisés sur les 2 SGBD + même numérotation |
| Streaming et virtual threads | Pas de `synchronized` long ni de buffer mémoire ; tests de charge légers en M6 |
