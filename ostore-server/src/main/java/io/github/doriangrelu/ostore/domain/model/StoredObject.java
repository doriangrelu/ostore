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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectName;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Objet stocké, désigné par son identifiant (ADR-0015) et pointant vers un blob physique.
 *
 * @param id identifiant de ressource (UUID v7), stable même si le contenu est remplacé
 * @param bucketId bucket contenant l'objet
 * @param name nom libre et optionnel (non unique)
 * @param blob emplacement physique du contenu courant
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param metadata métadonnées utilisateur
 * @param createdAt date de création, à la microseconde (voir {@link Bucket})
 * @param updatedAt date du dernier dépôt de contenu, à la microseconde
 */
public record StoredObject(
        UUID id,
        UUID bucketId,
        @Nullable ObjectName name,
        BlobLocation blob,
        long size,
        String etag,
        String contentType,
        ObjectMetadata metadata,
        Instant createdAt,
        Instant updatedAt) {

    /** Type MIME par défaut, quand le client n'en fournit pas. */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public StoredObject {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bucketId, "bucketId");
        Objects.requireNonNull(blob, "blob");
        Objects.requireNonNull(etag, "etag");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(metadata, "metadata");
        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt").truncatedTo(ChronoUnit.MICROS);
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt").truncatedTo(ChronoUnit.MICROS);
    }

    /** Nouvel objet du bucket donné, dont le contenu vient d'être écrit. */
    public static StoredObject create(
            UUID bucketId, @Nullable ObjectName name, Content content, ObjectMetadata metadata, Instant now) {
        return new StoredObject(
                Identifiers.newId(now),
                bucketId,
                name,
                content.blob(),
                content.size(),
                content.etag(),
                content.contentType(),
                metadata,
                now,
                now);
    }

    /**
     * Même objet (même identifiant) avec un nouveau contenu ; l'ancien blob est à purger par l'appelant.
     *
     * @param newName nouveau nom, ou {@code null} pour conserver le nom actuel
     */
    public StoredObject withContent(
            Content content, @Nullable ObjectName newName, ObjectMetadata newMetadata, Instant now) {
        return new StoredObject(
                id,
                bucketId,
                newName != null ? newName : name,
                content.blob(),
                content.size(),
                content.etag(),
                content.contentType(),
                newMetadata,
                createdAt,
                now);
    }

    /**
     * Contenu écrit sur le stockage : emplacement, taille et empreinte réellement reçues, type MIME.
     *
     * @param blob emplacement du blob
     * @param size taille en octets
     * @param etag empreinte MD5 hexadécimale
     * @param contentType type MIME
     */
    public record Content(BlobLocation blob, long size, String etag, String contentType) {}
}
