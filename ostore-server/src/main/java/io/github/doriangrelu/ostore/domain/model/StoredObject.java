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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectStatus;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Objet stocké, désigné par son identifiant (ADR-0015) et pointant vers un blob physique.
 *
 * <p>Une ligne peut aussi être un <b>remplacement en attente</b> ({@link #replaces()} renseigné, ADR-0016) : la
 * nouvelle version d'un objet validé, écrite dans une transaction et appliquée à l'objet visé au commit. Son
 * identifiant est interne et n'est jamais exposé : elle se présente sous l'identifiant de l'objet visé.
 *
 * @param id identifiant (UUID v7), stable même si le contenu est remplacé
 * @param bucketId bucket contenant l'objet
 * @param name nom libre et optionnel (non unique)
 * @param blob emplacement physique du contenu
 * @param size taille en octets
 * @param etag empreinte MD5 hexadécimale du contenu
 * @param contentType type MIME
 * @param metadata métadonnées utilisateur
 * @param status {@code PENDING} tant que sa transaction n'est pas validée, sinon {@code ACTIVE}
 * @param transactionId transaction de l'écriture en attente, ou de la dernière écriture transactionnelle
 * @param replaces objet visé, si cette ligne est un remplacement en attente
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
        ObjectStatus status,
        @Nullable UUID transactionId,
        @Nullable UUID replaces,
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
        Objects.requireNonNull(status, "status");
        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (status == ObjectStatus.PENDING && transactionId == null) {
            throw new IllegalArgumentException("a pending object belongs to a transaction");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt").truncatedTo(ChronoUnit.MICROS);
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt").truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Nouvel objet du bucket donné, dont le contenu vient d'être écrit.
     *
     * @param transactionId transaction à rejoindre (objet {@code PENDING}), ou {@code null} (objet {@code ACTIVE})
     */
    public static StoredObject create(
            UUID bucketId,
            @Nullable ObjectName name,
            Content content,
            ObjectMetadata metadata,
            @Nullable UUID transactionId,
            Instant now) {
        return new StoredObject(
                Identifiers.newId(now),
                bucketId,
                name,
                content.blob(),
                content.size(),
                content.etag(),
                content.contentType(),
                metadata,
                transactionId == null ? ObjectStatus.ACTIVE : ObjectStatus.PENDING,
                transactionId,
                null,
                now,
                now);
    }

    /** Remplacement en attente de cet objet validé, dans la transaction donnée (ligne interne, nouvel id). */
    public StoredObject pendingReplacement(
            Content content, @Nullable ObjectName newName, ObjectMetadata newMetadata, UUID transaction, Instant now) {
        return new StoredObject(
                Identifiers.newId(now),
                bucketId,
                newName != null ? newName : name,
                content.blob(),
                content.size(),
                content.etag(),
                content.contentType(),
                newMetadata,
                ObjectStatus.PENDING,
                transaction,
                id,
                createdAt,
                now);
    }

    /**
     * Même ligne avec un nouveau contenu (même identifiant, même statut) ; l'ancien blob est à purger.
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
                status,
                transactionId,
                replaces,
                createdAt,
                now);
    }

    /** Vue publique : un remplacement en attente se présente sous l'identifiant de l'objet visé. */
    public UUID publicId() {
        return replaces != null ? replaces : id;
    }

    /** Indique si l'objet attend la validation de sa transaction. */
    public boolean isPending() {
        return status == ObjectStatus.PENDING;
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
