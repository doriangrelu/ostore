---
name: db-migration
description: Ajouter ou modifier le schéma de base OStore via des migrations Flyway écrites en parallèle pour PostgreSQL et Oracle (instructions séparées, index systématiques, tablespaces et synonymes Oracle paramétrables). À utiliser pour toute création ou évolution de table, index ou contrainte.
---

# Migration PostgreSQL + Oracle

Références : `docs/conventions/database.md`, ADR-0005.
Emplacement : `ostore-server/src/main/resources/db/migration/{postgresql,oracle}/`.

## Règles absolues
- **Toujours les deux SGBD**, même numéro de version, même découpage de fichiers.
- Une migration publiée sur `main` n'est **jamais modifiée** : toute correction passe par une
  nouvelle migration.
- Une instruction DDL = un objet.

## Découpage d'une version `V<x>_<y>_<z>` (un fichier par étape)
| Fichier | Contenu |
|---|---|
| `V<ver>_001__<sujet>_tables.sql` | `CREATE TABLE` : colonnes + `NOT NULL` uniquement |
| `V<ver>_002__<sujet>_indexes.sql` | Tous les index, y compris ceux supportant PK/UK, et **chaque FK** |
| `V<ver>_003__<sujet>_primary_unique_keys.sql` | `ALTER TABLE … ADD CONSTRAINT PK_/UK_ …` (Oracle : `USING INDEX <index>`) |
| `V<ver>_004__<sujet>_check_constraints.sql` | `CK_…` (statuts, bornes) |
| `V<ver>_005__<sujet>_foreign_keys.sql` | `FK_…` |
| `V<ver>_006__<sujet>_comments.sql` | `COMMENT ON TABLE/COLUMN`, en français |

Omettre une étape vide plutôt que créer un fichier vide.

## Spécificités Oracle
- Table : `CREATE TABLE OST_X ( … ) ${ostore_table_tablespace_clause};`
- Index : `CREATE INDEX IX_… ON OST_X ( … ) ${ostore_index_tablespace_clause};`
- Placeholders vides par défaut : les scripts doivent rester valides sans tablespace.
- Types : `RAW(16)` (UUID), `VARCHAR2(n CHAR)`, `NUMBER(19)`, `TIMESTAMP WITH TIME ZONE`.
- Unicité conditionnelle : index unique fonctionnel `CASE WHEN … THEN … END`.
- Synonymes et grants : uniquement dans `db/optional/oracle/{synonyms,grants}/R__*.sql`,
  paramétrés par `${ostore_schema_owner}`, `${ostore_app_user}`, `${ostore_synonym_type}`.

## Spécificités PostgreSQL
- Types : `UUID`, `VARCHAR(n)`, `BIGINT`, `TIMESTAMP WITH TIME ZONE`.
- Unicité conditionnelle : index unique partiel `WHERE …`.

## Nommage
`OST_<TABLE>` au singulier, colonnes de 30 caractères maximum, `PK_<TABLE>`, `UK_<TABLE>_<COLS>`,
`FK_<TABLE>_<REF>`, `IX_<TABLE>_<COLS>`, `CK_<TABLE>_<COL>`.

## Vérification
- Le TI traversant de la tranche démarre l'application sur PostgreSQL **et** Oracle (Testcontainers) :
  les migrations y sont appliquées.
- Relire : chaque FK a son index ; chaque colonne de filtre, de tri ou de purge est indexée ; les
  index composites sont ordonnés « égalité puis intervalle ».
