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
 * Objet dans une liste.
 *
 * @param id identifiant de ressource
 * @param name nom libre, s'il existe
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param updatedAt date du dernier dépôt de contenu (UTC)
 */
@Schema(description = "Object in a listing")
public record ObjectSummaryResponse(
        UUID id, @Nullable String name, long size, String etag, String contentType, Instant updatedAt) {}
