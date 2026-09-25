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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Demande de création d'un bucket.
 *
 * <p>La validation ne contrôle ici que la forme générale. Les règles complètes de nommage S3 sont
 * appliquées par le serveur (erreur {@code INVALID_BUCKET_NAME}).
 *
 * @param name nom du bucket
 */
@Schema(description = "Bucket creation request")
public record CreateBucketRequest(
        @Schema(
                description = "Bucket name, following S3 naming rules",
                example = "invoices-2026",
                minLength = 3,
                maxLength = 63)
        @NotBlank
        @Size(min = 3, max = 63)
        String name) {}
