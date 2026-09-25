# ADR-0004 — Technologie d'accès aux données : Spring Data JDBC

- Statut : **Accepté** (choix utilisateur, 2026-09-25)

## Contexte
Deux SGBD (PostgreSQL, Oracle), schéma pouvant être accédé via synonymes, requêtes de purge
concurrentes, identifiants UUID (`UUID` en PG, `RAW(16)` en Oracle).

## Décision
**Spring Data JDBC**, dans le package `infrastructure.persistence` de `ostore-server`.
- Entités de persistance distinctes des objets du domaine (records `…Entity` + mappers) :
  le domaine reste sans annotation.
- Agrégats courts : `Bucket`, `Transaction`, `StoredObject` (+ ses métadonnées en collection).
- Verrouillage optimiste via `@Version` (colonne `VERSION`).
- Dialecte choisi automatiquement (Spring Data JDBC fournit PostgreSQL et Oracle).
- Convertisseurs dédiés `UUID ⇄ byte[]` pour Oracle (`RAW(16)`), enregistrés selon le dialecte.
- Requêtes spécifiques (`FOR UPDATE SKIP LOCKED`, index partiels) : `@Query` natives isolées dans
  des fragments de repository par dialecte, ou `JdbcClient` pour les jobs de purge.
- Noms de tables non qualifiés (résolution par `search_path`, synonymes ou `CURRENT_SCHEMA`).

## Alternatives écartées
- JPA / Hibernate : trop lourd, SQL implicite.
- `JdbcClient` seul : plus de contrôle mais plus de code de mapping ; reste utilisé ponctuellement.
