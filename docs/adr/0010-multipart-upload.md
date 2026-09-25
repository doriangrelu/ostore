# ADR-0010 — Multipart upload S3 en v1

- Statut : **Accepté** (validé par l'utilisateur le 2026-09-25 : multipart en v1, **non systématique**)

## Décision
- Le multipart est une **option à l'initiative du client** (opérations S3 multipart), jamais un
  passage obligé : un `PutObject` simple reste un objet **mono-blob**, sans table de parties ni
  lecture composite. Seuls les objets créés par `CompleteMultipartUpload` sont composites.
- Le support est activable/désactivable par configuration (`ostore.s3.multipart.enabled`, `true`
  par défaut) ; désactivé, les opérations multipart répondent `501 NotImplemented` comme S3.
- Livraison dans le jalon **M4**, selon la conception ci-dessous.

## Contexte : pourquoi la question se pose
- `aws cli` bascule en multipart **dès 8 Mo** ; les SDK AWS (Transfer Manager, S3 CRT) au-delà
  d'un seuil comparable.
- Un `PutObject` S3 simple est limité à **5 Go**.
- Sans multipart, les gros fichiers (exigence forte, ADR-0003) ne passeraient donc pas par l'API
  S3 avec les outils standards.

## Périmètre S3 à couvrir

| Opération | Requête | Nécessaire v1 |
|---|---|---|
| CreateMultipartUpload | `POST /{bucket}/{key}?uploads` | Oui |
| UploadPart | `PUT /{bucket}/{key}?partNumber=N&uploadId=U` | Oui |
| CompleteMultipartUpload | `POST /{bucket}/{key}?uploadId=U` (corps XML : parties + ETags) | Oui |
| AbortMultipartUpload | `DELETE /{bucket}/{key}?uploadId=U` | Oui |
| ListParts | `GET /{bucket}/{key}?uploadId=U` | Oui (reprise d'upload par les SDK) |
| ListMultipartUploads | `GET /{bucket}?uploads` | Non (v1.1) |
| UploadPartCopy | `PUT` + `x-amz-copy-source` | Non (v1.1) |

Règles S3 : parties numérotées de 1 à 10 000, **5 Mo minimum** sauf la dernière, 5 Go maximum
par partie. ETag final = `md5(concaténation des md5 binaires des parties)-N`.

## Conception proposée : objet composite, sans recopie
1. `CreateMultipartUpload` crée un objet `PENDING` et un **upload** rattaché à une transaction :
   explicite (`x-ostore-transaction-id`) ou implicite (TTL configurable, défaut 24 h).
   Un upload abandonné expire donc **exactement comme une transaction**. Aucun nouveau mécanisme
   de nettoyage n'est nécessaire.
2. `UploadPart` écrit chaque partie comme un **blob ordinaire** via la SPI existante
   (`StorageDriver.write`), et l'enregistre dans une table `OST_OBJECT_PART` (numéro, taille, md5,
   clé de stockage). Renvoyer une partie avec le même numéro remplace l'ancienne, qui part en purge.
3. `CompleteMultipartUpload` vérifie la liste (ordre, ETags, 5 Mo minimum) puis marque l'objet
   comme **composite** : il n'y a **aucune concaténation physique**.
4. Lecture : `GET` enchaîne les flux des parties dans l'ordre (`SequenceInputStream` paresseux).
   Un `Range` est résolu par les tailles cumulées des parties, puis seules les parties utiles sont lues.
5. Abort, rollback ou expiration : les parties partent dans `OST_BLOB_PURGE`, comme n'importe quel blob.

**La SPI des drivers ne change pas.** Aucun driver n'a à connaître le multipart. Le driver S3
pourra plus tard exploiter le multipart natif de S3 comme optimisation, sans impact sur le contrat.

## Estimation de difficulté
**Modérée.** L'essentiel réutilise des briques déjà prévues (transactions, purge, streaming) :

| Travail | Effort |
|---|---|
| 5 endpoints S3 + parsing/sérialisation XML | Moyen |
| Table `OST_OBJECT_PART` (PG + Oracle, index, FK) | Faible |
| Lecture composite et résolution des `Range` | Moyen, à tester soigneusement |
| Calcul de l'ETag multipart, contrôles de taille | Faible |
| Checksums SDK (CRC32/CRC64NVME en trailer `aws-chunked`) | Déjà nécessaire pour `PutObject` |
| Tests avec `aws cli` et SDK v2 sur fichiers de plusieurs Go | Moyen |

Ordre de grandeur : comparable à l'ensemble `PutObject` + `GetObject`. Aucun point bloquant identifié.

## Impact sur le plan
Jalon **M4** juste après l'API S3 de base. Dès M1, le domaine distingue les deux formes de
contenu d'un objet (sealed) : `SingleBlobContent` (cas par défaut) et `CompositeContent` (parties).
