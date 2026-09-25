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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriUtils;

/** Conversion entre objets du domaine, DTO du contrat et en-têtes HTTP. */
public final class ObjectDtoMapper {

    private ObjectDtoMapper() {}

    public static ObjectResponse toResponse(BucketName bucket, StoredObject object) {
        return new ObjectResponse(
                object.id(),
                bucket.value(),
                object.key().value(),
                object.size(),
                object.etag(),
                object.contentType(),
                object.createdAt(),
                object.metadata().entries());
    }

    public static ObjectListResponse toListResponse(BucketName bucket, String prefix, ObjectPage page) {
        return new ObjectListResponse(
                bucket.value(),
                prefix,
                page.objects().stream().map(ObjectDtoMapper::toSummary).toList(),
                page.lastKey().map(ObjectDtoMapper::toContinuationToken).orElse(null));
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

    /** En-têtes de réponse décrivant un objet servi en contenu. */
    public static HttpHeaders contentHeaders(StoredObject object) {
        var headers = new HttpHeaders();
        headers.setETag("\"" + object.etag() + "\"");
        headers.setLastModified(object.createdAt());
        headers.set(HttpHeaders.CONTENT_TYPE, object.contentType());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(OStoreHeaders.RESOURCE_ID, object.id().toString());
        object.metadata()
                .entries()
                .forEach((name, value) -> headers.add(OStoreHeaders.META, name + "=" + UriUtils.encode(value, UTF_8)));
        return headers;
    }

    /** Jeton de continuation : dernière clé de la page, en base64url. */
    public static String toContinuationToken(ObjectKey lastKey) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(lastKey.value().getBytes(UTF_8));
    }

    /** Clé de reprise portée par un jeton de continuation. */
    public static Optional<ObjectKey> fromContinuationToken(@Nullable String token) {
        return Optional.ofNullable(token)
                .filter(value -> !value.isBlank())
                .map(value -> new ObjectKey(new String(Base64.getUrlDecoder().decode(value), UTF_8)));
    }

    private static ObjectSummaryResponse toSummary(ObjectSummary summary) {
        return new ObjectSummaryResponse(
                summary.key().value(), summary.size(), summary.etag(), summary.contentType(), summary.createdAt());
    }

    private static String decodeValue(String header) {
        int separator = header.indexOf('=');
        return separator < 0
                ? header
                : header.substring(0, separator + 1) + UriUtils.decode(header.substring(separator + 1), UTF_8);
    }
}
