# Intégration continue et qualimétrie

## Pipelines GitHub Actions

| Workflow | Déclencheur | Contenu |
|---|---|---|
| [ci.yml](../../.github/workflows/ci.yml) — job `build` | push `main`, PR | `./mvnw verify` (format, licence, Enforcer, ArchUnit, tests, JaCoCo), rapport de tests, commentaire de couverture sur PR, analyse SonarCloud, archivage des rapports |
| [ci.yml](../../.github/workflows/ci.yml) — job `dependency-review` | PR | Refuse les dépendances ajoutées avec vulnérabilité ≥ *high* ou licence incompatible Apache 2.0 (GPL/AGPL) |
| [codeql.yml](../../.github/workflows/codeql.yml) | push `main`, PR, hebdomadaire | Analyse de sécurité statique CodeQL (`security-and-quality`) |
| [dependabot.yml](../../.github/dependabot.yml) | hebdomadaire | PR de mise à jour Maven et Actions, groupées, préfixes Conventional Commits |

## Bonnes pratiques appliquées
- **Actions épinglées par SHA** (commentaire `# vX.Y.Z`) : protection contre la réécriture d'un tag ;
  Dependabot maintient les SHA à jour.
- **Permissions minimales** : `contents: read` par défaut, élargies job par job.
- **Même commande en local et en CI** : `./mvnw -B -ntp verify`. La CI ne contient aucune règle
  qualité propre : tout est dans le POM.
- Annulation des exécutions obsolètes d'une même branche (`concurrency`).

## Activer SonarCloud (action manuelle du mainteneur)
1. Se connecter à <https://sonarcloud.io> avec GitHub et importer le dépôt `doriangrelu/ostore`
   (organisation `doriangrelu`, clé projet `doriangrelu_ostore` — sinon ajuster les propriétés
   `sonar.*` du POM parent).
2. Dans SonarCloud, **désactiver l'« Automatic Analysis »** (incompatible avec l'analyse CI).
3. Générer un jeton et l'ajouter au dépôt GitHub : *Settings → Secrets and variables → Actions →
   New repository secret* `SONAR_TOKEN`.
4. Le job `build` lance alors l'analyse à chaque push et PR (couverture JaCoCo incluse).

Sans ce secret, l'étape Sonar est simplement ignorée : la CI reste verte.

## Seuils
La couverture **fusionne tests unitaires et TI** (ADR-0011). C'est un indicateur, pas un objectif :
un seuil global modéré et la Quality Gate Sonar seront fixés au jalon M1, une fois les premiers TI
traversants en place.
