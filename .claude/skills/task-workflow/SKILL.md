---
name: task-workflow
description: Démarrer, réaliser et clôturer une tâche OStore du backlog docs/TASKS.md en trunk-based development (vérifications, mise à jour doc, commit Conventional Commits, push). À utiliser dès qu'on commence ou termine une tâche Tn.m.
---

# Cycle d'une tâche OStore

Références : `docs/conventions/workflow.md`, ADR-0011 (tests), ADR-0012 (trunk-based).

## Démarrer
1. Lire la ligne de la tâche dans `docs/TASKS.md`. Si elle n'existe pas ou n'a pas de critère
   vérifiable, l'ajouter d'abord.
2. Passer le statut à 🟦.
3. Si la tâche implique un choix structurant, créer un ADR avec le skill `new-adr`.
4. Découper en **incréments intégrables** sur `main` : chacun compile, passe `verify` et laisse le
   produit utilisable. Fonctionnalité incomplète → feature flag `ostore.<domaine>.<fonction>.enabled`.

## Réaliser (par incrément)
- Code en anglais, Javadoc en français, `package-info.java` pour tout nouveau package, avec l'en-tête
  de licence écrit à la main.
- Tests selon ADR-0011 : un TI traversant lisible (skill `integration-test`), et un test unitaire
  seulement pour une règle pure et combinatoire.
- `./mvnw.cmd -B -ntp spotless:apply` puis `./mvnw.cmd -B -ntp verify` : vert **obligatoire** avant commit.

## Commit & push (à chaque incrément)
- Message Conventional Commits en anglais, dans un fichier (PowerShell ne transmet pas `-F -`) :
  ```
  <type>(<scope>): <description>

  <pourquoi>

  Refs: Tn.m

  Co-Authored-By: ...
  ```
- `git add` ciblé → `git commit -F <fichier>` → `git push`. Vérifier que l'index ne contient que
  l'incrément (`git status --short`).
- Vérifier que la CI GitHub est verte :
  `Invoke-RestMethod https://api.github.com/repos/doriangrelu/ostore/actions/runs?head_sha=<sha>`.
  Si `main` est rouge, corriger ou revert immédiatement.

## Clôturer
1. Cocher chaque critère de validation avec sa preuve (commande, nom du TI).
2. Statut : ✅, ou 🟨 si la tâche est marquée 🔒 (validation utilisateur requise).
3. Mettre à jour la doc touchée : ADR, conventions, `CLAUDE.md` (commandes, directives), PLAN.
4. Résumer à l'utilisateur : ce qui est fait, les preuves, ce qui reste ou est à valider.
