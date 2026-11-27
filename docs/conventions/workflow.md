# Méthode de travail

## Cycle d'une tâche
1. **Cadrer** : la tâche existe dans [TASKS.md](../TASKS.md) avec des critères de validation
   vérifiables. Sinon, on la crée d'abord.
2. **Démarrer** : statut 🟦. Si la tâche implique un choix structurant → ADR « Proposé ».
3. **Réaliser** par petits incréments, tests écrits avec le code.
4. **Vérifier** : `./mvnw verify` (et tests d'intégration concernés) ; chaque critère coché
   explicitement, avec la preuve (commande / test).
5. **Clore** : 🟨 si la tâche est 🔒 (attente validation utilisateur), sinon ✅.
   Mise à jour de la doc touchée (ADR, conventions, CLAUDE.md, README).
6. **Commit** : un commit par tâche (ou sous-tâche cohérente), selon la convention ci-dessous.

## Superpowers

Le cycle ci-dessus est conduit avec les skills Superpowers, qui ne remplacent ni les directives de `CLAUDE.md`
ni les skills projet (`task-workflow`, `integration-test`, `db-migration`…) ; en cas de conflit, le projet prime.

| Étape du cycle | Skill Superpowers | Précision OStore |
|---|---|---|
| Cadrer | `brainstorming`, puis `writing-plans` | Options à trancher soumises à l'utilisateur ; choix structurant → ADR (`new-adr`) |
| Réaliser | `test-driven-development` | Le test qui échoue d'abord est un TI traversant (ADR-0011) ; test unitaire seulement pour une règle pure |
| Défaut, test rouge | `systematic-debugging` | Cause racine avant tout correctif |
| Étapes indépendantes | `subagent-driven-development`, `dispatching-parallel-agents` | Deux ou trois tâches lourdes en parallèle au plus |
| Isoler le travail | `using-git-worktrees` | Branche de moins d'un jour, intégrée sur `main` (ADR-0012) |
| Avant de conclure | `requesting-code-review`, puis `verification-before-completion` | `./mvnw verify` vert, sortie à l'appui |
| Intégrer | `finishing-a-development-branch` | Commit Conventional Commits et push sur `main` (`task-workflow`) |

## Trunk-based development (ADR-0012)
- On intègre sur `main` par **petits incréments** (commit direct ou branche de moins d'un jour).
- Avant chaque push : `./mvnw verify` vert en local. Si `main` casse, on corrige ou on revert
  **immédiatement**.
- Une fonctionnalité inachevée est livrée **désactivée** derrière un feature flag
  (`ostore.<domaine>.<fonction>.enabled`), jamais gardée sur une branche.
- Une tâche se découpe en incréments intégrables (migration → domaine → cas d'usage → API).

## Convention de commit
[Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/), messages en anglais :

```
<type>(<scope>): <description impérative, minuscule, sans point final>

<corps optionnel : le pourquoi>

Refs: T0.4
BREAKING CHANGE: <description>   # si rupture de compatibilité (ou « ! » après le type)
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `docs` | Documentation seule (ADR, conventions, Javadoc) |
| `build` | Maven, dépendances, wrapper |
| `ci` | Pipelines d'intégration continue |
| `test` | Ajout ou correction de tests |
| `refactor` | Restructuration sans changement de comportement |
| `perf` · `style` · `chore` | Performance · format · maintenance diverse |

Scopes : `contract`, `driver-spi`, `driver-fs`, `driver-s3`, `domain`, `application`, `api-rest`,
`api-s3`, `persistence`, `migration`, `deps`. Une rupture du contrat public ou de la SPI est
**toujours** marquée `!` / `BREAKING CHANGE`.

## Directives utilisateur
Toute directive (« c'est Maven pas Gradle ») est immédiatement :
- reportée dans `CLAUDE.md` § Directives ;
- répercutée dans l'ADR / la convention concernée (ou un nouvel ADR) ;
- appliquée aux tâches en cours.

## Définition de « Terminé »
- Build et tests verts, couverture non régressée.
- Javadoc française sur toute API publique (contrat, SPI, cas d'usage).
- Aucune violation ArchUnit / Spotless / Enforcer.
- Documentation à jour.
