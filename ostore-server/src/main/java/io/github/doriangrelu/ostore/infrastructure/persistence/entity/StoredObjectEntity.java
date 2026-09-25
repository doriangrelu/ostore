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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Ligne de la table {@code OST_OBJECT} et ses métadonnées (agrégat Spring Data JDBC). Une version nulle
 * signifie « nouvelle ligne à insérer ».
 */
@Table("OST_OBJECT")
public record StoredObjectEntity(
        @Id UUID id,
        UUID bucketId,
        String objectKey,
        String driverId,
        String blobPath,
        long sizeBytes,
        String etag,
        String contentType,
        Instant createdAt,
        @MappedCollection(idColumn = "OBJECT_ID") Set<ObjectMetadataEntity> metadata,
        @Version @Nullable Long version) {

    public static StoredObjectEntity newRow(StoredObject object) {
        var metadata = object.metadata().entries().entrySet().stream()
                .map(entry -> new ObjectMetadataEntity(entry.getKey(), entry.getValue()))
                .collect(toSet());
        return new StoredObjectEntity(
                object.id(),
                object.bucketId(),
                object.key().value(),
                object.blob().driverId(),
                object.blob().path(),
                object.size(),
                object.etag(),
                object.contentType(),
                object.createdAt(),
                metadata,
                null);
    }

    public StoredObject toDomain() {
        return new StoredObject(
                id,
                bucketId,
                new ObjectKey(objectKey),
                blobLocation(),
                sizeBytes,
                etag,
                contentType,
                ObjectMetadata.of(
                        metadata.stream().collect(toMap(ObjectMetadataEntity::name, ObjectMetadataEntity::value))),
                createdAt);
    }

    public BlobLocation blobLocation() {
        return new BlobLocation(driverId, blobPath);
    }
}
