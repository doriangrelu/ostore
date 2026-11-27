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
| T1.2 🔒 | Migration `OST_BUCKET` PG + Oracle selon les conventions (tables → index → PK/UK → checks → commentaires), placeholders de tablespace Oracle | ✅ | Relue par l'utilisateur ; appliquée sur les 2 SGBD par le TI |
| T1.3 | Domaine + cas d'usage buckets (`BucketName` avec règles de nommage, création, consultation, liste, suppression d'un bucket vide) | ✅ | Test unitaire paramétré des règles de nommage (seul test unitaire justifié) |
| T1.4 🔒 | Contrat `BucketApi` + DTO validés + erreurs `ProblemDetail` ; contrôleur = `implements` ; OpenAPI généré | ✅ | Relu par l'utilisateur ; ArchUnit : contrôleur REST sans annotation de mapping propre |
| T1.5 | ~~API S3 buckets (XML)~~ : livrée puis **retirée** (ADR-0014, API JSON uniquement) | ❌ | — |
| T1.6 | **TI traversant buckets** | ✅ | Scénarios lisibles par SGBD : créer → lire → lister → supprimer → absent ; doublon (409) et nom invalide (400) refusés |

## M2 — Tranche « Objets »

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T2.1 | SPI `StorageDriver` + kit de conformité (`test-jar`) + stratégies de chemins (`date`/`hashed`/`flat`) | ✅ | Kit exécuté par chaque driver ; chemins sûrs testés |
| T2.2 | Driver FileSystem (écriture atomique, chemin fourni par la stratégie) | ✅ | Kit vert |
| T2.3 | Driver S3 (AWS SDK v2) | ✅ | Kit vert sur Adobe S3Mock (MinIO ne publie plus d'images) |
| T2.4 | Objets de bout en bout, **désignés par identifiant** (nom libre) : migrations `V1_1_0` + `V1_2_0`, domaine, API JSON (création/lecture+Range/métadonnées/remplacement/copie/liste/suppression, `X-OStore-Meta`), purge asynchrone, chemins stockés en base | ✅ | — |
| T2.5 | **TI traversants objets** | ✅ | Aller-retour via le client du contrat ; fichier de 3 Gio avec `-Xmx512m` ; lecture `Range` ; même scénario sur chaque driver |

## M3 — Tranche « Transactions » (ADR-0016)

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T3.1 🔒 | Design de l'API des transactions (ADR-0016) | ✅ | Validé par l'utilisateur (TTL 15 min / 24 h, remplacement transactionnel inclus) |
| T3.2 | Migration `V1_3_0` : `OST_TRANSACTION`, statut / transaction / remplacement sur `OST_OBJECT` | ✅ | Appliquée sur PostgreSQL et Oracle par les TI |
| T3.3 | Domaine : machine à états, politique de durée de vie, objets en attente et remplacements | ✅ | Test unitaire de la machine à états |
| T3.4 | Cas d'usage : ouverture, commit, rollback, prolongation, expiration ; écritures transactionnelles (création, copie, remplacement) sous verrou | ✅ | — |
| T3.5 | API : `TransactionApi`, en-têtes `X-OStore-Transaction-Id` / `X-OStore-Pending-Ttl`, lecture de la version en attente | ✅ | — |
| T3.6 | **TI traversants transactions** | ✅ | Commit, rollback, expiration implicite, remplacement en attente, prolongation et erreurs ; sur PostgreSQL et Oracle |

## M3b — Durcissement (revue indépendante du 2026-11-27)

Constats et décisions : [revue du 2026-11-27](revues/2026-11-27-revue-independante.md). À livrer avant la
suite du jalon M4.

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T3b.1 | `Range` ignoré (plusieurs plages, inversé) : contenu complet servi en un passage, sans relecture par Spring (ADR-0015 §3) | ⬜ | TI : `Range` multi-plages et inversé sur un gros objet → `200` complet, flux du driver lu une seule fois (défaut d'abord reproduit par le TI) |
| T3b.2 | Objets `PENDING` réservés à leur transaction : remplacement, suppression, copie ; copie en transaction = version en attente ; conflits vérifiés avant l'envoi (ADR-0016 révisé) | ⬜ | TI sur PG et Oracle : remplacement, suppression et copie d'un `PENDING` sans sa transaction → `409` ; copie dans sa transaction → copie `PENDING`, annulée avec elle ; copie d'une version en attente |
| T3b.3 | Verrou de ligne sur l'objet visé (remplacement, suppression dans une `UnitOfWork`) ; violation d'intégrité → `409 OBJECT_LOCKED` ; bascule du remplacement au commit ramenée dans le domaine (directive 3) | ⬜ | TI concurrent (deux requêtes, latch dans un driver de test) : remplacement hors transaction refusé pendant une mise en attente ; aucun `500` |
| T3b.4 | Contenu jamais affiché par le navigateur : `attachment` toujours (nom par défaut = id), `nosniff`, CSP `sandbox` ; id de transaction retiré du message `OBJECT_LOCKED` | ⬜ | TI : en-têtes présents sur un objet sans nom ; `ProblemDetail` de `OBJECT_LOCKED` sans id de transaction |
| T3b.5 | Taille maximale `ostore.limits.max-object-size` → `413 OBJECT_TOO_LARGE` avant écriture (dépôt, remplacement, copie) ; `Content-Type` de plus de 255 caractères → `400` (ADR-0018 §1) | ⬜ | TI : dépôt au-delà de la limite refusé sans blob écrit ; copie refusée ; `Content-Type` trop long refusé |
| T3b.6 | Driver S3 : multipart natif au-delà du seuil, `AbortMultipartUpload` en cas d'échec, `abort()` d'un téléchargement fermé avant la fin, pool et délais configurables (ADR-0018 §2 et §4) | ⬜ | Kit de conformité vert ; envoi au-delà du seuil à mémoire bornée sur S3Mock ; téléchargement interrompu sans lecture du reste |
| T3b.7 | SPI : copie native optionnelle (méthode par défaut), FileSystem et S3, repli en flux entre instances (ADR-0018 §3) | ⬜ | Kit de conformité étendu, vert sur les deux drivers ; TI de copie inchangés |
| T3b.8 🔒 | Robustesse des erreurs : règle commune PG / Oracle pour une métadonnée vide (à valider), expiration isolée par transaction, jeton de continuation invalide → `400`, `DomainException` `sealed`, échec de planification de purge conservé en `suppressed` | ⬜ | TI sur PG et Oracle : métadonnée vide traitée à l'identique ; une transaction en échec ne bloque pas l'expiration des suivantes |
| T3b.9 | Purge : délai de grâce `ostore.purge.delay`, intention de purge enregistrée avant l'écriture du blob, lot limité en SQL, suppressions de blobs hors transaction SQL | ⬜ | TI : lecture d'un objet remplacé pendant le délai de grâce ; blob purgé après un échec d'insertion |
| T3b.10 | Clôture des transactions (commit, rollback, expiration) en SQL ensembliste, sans N+1 sous verrou ; pool JDBC documenté et configurable | ⬜ | TI transactions verts sur PG et Oracle ; nombre d'ordres SQL indépendant du nombre d'objets |
| T3b.11 🔒 | CI et défauts réseau : `SONAR_TOKEN` limité à l'étape Sonar, `persist-credentials: false`, `distributionSha256Sum` du wrapper ; écoute locale par défaut jusqu'à M5 (à valider) | ⬜ | CI verte |

Suivis hors de ce jalon : listage par préfixe en O(taille du bucket) et rétention de `OST_TRANSACTION`
(M7, exploitation) ; autorisation et transaction rattachée à un principal (M5, voir la revue).

## M4 — Console d'administration (ADR-0017)

| ID | Tâche | Statut | Critères de validation |
|---|---|---|---|
| T4.1 🔒 | Design de la console et des compléments du contrat (ADR-0017) | 🟨 | Validé par l'utilisateur |
| T4.2 | Contrat : liste des transactions (filtres statut / référence, pagination) ; `objectCount` et `totalSize` sur le détail d'un bucket ; migration `V1_4_0` | ⬜ | TI sur PostgreSQL et Oracle |
| T4.3 | Module `ostore-console` : build Vite via `frontend-maven-plugin` (Node local), auto-configuration `ostore.console.enabled`, repli SPA | ⬜ | TI : console servie si activée, `404` sinon, `index.html` sur une route de la SPA ; `./mvnw verify` sans Node installé |
| T4.4 | Spécification OpenAPI versionnée dans la console, client TypeScript généré | ⬜ | TI : échec si la spécification diverge du contrat ; `vue-tsc` vert |
| T4.5 | Écrans buckets et objets (liste, détail, dépôt en flux, téléchargement, remplacement, copie, suppression) | ⬜ | Vérification manuelle ; confirmation avant toute suppression |
| T4.6 | Écrans transactions (liste filtrée, détail et objets, commit, rollback, prolongation) | ⬜ | Vérification manuelle |
| T4.7 | Tableau de bord | ⬜ | Vérification manuelle |
| T4.8 | CI (cache Node) et documentation (activation, mode développement, avertissement sécurité) | ⬜ | CI verte |

## M5 — Sécurité · M6 — Multipart · M7 — Exploitation · M8 — Release

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
