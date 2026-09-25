# Conventions base de données

## Nommage
- Tables : `OST_<NOM>` au singulier, majuscules, `SNAKE_CASE`. Colonnes idem, ≤ 30 caractères
  (compatibilité anciennes versions Oracle et lisibilité).
- Contraintes et index, préfixés et explicites :

| Objet | Motif | Exemple |
|---|---|---|
| Clé primaire | `PK_<TABLE>` | `PK_OST_OBJECT` |
| Unique | `UK_<TABLE>_<COLS>` | `UK_OST_BUCKET_NAME` |
| Clé étrangère | `FK_<TABLE>_<REF>` | `FK_OST_OBJECT_BUCKET` |
| Index | `IX_<TABLE>_<COLS>` | `IX_OST_OBJECT_TX` |
| Check | `CK_<TABLE>_<COL>` | `CK_OST_TRANSACTION_STATUS` |

## Types
| Concept | PostgreSQL | Oracle |
|---|---|---|
| Identifiant UUID v7 | `UUID` | `RAW(16)` |
| Texte court | `VARCHAR(n)` | `VARCHAR2(n CHAR)` |
| Clé objet S3 | `VARCHAR(1024)` | `VARCHAR2(1024 CHAR)` |
| Horodatage | `TIMESTAMP WITH TIME ZONE` | `TIMESTAMP WITH TIME ZONE` |
| Taille | `BIGINT` | `NUMBER(19)` |
| Statut | `VARCHAR(16)` + `CHECK` | `VARCHAR2(16 CHAR)` + `CHECK` |
| Version optimiste | `BIGINT` | `NUMBER(19)` |

## Séparation des instructions
Une instruction DDL = un objet. Ordre des fichiers d'une migration :
1. `…_001__tables.sql` — `CREATE TABLE` avec colonnes et `NOT NULL` **uniquement** ;
2. `…_002__indexes.sql` — tous les index, y compris ceux supportant PK/UK ;
3. `…_003__primary_unique_keys.sql` — `ALTER TABLE … ADD CONSTRAINT … [USING INDEX …]` ;
4. `…_004__check_constraints.sql` ;
5. `…_005__foreign_keys.sql` — `ALTER TABLE … ADD CONSTRAINT FK_…` ;
6. `…_006__comments.sql` — `COMMENT ON TABLE/COLUMN` (en français).

## Indexation
- **Chaque FK est indexée** (évite les verrous de table Oracle et les scans à la suppression).
- Index sur toute colonne de filtre, de tri ou de purge (`STATUS`, `EXPIRES_AT`, `NOT_BEFORE`).
- Index composites ordonnés « égalité puis intervalle » (ex. `(STATUS, EXPIRES_AT)`).
- Unicité conditionnelle : index partiel (PG) / index fonctionnel `CASE WHEN` (Oracle).

## Oracle : tablespaces et synonymes
- Tables : `CREATE TABLE … ( … ) ${ostore_table_tablespace_clause};`
- Index : `CREATE INDEX … ON … ( … ) ${ostore_index_tablespace_clause};`
- Placeholders vides par défaut → scripts valides sans tablespace dédié.
- Synonymes et grants dans des locations optionnelles, activées par configuration (voir ADR-0005).

## Exécution des migrations
- Scripts dans `ostore-server/src/main/resources/db/migration/{postgresql,oracle}` ; Spring Boot choisit
  le répertoire selon le SGBD (`spring.flyway.locations=classpath:db/migration/{vendor}`).
- Table d'historique Flyway : `OST_SCHEMA_HISTORY` (Oracle) / `ost_schema_history` (PostgreSQL), dans la
  casse native du SGBD pour être interrogeable sans guillemets.
- Tablespaces Oracle : `ostore.database.table-tablespace` et `ostore.database.index-tablespace`
  (noms validés comme identifiants SQL).
- Versions : `V<majeure>_<mineure>_<patch>_<étape>__<sujet>_<nature>.sql`, ex.
  `V1_0_0_002__bucket_indexes.sql`.

## Évolution
- Une migration publiée n'est **jamais** modifiée ; toute correction = nouvelle migration.
- Toute migration est ajoutée **simultanément** pour PostgreSQL et Oracle, testée sur les deux.
