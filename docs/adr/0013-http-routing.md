# ADR-0013 — Routage HTTP : API S3 sous `/s3`, API REST sous `/api/v1`

- Statut : **Remplacé par ADR-0014** (2026-09-25) : l'API S3 est abandonnée, il n'y a plus qu'une API JSON sous `/api/v1`.

## Contexte
L'API S3 en adressage *path-style* place le nom du bucket en premier segment (`/{bucket}/{key}`).
Servie à la racine, elle entrerait en collision avec l'API REST : un bucket nommé `api` masquerait
`/api/v1/...`, et inversement.

## Décision
- API compatible S3 sous un préfixe configurable, **`ostore.s3.base-path`** (défaut `/s3`).
  Les SDK AWS et `aws cli` le prennent en compte via l'endpoint (`--endpoint-url http://hôte:port/s3`,
  `endpointOverride(...)`, `forcePathStyle(true)`). Comportement vérifié par les TI avec le SDK AWS v2.
- API REST sous **`/api/v1`** (constante `ApiPaths.API_V1` du contrat) ; seule l'API REST figure dans
  OpenAPI (`springdoc.paths-to-match=/api/**`).
- Adressage *virtual-hosted* (`bucket.hôte`) : non supporté en v1.
- Réponses S3 en XML produites par l'adaptateur S3 lui-même (et non par un convertisseur HTTP global) :
  l'API REST reste en JSON quelle que soit la négociation de contenu.

## Conséquences
- Un déploiement derrière un reverse proxy peut exposer l'API S3 à la racine d'un nom d'hôte dédié
  (`s3.exemple.com` → `/s3`).
- Aucun nom de bucket n'est réservé.
