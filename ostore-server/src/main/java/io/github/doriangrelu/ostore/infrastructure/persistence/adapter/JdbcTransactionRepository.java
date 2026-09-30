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

import io.github.doriangrelu.ostore.application.port.out.TransactionRepository;
import io.github.doriangrelu.ostore.domain.model.Transaction;
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.TransactionEntity;
import io.github.doriangrelu.ostore.infrastructure.persistence.projection.TransactionIdRow;
import io.github.doriangrelu.ostore.infrastructure.persistence.repository.TransactionEntityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptateur du port {@link TransactionRepository} sur Spring Data JDBC. */
@Repository
public class JdbcTransactionRepository implements TransactionRepository {

    private final TransactionEntityRepository entities;

    public JdbcTransactionRepository(TransactionEntityRepository entities) {
        this.entities = entities;
    }

    @Override
    public Transaction insert(Transaction transaction) {
        return entities.save(TransactionEntity.newRow(transaction)).toDomain();
    }

    @Override
    public Optional<Transaction> find(UUID id) {
        return entities.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public Optional<Transaction> lock(UUID id) {
        return entities.findLockedById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public Optional<Transaction> tryLockOpen(UUID id) {
        return entities.tryLockOpen(id).map(TransactionEntity::toDomain);
    }

    @Override
    public List<UUID> findDue(Instant now, int limit) {
        return entities.findDue(now, limit).stream().map(TransactionIdRow::id).toList();
    }

    @Override
    public Transaction update(Transaction transaction) {
        var current = entities.findById(transaction.id()).orElseThrow();
        return entities.save(current.updatedWith(transaction)).toDomain();
    }
}
