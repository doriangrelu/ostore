# ADR-0012 — Trunk-based development

- Statut : **Accepté** (directive utilisateur, 2026-09-25)

## Décision
- Une seule branche de référence : **`main`**, toujours verte et publiable.
- Intégration **au moins quotidienne** : commits directs et petits sur `main`, ou branches de
  très courte durée (< 1 jour, quelques commits) fusionnées par PR. Pas de branche `develop`,
  `release/*` de longue durée ni de gitflow.
- **Chaque commit laisse `main` verte** : `./mvnw verify` passe en local avant le push.
  Un `main` rouge se corrige ou se revert immédiatement, avant toute autre tâche.
- **Fonctionnalités incomplètes masquées par des feature flags** de configuration
  (`ostore.<domaine>.<fonction>.enabled`, ex. `ostore.s3.multipart.enabled`), jamais par une branche longue.
- Découpage d'une tâche en incréments intégrables : migration → domaine → cas d'usage → API, chacun
  compilable et testé.
- Historique linéaire : *rebase* avant intégration, *squash* des branches courtes.
- Releases par **tag** sur `main` (`vX.Y.Z`, SemVer) ; un correctif de release se fait sur `main`
  puis se retague, sans branche de maintenance tant que ce n'est pas nécessaire.
- Les messages Conventional Commits permettront de générer le changelog.

## Recommandé côté GitHub (action du mainteneur)
Protection de `main` : statut `CI / Build, tests & quality` requis, historique linéaire, interdiction
du force-push.
