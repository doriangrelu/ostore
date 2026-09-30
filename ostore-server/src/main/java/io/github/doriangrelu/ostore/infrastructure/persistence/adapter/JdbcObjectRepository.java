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
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.StoredObjectEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.ObjectSummaryRow;
import io.github.doriangrelu.ostore.infrastructure.persistence.repository.StoredObjectEntityRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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
    public StoredObject insert(StoredObject object) {
        return entities.save(StoredObjectEntity.newRow(object)).toDomain();
    }

    @Override
    @Transactional
    public StoredObject replace(StoredObject updated) {
        var current =
                entities.findById(updated.id()).orElseThrow(() -> new ConcurrentObjectUpdateException(updated.id()));
        try {
            var saved = entities.save(current.updatedWith(updated));
            purgeQueue.schedule(current.blobLocation());
            return saved.toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentObjectUpdateException(updated.id());
        } catch (DbActionExecutionException e) {
            if (e.getCause() instanceof OptimisticLockingFailureException) {
                throw new ConcurrentObjectUpdateException(updated.id());
            }
            throw e;
        }
    }

    @Override
    public Optional<StoredObject> find(UUID id) {
        return entities.findById(id).map(StoredObjectEntity::toDomain);
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
        return rows.stream().map(ObjectSummaryRow::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(StoredObject object) {
        entities.findById(object.id()).ifPresent(entity -> {
            entities.delete(entity);
            purgeQueue.schedule(entity.blobLocation());
        });
    }

    @Override
    public boolean existsInBucket(UUID bucketId) {
        return entities.existsByBucketId(bucketId);
    }

    private static String escapeLike(String prefix) {
        return prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
