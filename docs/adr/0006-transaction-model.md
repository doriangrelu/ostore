# ADR-0006 — Modèle de transaction, versions physiques et purge

- Statut : **Accepté** (validé par l'utilisateur, 2026-09-25)

## Décision
1. **Version physique par écriture** : chaque `PUT` crée une ligne `OST_OBJECT` et un blob sous une
   clé interne UUID v7. La clé utilisateur n'est jamais utilisée comme chemin physique
   (pas de collision, pas d'injection de chemin, remplacement atomique).
2. **Commit** : en une transaction SQL, les objets `PENDING` passent `ACTIVE`, les anciens `ACTIVE`
   de même clé passent `SUPERSEDED` et sont enfilés dans `OST_BLOB_PURGE`.
3. **Rollback / expiration** : objets `PENDING` supprimés + blobs enfilés dans `OST_BLOB_PURGE`.
4. **Jobs** (virtual threads, planification configurable) :
   - `TransactionExpirationJob` : `OPEN` avec `EXPIRES_AT < now` → `EXPIRED` ;
   - `BlobPurgeJob` : suppression physique idempotente, retry avec backoff exponentiel.
   Les deux prennent des lots via `FOR UPDATE SKIP LOCKED` → plusieurs instances sans coordination.
5. **Idempotence** : `commit` d'une transaction déjà `COMMITTED` → succès (200) ; `commit` d'une
   transaction `EXPIRED`/`ROLLED_BACK` → `409 Conflict`.
6. **TTL** : défaut et maximum configurables (`ostore.transactions.default-ttl`, `max-ttl`) ;
   **prolongation** possible tant que `OPEN` : `POST /api/v1/transactions/{id}/extend` avec un
   nouveau TTL ; la nouvelle échéance est bornée par `max-ttl` calculé depuis la création
   (`409` sinon), et une transaction déjà expirée ne peut pas être prolongée (`409`).
