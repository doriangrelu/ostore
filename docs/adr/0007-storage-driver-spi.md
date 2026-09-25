# ADR-0007 — SPI des drivers de stockage

- Statut : **Accepté**

## Décision
- Module agrégateur `ostore-drivers`, un sous-module par driver (`ostore-driver-<type>`).
- Sous-module `ostore-driver-spi` minimal (JDK seul) : interfaces `StorageDriver`,
  `StorageDriverFactory` et `BlobPathLayout`, types `BlobPath` et `ByteRange`, exceptions.
- Un driver = un module Maven, enregistré par une `StorageDriverFactory` découverte via
  `ServiceLoader` (utilisable hors Spring) et exposée en bean par `ostore-server` (package `infrastructure.storage`). Les drivers fournis
  sont des dépendances `runtime` du serveur ; un driver tiers s'ajoute au classpath.
- Configuration par driver sous `ostore.storage.drivers.<id>.*` ; chaque bucket référence un driver
  (`DRIVER_ID`), ce qui permet plusieurs backends simultanés.
- **Kit de conformité** (`test-jar` de la SPI) : suite JUnit abstraite que chaque driver étend.
  Un driver n'est accepté que si le kit passe.
- Drivers fournis : `filesystem` (écriture dans un fichier temporaire puis `ATOMIC_MOVE`, répertoires
  organisés par la stratégie de chemins) et `s3` (AWS SDK v2, testé sur Adobe S3Mock : MinIO ne publie
  plus d'images).
- **Instances nommées** : `ostore.storage.drivers.<id>` (type, stratégie de chemins, propriétés). Les buckets
  référencent une instance, ce qui permet plusieurs backends à la fois.
- **Chemins** : les blobs sont rangés au `BlobPath` calculé par une `BlobPathLayout` et stocké en base
  (ADR-0015 §7).
