# ADR-0008 — Authentification de l'API S3

- Statut : **Reporté au jalon M5** (hors périmètre fonctionnel v1, mais les SDK AWS signent toujours leurs requêtes)

## Options
1. **Vérification SigV4 avec clés statiques** en configuration (recommandé pour v1) : sécurité réelle,
   compatible avec tous les SDK, effort modéré.
2. Aucune vérification (signature ignorée) : uniquement derrière une passerelle qui authentifie.
3. Délégation OAuth2 / JWT pour l'API REST, SigV4 pour l'API S3 (cible v2).

**Décision reportée au jalon M5** (choix utilisateur, 2026-09-25). D'ici là, l'API S3 n'est pas
destinée à être exposée hors d'un environnement de développement.
