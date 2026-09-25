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
import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.StoredObjectEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.ObjectSummaryRow;
import io.github.doriangrelu.ostore.infrastructure.persistence.repository.StoredObjectEntityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptateur du port {@link ObjectRepository} sur Spring Data JDBC. */
@Repository
public class JdbcObjectRepository implements ObjectRepository {

    private final StoredObjectEntityRepository entities;
    private final BlobPurgeQueue purgeQueue;

    public JdbcObjectRepository(StoredObjectEntityRepository entities, BlobPurgeQueue purgeQueue) {
        this.entities = entities;
        this.purgeQueue = purgeQueue;
    }

    @Override
    @Transactional
    public StoredObject putActive(StoredObject object) {
        try {
            entities.findByBucketIdAndObjectKey(object.bucketId(), object.key().value())
                    .ifPresent(this::deleteAndPurge);
            return entities.save(StoredObjectEntity.newRow(object)).toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentObjectUpdateException(object.key());
        } catch (DbActionExecutionException e) {
            if (e.getCause() instanceof DuplicateKeyException
                    || e.getCause() instanceof OptimisticLockingFailureException) {
                throw new ConcurrentObjectUpdateException(object.key());
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredObject> find(UUID bucketId, ObjectKey key) {
        return entities.findByBucketIdAndObjectKey(bucketId, key.value()).map(StoredObjectEntity::toDomain);
    }

    @Override
    public List<ObjectSummary> list(UUID bucketId, String prefix, Optional<ObjectKey> after, int limit) {
        var pattern = escapeLike(prefix) + "%";
        var rows = after.map(key -> entities.listAfter(bucketId, pattern, key.value(), limit))
                .orElseGet(() -> entities.listFirst(bucketId, pattern, limit));
        return rows.stream().map(ObjectSummaryRow::toDomain).toList();
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

    private void deleteAndPurge(StoredObjectEntity entity) {
        entities.delete(entity);
        purgeQueue.schedule(entity.blobLocation());
    }

    private static String escapeLike(String prefix) {
        return prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
