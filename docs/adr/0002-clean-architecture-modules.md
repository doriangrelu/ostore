# ADR-0002 — Clean architecture et découpage en modules

- Statut : **Accepté** (révisé le 2026-09-25 sur directive utilisateur : éviter le sur-découpage)

## Contexte
Une première version découpait chaque couche en module Maven (12 modules). L'utilisateur l'a jugée
sur-découpée : l'interne du backend n'a pas vocation à être publié ni remplacé séparément.

## Décision
Un module Maven n'existe que s'il est **publié ou remplaçable séparément** :

| Module | Raison d'exister |
|---|---|
| `ostore-contract` | Publié seul pour générer des clients (ADR-0003) |
| `ostore-drivers` → `ostore-driver-spi`, `ostore-driver-filesystem`, `ostore-driver-s3` | SPI publiée pour les drivers tiers ; chaque driver est un artefact indépendant (ADR-0007) |
| `ostore-server` | Tout le backend, structuré en packages |

Couches de `ostore-server` (packages sous `io.github.doriangrelu.ostore`) :

| Package | Rôle | Peut dépendre de |
|---|---|---|
| `domain` | Modèle métier | JDK, JSpecify |
| `application` (`port.in`, `port.out`) | Cas d'usage et ports | `domain`, `driver.spi`, JDK |
| `api.rest`, `api.s3` | Adaptateurs entrants | `application`, `domain`, contrat, Spring |
| `infrastructure.*` | Adaptateurs sortants, configuration | `application`, `domain`, `driver.spi`, Spring |

- `api` et `infrastructure` ne se connaissent pas et ne sont appelés par personne.
- Aucune annotation Spring dans `domain` / `application` : le câblage des cas d'usage se fait par
  des classes `@Configuration` dans `infrastructure`.
- Le contrat n'est utilisé que par `api.rest`.

## Garanties
- Les couches n'étant que des packages, **ArchUnit** (`ArchitectureTest`) est le garde-fou :
  toute violation fait échouer `./mvnw verify` (vérifié par une violation volontaire).
- Les drivers fournis sont en scope `runtime` pour le serveur : le compilateur interdit de les
  utiliser directement.

## Alternatives écartées
- Un module par couche : sur-découpage, coût de build et de lecture sans bénéfice de publication.
- Spring Modulith : pertinent pour des modules métier, moins pour des couches techniques.
