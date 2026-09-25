# Conventions de code Java

## Langue
- Identifiants, logs, messages d'exception, commits : **anglais**.
- Javadoc et commentaires : **français**. La Javadoc explique le **pourquoi** et le contrat
  (invariants, exceptions, nullité), pas la paraphrase du code.

## Format et en-têtes
- Format **palantir-java-format** (4 espaces, 120 colonnes), imports ordonnés, imports inutiles
  supprimés : appliqué par `./mvnw spotless:apply` et vérifié au build (`verify`).
- Chaque fichier Java commence par l'en-tête Apache 2.0 (« Copyright <année> the OStore contributors »).
  Spotless l'ajoute automatiquement, sauf dans `package-info.java` où il est écrit à la main.
- Chaque package possède un `package-info.java` : Javadoc française décrivant son rôle + `@NullMarked`.

## Langage (Java 25)
- `record` pour DTO, value objects, commandes, événements. Validation des invariants dans le
  constructeur compact.
- `sealed interface` + `switch` exhaustif avec pattern matching pour les états et résultats
  (ex. `TransactionState`, `PutObjectResult`).
- `var` quand le type est évident à droite ; sinon type explicite.
- Streams et lambdas pour les transformations de collections ; boucle classique si elle est plus
  lisible (effets de bord, sortie anticipée). Pas de stream imbriqué illisible.
- `Optional` uniquement en type de retour, jamais en paramètre ni en champ.
- Nullité : **JSpecify** `@NullMarked` au niveau package ; `@Nullable` explicite sinon.
- Immutabilité par défaut (`List.copyOf`, `Map.copyOf`), pas de setter dans le domaine.
- Pas de Lombok (les records couvrent le besoin).

## Virtual threads
- `spring.threads.virtual.enabled=true`.
- Pas de `ThreadLocal` coûteux, pas de pools de threads maison pour l'I/O.
- Préférer `ReentrantLock` à `synchronized` autour d'I/O longues (lisibilité, même si JEP 491 lève le pinning).
- Streaming : `InputStream` → driver sans buffer intégral ; `transferTo` ; tailles contrôlées.

## Packages typés (directive utilisateur)
Un package ne contient **qu'un seul type de classe**. Règles vérifiées par `PackageConventionTest` (ArchUnit).

| Package | Contenu |
|---|---|
| `domain.model` | Agrégats et entités métier (records immuables) |
| `domain.model.vo` | Value objects : valeurs sans identité validées à la construction (`BucketName`…) |
| `domain.exception` | Exceptions métier (`DomainException` et filles) |
| `domain.util` | Utilitaires purs (identifiants…) |
| `application.port.in` / `application.port.out` | Ports entrants / sortants |
| `application.service` | Services applicatifs (**classes, jamais de records**, champs `private final`) |
| `api.rest.controller` / `.handler` / `.mapper` | Contrôleurs / traduction d'erreurs / conversion DTO |
| `infrastructure.config` | Classes `@Configuration` |
| `infrastructure.properties` | `@ConfigurationProperties` |
| `infrastructure.persistence.entity` / `.repository` / `.adapter` / `.converter` | Entités / repositories Spring Data / adaptateurs des ports / convertisseurs |
| `contract.api` / `.dto` / `.error` / `.constant` | Interfaces REST / DTO / codes d'erreur / constantes |

Pourquoi pas de `record` pour un service : ses composants sont exposés par des accesseurs publics ;
un cast vers la classe concrète donnerait accès aux ports de sortie et casserait l'isolation.

## Clean architecture
- Domaine : aucun import `org.springframework`, `jakarta.persistence`, `software.amazon`.
- **Un port entrant par agrégat** (`BucketUseCases`), implémenté par un service de `application`
  (`BucketService`, un `record` sans annotation) et câblé dans `infrastructure.config`.
  Les cas d'usage reçoivent des types du domaine (`BucketName`), jamais des chaînes brutes.
- Un port sortant par besoin technique (`BucketRepository`), implémenté dans `infrastructure`.
- Exceptions métier dans le domaine (`BucketNotFoundException`…) ; traduction HTTP (ProblemDetail)
  **uniquement** dans l'adaptateur REST.
- Mapping entre couches via des méthodes statiques ou mappers dédiés, pas de MapStruct au départ.

## Tests (ADR-0011)
- **Par défaut, un test d'intégration traversant** (`*IT`, failsafe) : application démarrée,
  Testcontainers, appel par un vrai client (construit sur le contrat). Un test = un scénario
  métier lisible, avec *given / when / then* visibles.
- **Test unitaire** (`*Test`, surefire, sans Spring) seulement pour une règle pure et combinatoire,
  de préférence en `@ParameterizedTest`.
- JUnit + AssertJ ; noms de méthodes en phrase : `should_<résultat>_when_<condition>`.
- Pas de mock de nos propres classes ; pas d'objectif de nombre de tests.
