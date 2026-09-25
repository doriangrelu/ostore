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

/**
 * Représentation d'un bucket.
 *
 * @param id identifiant de ressource
 * @param name nom du bucket (clé S3)
 * @param createdAt date de création
 */
@Schema(description = "Bucket")
public record BucketResponse(
        @Schema(description = "Resource identifier") UUID id,

        @Schema(description = "Bucket name (S3 key)", example = "invoices-2026")
        String name,

        @Schema(description = "Creation date") Instant createdAt) {}
