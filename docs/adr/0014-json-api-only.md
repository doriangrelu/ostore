# ADR-0014 — Une seule API, en JSON : abandon de la compatibilité protocolaire S3

- Statut : **Accepté** (directive utilisateur, 2026-09-25). Remplace ADR-0013 et révise ADR-0008,
  ADR-0010.

## Contexte
Le plan initial exposait deux API : une API REST (JSON) et une API compatible S3, en XML, pour être
utilisable avec les SDK AWS, `aws cli` ou rclone. Le protocole S3 impose une dette qui vient d'AWS :

- documents XML et codes d'erreur AWS ;
- encodage `aws-chunked` et checksums (CRC32, CRC64NVME) en trailer ;
- signature SigV4 ;
- idiosyncrasies des SDK, qui changent au fil des versions.

L'utilisateur refuse cette dette.

## Décision
- OStore expose **une seule API HTTP, en JSON**, entièrement définie par le module `ostore-contract`
  (interfaces Spring MVC + DTO, ADR-0003), sous **`/api/v1`**.
- **Aucune compatibilité protocolaire S3** : ni XML, ni SigV4, ni `aws-chunked`, ni codes d'erreur AWS.
- Les **sémantiques inspirées de S3** restent, car elles sont éprouvées :
  - buckets et clés d'objet (jusqu'à 1024 octets UTF-8) ;
  - ETag, lecture partielle `Range` (RFC 9110) ;
  - métadonnées utilisateur ;
  - upload multipart (ADR-0010).
- Le contenu binaire transite en `application/octet-stream`, en flux. Les métadonnées de la requête
  et les identifiants de réponse passent par des en-têtes `x-ostore-*` ou par un corps JSON, selon
  l'opération (détaillé au jalon M2).
- Le **driver de stockage S3** est conservé : OStore peut *stocker* ses blobs dans un S3 (AWS, MinIO…).
  C'est un backend, pas un protocole exposé.

## Conséquences
- Les clients AWS (SDK, `aws cli`, rclone, outils de sauvegarde) ne fonctionnent pas avec OStore.
  Les clients s'appuient sur le contrat (Spring Cloud OpenFeign, OpenFeign, générateurs OpenAPI).
- Le package `api.s3`, la dépendance XML et les tests avec le SDK AWS sont supprimés. Il n'y a plus de
  collision de routes : ADR-0013 est remplacé.
- Le principal risque technique du plan (aws-chunked, SigV4) disparaît.
- Réversibilité : grâce à l'architecture hexagonale, un adaptateur S3 optionnel pourrait être ajouté
  plus tard (nouveau package `api.s3` branché sur les mêmes cas d'usage), sans toucher au cœur.
