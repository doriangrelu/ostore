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
| T0.5 | CI GitHub Actions + qualimétrie : build/tests, rapports, couverture PR, SonarCloud (optionnel), CodeQL, dependency review, Dependabot | ✅ | Pipelines CI et CodeQL verts sur `main` ; SonarCloud actif une fois `SONAR_TOKEN` configuré (action mainteneur) |
| T0.6 | Skills projet `.claude/skills/` (voir §Skills) | ✅ | 6 skills créés et référencés dans CLAUDE.md |

## M1 — Tranche « Buckets »

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T1.1 | Socle technique serveur : Spring Web MVC, Spring Data JDBC, Flyway (PG + Oracle), springdoc, Testcontainers (conteneurs singleton PG + Oracle Free), classe de base des TI | ✅ | L'application démarre sur PG et sur Oracle dans un TI ; CI verte |
| T1.2 🔒 | Migration `OST_BUCKET` PG + Oracle selon les conventions (tables → index → PK/UK → checks → commentaires), placeholders de tablespace Oracle | 🟨 | Relue par l'utilisateur ; appliquée sur les 2 SGBD par le TI |
| T1.3 | Domaine + cas d'usage buckets (`BucketName` avec règles de nommage, création, consultation, liste, suppression d'un bucket vide) | ✅ | Test unitaire paramétré des règles de nommage (seul test unitaire justifié) |
| T1.4 🔒 | Contrat `BucketApi` + DTO validés + erreurs `ProblemDetail` ; contrôleur = `implements` ; OpenAPI généré | 🟨 | Relu par l'utilisateur ; ArchUnit : contrôleur REST sans annotation de mapping propre |
| T1.5 | ~~API S3 buckets (XML)~~ : livrée puis **retirée** (ADR-0014, API JSON uniquement) | ❌ | — |
| T1.6 | **TI traversant buckets** | ✅ | Scénarios lisibles par SGBD : créer → lire → lister → supprimer → absent ; doublon (409) et nom invalide (400) refusés |

## M2 — Tranche « Objets »

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T2.1 | SPI `StorageDriver` + kit de conformité (`test-jar`) | ⬜ | Kit exécuté par chaque driver |
| T2.2 | Driver FileSystem (écriture atomique, sharding) | ⬜ | Kit vert |
| T2.3 | Driver S3 (AWS SDK v2) | ⬜ | Kit vert sur MinIO |
| T2.4 | Objets de bout en bout : migration, domaine (contenu sealed `SingleBlobContent` / `CompositeContent`), API JSON (put/get+Range/head/delete/list/copy, métadonnées `x-ostore-meta-*`) | ⬜ | — |
| T2.5 | **TI traversants objets** | ⬜ | Aller-retour via le client du contrat ; fichier de plusieurs Go avec `-Xmx256m` ; lecture `Range` ; même scénario sur chaque driver |

## M3 — Transactions · M4 — Multipart · M5 — Exploitation · M6 — Release

_Détaillés à l'ouverture de chaque jalon (voir [PLAN.md](PLAN.md) §5)._

## Skills projet à créer (T0.6)

| Skill | Usage |
|---|---|
| `task-workflow` | Démarrer / clôturer une tâche en trunk-based : TASKS.md, vérifications, commit conventionnel, push |
| `new-adr` | Créer un ADR MADR numéroté, le référencer dans l'index |
| `db-migration` | Ajouter une migration en parallèle PG + Oracle selon les conventions (ordre, placeholders, index) |
| `new-storage-driver` | Squelette d'un sous-module `ostore-drivers/ostore-driver-<id>` + branchement du kit de conformité |
| `add-directive` | Reporter une directive utilisateur dans CLAUDE.md + doc concernée |
| `integration-test` | Écrire un TI traversant lisible selon ADR-0011 |
