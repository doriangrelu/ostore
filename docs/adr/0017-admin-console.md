# ADR-0017 — Console d'administration

- Statut : **Proposé** (choix utilisateur du 2026-09-30 : Vue 3 + Vite, consultation et actions, console avant
  la sécurité). Le design détaillé attend la validation de T4.1.

## Contexte
Les exploitants veulent consulter et administrer les buckets, les objets et les transactions sans écrire
d'appels HTTP. OStore n'expose qu'une API JSON (ADR-0014) et n'est pas authentifié avant le jalon sécurité
(ADR-0008).

## Décision

### 1. La console n'est qu'un client de l'API publique
- Une SPA **Vue 3 + Vite + TypeScript** qui n'appelle **que** l'API JSON du contrat (`/api/v1`).
  Pas d'API interne, pas de rendu serveur : tout ce que fait la console, un client peut le faire.
- Client TypeScript **généré** depuis la spécification OpenAPI (`openapi-typescript` + `openapi-fetch` :
  types seuls, sans runtime lourd). La spécification reste générée depuis le contrat (ADR-0003) : elle est
  versionnée dans `ostore-console` et un TI échoue si elle ne correspond plus au contrat.
- Bibliothèque de composants : **PrimeVue** (tables paginées, formulaires, boîtes de dialogue) ;
  `vue-router` en mode *history*. Pas de store global tant qu'il n'est pas nécessaire.

### 2. Un module dédié, retirable (ADR-0002)
- Module `ostore-console` : les sources dans `src/main/frontend`, compilées par Maven via
  `frontend-maven-plugin`. Ce plugin installe une version de Node **locale au projet** : le wrapper Maven suffit,
  sans Node installé sur le poste ni en CI.
- Le jar ne contient que les ressources statiques (`classpath:/ostore-console/`) et une auto-configuration
  Spring de quelques lignes : pas de logique métier.
- `ostore-server` dépend du module ; le retirer supprime la console.

### 3. Désactivable
- `ostore.console.enabled` (défaut **`false`**) : désactivée, `/console/**` répond `404`. Tant que
  l'API n'est pas authentifiée, l'activer n'est acceptable qu'en développement ou sur un réseau de confiance.
- Servie sous `/console`, même origine que l'API (pas de CORS). Toute route inconnue sous `/console/**` renvoie
  `index.html` (routes de la SPA).
- Développement : `npm run dev` (Vite) relaie `/api` vers l'application locale.

### 4. Périmètre v1

| Écran | Contenu |
|---|---|
| Tableau de bord | Nombre de buckets, transactions ouvertes, transactions proches de l'échéance |
| Buckets | Liste, création, suppression ; volume et nombre d'objets |
| Objets d'un bucket | Liste paginée filtrable par nom, détail (métadonnées, statut), téléchargement, dépôt, remplacement, copie, suppression |
| Transactions | Liste paginée filtrable par statut et par référence ; détail avec ses objets ; commit, rollback, prolongation |

Toute action irréversible (suppression, commit, rollback) passe par une confirmation.

### 5. Compléments du contrat
Ils servent à tous les clients, pas seulement à la console :
- `GET /api/v1/transactions?status=&reference=&limit=&continuationToken=` : liste paginée par identifiant.
  Migration `V1_4_0` : index sur la référence client.
- `GET /api/v1/buckets/{name}` renvoie `objectCount` et `totalSize` des objets actifs. Ils sont calculés à la
  demande (`COUNT` / `SUM`) et absents de la liste des buckets pour qu'elle reste peu coûteuse.

### 6. Tests (ADR-0011)
- Un TI Java traversant : console servie quand elle est activée, `404` sinon, repli sur `index.html` ;
  spécification OpenAPI à jour.
- Côté front : `vue-tsc` (typage contre le client généré) et lint dans le build ; tests unitaires Vitest
  réservés aux règles pures (formatage des tailles, des durées…).
- Tests navigateur (Playwright) : hors v1, à rediscuter si la console grossit.

## Conséquences
- Le build requiert le téléchargement de Node (mis en cache en CI).
- Nouveau point de risque tant qu'il n'y a pas d'authentification : la console est désactivée par défaut et la
  documentation le rappelle. Le jalon sécurité couvrira l'API **et** la console.

## Options écartées
- **Thymeleaf + htmx** : pas de Node, mais la console appellerait le backend en direct au lieu de consommer
  l'API publique.
- **Angular / React** : équivalents ; Vue 3 est retenu par l'utilisateur pour sa légèreté.
- **Console dans `ostore-server`** : impossible à retirer séparément, et le build Node s'imposerait au backend.
