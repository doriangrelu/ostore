---
name: integration-test
description: Écrire un test d'intégration traversant OStore (application réelle, Testcontainers PostgreSQL/Oracle, vrai client SDK AWS ou client du contrat) simple et lisible selon ADR-0011. À utiliser pour prouver une tranche ou un scénario métier, plutôt que d'écrire des tests unitaires.
---

# TI traversant lisible (ADR-0011)

## Principes
- **Un test = un scénario métier**, raconté comme une histoire. On préfère un parcours complet
  avec plusieurs assertions à un test par assertion.
- **Vraies frontières** : l'application Spring Boot est démarrée (`webEnvironment = RANDOM_PORT`),
  la base est réelle (Testcontainers), le driver est réel, et l'appel passe par un **vrai client** :
  SDK AWS v2 (path-style) pour l'API S3, client construit sur le contrat pour l'API REST.
- **Aucun mock de nos classes.**
- **Peu de tests** : on ajoute un scénario seulement s'il couvre un comportement nouveau.
  Les variantes de données passent par `@ParameterizedTest`.

## Forme
```java
/** Scénario : un fichier déposé en transaction n'est visible qu'après validation. */
class PendingObjectVisibilityIT extends PostgresAndOracleIT {   // classe de base : conteneurs singleton

    @Test
    void should_expose_object_only_after_commit() {
        // Given : un bucket et une transaction ouverte
        var bucket = scenario.newBucket();
        var tx = rest.openTransaction(Duration.ofMinutes(5));

        // When : le fichier est déposé dans la transaction
        var put = s3.putObject(bucket, "invoice.pdf", SAMPLE_PDF, tx);

        // Then : invisible sans la transaction, visible après commit
        assertThat(s3.exists(bucket, "invoice.pdf")).isFalse();
        rest.commit(tx);
        assertThat(s3.read(bucket, "invoice.pdf")).hasSameContentAs(SAMPLE_PDF);
        assertThat(put.resourceId()).isNotNull();
    }
}
```

## Règles pratiques
- Suffixe `*IT` (exécuté par failsafe), nom de méthode en phrase `should_<résultat>_when_<condition>`.
- Le scénario doit tourner sur **PostgreSQL et Oracle** (classe de base paramétrée ou deux
  sous-classes) dès qu'il touche la persistance.
- **Isolation par données uniques** (un bucket aléatoire par test), jamais par nettoyage de base :
  les conteneurs et le contexte Spring restent partagés, ce qui garde les tests rapides.
- Les helpers (`scenario`, `rest`, `s3`) forment un petit DSL de test. Ils masquent la plomberie,
  jamais le comportement vérifié.
- Gros fichiers : générer le contenu en flux (`InputStream` synthétique), sans tableau en mémoire,
  et vérifier par empreinte (SHA-256) calculée en flux.
