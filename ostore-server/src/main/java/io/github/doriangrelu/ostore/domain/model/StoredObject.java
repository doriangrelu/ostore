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

import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Objet stocké : une clé dans un bucket, pointant vers un blob physique.
 *
 * @param id identifiant de ressource (UUID v7)
 * @param bucketId bucket contenant l'objet
 * @param key clé dans le bucket
 * @param blob emplacement physique du contenu
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param metadata métadonnées utilisateur
 * @param createdAt date de dépôt, à la microseconde (voir {@link Bucket})
 */
public record StoredObject(
        UUID id,
        UUID bucketId,
        ObjectKey key,
        BlobLocation blob,
        long size,
        String etag,
        String contentType,
        ObjectMetadata metadata,
        Instant createdAt) {

    /** Type MIME par défaut, quand le client n'en fournit pas. */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public StoredObject {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bucketId, "bucketId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(blob, "blob");
        Objects.requireNonNull(etag, "etag");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(metadata, "metadata");
        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt").truncatedTo(ChronoUnit.MICROS);
    }

    /** Nouvel objet, dont le contenu vient d'être écrit à l'emplacement donné. */
    public static StoredObject create(
            Bucket bucket,
            ObjectKey key,
            BlobLocation blob,
            long size,
            String etag,
            String contentType,
            ObjectMetadata metadata,
            Instant now) {
        return new StoredObject(Identifiers.newId(now), bucket.id(), key, blob, size, etag, contentType, metadata, now);
    }
}
