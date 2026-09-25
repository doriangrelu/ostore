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

## Clean architecture
- Domaine : aucun import `org.springframework`, `jakarta.persistence`, `software.amazon`.
- Un cas d'usage = une interface (port entrant) + une implémentation ; commande et résultat en `record`.
- Exceptions métier dans le domaine (`BucketNotFoundException`…) ; traduction HTTP (ProblemDetail /
  erreur S3 XML) **uniquement** dans les adaptateurs web.
- Mapping entre couches via des méthodes statiques ou mappers dédiés, pas de MapStruct au départ.

## Tests
- JUnit 5 + AssertJ ; noms de méthodes `should_<résultat>_when_<condition>`.
- Unitaires (`*Test`, surefire) sans Spring ; intégration (`*IT`, failsafe) avec Testcontainers.
- Fakes en mémoire pour les ports plutôt que des mocks quand c'est plus lisible.
