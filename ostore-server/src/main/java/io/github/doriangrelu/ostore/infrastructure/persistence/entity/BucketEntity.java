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
package io.github.doriangrelu.ostore.infrastructure.persistence.entity;

import io.github.doriangrelu.ostore.domain.model.Bucket;
import io.github.doriangrelu.ostore.domain.model.BucketName;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Ligne de la table {@code OST_BUCKET}. Une version nulle signifie « nouvelle ligne à insérer ». */
@Table("OST_BUCKET")
public record BucketEntity(
        @Id UUID id,
        String name,
        String driverId,
        Instant createdAt,
        @Version @Nullable Long version) {

    public static BucketEntity newRow(Bucket bucket) {
        return new BucketEntity(bucket.id(), bucket.name().value(), bucket.driverId(), bucket.createdAt(), null);
    }

    public Bucket toDomain() {
        return new Bucket(id, new BucketName(name), driverId, createdAt);
    }
}
