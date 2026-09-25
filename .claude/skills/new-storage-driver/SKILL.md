---
name: new-storage-driver
description: Créer un nouveau driver de stockage OStore (sous-module Maven de ostore-drivers implémentant la SPI, enregistré par ServiceLoader, validé par le kit de conformité). À utiliser pour ajouter un backend de stockage (Azure Blob, GCS, etc.).
---

# Nouveau driver de stockage

Références : ADR-0007, `ostore-drivers/ostore-driver-spi`.

## Étapes
1. **Module** `ostore-drivers/ostore-driver-<id>/pom.xml` : parent `ostore-drivers`, dépendance vers
   `ostore-driver-spi`, en plus du SDK du backend. Aucune dépendance Spring.
2. Déclarer le module dans `ostore-drivers/pom.xml` (`<modules>`), puis dans le
   `dependencyManagement` du POM racine.
3. **Package** `io.github.doriangrelu.ostore.driver.<id>` avec un `package-info.java` (en-tête de
   licence, Javadoc française, `@NullMarked`).
4. **Implémentation** :
   - `StorageDriver` : `write`, `read` (avec `ByteRange` optionnel), `delete` idempotent, `exists`.
     Tout en **streaming**, sans jamais charger le contenu en mémoire.
   - `StorageDriverFactory`, déclarée dans
     `src/main/resources/META-INF/services/io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory`.
   - Configuration lue sous `ostore.storage.drivers.<id>.*`.
   - Le driver ne connaît **ni** les transactions **ni** les clés utilisateur : il ne manipule que
     des `BlobLocation` opaques.
5. **Tests** : une seule classe `*ConformanceIT` qui **étend le kit de conformité** de la SPI
   (`test-jar`) et fournit le driver, avec un backend réel en Testcontainers si possible
   (ex. MinIO, Azurite). Pas d'autres tests, sauf une règle pure propre au driver.
6. Si le driver est livré avec le serveur : l'ajouter en scope **`runtime`** dans `ostore-server/pom.xml`.
7. Documenter le driver et sa configuration (README du module, en français).

## Vérification
`./mvnw.cmd -B -ntp verify -pl ostore-drivers/ostore-driver-<id> -am` : vert, kit de conformité inclus.
