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
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Représentation d'un objet (sans son contenu).
 *
 * @param id identifiant de ressource, qui désigne l'objet dans toutes les opérations
 * @param name nom libre, s'il a été fourni
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param status {@code PENDING} tant que la transaction n'est pas validée, sinon {@code ACTIVE}
 * @param transactionId transaction de l'écriture, s'il y en a une
 * @param createdAt date de création (UTC)
 * @param updatedAt date du dernier dépôt de contenu (UTC)
 * @param metadata métadonnées utilisateur
 */
@Schema(description = "Object (without its content)")
public record ObjectResponse(
        @Schema(description = "Resource identifier, used to address the object")
        UUID id,

        @Schema(description = "Free, optional name (not unique)", example = "invoice-42.pdf") @Nullable
        String name,

        @Schema(description = "Size in bytes") long size,

        @Schema(description = "Hexadecimal MD5 of the content")
        String etag,

        @Schema(description = "MIME type", example = "application/pdf")
        String contentType,

        @Schema(description = "PENDING until the transaction is committed, then ACTIVE")
        ObjectStatus status,

        @Schema(description = "Transaction of the write, when there is one") @Nullable
        UUID transactionId,

        @Schema(description = "Creation date (UTC)") Instant createdAt,

        @Schema(description = "Last content upload date (UTC)")
        Instant updatedAt,

        @Schema(description = "User metadata") Map<String, String> metadata) {

    public ObjectResponse {
        metadata = Map.copyOf(metadata);
    }
}
