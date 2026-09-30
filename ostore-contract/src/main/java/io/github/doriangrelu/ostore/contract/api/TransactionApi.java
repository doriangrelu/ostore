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
package io.github.doriangrelu.ostore.contract.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.github.doriangrelu.ostore.contract.constant.ApiPaths;
import io.github.doriangrelu.ostore.contract.dto.ExtendTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.OpenTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ressource REST des transactions (ADR-0016).
 *
 * <p>Une transaction regroupe des écritures d'objets en attente ({@code X-OStore-Transaction-Id} sur les
 * dépôts, copies et remplacements) : elles ne deviennent effectives qu'au commit ; au rollback ou à l'échéance,
 * elles sont supprimées. Commit et rollback sont idempotents.
 */
@Tag(name = "Transactions", description = "Pending writes, committed or discarded as a whole")
public interface TransactionApi {

    /** Ouvre une transaction. */
    @Operation(summary = "Open a transaction")
    @ApiResponse(responseCode = "201", description = "Transaction opened")
    @ApiResponse(responseCode = "400", description = "INVALID_TTL")
    @PostMapping(path = ApiPaths.TRANSACTIONS, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse open(@Valid @RequestBody OpenTransactionRequest request);

    /** Lit une transaction. */
    @Operation(summary = "Get a transaction")
    @ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND")
    @GetMapping(path = ApiPaths.TRANSACTION, produces = APPLICATION_JSON_VALUE)
    TransactionResponse get(@PathVariable("id") UUID id);

    /** Liste les objets d'une transaction (tous buckets), pour inspection avant validation. */
    @Operation(summary = "List the objects written in a transaction")
    @ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND")
    @GetMapping(path = ApiPaths.TRANSACTION_OBJECTS, produces = APPLICATION_JSON_VALUE)
    ObjectListResponse objects(
            @PathVariable("id") UUID id,
            @Parameter(description = "Page size (1-1000)")
                    @RequestParam(value = "limit", defaultValue = "1000")
                    @Min(1)
                    @Max(1000)
                    int limit,
            @Parameter(description = "Token returned by the previous page")
                    @RequestParam(value = "continuationToken", required = false)
                    @Nullable
                    String continuationToken);

    /** Valide la transaction : ses écritures deviennent effectives, atomiquement. */
    @Operation(summary = "Commit a transaction (idempotent)")
    @ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRANSACTION_CLOSED (rolled back or expired)")
    @PostMapping(path = ApiPaths.TRANSACTION_COMMIT, produces = APPLICATION_JSON_VALUE)
    TransactionResponse commit(@PathVariable("id") UUID id);

    /** Annule la transaction : ses écritures en attente sont supprimées. */
    @Operation(summary = "Roll back a transaction (idempotent)")
    @ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRANSACTION_CLOSED (committed)")
    @PostMapping(path = ApiPaths.TRANSACTION_ROLLBACK, produces = APPLICATION_JSON_VALUE)
    TransactionResponse rollback(@PathVariable("id") UUID id);

    /** Repousse l'échéance d'une transaction ouverte. */
    @Operation(summary = "Extend a transaction lifetime")
    @ApiResponse(responseCode = "400", description = "INVALID_TTL (beyond the maximum lifetime)")
    @ApiResponse(responseCode = "404", description = "TRANSACTION_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "TRANSACTION_CLOSED")
    @PostMapping(
            path = ApiPaths.TRANSACTION_EXTEND,
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE)
    TransactionResponse extend(@PathVariable("id") UUID id, @Valid @RequestBody ExtendTransactionRequest request);
}
