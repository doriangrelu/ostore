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
import org.jspecify.annotations.Nullable;

/**
 * Copie d'un objet du bucket courant vers une autre clé, éventuellement dans un autre bucket.
 *
 * @param sourceKey clé de l'objet à copier
 * @param targetBucket bucket cible ({@code null} = bucket courant)
 * @param targetKey clé cible
 */
@Schema(description = "Object copy request")
public record CopyObjectRequest(
        @Schema(description = "Key of the object to copy") @NotBlank
        String sourceKey,

        @Schema(description = "Target bucket, current bucket when absent") @Nullable
        String targetBucket,

        @Schema(description = "Target key") @NotBlank String targetKey) {}
