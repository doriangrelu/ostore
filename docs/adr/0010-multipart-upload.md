# ADR-0010 — Upload multipart en v1

- Statut : **Accepté** (validé par l'utilisateur le 2026-09-25 : multipart en v1, **non systématique**).
  Révisé par ADR-0014 : les opérations sont des endpoints JSON du contrat, et non plus le protocole S3.

## Contexte
Les gros fichiers (plusieurs Go) sont une exigence forte (ADR-0003). Un envoi en une seule requête
reste possible et streamé, mais il ne peut pas être repris après une coupure ni parallélisé.
Le multipart découpe l'envoi en parties indépendantes, qu'on peut reprendre et paralléliser.

## Décision
- Le multipart est une **option à l'initiative du client**, jamais un passage obligé. Un envoi
  simple reste un objet **mono-blob**, sans table de parties ni lecture composite. Seuls les objets
  finalisés par multipart sont composites.
- Activable par configuration : `ostore.multipart.enabled` (`true` par défaut). Désactivé, les
  endpoints répondent `501` avec le code `MULTIPART_DISABLED`.
- Livraison au jalon **M4**.

## Opérations (API JSON, sous `/api/v1/buckets/{bucket}`)

| Opération | Requête | Réponse |
|---|---|---|
| Démarrer | `POST /uploads` — JSON : clé, type de contenu, métadonnées, transaction éventuelle | `uploadId`, `resourceId`, `transactionId` |
| Envoyer une partie | `PUT /uploads/{uploadId}/parts/{partNumber}` — corps binaire en flux | numéro, taille, ETag de la partie |
| Lister les parties | `GET /uploads/{uploadId}/parts` | parties reçues (pour reprendre l'envoi) |
| Finaliser | `POST /uploads/{uploadId}/complete` — JSON : liste ordonnée (numéro, ETag) | objet créé (id, clé, ETag, transaction) |
| Abandonner | `DELETE /uploads/{uploadId}` | `204` |

Règles (configurables) : parties numérotées de 1 à 10 000, **5 Mio minimum** sauf la dernière.
ETag final = `md5(concaténation des md5 binaires des parties)-N`, convention reprise de S3 car
éprouvée.

## Conception : objet composite, sans recopie
1. Démarrer crée un objet `PENDING` et un **upload** rattaché à une transaction : explicite, ou
   implicite avec un TTL configurable (24 h par défaut). Un upload abandonné expire donc **exactement
   comme une transaction**. Aucun nouveau mécanisme de nettoyage n'est nécessaire.
2. Chaque partie est écrite comme un **blob ordinaire** via la SPI (`StorageDriver.write`) et
   enregistrée dans `OST_OBJECT_PART` (numéro, taille, md5, clé de stockage). Renvoyer un numéro
   remplace la partie, et l'ancienne part en purge.
3. Finaliser vérifie la liste (ordre, ETags, tailles) puis marque l'objet **composite** : il n'y a
   **aucune concaténation physique**.
4. Lecture : les flux des parties sont enchaînés dans l'ordre (paresseusement). Un `Range` est résolu
   par les tailles cumulées, et seules les parties utiles sont lues.
5. Abandon, rollback ou expiration : les parties partent dans `OST_BLOB_PURGE`.

**La SPI des drivers ne change pas.** Le driver S3 pourra plus tard exploiter le multipart natif de
son backend comme optimisation, sans impact sur le contrat.

## Impact sur le plan
Dès le jalon M2, le domaine distingue les deux formes de contenu d'un objet (sealed) :
`SingleBlobContent` (par défaut) et `CompositeContent` (parties).
