# ADR-0009 — Visibilité des objets en attente

- Statut : **Accepté** (choix utilisateur, 2026-09-25). Révisé le 2026-09-30 : les objets sont désignés par
  leur identifiant (ADR-0015), il n'y a plus d'accès par clé.

## Décision
Sémantique « read committed » :
- Les **listes** d'un bucket ne montrent que les objets validés (`ACTIVE`), sauf si la liste est filtrée sur
  une transaction.
- Un objet en attente (`PENDING`) reste accessible **par son identifiant** (`GET /api/v1/objects/{id}`,
  `/content`), avec son statut et l'identifiant de sa transaction dans la réponse : le micro-service
  validateur, qui reçoit ces identifiants, peut inspecter les fichiers avant de valider la transaction.
- Connaître l'identifiant vaut droit de lecture tant que l'authentification n'est pas en place (ADR-0008).

## Point ouvert (jalon M3)
Remplacer le contenu d'un objet **déjà validé** au sein d'une transaction : la version en attente doit
coexister avec la version active sous le même identifiant. Lecture par défaut = version active, version en
attente lisible avec l'en-tête `X-OStore-Transaction-Id`. À confirmer à l'ouverture de M3.
