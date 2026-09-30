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

import io.github.doriangrelu.ostore.infrastructure.persistence.entity.TransactionEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.TransactionIdRow;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository Spring Data JDBC de {@link TransactionEntity}.
 *
 * <p>La sélection des transactions échues n'est pas verrouillée (Oracle interdit {@code FETCH FIRST} avec
 * {@code FOR UPDATE}) : chaque transaction est ensuite verrouillée une à une, sans attente
 * ({@code SKIP LOCKED}), ce qui répartit le travail entre instances.
 */
public interface TransactionEntityRepository extends ListCrudRepository<TransactionEntity, UUID> {

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<TransactionEntity> findLockedById(UUID id);

    @Query("SELECT * FROM OST_TRANSACTION WHERE ID = :id AND STATUS = 'OPEN' FOR UPDATE SKIP LOCKED")
    Optional<TransactionEntity> tryLockOpen(UUID id);

    @Query("""
            SELECT ID FROM OST_TRANSACTION WHERE STATUS = 'OPEN' AND EXPIRES_AT <= :now
            ORDER BY EXPIRES_AT FETCH FIRST :limit ROWS ONLY""")
    List<TransactionIdRow> findDue(Instant now, int limit);
}
