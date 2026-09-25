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
package io.github.doriangrelu.ostore.infrastructure.persistence.repository;

import io.github.doriangrelu.ostore.infrastructure.persistence.entity.StoredObjectEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.ObjectSummaryRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository Spring Data JDBC de {@link StoredObjectEntity}.
 *
 * <p>Les listes s'appuient sur l'index {@code UK_OST_OBJECT_BUCKET_KEY} et l'ordre binaire des clés (collation
 * {@code "C"} sur PostgreSQL, session {@code NLS_SORT=BINARY} sur Oracle). Deux requêtes distinctes pour la
 * première page et les suivantes : Oracle assimile la chaîne vide à {@code NULL}, ce qui interdit un curseur
 * « vide » unique. Le motif {@code LIKE} échappe {@code !}, {@code %} et {@code _} avec {@code !}.
 */
public interface StoredObjectEntityRepository extends ListCrudRepository<StoredObjectEntity, UUID> {

    Optional<StoredObjectEntity> findByBucketIdAndObjectKey(UUID bucketId, String objectKey);

    boolean existsByBucketId(UUID bucketId);

    @Query("""
            SELECT OBJECT_KEY, SIZE_BYTES, ETAG, CONTENT_TYPE, CREATED_AT FROM OST_OBJECT
            WHERE BUCKET_ID = :bucketId AND OBJECT_KEY LIKE :pattern ESCAPE '!'
            ORDER BY OBJECT_KEY FETCH FIRST :limit ROWS ONLY""")
    List<ObjectSummaryRow> listFirst(UUID bucketId, String pattern, int limit);

    @Query("""
            SELECT OBJECT_KEY, SIZE_BYTES, ETAG, CONTENT_TYPE, CREATED_AT FROM OST_OBJECT
            WHERE BUCKET_ID = :bucketId AND OBJECT_KEY LIKE :pattern ESCAPE '!' AND OBJECT_KEY > :after
            ORDER BY OBJECT_KEY FETCH FIRST :limit ROWS ONLY""")
    List<ObjectSummaryRow> listAfter(UUID bucketId, String pattern, String after, int limit);
}
