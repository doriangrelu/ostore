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
import org.jspecify.annotations.Nullable;

/**
 * Copie d'un objet en un nouvel objet, éventuellement dans un autre bucket.
 *
 * @param targetBucket bucket cible ({@code null} = bucket de l'objet copié)
 * @param name nom de la copie ({@code null} = nom de l'objet copié)
 */
@Schema(description = "Object copy request")
public record CopyObjectRequest(
        @Schema(description = "Target bucket, source bucket when absent") @Nullable
        String targetBucket,

        @Schema(description = "Name of the copy, source name when absent") @Nullable
        String name) {}
