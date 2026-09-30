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
package io.github.doriangrelu.ostore.infrastructure.persistence.entity;

import io.github.doriangrelu.ostore.domain.model.Transaction;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Ligne de la table {@code OST_TRANSACTION}. Une version nulle signifie « nouvelle ligne à insérer ». */
@Table("OST_TRANSACTION")
public record TransactionEntity(
        @Id UUID id,
        String status,
        @Nullable String clientReference,
        Instant createdAt,
        Instant expiresAt,
        @Nullable Instant closedAt,
        @Version @Nullable Long version) {

    public static TransactionEntity newRow(Transaction transaction) {
        return of(transaction, null);
    }

    /** Mise à jour de cette ligne avec l'état de la transaction, à la version lue. */
    public TransactionEntity updatedWith(Transaction transaction) {
        return of(transaction, version);
    }

    public Transaction toDomain() {
        return new Transaction(id, TransactionStatus.valueOf(status), clientReference, createdAt, expiresAt, closedAt);
    }

    private static TransactionEntity of(Transaction transaction, @Nullable Long version) {
        return new TransactionEntity(
                transaction.id(),
                transaction.status().name(),
                transaction.reference(),
                transaction.createdAt(),
                transaction.expiresAt(),
                transaction.closedAt(),
                version);
    }
}
