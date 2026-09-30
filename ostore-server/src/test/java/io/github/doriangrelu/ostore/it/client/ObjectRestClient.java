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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
 * {@code Content-Length}) : il sert à prouver qu'OStore traite des fichiers de plusieurs Go à mémoire bornée. Des
 * raccourcis (contenu en mémoire, sans transaction) gardent les scénarios lisibles.
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
    public ObjectResponse create(
            String bucket,
            @Nullable String name,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            @Nullable UUID transactionId,
            @Nullable String pendingTtl,
            InputStreamResource content) {
        return http.post()
                .uri(builder -> {
                    var variables = new HashMap<String, Object>();
                    variables.put("bucket", bucket);
                    builder.path(ApiPaths.BUCKET_OBJECTS);
                    if (name != null) {
                        builder.queryParam("name", "{name}");
                        variables.put("name", name);
                    }
                    return builder.build(variables);
                })
                .headers(headers ->
                        writeHeaders(headers, contentLength, contentType, metadata, transactionId, pendingTtl))
                .accept(MediaType.APPLICATION_JSON)
                .body(content)
                .retrieve()
                .body(ObjectResponse.class);
    }

    /** Raccourci : dépôt en flux, hors transaction. */
    public ObjectResponse create(
            String bucket,
            @Nullable String name,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            InputStreamResource content) {
        return create(bucket, name, contentLength, contentType, metadata, null, null, content);
    }

    /** Raccourci : dépôt d'un contenu en mémoire, hors transaction. */
    public ObjectResponse create(
            String bucket, @Nullable String name, String contentType, byte[] content, String... metadata) {
        return create(bucket, name, content.length, contentType, List.of(metadata), null, null, inMemory(content));
    }

    /** Raccourci : dépôt d'un contenu en mémoire dans une transaction existante. */
    public ObjectResponse createIn(UUID transactionId, String bucket, @Nullable String name, byte[] content) {
        return create(bucket, name, content.length, "text/plain", null, transactionId, null, inMemory(content));
    }

    /** Raccourci : dépôt d'un contenu en mémoire dans une transaction créée pour lui (mode implicite). */
    public ObjectResponse createPending(String bucket, @Nullable String name, byte[] content, String ttl) {
        return create(bucket, name, content.length, "text/plain", null, null, ttl, inMemory(content));
    }

    @Override
    public ObjectListResponse list(
            String bucket, @Nullable String namePrefix, int limit, @Nullable String continuationToken) {
        return http.get()
                .uri(builder -> {
                    var variables = new HashMap<String, Object>();
                    variables.put("bucket", bucket);
                    builder.path(ApiPaths.BUCKET_OBJECTS).queryParam("limit", limit);
                    if (namePrefix != null) {
                        builder.queryParam("namePrefix", "{prefix}");
                        variables.put("prefix", namePrefix);
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
    public ObjectResponse metadata(UUID id, @Nullable UUID transactionId) {
        return http.get()
                .uri(ApiPaths.OBJECT, id)
                .headers(headers -> transactionHeader(headers, transactionId))
                .retrieve()
                .body(ObjectResponse.class);
    }

    /** Raccourci : métadonnées de la version validée. */
    public ObjectResponse metadata(UUID id) {
        return metadata(id, null);
    }

    @Override
    public ResponseEntity<Resource> content(UUID id, @Nullable String range, @Nullable UUID transactionId) {
        return http.get()
                .uri(ApiPaths.OBJECT_CONTENT, id)
                .headers(headers -> {
                    if (range != null) {
                        headers.set(HttpHeaders.RANGE, range);
                    }
                    transactionHeader(headers, transactionId);
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

    /** Raccourci : contenu de la version validée. */
    public ResponseEntity<Resource> content(UUID id, @Nullable String range) {
        return content(id, range, null);
    }

    /** Raccourci : contenu complet de la version validée, lu en mémoire. */
    public byte[] read(UUID id) {
        return read(id, null);
    }

    /** Raccourci : contenu complet, lu en mémoire ; avec une transaction, sa version en attente. */
    public byte[] read(UUID id, @Nullable UUID transactionId) {
        try (var body = content(id, null, transactionId).getBody().getInputStream()) {
            return body.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ObjectResponse replace(
            UUID id,
            @Nullable String name,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            @Nullable UUID transactionId,
            @Nullable String pendingTtl,
            InputStreamResource content) {
        return http.put()
                .uri(ApiPaths.OBJECT_CONTENT, id)
                .headers(headers ->
                        writeHeaders(headers, contentLength, contentType, metadata, transactionId, pendingTtl))
                .accept(MediaType.APPLICATION_JSON)
                .body(content)
                .retrieve()
                .body(ObjectResponse.class);
    }

    /** Raccourci : remplacement par un contenu en mémoire, hors transaction. */
    public ObjectResponse replace(UUID id, String contentType, byte[] content, String... metadata) {
        return replace(id, null, content.length, contentType, List.of(metadata), null, null, inMemory(content));
    }

    /** Raccourci : remplacement en attente dans une transaction existante. */
    public ObjectResponse replaceIn(UUID transactionId, UUID id, byte[] content) {
        return replace(id, null, content.length, "text/plain", null, transactionId, null, inMemory(content));
    }

    @Override
    public ObjectResponse copy(
            UUID id, @Nullable UUID transactionId, @Nullable String pendingTtl, CopyObjectRequest request) {
        return http.post()
                .uri(ApiPaths.OBJECT_COPY, id)
                .headers(headers -> {
                    transactionHeader(headers, transactionId);
                    if (pendingTtl != null) {
                        headers.set(OStoreHeaders.PENDING_TTL, pendingTtl);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ObjectResponse.class);
    }

    /** Raccourci : copie hors transaction. */
    public ObjectResponse copy(UUID id, CopyObjectRequest request) {
        return copy(id, null, null, request);
    }

    @Override
    public void delete(UUID id) {
        http.delete().uri(ApiPaths.OBJECT, id).retrieve().toBodilessEntity();
    }

    private static void writeHeaders(
            HttpHeaders headers,
            long contentLength,
            @Nullable String contentType,
            @Nullable List<String> metadata,
            @Nullable UUID transactionId,
            @Nullable String pendingTtl) {
        headers.setContentLength(contentLength);
        headers.set(
                HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (metadata != null && !metadata.isEmpty()) {
            headers.put(OStoreHeaders.META, metadata);
        }
        transactionHeader(headers, transactionId);
        if (pendingTtl != null) {
            headers.set(OStoreHeaders.PENDING_TTL, pendingTtl);
        }
    }

    private static void transactionHeader(HttpHeaders headers, @Nullable UUID transactionId) {
        if (transactionId != null) {
            headers.set(OStoreHeaders.TRANSACTION_ID, transactionId.toString());
        }
    }

    private static InputStreamResource inMemory(byte[] content) {
        return new InputStreamResource(new ByteArrayInputStream(content));
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
