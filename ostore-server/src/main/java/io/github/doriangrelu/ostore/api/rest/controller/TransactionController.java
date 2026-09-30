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

import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.fromContinuationToken;
import static io.github.doriangrelu.ostore.api.rest.mapper.ObjectDtoMapper.toListResponse;
import static io.github.doriangrelu.ostore.api.rest.mapper.TransactionDtoMapper.toResponse;

import io.github.doriangrelu.ostore.application.command.OpenTransactionCommand;
import io.github.doriangrelu.ostore.application.port.in.TransactionUseCases;
import io.github.doriangrelu.ostore.contract.api.TransactionApi;
import io.github.doriangrelu.ostore.contract.dto.ExtendTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.OpenTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.TransactionResponse;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

/** Adaptateur REST des transactions : mapping HTTP, validation et documentation viennent du contrat. */
@RestController
public class TransactionController implements TransactionApi {

    private final TransactionUseCases transactions;

    public TransactionController(TransactionUseCases transactions) {
        this.transactions = transactions;
    }

    @Override
    public TransactionResponse open(OpenTransactionRequest request) {
        return toResponse(transactions.open(new OpenTransactionCommand(request.ttl(), request.reference())));
    }

    @Override
    public TransactionResponse get(UUID id) {
        return toResponse(transactions.get(id));
    }

    @Override
    public ObjectListResponse objects(UUID id, int limit, @Nullable String continuationToken) {
        return toListResponse(
                null, null, transactions.listObjects(id, fromContinuationToken(continuationToken), limit));
    }

    @Override
    public TransactionResponse commit(UUID id) {
        return toResponse(transactions.commit(id));
    }

    @Override
    public TransactionResponse rollback(UUID id) {
        return toResponse(transactions.rollback(id));
    }

    @Override
    public TransactionResponse extend(UUID id, ExtendTransactionRequest request) {
        return toResponse(transactions.extend(id, request.ttl()));
    }
}
