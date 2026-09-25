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
package io.github.doriangrelu.ostore.infrastructure.persistence.projection;

import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import java.time.Instant;

/**
 * Projection allégée d'une ligne de {@code OST_OBJECT} pour les listes : évite de charger les métadonnées
 * (une requête par objet) quand elles ne sont pas affichées.
 */
public record ObjectSummaryRow(String objectKey, long sizeBytes, String etag, String contentType, Instant createdAt) {

    public ObjectSummary toDomain() {
        return new ObjectSummary(new ObjectKey(objectKey), sizeBytes, etag, contentType, createdAt);
    }
}
