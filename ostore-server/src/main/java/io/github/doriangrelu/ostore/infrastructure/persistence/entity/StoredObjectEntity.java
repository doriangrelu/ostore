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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectName;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Ligne de la table {@code OST_OBJECT} et ses métadonnées (agrégat Spring Data JDBC). Une version nulle
 * signifie « nouvelle ligne à insérer » ; sinon la mise à jour vérifie la version (verrouillage optimiste).
 */
@Table("OST_OBJECT")
public record StoredObjectEntity(
        @Id UUID id,
        UUID bucketId,
        @Nullable String objectName,
        String driverId,
        String blobPath,
        long sizeBytes,
        String etag,
        String contentType,
        String status,
        @Nullable UUID transactionId,
        @Nullable UUID replacesObjectId,
        Instant createdAt,
        Instant updatedAt,
        @MappedCollection(idColumn = "OBJECT_ID") Set<ObjectMetadataEntity> metadata,
        @Version @Nullable Long version) {

    /** Nouvelle ligne à insérer. */
    public static StoredObjectEntity newRow(StoredObject object) {
        return of(object, null);
    }

    /** Mise à jour de cette ligne avec l'état de l'objet, à la version lue. */
    public StoredObjectEntity updatedWith(StoredObject object) {
        return of(object, version);
    }

    /**
     * Commit d'un remplacement : cette ligne (objet visé) prend le contenu de la version en attente, garde son
     * identifiant et sa date de création, et redevient active.
     */
    public StoredObjectEntity withContentOf(StoredObjectEntity replacement) {
        var copiedMetadata = replacement.metadata().stream()
                .map(entry -> new ObjectMetadataEntity(entry.name(), entry.value()))
                .collect(Collectors.toSet());
        return new StoredObjectEntity(
                id,
                bucketId,
                replacement.objectName(),
                replacement.driverId(),
                replacement.blobPath(),
                replacement.sizeBytes(),
                replacement.etag(),
                replacement.contentType(),
                ObjectStatus.ACTIVE.name(),
                replacement.transactionId(),
                null,
                createdAt,
                replacement.updatedAt(),
                copiedMetadata,
                version);
    }

    public StoredObject toDomain() {
        return new StoredObject(
                id,
                bucketId,
                Optional.ofNullable(objectName).map(ObjectName::new).orElse(null),
                blobLocation(),
                sizeBytes,
                etag,
                contentType,
                ObjectMetadata.of(
                        metadata.stream().collect(toMap(ObjectMetadataEntity::name, ObjectMetadataEntity::value))),
                ObjectStatus.valueOf(status),
                transactionId,
                replacesObjectId,
                createdAt,
                updatedAt);
    }

    public BlobLocation blobLocation() {
        return new BlobLocation(driverId, blobPath);
    }

    private static StoredObjectEntity of(StoredObject object, @Nullable Long version) {
        var metadata = object.metadata().entries().entrySet().stream()
                .map(entry -> new ObjectMetadataEntity(entry.getKey(), entry.getValue()))
                .collect(toSet());
        return new StoredObjectEntity(
                object.id(),
                object.bucketId(),
                Optional.ofNullable(object.name()).map(ObjectName::value).orElse(null),
                object.blob().driverId(),
                object.blob().path(),
                object.size(),
                object.etag(),
                object.contentType(),
                object.status().name(),
                object.transactionId(),
                object.replaces(),
                object.createdAt(),
                object.updatedAt(),
                metadata,
                version);
    }
}
