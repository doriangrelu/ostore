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
package io.github.doriangrelu.ostore.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Représentation d'une transaction.
 *
 * @param id identifiant de la transaction
 * @param status statut réel (une transaction échue est expirée)
 * @param reference référence libre du client
 * @param createdAt date d'ouverture (UTC)
 * @param expiresAt échéance (UTC)
 * @param closedAt date de clôture (UTC), absente tant qu'elle est ouverte
 * @param objectCount nombre d'objets rattachés
 */
@Schema(description = "Transaction")
public record TransactionResponse(
        UUID id,
        TransactionStatus status,
        @Nullable String reference,
        Instant createdAt,
        Instant expiresAt,
        @Nullable Instant closedAt,
        long objectCount) {}
