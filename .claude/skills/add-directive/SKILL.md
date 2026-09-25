---
name: add-directive
description: Reporter immédiatement une directive ou une préférence de l'utilisateur (technique, méthode, convention) dans CLAUDE.md et la documentation OStore concernée. À utiliser dès que l'utilisateur donne une consigne durable (« on utilise X », « évite Y », « c'est Maven pas Gradle »).
---

# Reporter une directive utilisateur

1. **Reformuler** la directive en une règle vérifiable. En cas d'ambiguïté réelle, poser la question.
   Sinon, retenir l'interprétation la plus raisonnable et l'annoncer.
2. **`CLAUDE.md` § Directives** : ajouter ou modifier la directive numérotée, en gardant une
   numérotation continue et dans l'ordre. Rester concis et renvoyer vers l'ADR ou la convention.
3. **Documentation détaillée** :
   - choix structurant → ADR (skill `new-adr`), statut « Accepté (directive utilisateur, date) » ;
   - bonne pratique → `docs/conventions/<code|database|workflow|ci>.md` ;
   - impact sur le périmètre ou la roadmap → `docs/PLAN.md` et `docs/TASKS.md`.
4. **Cohérence** : rechercher (Grep) les anciennes formulations contradictoires dans `docs/`,
   `CLAUDE.md`, `README.md` et les corriger toutes.
5. **Appliquer** la directive au travail en cours, ou planifier la mise en conformité dans le backlog.
6. Commit dédié : `docs: <résumé de la directive>`.
