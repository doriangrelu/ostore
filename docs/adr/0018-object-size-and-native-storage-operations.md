# ADR-0018 — Taille maximale des objets, multipart S3 natif et copie native

- Statut : **Accepté** (décision de l'utilisateur après la revue indépendante, 2026-11-27). Complète
  ADR-0007 (SPI) et ADR-0015 ; ne change pas ADR-0010 (multipart à l'initiative du client).

## Contexte
La revue du 2026-11-27 a relevé trois limites liées à la taille des objets :

- **aucune taille maximale** : un seul dépôt peut remplir le volume de stockage, et une copie de quelques
  octets duplique un objet de plusieurs Go ;
- **un seul `PutObject` dans le driver S3** : S3 refuse un envoi unique de plus de 5 Gio, alors que les
  fichiers de plusieurs Go sont une exigence (ADR-0003) et que le driver FileSystem les accepte ;
- **la copie relit et réécrit tout le contenu à travers le serveur**, même quand le backend sait copier
  lui-même (S3 `CopyObject`, copie de fichier locale).

## Décision

### 1. Taille maximale d'un objet
- Propriété `ostore.limits.max-object-size` (`DataSize`, **10 Gio** par défaut).
- Dépôt et remplacement : un `Content-Length` supérieur est refusé **avant d'écrire** le moindre octet, en
  `413` avec le code `OBJECT_TOO_LARGE`.
- Copie : refusée de même si la taille de la source dépasse la limite (la limite a pu baisser depuis son
  dépôt).
- Multipart client (ADR-0010, jalon M6) : la somme des parties est soumise à la même limite.
- Les quotas par bucket ou par appelant et la limitation de débit relèvent du jalon M5 (sécurité).

### 2. Multipart S3 natif, interne au driver
- Le driver S3 envoie en **un seul `PutObject`** un contenu jusqu'à `multipart-threshold` (64 Mio par défaut),
  et au-delà en **multipart S3 natif** : parties de `part-size` (64 Mio par défaut), lues une à une dans le
  flux d'entrée (`InputStream` borné à la taille de la partie), donc à **mémoire bornée**.
- En cas d'échec, l'upload S3 est abandonné (`AbortMultipartUpload`) : aucun fragment ne reste facturé.
- C'est un détail d'implémentation du driver : ni la SPI ni le contrat ne changent, et l'objet reste
  mono-blob au sens d'ADR-0010. Les deux seuils se règlent sous `ostore.storage.drivers.<id>.properties`.

### 3. Copie native optionnelle dans la SPI
- `StorageDriver` gagne une méthode **par défaut** `boolean copy(BlobPath source, BlobPath target)`, qui
  renvoie `false` (« pas de copie native ») : un driver existant ou tiers n'a rien à changer.
- Le serveur l'appelle quand la source et la cible sont sur la **même instance** de driver ; si elle renvoie
  `false`, il revient à la copie en flux actuelle. Entre deux instances, la copie reste en flux.
- Le contenu ne change pas : l'ETag (MD5) et la taille de la source sont repris sans relecture.
- Implémentations fournies : FileSystem (`Files.copy` vers un fichier temporaire puis `ATOMIC_MOVE`, comme
  l'écriture) ; S3 (`CopyObject` jusqu'à 5 Gio, `UploadPartCopy` au-delà).
- Le kit de conformité vérifie que, si un driver annonce une copie native, le contenu copié est identique et
  la source intacte.

### 4. Ressources du driver S3
- Le téléchargement renvoie un flux qui **abandonne** la connexion (`abort()`) s'il est fermé avant la fin :
  un client qui coupe un téléchargement de 20 Go ne fait pas tirer le reste de l'objet.
- La taille du pool de connexions et les délais (acquisition, socket) sont des propriétés du driver.

## Conséquences
- Nouveau code d'erreur `OBJECT_TOO_LARGE` (413) dans le contrat.
- Évolution **compatible** de la SPI (méthode par défaut) : pas de rupture pour les drivers tiers.
- Tests : TI de dépôt refusé au-delà de la limite ; kit de conformité étendu à la copie native ; test du
  driver S3 au-delà du seuil de multipart, à mémoire bornée, sur Adobe S3Mock.
- Un objet copié nativement partage son contenu avec la source seulement le temps de la copie : chaque objet
  garde son propre blob, la purge est inchangée.
