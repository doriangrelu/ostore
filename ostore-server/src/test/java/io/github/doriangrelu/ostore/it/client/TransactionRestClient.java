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

import io.github.doriangrelu.ostore.contract.api.TransactionApi;
import io.github.doriangrelu.ostore.contract.constant.ApiPaths;
import io.github.doriangrelu.ostore.contract.dto.ExtendTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.OpenTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.TransactionResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Client REST de test des transactions, qui implémente l'interface du contrat. */
public final class TransactionRestClient implements TransactionApi {

    private final RestClient http;

    public TransactionRestClient(String baseUrl) {
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public TransactionResponse open(OpenTransactionRequest request) {
        return http.post()
                .uri(ApiPaths.TRANSACTIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TransactionResponse.class);
    }

    /** Raccourci : ouverture avec une durée de vie donnée, sans référence. */
    public TransactionResponse open(@Nullable Duration ttl) {
        return open(new OpenTransactionRequest(ttl, null));
    }

    @Override
    public TransactionResponse get(UUID id) {
        return http.get().uri(ApiPaths.TRANSACTION, id).retrieve().body(TransactionResponse.class);
    }

    @Override
    public ObjectListResponse objects(UUID id, int limit, @Nullable String continuationToken) {
        return http.get()
                .uri(builder -> {
                    var variables = new HashMap<String, Object>();
                    variables.put("id", id);
                    builder.path(ApiPaths.TRANSACTION_OBJECTS).queryParam("limit", limit);
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
    public TransactionResponse commit(UUID id) {
        return http.post().uri(ApiPaths.TRANSACTION_COMMIT, id).retrieve().body(TransactionResponse.class);
    }

    @Override
    public TransactionResponse rollback(UUID id) {
        return http.post().uri(ApiPaths.TRANSACTION_ROLLBACK, id).retrieve().body(TransactionResponse.class);
    }

    @Override
    public TransactionResponse extend(UUID id, ExtendTransactionRequest request) {
        return http.post()
                .uri(ApiPaths.TRANSACTION_EXTEND, id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TransactionResponse.class);
    }
}
