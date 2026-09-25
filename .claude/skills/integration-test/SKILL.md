---
name: integration-test
description: Écrire un test d'intégration traversant OStore (application réelle, Testcontainers PostgreSQL/Oracle, client construit sur le contrat) simple et lisible selon ADR-0011. À utiliser pour prouver une tranche ou un scénario métier, plutôt que d'écrire des tests unitaires.
---

# TI traversant lisible (ADR-0011)

## Principes
- **Un test = un scénario métier**, raconté comme une histoire. On préfère un parcours complet
  avec plusieurs assertions à un test par assertion.
- **Vraies frontières** : l'application Spring Boot est démarrée (`webEnvironment = RANDOM_PORT`),
  la base est réelle (Testcontainers), le driver est réel, et l'appel passe par un **vrai client** :
  le client de test construit sur les interfaces du contrat (`OStoreRestClient`).
- **Aucun mock de nos classes.**
- **Peu de tests** : on ajoute un scénario seulement s'il couvre un comportement nouveau.
  Les variantes de données passent par `@ParameterizedTest`.

## Forme
```java
/** Scénario écrit une fois (interface), exécuté sur chaque SGBD par une classe vide. */
public interface PendingObjectScenarios extends IntegrationScenario {

    @Test
    default void should_expose_object_only_after_commit() {
        // Given : un bucket et une transaction ouverte
        var bucket = uniqueBucketName();
        rest().create(new CreateBucketRequest(bucket));
        var tx = rest().open(new OpenTransactionRequest(Duration.ofMinutes(5)));

        // When : le fichier est déposé dans la transaction
        var put = rest().putObject(bucket, "invoice.pdf", SAMPLE_PDF, tx.id());

        // Then : invisible sans la transaction, visible après commit
        assertThat(problemOf(() -> rest().getObject(bucket, "invoice.pdf")).code()).isEqualTo("OBJECT_NOT_FOUND");
        rest().commit(tx.id());
        assertThat(rest().getObject(bucket, "invoice.pdf")).hasSameContentAs(SAMPLE_PDF);
        assertThat(put.resourceId()).isNotNull();
    }
}

class PendingObjectPostgresIT extends PostgresIntegrationTest implements PendingObjectScenarios {}
class PendingObjectOracleIT extends OracleIntegrationTest implements PendingObjectScenarios {}
```

## Règles pratiques
- Suffixe `*IT` (exécuté par failsafe), nom de méthode en phrase `should_<résultat>_when_<condition>`.
- Le scénario tourne sur **PostgreSQL et Oracle** : interface `*Scenarios` dans `it.scenario`, deux
  classes vides `*PostgresIT` / `*OracleIT` dans `it` (voir `BucketScenarios`).
- **Isolation par données uniques** (un bucket aléatoire par test), jamais par nettoyage de base :
  les conteneurs et le contexte Spring restent partagés, ce qui garde les tests rapides.
- Les helpers (`rest()`, `uniqueBucketName()`, `problemOf`) forment un petit DSL de test. Ils masquent la plomberie,
  jamais le comportement vérifié.
- Gros fichiers : générer le contenu en flux (`InputStream` synthétique), sans tableau en mémoire,
  et vérifier par empreinte (SHA-256) calculée en flux.
