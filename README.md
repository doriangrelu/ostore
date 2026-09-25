# OStore

[![CI](https://github.com/doriangrelu/ostore/actions/workflows/ci.yml/badge.svg)](https://github.com/doriangrelu/ostore/actions/workflows/ci.yml)
[![CodeQL](https://github.com/doriangrelu/ostore/actions/workflows/codeql.yml/badge.svg)](https://github.com/doriangrelu/ostore/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Stockage d'objets avec une **API JSON** et des **transactions** : un fichier peut être déposé en attente et
n'est conservé que si un autre service valide la transaction avant son expiration. Sinon il est
entièrement supprimé.

> ⚠️ Projet en cours de construction (jalon M0). Rien n'est encore utilisable.

- Java 25 · Spring Boot 4 · virtual threads
- API JSON unique décrite par OpenAPI, sémantiques inspirées de S3 (buckets, clés, ETag, Range, multipart)
- Stockage : système de fichiers ou S3 en backend (drivers extensibles)
- Métadonnées : PostgreSQL ou Oracle

## Construire

```bash
./mvnw verify
```

## Documentation
- [Plan directeur](docs/PLAN.md) · [Backlog](docs/TASKS.md) · [Décisions d'architecture](docs/adr/README.md)
- Conventions : [code](docs/conventions/code.md) · [base de données](docs/conventions/database.md) ·
  [méthode](docs/conventions/workflow.md) · [CI](docs/conventions/ci.md)

## Licence
[Apache License 2.0](LICENSE)
