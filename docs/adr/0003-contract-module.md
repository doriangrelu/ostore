# ADR-0003 — Module contrat : interfaces Spring MVC, source unique des clients et d'OpenAPI

- Statut : **Accepté** (directive utilisateur, 2026-09-25)

## Contexte
Des tiers doivent pouvoir construire leurs clients depuis le contrat, par exemple avec
Spring Cloud OpenFeign, OpenFeign seul, ou n'importe quel client **généré** depuis une
spécification.

Côté serveur, la directive est : **un contrôleur = `implements` de l'interface, rien d'autre**
(aucune annotation de mapping, de validation ou de documentation dupliquée).
La spécification OpenAPI est **générée depuis le code** du contrat, jamais écrite à la main.

## Décision
`ostore-contract` contient des interfaces annotées avec les annotations **Spring MVC**
(`@RequestMapping`, `@GetMapping`, `@PathVariable`, `@RequestParam`, `@RequestHeader`,
`@RequestBody`), qui sont le seul dénominateur commun :

| Consommateur | Support des interfaces Spring MVC |
|---|---|
| Serveur Spring MVC | Héritage des mappings et des annotations de paramètres (`@Valid` compris) depuis l'interface |
| Spring Cloud OpenFeign | Natif (`SpringMvcContract`) : `interface BucketClient extends BucketApi` + `@FeignClient` |
| OpenFeign seul | Module `feign-spring` (`SpringContract`) |
| springdoc-openapi | Lit les annotations de l'interface implémentée |
| Autres langages | OpenAPI 3.1 généré au build → `openapi-generator` |

Contenu du module :
1. **Interfaces de ressources** (`BucketApi`, `ObjectApi`, `TransactionApi`) : mapping HTTP,
   `@Valid`, et documentation `@Tag` / `@Operation` / `@ApiResponse`.
2. **DTO en `record`**, annotés Jakarta Validation et `@Schema`.
3. **Constantes** : chemins, en-têtes `x-ostore-*`, codes d'erreur (`ProblemDetail`, RFC 9457).

Côté serveur :
```java
/** Adaptateur HTTP des buckets : délègue aux cas d'usage. */
@RestController
class BucketController implements BucketApi { ... }
```
`@RestController` est la seule annotation (nécessaire à la détection du bean).

Génération OpenAPI : springdoc expose `/v3/api-docs`, généré depuis les interfaces du contrat
(vérifié par `OpenApiIT`). La publication du fichier comme artefact Maven (classifier `openapi`)
est prévue pour la release (jalon M6).

Erreurs : `ProblemDetail` (RFC 9457) avec une propriété `code` issue de l'énumération `ErrorCode`
du contrat, qui porte aussi le statut HTTP associé.

## Dépendances du module
`spring-web` (annotations uniquement), `jakarta.validation-api`, `swagger-annotations-jakarta`.
Jamais de dépendance vers les autres modules OStore.

## Alternative écartée
`@HttpExchange` (clients HTTP natifs de Spring Framework 7) : non supporté par Feign.
Un client `@HttpExchange` pourra être généré plus tard si besoin, sans changer le contrat.

## Responsabilités (précision utilisateur, 2026-09-25)
- **OStore garantit** : des interfaces aux annotations Spring MVC standard et une spec OpenAPI
  fidèle, générée depuis ces interfaces.
- **Le client est responsable** de la technologie d'appel (Feign, générateur, client HTTP…) et de
  ses éventuelles limites. Feign reste un exemple d'usage, pas un engagement : aucun test ni aucune
  adaptation du contrat pour un client particulier.

## Gros fichiers (exigence)
Le serveur doit accepter et servir des fichiers volumineux (plusieurs Go) **à mémoire bornée** :
- upload : corps brut `application/octet-stream` lu en flux (`InputStream` / `InputStreamResource`),
  jamais `byte[]` ni `MultipartFile` ;
- download : `ResponseEntity<Resource>` en flux, avec support `Range` ;
- aucune limite de taille implicite (multipart Spring désactivé sur ces routes, timeouts adaptés).
À valider en T1.5 par un test envoyant un fichier de plusieurs Go avec un tas JVM réduit.
