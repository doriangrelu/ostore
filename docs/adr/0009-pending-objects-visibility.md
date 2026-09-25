# ADR-0009 — Visibilité des objets en attente

- Statut : **Accepté** (choix utilisateur, 2026-09-25)

## Décision
Sémantique « read committed » :
- `GET` / `HEAD` / `ListObjects` par clé ne voient que les objets `ACTIVE`.
- Un objet `PENDING` est lisible **uniquement** :
  - par son `resourceId` (API REST `GET /api/v1/objects/{resourceId}`) ;
  - ou par clé **avec** l'en-tête `x-ostore-transaction-id` de sa transaction (API S3 et REST).
- Le micro-service validateur peut ainsi inspecter le fichier avant de valider la transaction.
- Si une version `ACTIVE` et une version `PENDING` coexistent pour une clé, la lecture sans
  en-tête renvoie la version `ACTIVE` ; avec l'en-tête, la version `PENDING`.
