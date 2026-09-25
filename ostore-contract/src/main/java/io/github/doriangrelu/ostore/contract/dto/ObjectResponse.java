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

/**
 * Représentation d'un objet (sans son contenu).
 *
 * @param resourceId identifiant de ressource
 * @param bucket nom du bucket
 * @param key clé de l'objet
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param lastModified date du dépôt (UTC)
 * @param metadata métadonnées utilisateur, triées par nom
 */
@Schema(description = "Object (without its content)")
public record ObjectResponse(
        @Schema(description = "Resource identifier") UUID resourceId,

        @Schema(description = "Bucket name", example = "invoices-2026")
        String bucket,

        @Schema(description = "Object key", example = "2026/09/invoice-42.pdf")
        String key,

        @Schema(description = "Size in bytes") long size,

        @Schema(description = "Hexadecimal MD5 of the content")
        String etag,

        @Schema(description = "MIME type", example = "application/pdf")
        String contentType,

        @Schema(description = "Upload date (UTC)") Instant lastModified,
        @Schema(description = "User metadata") Map<String, String> metadata) {

    public ObjectResponse {
        metadata = Map.copyOf(metadata);
    }
}
