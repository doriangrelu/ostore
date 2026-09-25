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

import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import io.github.doriangrelu.ostore.domain.exception.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.model.Bucket;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.BucketEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.repository.BucketEntityRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Repository;

/** Adaptateur du port {@link BucketRepository} sur Spring Data JDBC. */
@Repository
public class JdbcBucketRepository implements BucketRepository {

    private static final Sort BY_NAME = Sort.by("name");

    private final BucketEntityRepository entities;

    public JdbcBucketRepository(BucketEntityRepository entities) {
        this.entities = entities;
    }

    @Override
    public Bucket insert(Bucket bucket) {
        try {
            return entities.save(BucketEntity.newRow(bucket)).toDomain();
        } catch (DbActionExecutionException e) {
            // Création concurrente : la contrainte UK_OST_BUCKET_NAME a tranché.
            if (e.getCause() instanceof DuplicateKeyException) {
                throw new BucketAlreadyExistsException(bucket.name());
            }
            throw e;
        }
    }

    @Override
    public Optional<Bucket> findByName(BucketName name) {
        return entities.findByName(name.value()).map(BucketEntity::toDomain);
    }

    @Override
    public List<Bucket> findAll() {
        return entities.findAll(BY_NAME).stream().map(BucketEntity::toDomain).toList();
    }

    @Override
    public void delete(Bucket bucket) {
        entities.deleteById(bucket.id());
    }
}
