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
package io.github.doriangrelu.ostore.it.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.github.doriangrelu.ostore.contract.api.ObjectApi;
import io.github.doriangrelu.ostore.contract.constant.ApiPaths;
import io.github.doriangrelu.ostore.contract.constant.OStoreHeaders;
import io.github.doriangrelu.ostore.contract.dto.CopyObjectRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.ObjectResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Client REST de test des objets, qui implémente l'interface du contrat comme le ferait un client tiers.
 *
 * <p>Les contenus sont envoyés et reçus <b>en flux</b> (client HTTP du JDK, taille fixée par
 * {@code Content-Length}) : il sert à prouver qu'OStore traite des fichiers de plusieurs Go à mémoire bornée.
 */
public final class ObjectRestClient implements ObjectApi {

    private final RestClient http;

    public ObjectRestClient(String baseUrl) {
        var jdk = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdk))
                .build();
    }

    @Override
    public ObjectResponse put(
            String bucket,
            String key,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            InputStreamResource content) {
        return http.put()
                .uri(builder -> builder.path(ApiPaths.OBJECTS)
                        .queryParam("key", "{key}")
                        .build(bucket, key))
                .headers(headers -> {
                    headers.setContentLength(contentLength);
                    headers.set(
                            HttpHeaders.CONTENT_TYPE,
                            contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
                    if (metadata != null) {
                        headers.put(OStoreHeaders.META, metadata);
                    }
                })
                .accept(MediaType.APPLICATION_JSON)
                .body(content)
                .retrieve()
                .body(ObjectResponse.class);
    }

    /** Raccourci : dépôt d'un contenu en mémoire (petits fichiers de test). */
    public ObjectResponse put(String bucket, String key, String contentType, byte[] content, String... metadata) {
        return put(
                bucket,
                key,
                content.length,
                contentType,
                List.of(metadata),
                new InputStreamResource(new java.io.ByteArrayInputStream(content)));
    }

    @Override
    public ResponseEntity<Resource> content(String bucket, String key, @Nullable String range) {
        return http.get()
                .uri(builder -> builder.path(ApiPaths.OBJECTS + "/content")
                        .queryParam("key", "{key}")
                        .build(bucket, key))
                .headers(headers -> {
                    if (range != null) {
                        headers.set(HttpHeaders.RANGE, range);
                    }
                })
                // Flux laissé ouvert (close = false) : l'appelant lit le contenu puis le ferme.
                .exchange(
                        (request, response) -> {
                            if (response.getStatusCode().isError()) {
                                throw errorOf(response);
                            }
                            return ResponseEntity.status(response.getStatusCode())
                                    .headers(response.getHeaders())
                                    .body((Resource) new InputStreamResource(response.getBody()));
                        },
                        false);
    }

    /** Raccourci : contenu complet lu en mémoire (petits fichiers de test). */
    public byte[] read(String bucket, String key) {
        try (var body = content(bucket, key, null).getBody().getInputStream()) {
            return body.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ObjectResponse metadata(String bucket, String key) {
        return http.get()
                .uri(builder -> builder.path(ApiPaths.OBJECTS + "/metadata")
                        .queryParam("key", "{key}")
                        .build(bucket, key))
                .retrieve()
                .body(ObjectResponse.class);
    }

    @Override
    public ObjectListResponse list(
            String bucket, @Nullable String prefix, int maxKeys, @Nullable String continuationToken) {
        return http.get()
                .uri(builder -> {
                    var variables = new java.util.HashMap<String, Object>();
                    variables.put("bucket", bucket);
                    builder.path(ApiPaths.OBJECTS).queryParam("maxKeys", maxKeys);
                    if (prefix != null) {
                        builder.queryParam("prefix", "{prefix}");
                        variables.put("prefix", prefix);
                    }
                    if (continuationToken != null) {
                        builder.queryParam("continuationToken", "{token}");
                        variables.put("token", continuationToken);
                    }
                    return builder.build(variables);
                })
                .retrieve()
                .body(ObjectListResponse.class);
    }

    @Override
    public ObjectResponse copy(String bucket, CopyObjectRequest request) {
        return http.post()
                .uri(ApiPaths.OBJECTS + "/copy", bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ObjectResponse.class);
    }

    @Override
    public void delete(String bucket, String key) {
        http.delete()
                .uri(builder -> builder.path(ApiPaths.OBJECTS)
                        .queryParam("key", "{key}")
                        .build(bucket, key))
                .retrieve()
                .toBodilessEntity();
    }

    private static RestClientResponseException errorOf(ClientHttpResponse response) throws IOException {
        var status = response.getStatusCode();
        var body = response.getBody().readAllBytes();
        RestClientResponseException exception = status.is4xxClientError()
                ? HttpClientErrorException.create(status, status.toString(), response.getHeaders(), body, UTF_8)
                : HttpServerErrorException.create(status, status.toString(), response.getHeaders(), body, UTF_8);
        // Permet getResponseBodyAs(...) comme pour les erreurs levées par retrieve().
        var json = new JsonMapper();
        exception.setBodyConvertFunction(
                type -> json.readValue(body, json.getTypeFactory().constructType(type.getType())));
        return exception;
    }
}
