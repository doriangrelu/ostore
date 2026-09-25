# ADR-0001 — Build Maven multi-module

- Statut : **Accepté** (directive utilisateur, 2026-09-25)

## Contexte
Projet open source multi-module, public cible majoritairement « entreprise » (Oracle, DBA).

## Décision
- **Maven 3.9.x** via le wrapper `./mvnw` (wrapper 3.3.4, type `only-script` : aucun jar binaire
  versionné ; version de Maven figée dans `.mvn/wrapper/maven-wrapper.properties`).
  Maven 4 sera évalué à sa sortie en version stable.
- Coordonnées : `groupId` **`io.github.doriangrelu`** (namespace GitHub, validable sur Maven Central),
  artifacts `ostore-*`, package racine `io.github.doriangrelu.ostore`.
- POM parent `ostore-parent`, **hérite de `spring-boot-starter-parent`** : versions des dépendances
  *et* des plugins gérées par Spring Boot, `-parameters` activé, `java.version=25`.
  Les versions non gérées par Boot (springdoc, ArchUnit, AWS SDK) sont des propriétés du parent.
  Un BOM importé seul n'aurait pas apporté le `pluginManagement`.
- Les modules internes sont déclarés dans le `dependencyManagement` du parent : les POM des modules
  ne portent jamais de version.
- Plugins transverses : `maven-enforcer-plugin` (Java 25, convergence des dépendances, interdiction
  des doublons), `spotless-maven-plugin` (format + en-tête licence), `jacoco-maven-plugin`,
  `maven-surefire-plugin` (unitaires) / `maven-failsafe-plugin` (intégration, suffixe `*IT`).
- `ostore-contract` n'hérite **pas** des dépendances Spring Boot applicatives : il ne dépend que de
  `spring-web` (annotations), `jakarta.validation-api` et `swagger-annotations`.

## Conséquences
- Gradle interdit (le Gradle local 7.4.2 est de toute façon incompatible Java 25).
- Versions toujours vérifiées au bootstrap (T0.3) plutôt que figées de mémoire.
