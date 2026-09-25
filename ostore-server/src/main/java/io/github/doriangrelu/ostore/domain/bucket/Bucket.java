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
package io.github.doriangrelu.ostore.domain.bucket;

import io.github.doriangrelu.ostore.domain.Identifiers;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bucket : conteneur logique d'objets.
 *
 * @param id identifiant technique (UUID v7)
 * @param name nom unique, conforme aux règles S3
 * @param driverId driver de stockage physique des objets du bucket
 * @param createdAt date de création
 */
public record Bucket(UUID id, BucketName name, String driverId, Instant createdAt) {

    public Bucket {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(driverId, "driverId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Crée un nouveau bucket, stocké par le driver donné. */
    public static Bucket create(BucketName name, String driverId, Instant now) {
        return new Bucket(Identifiers.newId(now), name, driverId, now);
    }
}
