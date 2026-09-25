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
package io.github.doriangrelu.ostore.api.rest.controller;

import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.contentHeaders;
import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.fromContinuationToken;
import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.toListResponse;
import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.toMetadata;
import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.toResponse;

import io.github.doriangrelu.ostore.application.command.CopyObjectCommand;
import io.github.doriangrelu.ostore.application.command.PutObjectCommand;
import io.github.doriangrelu.ostore.application.port.in.ObjectUseCases;
import io.github.doriangrelu.ostore.contract.api.ObjectApi;
import io.github.doriangrelu.ostore.contract.dto.CopyObjectRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.ObjectResponse;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.RangeRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur REST des objets : mapping HTTP, validation et documentation viennent du contrat.
 *
 * <p>Les contenus sont servis par un {@link InputStreamResource} dont la taille est fixée par l'en-tête
 * {@code Content-Length} : Spring les transmet en flux, sans lire le contenu pour en calculer la taille.
 */
@RestController
public class ObjectController implements ObjectApi {

    private final ObjectUseCases objects;

    public ObjectController(ObjectUseCases objects) {
        this.objects = objects;
    }

    @Override
    public ObjectResponse put(
            String bucket,
            String key,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            InputStreamResource content) {
        var bucketName = new BucketName(bucket);
        var command = new PutObjectCommand(
                bucketName,
                new ObjectKey(key),
                contentLength,
                Optional.ofNullable(contentType),
                toMetadata(metadata),
                open(content));
        return toResponse(bucketName, objects.put(command));
    }

    @Override
    public ResponseEntity<Resource> content(String bucket, String key, @Nullable String range) {
        var content = objects.open(new BucketName(bucket), new ObjectKey(key), RangeRequest.parse(range));
        var headers = contentHeaders(content.object());
        Resource body = new InputStreamResource(content.stream());
        return content.range()
                .map(served -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .header(
                                HttpHeaders.CONTENT_RANGE,
                                "bytes %d-%d/%d".formatted(served.start(), served.end(), served.totalSize()))
                        .contentLength(served.length())
                        .body(body))
                .orElseGet(() -> ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(content.object().size())
                        .body(body));
    }

    @Override
    public ObjectResponse metadata(String bucket, String key) {
        var bucketName = new BucketName(bucket);
        return toResponse(bucketName, objects.get(bucketName, new ObjectKey(key)));
    }

    @Override
    public ObjectListResponse list(
            String bucket, @Nullable String prefix, int maxKeys, @Nullable String continuationToken) {
        var bucketName = new BucketName(bucket);
        var effectivePrefix = Objects.requireNonNullElse(prefix, "");
        var page = objects.list(bucketName, effectivePrefix, fromContinuationToken(continuationToken), maxKeys);
        return toListResponse(bucketName, effectivePrefix, page);
    }

    @Override
    public ObjectResponse copy(String bucket, CopyObjectRequest request) {
        var source = new BucketName(bucket);
        var target =
                Optional.ofNullable(request.targetBucket()).map(BucketName::new).orElse(source);
        var command = new CopyObjectCommand(
                source, new ObjectKey(request.sourceKey()), target, new ObjectKey(request.targetKey()));
        return toResponse(target, objects.copy(command));
    }

    @Override
    public void delete(String bucket, String key) {
        objects.delete(new BucketName(bucket), new ObjectKey(key));
    }

    private static java.io.InputStream open(Resource content) {
        try {
            return content.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
