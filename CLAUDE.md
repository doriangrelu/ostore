# CLAUDE.md — OStore

Stockage d'objets **compatible S3**, open source (**Apache 2.0**), enrichi d'une mécanique de
**transactions / réservations** : un fichier peut être déposé en état *temporaire* et n'est
conservé que si un tiers (autre micro-service) valide la transaction avant son expiration.

> Ce fichier est la source de vérité des directives. Toute nouvelle directive utilisateur
> doit y être reportée (section « Directives ») **et** dans la doc associée (ADR / conventions).

## Documents de référence

| Document | Rôle |
|---|---|
| [docs/PLAN.md](docs/PLAN.md) | Vision, périmètre, architecture cible, roadmap |
| [docs/TASKS.md](docs/TASKS.md) | Backlog : tâches, statut, critères de validation |
| [docs/adr/](docs/adr/) | Décisions d'architecture (format MADR) |
| [docs/conventions/code.md](docs/conventions/code.md) | Conventions Java / clean architecture |
| [docs/conventions/database.md](docs/conventions/database.md) | Conventions SQL, migrations Postgres / Oracle |
| [docs/conventions/workflow.md](docs/conventions/workflow.md) | Méthode de travail (tâches, validation, commits) |
| [docs/conventions/ci.md](docs/conventions/ci.md) | CI GitHub Actions, qualimétrie, activation SonarCloud |

## Directives (à respecter strictement)

1. **Langues** : code, identifiants, messages de log, commits → **anglais**.
   Javadoc, commentaires, documentation (`docs/`, README) → **français**.
2. **Stack** : Java 25, Spring Boot 4.x, virtual threads activés.
   **Build : Maven** multi-module via le wrapper `./mvnw` (jamais Gradle, jamais le Maven installé localement).
3. **Clean architecture** : le domaine ne dépend d'aucun framework. Les dépendances pointent vers le domaine.
4. **Multi-module sans sur-découpage** (ADR-0002) : un module n'existe que s'il est publié ou
   remplaçable séparément → `ostore-contract`, `ostore-drivers/` (SPI + un sous-module par driver),
   `ostore-server` (tout le backend, couches = **packages**, protégées par ArchUnit).
   Ne jamais créer de module pour une couche interne. Le module `ostore-contract` (interfaces de ressources + DTO annotés et validés)
   publiable seul (ADR-0003) :
   - annotations **Spring MVC** sur les interfaces → utilisables par Spring Cloud OpenFeign,
     OpenFeign (`feign-spring`) et tout générateur ;
   - côté serveur, un contrôleur = `@RestController` + `implements XxxApi`, **rien d'autre** ;
   - la spec **OpenAPI est générée depuis le contrat**, jamais écrite à la main ;
   - le choix et les limites de la technologie cliente (Feign ou autre) relèvent **du client**, pas d'OStore.
5. **Périmètre v1** : Buckets, Objets (fichiers), Transactions. Rien d'autre sans accord explicite.
6. **Drivers de stockage extensibles** : S3 réel, FileSystem ; ajout d'un driver = nouveau module implémentant la SPI.
7. **Base relationnelle** pour les métadonnées : scripts **PostgreSQL** et **Oracle** maintenus en parallèle.
   - Oracle : tablespaces paramétrables (tables **et** index), synonymes optionnels.
   - Instructions séparées au maximum : tables → index → contraintes PK/UK → FK → synonymes/grants.
   - Indexer systématiquement (FK, colonnes de filtre, de tri, de purge).
8. **Persistance** : Spring Data JDBC (ADR-0004), entités de persistance séparées du domaine.
9. **Objets en attente** : invisibles sauf via `resourceId` ou en-tête `x-ostore-transaction-id` (ADR-0009).
10. **Authentification S3** : décision reportée au jalon M5 (ADR-0008).
    **Multipart S3** : en v1, **non systématique** : uniquement si le client l'initie, désactivable ;
    un `PutObject` simple reste mono-blob (ADR-0010).
11. **Code** : lisible, simple, factorisé, moderne (records, sealed, pattern matching, streams, lambdas, `Optional`).
12. **Méthode** : travailler par tâches (`docs/TASKS.md`), chacune avec critères de validation ;
    une tâche n'est « Terminée » qu'une fois ses critères vérifiés (build + tests) et, pour les tâches
    marquées 🔒, validée par l'utilisateur.
13. **Documenter** tout choix structurant dans un ADR, toute bonne pratique dans `docs/conventions/`.
14. **Commits** : [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/), en anglais
    (`<type>(<scope>)!: <description>`), détail dans [workflow.md](docs/conventions/workflow.md).

## Commandes

Sous Windows, utiliser `mvnw.cmd` (PowerShell) ; le Git Bash local n'a ni `sed` ni `grep`.

| Action | Commande |
|---|---|
| Build complet + tests | `./mvnw -B verify` |
| Un module et ses dépendances | `./mvnw -B verify -pl ostore-server -am` |
| Formater + en-têtes de licence | `./mvnw -B spotless:apply` |
| Lancer l'application | `./mvnw -B spring-boot:run -pl ostore-server` |

Le build échoue si : format ou en-tête de licence incorrect (Spotless), règle d'architecture violée
(ArchUnit), Java < 25 ou Maven < 3.9 (Enforcer). Les `package-info.java` reçoivent l'en-tête de
licence **à la main** (Spotless les ignore).

Coordonnées Maven : `io.github.doriangrelu:ostore-*`, package racine `io.github.doriangrelu.ostore`.
Dépôt : `git@github.com:doriangrelu/ostore.git`.

## Points d'attention

- **Gros fichiers (plusieurs Go)** : jamais de fichier entier en mémoire, streaming de bout en bout
  (`InputStream`), ni `byte[]` ni `MultipartFile` pour les contenus ; mémoire bornée vérifiée par test.
- Transactions prolongeables (`/extend`) dans la limite de `max-ttl` (ADR-0006).
- Migrations exécutables par l'application **ou** rendues en SQL pour les DBA (ADR-0005).
- La base est la source de vérité ; le stockage physique est nettoyé de façon asynchrone et idempotente.
- Toute requête de purge doit être sûre en cluster (`FOR UPDATE SKIP LOCKED`).
