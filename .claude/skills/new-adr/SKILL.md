---
name: new-adr
description: Créer ou faire évoluer un Architecture Decision Record OStore (format MADR, en français) dans docs/adr et l'index. À utiliser pour tout choix structurant (technologie, découpage, protocole, convention transverse) ou quand l'utilisateur tranche une option.
---

# Créer un ADR

1. Numéro suivant : lire `docs/adr/README.md` et prendre le dernier numéro + 1, sur 4 chiffres.
2. Fichier `docs/adr/NNNN-<titre-kebab-en-anglais>.md` :
   ```markdown
   # ADR-NNNN — <Titre en français>

   - Statut : **Proposé** | **Accepté** (<origine>, AAAA-MM-JJ) | **Reporté (<jalon>)** | **Remplacé par ADR-XXXX**

   ## Contexte
   <problème, contraintes, forces en présence>

   ## Options            (si le choix n'est pas encore fait)
   | Option | + | − |

   ## Décision
   <ce qui est décidé, concret et vérifiable>

   ## Conséquences
   <impacts, travaux induits, risques>
   ```
3. Ajouter la ligne dans le tableau de `docs/adr/README.md` (numéro, titre, statut).
4. Si la décision est une directive, la reporter dans `CLAUDE.md` § Directives (skill `add-directive`).

## Règles
- Un ADR n'est **jamais supprimé**. Une décision remplacée passe « Remplacé par ADR-XXXX », et le
  nouvel ADR référence l'ancien.
- Une révision mineure demandée par l'utilisateur se note dans le statut :
  « révisé le AAAA-MM-JJ : <motif> ».
- Statut « Accepté » **seulement** après accord explicite de l'utilisateur ou s'il s'agit de sa
  directive. Sinon « Proposé », avec une recommandation claire.
- Les dates sont absolues (AAAA-MM-JJ).
