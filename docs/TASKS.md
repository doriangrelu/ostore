# Backlog des tâches

Méthode : voir [conventions/workflow.md](conventions/workflow.md).
Statuts : ⬜ À faire · 🟦 En cours · 🟨 À valider (utilisateur) · ✅ Terminée · ⛔ Bloquée
🔒 = validation utilisateur obligatoire avant de passer à « Terminée ».

## M0 — Fondations

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T0.1 🔒 | Plan, méthode, CLAUDE.md, ADR initiaux | ✅ | Décisions ouvertes tranchées (ADR « Proposé » → « Accepté ») |
| T0.2 | `git init`, `.gitignore`, `.gitattributes`, LICENSE Apache 2.0, NOTICE | ✅ | Fichiers présents, remote `origin` configuré (en-tête de licence des sources → T0.4) |
| T0.3 | POM parent + wrapper Maven, Spring Boot 4.1.1, modules | ✅ | `./mvnw verify` vert sur Java 25 (Temurin 25.0.3, Maven 3.9.16) |
| T0.3b | Restructuration anti sur-découpage : `contract`, `drivers/{spi,filesystem,s3}`, `server` (packages) | ✅ | 7 modules Maven, `./mvnw verify` vert ; ADR-0002 révisé |
| T0.4 | Qualité : Spotless (palantir + en-tête licence), Enforcer (Java 25, Maven 3.9), JaCoCo, ArchUnit | ✅ | Violation volontaire de format → build rouge ; dépendance Spring dans `domain` → build rouge (vérifié) |
| T0.5 | CI GitHub Actions + qualimétrie : build/tests, rapports, couverture PR, SonarCloud (optionnel), CodeQL, dependency review, Dependabot | 🟦 | Pipelines CI et CodeQL verts sur `main` ; SonarCloud actif une fois `SONAR_TOKEN` configuré (action mainteneur) |
| T0.6 | Skills projet `.claude/skills/` (voir §Skills) | ⬜ | Skills utilisables et documentés |

## M1 — Domaine & contrat

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T1.1 | Domaine : `Bucket`, `StoredObject`, `Transaction`, value objects (`BucketName`, `ObjectKey`, `ResourceId`…) | ⬜ | Règles de nommage S3 testées, invariants testés |
| T1.2 | Machine à états des transactions (sealed + pattern matching) | ⬜ | Toutes les transitions testées, transitions illégales rejetées |
| T1.3 | Ports + cas d'usage (application) | ⬜ | Tests unitaires avec fakes en mémoire |
| T1.4 | Compléter ArchUnit : contrôleurs REST = `implements` d'une interface du contrat, sans annotation de mapping | ⬜ | Contrôleur non conforme → build rouge |
| T1.6 | Contenu d'objet sealed : `SingleBlobContent` (défaut) / `CompositeContent` (parties, ADR-0010) | ⬜ | Invariants testés (numérotation, 5 Mo min sauf dernière partie, ETag multipart) |
| T1.5 🔒 | Module `ostore-contract` : interfaces Spring MVC + DTO validés + annotations OpenAPI | ⬜ | Relu par l'utilisateur ; OpenAPI généré depuis les interfaces ; upload/download d'un fichier de plusieurs Go avec un tas JVM réduit (ex. `-Xmx256m`) |

## M2 — Persistance

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T2.1 🔒 | Scripts PostgreSQL (tables / index / PK-UK / FK séparés) | ⬜ | Migration OK sur PG (Testcontainers) |
| T2.2 🔒 | Scripts Oracle + placeholders tablespace + synonymes optionnels | ⬜ | Migration OK sur Oracle Free, avec et sans tablespaces/synonymes |
| T2.3 | Outil de rendu SQL pour DBA (placeholders résolus, sans exécution) | ⬜ | Sortie SQL exécutable manuellement |
| T2.4 | Adaptateur de persistance (`infrastructure.persistence`) | ⬜ | Tests de repository exécutés sur PG **et** Oracle |

## M3 — Drivers de stockage

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T3.1 | SPI `StorageDriver` + kit de tests de conformité réutilisable | ⬜ | Kit publié en `test-jar` |
| T3.2 | Driver FileSystem (écriture atomique, arborescence sharding) | ⬜ | Kit de conformité vert |
| T3.3 | Driver S3 (AWS SDK v2) | ⬜ | Kit de conformité vert sur MinIO |

## M4 — API REST · M5 — API S3 · M5b — Multipart (ADR-0010) · M6 — Transactions bout en bout · M7 — Release

_Détaillés à l'ouverture de chaque jalon (voir [PLAN.md](PLAN.md) §5)._

## Skills projet à créer (T0.6)

| Skill | Usage |
|---|---|
| `task-workflow` | Démarrer / clôturer une tâche : mise à jour de TASKS.md, vérifications, résumé |
| `new-adr` | Créer un ADR MADR numéroté, le référencer dans l'index |
| `db-migration` | Ajouter une migration en parallèle PG + Oracle selon les conventions (ordre, placeholders, index) |
| `new-storage-driver` | Squelette d'un sous-module `ostore-drivers/ostore-driver-<id>` + branchement du kit de conformité |
| `add-directive` | Reporter une directive utilisateur dans CLAUDE.md + doc concernée |
