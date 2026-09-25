# ADR-0008 — Authentification de l'API

- Statut : **Reporté au jalon M5** (choix utilisateur, 2026-09-25). Révisé par ADR-0014 : il n'y a plus
  d'API S3, donc plus de SigV4 ; seule l'API JSON est à protéger.

## Options
1. **OAuth2 Resource Server (JWT)** via Spring Security (recommandé) : c'est le standard entre
   micro-services, compatible avec les fournisseurs d'identité usuels (Keycloak, Entra ID…), et
   chaque appelant peut recevoir des droits distincts (par exemple dépôt d'un côté, validation des
   transactions de l'autre).
2. **Clés d'API statiques** (en-tête dédié) : simple, adapté aux petits déploiements, mais sans
   rotation ni délégation.
3. **mTLS** : robuste en réseau interne, mais lourd à exploiter.
4. Aucune authentification applicative : uniquement derrière une passerelle qui authentifie.

## Décision
**Reportée au jalon M5.** D'ici là, l'API n'est pas destinée à être exposée hors d'un environnement de
développement.
