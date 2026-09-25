# ADR-0011 — Stratégie de tests : peu de tests, lisibles, traversants d'abord

- Statut : **Accepté** (directive utilisateur, 2026-09-25)

## Contexte
Une batterie de centaines de tests unitaires finement couplés à l'implémentation coûte cher à
maintenir, se lit mal et freine le refactoring. L'utilisateur préfère un test d'intégration
traversant bien écrit à une série de tests illisibles.

## Décision
**Pyramide inversée maîtrisée : les tests d'intégration traversants (TI) sont la base.**

| Niveau | Quand | Forme |
|---|---|---|
| **TI traversant** (`*IT`, failsafe) | Par défaut, pour chaque scénario métier | Application démarrée, vraie base (Testcontainers PostgreSQL/Oracle), vrai driver, appelée par HTTP comme un client réel (client construit sur le contrat) |
| **Test de conformité** | Chaque driver de stockage | Kit commun de la SPI (ADR-0007), un seul jeu de tests pour tous les drivers |
| **Test de migration** | Chaque SGBD | Migrations appliquées sur PostgreSQL et Oracle réels |
| **Test unitaire** | **Seulement** pour une règle pure, dense et combinatoire | Ex. : validation des noms de bucket, machine à états des transactions, résolution de `Range`, ETag multipart |
| **Test d'architecture** | Permanent | ArchUnit |

Règles d'écriture :
- Un TI = **un scénario lisible de bout en bout**, écrit comme une histoire
  (*given / when / then* visibles), par exemple : dépôt en transaction → invisible → commit → visible.
- Le scénario prime sur l'exhaustivité : plusieurs assertions sur un même parcours plutôt qu'un test
  par assertion.
- Tests paramétrés (`@ParameterizedTest`) plutôt que des copies de tests.
- Pas de mock de nos propres classes. Doublures acceptées uniquement aux frontières externes
  impossibles à conteneuriser.
- Un helper de test (DSL de scénario) est préférable à de la duplication, s'il reste plus simple
  que ce qu'il cache.
- Pas d'objectif de nombre de tests. La couverture JaCoCo **fusionne unitaires et TI** et sert
  d'indicateur, pas de but.

## Conséquences
- Testcontainers est requis en local (Docker) et en CI.
- Les TI doivent rester rapides : conteneurs partagés entre classes (singleton containers), contexte
  Spring réutilisé, données isolées par bucket unique par test plutôt que par nettoyage de base.
