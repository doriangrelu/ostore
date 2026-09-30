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
package io.github.doriangrelu.ostore.api.rest.mapper;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.contract.constant.OStoreHeaders;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.ObjectResponse;
import io.github.doriangrelu.ostore.contract.dto.ObjectSummaryResponse;
import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectName;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriUtils;

/** Conversion entre objets du domaine, DTO du contrat et en-têtes HTTP. */
public final class ObjectDtoMapper {

    private ObjectDtoMapper() {}

    public static ObjectResponse toResponse(StoredObject object) {
        return new ObjectResponse(
                object.id(),
                nameOf(object.name()),
                object.size(),
                object.etag(),
                object.contentType(),
                object.createdAt(),
                object.updatedAt(),
                object.metadata().entries());
    }

    public static ObjectListResponse toListResponse(BucketName bucket, @Nullable String namePrefix, ObjectPage page) {
        return new ObjectListResponse(
                bucket.value(),
                namePrefix,
                page.objects().stream().map(ObjectDtoMapper::toSummary).toList(),
                page.lastId().map(UUID::toString).orElse(null));
    }

    /** Nom optionnel reçu en paramètre ; une valeur vide équivaut à une absence. */
    public static @Nullable ObjectName toName(@Nullable String name) {
        return Optional.ofNullable(name)
                .filter(value -> !value.isEmpty())
                .map(ObjectName::new)
                .orElse(null);
    }

    /** Point de reprise porté par un jeton de continuation (identifiant du dernier objet de la page). */
    public static Optional<UUID> fromContinuationToken(@Nullable String token) {
        return Optional.ofNullable(token).filter(value -> !value.isBlank()).map(UUID::fromString);
    }

    /**
     * Métadonnées reçues dans les en-têtes {@code X-OStore-Meta: nom=valeur} ; la valeur est percent-décodée.
     */
    public static ObjectMetadata toMetadata(@Nullable List<String> headers) {
        if (headers == null) {
            return ObjectMetadata.EMPTY;
        }
        return ObjectMetadata.parse(
                headers.stream().map(ObjectDtoMapper::decodeValue).toList());
    }

    /** En-têtes de réponse décrivant un objet servi en contenu ; le nom devient le nom de fichier proposé. */
    public static HttpHeaders contentHeaders(StoredObject object) {
        var headers = new HttpHeaders();
        headers.setETag("\"" + object.etag() + "\"");
        headers.setLastModified(object.updatedAt());
        headers.set(HttpHeaders.CONTENT_TYPE, object.contentType());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(OStoreHeaders.RESOURCE_ID, object.id().toString());
        if (object.name() != null) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(object.name().value(), UTF_8)
                    .build());
        }
        object.metadata()
                .entries()
                .forEach((name, value) -> headers.add(OStoreHeaders.META, name + "=" + UriUtils.encode(value, UTF_8)));
        return headers;
    }

    private static ObjectSummaryResponse toSummary(ObjectSummary summary) {
        return new ObjectSummaryResponse(
                summary.id(),
                nameOf(summary.name()),
                summary.size(),
                summary.etag(),
                summary.contentType(),
                summary.updatedAt());
    }

    private static @Nullable String nameOf(@Nullable ObjectName name) {
        return name != null ? name.value() : null;
    }

    private static String decodeValue(String header) {
        int separator = header.indexOf('=');
        return separator < 0
                ? header
                : header.substring(0, separator + 1) + UriUtils.decode(header.substring(separator + 1), UTF_8);
    }
}
