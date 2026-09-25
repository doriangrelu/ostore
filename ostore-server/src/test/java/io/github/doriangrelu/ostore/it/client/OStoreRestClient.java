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

import io.github.doriangrelu.ostore.contract.api.BucketApi;
import io.github.doriangrelu.ostore.contract.constant.ApiPaths;
import io.github.doriangrelu.ostore.contract.dto.BucketListResponse;
import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Client REST de test qui implémente les interfaces du contrat, comme le ferait un client tiers.
 *
 * <p>Une erreur HTTP lève une {@code HttpClientErrorException} (voir {@code IntegrationScenario#problemOf}).
 */
public final class OStoreRestClient implements BucketApi {

    private final RestClient http;

    public OStoreRestClient(String baseUrl) {
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public @Nullable BucketResponse create(@NonNull CreateBucketRequest request) {
        return http.post()
                .uri(ApiPaths.BUCKETS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(BucketResponse.class);
    }

    @Override
    public @Nullable BucketListResponse list() {
        return http.get().uri(ApiPaths.BUCKETS).retrieve().body(BucketListResponse.class);
    }

    @Override
    public @Nullable BucketResponse get(@NonNull String name) {
        return http.get().uri(ApiPaths.BUCKETS + "/{name}", name).retrieve().body(BucketResponse.class);
    }

    @Override
    public void delete(@NonNull String name) {
        http.delete().uri(ApiPaths.BUCKETS + "/{name}", name).retrieve().toBodilessEntity();
    }

    /** Document OpenAPI publié par le serveur. */
    public String openApiDocument() {
        return http.get().uri("/v3/api-docs").retrieve().body(String.class);
    }
}
