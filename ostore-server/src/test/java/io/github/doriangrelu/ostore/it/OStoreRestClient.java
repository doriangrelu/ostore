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
package io.github.doriangrelu.ostore.it;

import io.github.doriangrelu.ostore.contract.ApiPaths;
import io.github.doriangrelu.ostore.contract.bucket.BucketApi;
import io.github.doriangrelu.ostore.contract.bucket.BucketListResponse;
import io.github.doriangrelu.ostore.contract.bucket.BucketResponse;
import io.github.doriangrelu.ostore.contract.bucket.CreateBucketRequest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Client REST de test qui implémente les interfaces du contrat, comme le ferait un client tiers.
 *
 * <p>Une erreur HTTP lève une {@code HttpClientErrorException} (voir {@link IntegrationScenario#problemOf}).
 */
public final class OStoreRestClient implements BucketApi {

    private final RestClient http;

    OStoreRestClient(String baseUrl) {
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public BucketResponse create(CreateBucketRequest request) {
        return http.post()
                .uri(ApiPaths.BUCKETS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(BucketResponse.class);
    }

    @Override
    public BucketListResponse list() {
        return http.get().uri(ApiPaths.BUCKETS).retrieve().body(BucketListResponse.class);
    }

    @Override
    public BucketResponse get(String name) {
        return http.get().uri(ApiPaths.BUCKETS + "/{name}", name).retrieve().body(BucketResponse.class);
    }

    @Override
    public void delete(String name) {
        http.delete().uri(ApiPaths.BUCKETS + "/{name}", name).retrieve().toBodilessEntity();
    }

    /** Document OpenAPI publié par le serveur. */
    public String openApiDocument() {
        return http.get().uri("/v3/api-docs").retrieve().body(String.class);
    }
}
