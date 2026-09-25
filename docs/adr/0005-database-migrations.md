# ADR-0005 — Migrations Flyway PostgreSQL / Oracle

- Statut : **Accepté** (validé par l'utilisateur, 2026-09-25)

## Décision
- **Flyway** (OSS, module `flyway-database-oracle`), scripts **SQL natifs** — lisibles par un DBA.
- Un répertoire par SGBD, **même numérotation** des deux côtés :
  `db/migration/{postgresql,oracle}/V<version>_<seq>__<description>.sql`.
- Une migration « logique » = plusieurs fichiers séparés, dans cet ordre :
  `001__tables` → `002__indexes` → `003__primary_unique_keys` → `004__foreign_keys` → `005__comments`.
- **Oracle** — placeholders contenant la clause **complète** (vide par défaut, donc optionnelle) :
  - `${ostore_table_tablespace_clause}` → ex. `TABLESPACE OSTORE_DATA`
  - `${ostore_index_tablespace_clause}` → ex. `TABLESPACE OSTORE_IDX`
  - Les PK/UK sont créées **après** leur index explicite (`USING INDEX <idx>`) pour que l'index
    atterrisse dans le tablespace d'index.
- **Synonymes / grants** : location Flyway séparée et **optionnelle**
  (`db/optional/oracle/synonyms`, `.../grants`), scripts répétables `R__`, paramétrés par
  `${ostore_schema_owner}`, `${ostore_app_user}`, `${ostore_synonym_type}` (`PRIVATE`/`PUBLIC`).
  Cas couverts : schéma propriétaire ≠ utilisateur applicatif ; pas de synonyme (accès qualifié).
- **Exécution** : soit par l'application au démarrage (`ostore.db.migrate=true`), soit par les DBA
  via l'outil de rendu SQL (T2.3), Flyway OSS n'offrant pas de *dry-run*.

Détails de style : [conventions/database.md](../conventions/database.md).
