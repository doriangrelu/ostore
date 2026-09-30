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
package io.github.doriangrelu.ostore.api.rest.mapper;

import io.github.doriangrelu.ostore.application.command.TransactionMode;
import io.github.doriangrelu.ostore.application.result.TransactionDetails;
import io.github.doriangrelu.ostore.contract.dto.TransactionResponse;
import io.github.doriangrelu.ostore.contract.dto.TransactionStatus;
import io.github.doriangrelu.ostore.domain.exception.ConflictingTransactionOptionsException;
import io.github.doriangrelu.ostore.domain.exception.InvalidTtlException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Conversion entre transactions du domaine, DTO du contrat et en-têtes transactionnels. */
public final class TransactionDtoMapper {

    private TransactionDtoMapper() {}

    public static TransactionResponse toResponse(TransactionDetails details) {
        var transaction = details.transaction();
        return new TransactionResponse(
                transaction.id(),
                TransactionStatus.valueOf(details.status().name()),
                transaction.reference(),
                transaction.createdAt(),
                transaction.expiresAt(),
                transaction.closedAt(),
                details.objectCount());
    }

    /**
     * Mode transactionnel d'une écriture à partir des en-têtes {@code X-OStore-Transaction-Id} et
     * {@code X-OStore-Pending-Ttl}.
     *
     * @throws ConflictingTransactionOptionsException si les deux en-têtes sont présents
     * @throws InvalidTtlException si la durée n'est pas au format ISO-8601
     */
    public static TransactionMode toTransactionMode(@Nullable UUID transactionId, @Nullable String pendingTtl) {
        if (transactionId != null && pendingTtl != null) {
            throw new ConflictingTransactionOptionsException();
        }
        return TransactionMode.of(transactionId, pendingTtl == null ? null : parseTtl(pendingTtl));
    }

    private static Duration parseTtl(String value) {
        try {
            return Duration.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidTtlException("'%s' is not an ISO-8601 duration".formatted(value));
        }
    }
}
