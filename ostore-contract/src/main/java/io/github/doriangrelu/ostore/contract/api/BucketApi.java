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
import io.github.doriangrelu.ostore.contract.dto.BucketListResponse;
import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ressource REST des buckets.
 *
 * <p>Implémentée telle quelle par le serveur, et utilisable directement pour construire un client
 * (Spring Cloud OpenFeign, OpenFeign avec {@code feign-spring}, générateur OpenAPI…). Les erreurs sont
 * des {@code ProblemDetail} portant un {@link io.github.doriangrelu.ostore.contract.error.ErrorCode}.
 */
@Tag(name = "Buckets", description = "Logical containers of objects, also exposed through the S3 API")
@RequestMapping(path = ApiPaths.BUCKETS, produces = APPLICATION_JSON_VALUE)
public interface BucketApi {

    /** Crée un bucket. */
    @Operation(summary = "Create a bucket")
    @ApiResponse(responseCode = "201", description = "Bucket created")
    @ApiResponse(responseCode = "400", description = "INVALID_BUCKET_NAME or invalid request")
    @ApiResponse(responseCode = "409", description = "BUCKET_ALREADY_EXISTS")
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    BucketResponse create(@Valid @RequestBody CreateBucketRequest request);

    /** Liste les buckets, triés par nom. */
    @Operation(summary = "List buckets")
    @GetMapping
    BucketListResponse list();

    /** Retourne un bucket par son nom. */
    @Operation(summary = "Get a bucket")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @GetMapping("/{name}")
    BucketResponse get(@Parameter(description = "Bucket name") @PathVariable("name") String name);

    /** Supprime un bucket. */
    @Operation(summary = "Delete a bucket")
    @ApiResponse(responseCode = "204", description = "Bucket deleted")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(description = "Bucket name") @PathVariable("name") String name);
}
