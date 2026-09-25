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
import io.github.doriangrelu.ostore.contract.constant.OStoreHeaders;
import io.github.doriangrelu.ostore.contract.dto.CopyObjectRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectListResponse;
import io.github.doriangrelu.ostore.contract.dto.ObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ressource REST des objets d'un bucket (ADR-0015).
 *
 * <p>La clé d'un objet est passée en paramètre de requête {@code key} : elle peut contenir des {@code /} sans
 * aucun problème d'encodage, quel que soit le client. Les contenus transitent <b>en flux</b>
 * ({@code application/octet-stream} ou type réel), jamais chargés en mémoire. Le corps d'un dépôt est déclaré
 * {@link InputStreamResource} et non {@link Resource} : pour ce dernier type, Spring lirait tout le corps en
 * mémoire ({@code ResourceHttpMessageConverter}).
 */
@Tag(name = "Objects", description = "Binary objects stored in a bucket")
@RequestMapping(ApiPaths.OBJECTS)
public interface ObjectApi {

    /** Dépose un objet (création ou remplacement de la clé). */
    @Operation(
            summary = "Upload an object (create or replace)",
            description = "The body is streamed. Content-Length is required and checked. User metadata are sent"
                    + " as repeated X-OStore-Meta: name=value headers (percent-encoded values).",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            mediaType = "application/octet-stream",
                                            schema = @Schema(type = "string", format = "binary"))))
    @ApiResponse(responseCode = "201", description = "Object stored")
    @ApiResponse(responseCode = "400", description = "INVALID_OBJECT_KEY, INVALID_METADATA, CONTENT_LENGTH_MISMATCH")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CONCURRENT_UPDATE")
    @PutMapping(produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ObjectResponse put(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Object key") @RequestParam("key") String key,
            @RequestHeader(HttpHeaders.CONTENT_LENGTH) long contentLength,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) @Nullable String contentType,
            @Parameter(description = "User metadata, name=value (percent-encoded value), repeatable")
                    @RequestHeader(value = OStoreHeaders.META, required = false)
                    @Nullable
                    List<String> metadata,
            @RequestBody InputStreamResource content);

    /** Lit le contenu d'un objet, entier ou partiel ({@code Range}). */
    @Operation(summary = "Download an object content, entirely or partially (single Range)")
    @ApiResponse(responseCode = "200", description = "Whole content")
    @ApiResponse(responseCode = "206", description = "Requested range")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND, OBJECT_NOT_FOUND")
    @ApiResponse(responseCode = "416", description = "RANGE_NOT_SATISFIABLE")
    @GetMapping("/content")
    ResponseEntity<Resource> content(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Object key") @RequestParam("key") String key,
            @Parameter(description = "Single byte range, e.g. bytes=0-1023")
                    @RequestHeader(value = HttpHeaders.RANGE, required = false)
                    @Nullable
                    String range);

    /** Lit les métadonnées d'un objet. */
    @Operation(summary = "Get an object metadata")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND, OBJECT_NOT_FOUND")
    @GetMapping(path = "/metadata", produces = APPLICATION_JSON_VALUE)
    ObjectResponse metadata(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Object key") @RequestParam("key") String key);

    /** Liste les objets, en ordre binaire des clés, par pages. */
    @Operation(summary = "List objects by key prefix, paginated")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    ObjectListResponse list(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Key prefix") @RequestParam(value = "prefix", required = false) @Nullable
                    String prefix,
            @Parameter(description = "Page size (1-1000)")
                    @RequestParam(value = "maxKeys", defaultValue = "1000")
                    @Min(1)
                    @Max(1000)
                    int maxKeys,
            @Parameter(description = "Token returned by the previous page")
                    @RequestParam(value = "continuationToken", required = false)
                    @Nullable
                    String continuationToken);

    /** Copie un objet vers une autre clé, éventuellement dans un autre bucket. */
    @Operation(summary = "Copy an object")
    @ApiResponse(responseCode = "201", description = "Copy stored")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND, OBJECT_NOT_FOUND")
    @PostMapping(path = "/copy", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ObjectResponse copy(@PathVariable("bucket") String bucket, @Valid @RequestBody CopyObjectRequest request);

    /** Supprime un objet ; son contenu est purgé ensuite. */
    @Operation(summary = "Delete an object")
    @ApiResponse(responseCode = "204", description = "Object deleted")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND, OBJECT_NOT_FOUND")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Object key") @RequestParam("key") String key);
}
