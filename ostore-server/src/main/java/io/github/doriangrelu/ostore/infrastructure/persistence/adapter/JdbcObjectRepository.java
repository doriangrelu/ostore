/*
 * Copyright 2026 the OStore contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.doriangrelu.ostore.infrastructure.persistence.adapter;

import io.github.doriangrelu.ostore.application.port.out.BlobPurgeQueue;
import io.github.doriangrelu.ostore.application.port.out.ObjectRepository;
import io.github.doriangrelu.ostore.domain.exception.ConcurrentObjectUpdateException;
import io.github.doriangrelu.ostore.domain.exception.ObjectLockedException;
import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectStatus;
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.StoredObjectEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.ObjectSummaryRow;
import io.github.doriangrelu.ostore.infrastructure.persistence.repository.StoredObjectEntityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptateur du port {@link ObjectRepository} sur Spring Data JDBC. Les opérations en plusieurs étapes sont
 * transactionnelles et rejoignent l'unité de travail appelante s'il y en a une.
 */
@Repository
public class JdbcObjectRepository implements ObjectRepository {

    private static final String PENDING = ObjectStatus.PENDING.name();

    private final StoredObjectEntityRepository entities;
    private final BlobPurgeQueue purgeQueue;

    public JdbcObjectRepository(StoredObjectEntityRepository entities, BlobPurgeQueue purgeQueue) {
        this.entities = entities;
        this.purgeQueue = purgeQueue;
    }

    /**
     * Un second remplacement en attente du même objet viole l'unicité de {@code REPLACES_OBJECT_ID} : filet de
     * sécurité derrière le verrou de l'objet, traduit en conflit métier plutôt qu'en erreur serveur.
     */
    @Override
    public StoredObject insert(StoredObject object) {
        try {
            return entities.save(StoredObjectEntity.newRow(object)).toDomain();
        } catch (DuplicateKeyException | DbActionExecutionException e) {
            if (object.replaces() != null && isDuplicateKey(e)) {
                throw new ObjectLockedException(object.publicId());
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredObject> lock(UUID id) {
        return entities.findLockedById(id).map(StoredObjectEntity::toDomain);
    }

    @Override
    @Transactional
    public StoredObject replace(StoredObject updated) {
        var current = entities.findById(updated.id())
                .orElseThrow(() -> new ConcurrentObjectUpdateException(updated.publicId()));
        try {
            var saved = entities.save(current.updatedWith(updated));
            purgeQueue.schedule(current.blobLocation());
            return saved.toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentObjectUpdateException(updated.publicId());
        } catch (DbActionExecutionException e) {
            if (e.getCause() instanceof OptimisticLockingFailureException) {
                throw new ConcurrentObjectUpdateException(updated.publicId());
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredObject> find(UUID id) {
        return entities.findById(id).map(StoredObjectEntity::toDomain);
    }

    @Override
    public Optional<StoredObject> findPendingReplacement(UUID objectId) {
        return entities.findByReplacesObjectId(objectId).map(StoredObjectEntity::toDomain);
    }

    @Override
    public List<ObjectSummary> list(UUID bucketId, @Nullable String namePrefix, Optional<UUID> after, int limit) {
        List<ObjectSummaryRow> rows;
        if (namePrefix == null || namePrefix.isEmpty()) {
            rows = after.map(id -> entities.listAfter(bucketId, id, limit))
                    .orElseGet(() -> entities.listFirst(bucketId, limit));
        } else {
            var pattern = escapeLike(namePrefix) + "%";
            rows = after.map(id -> entities.listAfterByName(bucketId, pattern, id, limit))
                    .orElseGet(() -> entities.listFirstByName(bucketId, pattern, limit));
        }
        return summaries(rows);
    }

    @Override
    public List<ObjectSummary> listByTransaction(UUID transactionId, Optional<UUID> after, int limit) {
        return summaries(after.map(id -> entities.listAfterByTransaction(transactionId, id, limit))
                .orElseGet(() -> entities.listFirstByTransaction(transactionId, limit)));
    }

    @Override
    public long countByTransaction(UUID transactionId) {
        return entities.countByTransactionId(transactionId);
    }

    @Override
    public List<StoredObject> findPendingReplacements(UUID transactionId) {
        return entities.findByTransactionIdAndStatus(transactionId, PENDING).stream()
                .filter(row -> row.replacesObjectId() != null)
                .map(StoredObjectEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void commitReplacement(StoredObject replacement, StoredObject committed) {
        // La ligne interne disparaît d'abord : elle référence l'objet visé. Son blob n'est pas purgé, il est repris.
        entities.deleteById(replacement.id());
        replace(committed);
    }

    @Override
    public void activatePending(UUID transactionId) {
        entities.activatePending(transactionId);
    }

    @Override
    @Transactional
    public void deletePending(UUID transactionId) {
        entities.findByTransactionIdAndStatus(transactionId, PENDING).forEach(this::deleteAndPurge);
    }

    @Override
    @Transactional
    public void delete(StoredObject object) {
        entities.findById(object.id()).ifPresent(this::deleteAndPurge);
    }

    @Override
    public boolean existsInBucket(UUID bucketId) {
        return entities.existsByBucketId(bucketId);
    }

    private static boolean isDuplicateKey(RuntimeException e) {
        return e instanceof DuplicateKeyException || e.getCause() instanceof DuplicateKeyException;
    }

    private void deleteAndPurge(StoredObjectEntity entity) {
        entities.delete(entity);
        purgeQueue.schedule(entity.blobLocation());
    }

    private static List<ObjectSummary> summaries(List<ObjectSummaryRow> rows) {
        return rows.stream().map(ObjectSummaryRow::toDomain).toList();
    }

    private static String escapeLike(String prefix) {
        return prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
