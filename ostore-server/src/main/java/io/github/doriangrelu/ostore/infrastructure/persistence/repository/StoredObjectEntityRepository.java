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
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository Spring Data JDBC de {@link StoredObjectEntity}.
 *
 * <p>Les listes sont paginées par identifiant croissant (UUID v7, donc ordre de création), comparé octet par
 * octet sur PostgreSQL ({@code UUID}) comme sur Oracle ({@code RAW(16)}). Une liste de bucket ne montre que les
 * objets validés ({@code IX_OST_OBJECT_BUCKET_STATUS}) ; celle d'une transaction montre tout ce qu'elle contient
 * ({@code IX_OST_OBJECT_TRANSACTION}). Une requête par cas : chacune reste simple et ne dépend d'aucun
 * paramètre {@code NULL}, qu'Oracle confond avec la chaîne vide. Le motif {@code LIKE} échappe {@code !},
 * {@code %} et {@code _} avec {@code !}.
 */
public interface StoredObjectEntityRepository extends ListCrudRepository<StoredObjectEntity, UUID> {

    String SUMMARY = "SELECT ID, REPLACES_OBJECT_ID, OBJECT_NAME, SIZE_BYTES, ETAG, CONTENT_TYPE, STATUS, UPDATED_AT"
            + " FROM OST_OBJECT ";
    String ACTIVE_IN_BUCKET = "WHERE BUCKET_ID = :bucketId AND STATUS = 'ACTIVE'";
    String PAGE = " ORDER BY ID FETCH FIRST :limit ROWS ONLY";

    boolean existsByBucketId(UUID bucketId);

    long countByTransactionId(UUID transactionId);

    Optional<StoredObjectEntity> findByReplacesObjectId(UUID replacesObjectId);

    List<StoredObjectEntity> findByTransactionIdAndStatus(UUID transactionId, String status);

    @Modifying
    @Query("""
            UPDATE OST_OBJECT SET STATUS = 'ACTIVE', VERSION = VERSION + 1
            WHERE TRANSACTION_ID = :transactionId AND STATUS = 'PENDING' AND REPLACES_OBJECT_ID IS NULL""")
    int activatePending(UUID transactionId);

    @Query(SUMMARY + ACTIVE_IN_BUCKET + PAGE)
    List<ObjectSummaryRow> listFirst(UUID bucketId, int limit);

    @Query(SUMMARY + ACTIVE_IN_BUCKET + " AND ID > :after" + PAGE)
    List<ObjectSummaryRow> listAfter(UUID bucketId, UUID after, int limit);

    @Query(SUMMARY + ACTIVE_IN_BUCKET + " AND OBJECT_NAME LIKE :pattern ESCAPE '!'" + PAGE)
    List<ObjectSummaryRow> listFirstByName(UUID bucketId, String pattern, int limit);

    @Query(SUMMARY + ACTIVE_IN_BUCKET + " AND OBJECT_NAME LIKE :pattern ESCAPE '!' AND ID > :after" + PAGE)
    List<ObjectSummaryRow> listAfterByName(UUID bucketId, String pattern, UUID after, int limit);

    @Query(SUMMARY + "WHERE TRANSACTION_ID = :transactionId" + PAGE)
    List<ObjectSummaryRow> listFirstByTransaction(UUID transactionId, int limit);

    @Query(SUMMARY + "WHERE TRANSACTION_ID = :transactionId AND ID > :after" + PAGE)
    List<ObjectSummaryRow> listAfterByTransaction(UUID transactionId, UUID after, int limit);
}
