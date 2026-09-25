# Architecture Decision Records

Format [MADR](https://adr.github.io/madr/). Un fichier par décision, numérotation continue, jamais
supprimé : une décision remplacée passe en « Remplacé par ADR-XXXX ».

| ADR | Titre | Statut |
|---|---|---|
| [0001](0001-build-maven-multi-module.md) | Build Maven multi-module | Accepté |
| [0002](0002-clean-architecture-modules.md) | Clean architecture et découpage en modules | Accepté |
| [0003](0003-contract-module.md) | Module contrat : interfaces Spring MVC (Feign, OpenAPI) | Accepté |
| [0004](0004-persistence-technology.md) | Accès aux données : Spring Data JDBC | Accepté |
| [0005](0005-database-migrations.md) | Migrations Flyway PostgreSQL / Oracle | Accepté |
| [0006](0006-transaction-model.md) | Modèle de transaction, versions physiques et purge | Accepté |
| [0007](0007-storage-driver-spi.md) | SPI des drivers de stockage | Accepté |
| [0008](0008-s3-authentication.md) | Authentification de l'API S3 | Reporté (M5) |
| [0009](0009-pending-objects-visibility.md) | Visibilité des objets en attente | Accepté |
| [0010](0010-multipart-upload.md) | Multipart upload S3 en v1 (non systématique) | Accepté |
| [0011](0011-test-strategy.md) | Stratégie de tests : TI traversants d'abord | Accepté |
| [0012](0012-trunk-based-development.md) | Trunk-based development | Accepté |
| [0013](0013-http-routing.md) | Routage HTTP : S3 sous `/s3`, REST sous `/api/v1` | Accepté |
