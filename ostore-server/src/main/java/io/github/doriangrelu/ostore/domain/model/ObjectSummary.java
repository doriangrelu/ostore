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
package io.github.doriangrelu.ostore.domain.model;

import io.github.doriangrelu.ostore.domain.model.vo.ObjectName;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Vue allégée d'un objet, pour les listes (sans métadonnées ni emplacement physique).
 *
 * @param id identifiant de ressource
 * @param name nom libre, s'il existe
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale
 * @param contentType type MIME
 * @param updatedAt date du dernier dépôt de contenu
 */
public record ObjectSummary(
        UUID id, @Nullable ObjectName name, long size, String etag, String contentType, Instant updatedAt) {}
