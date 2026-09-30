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
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ressource REST des objets (ADR-0015).
 *
 * <p>Un objet est désigné <b>uniquement par son identifiant</b> ({@code id}, UUID), renvoyé au dépôt. Son nom
 * est libre, optionnel et non unique : il sert au filtrage des listes et au téléchargement. Les contenus
 * transitent <b>en flux</b>, jamais chargés en mémoire ; le corps d'un dépôt est un {@link InputStreamResource}
 * (déclaré {@link Resource}, Spring le lirait entièrement en mémoire).
 */
@Tag(name = "Objects", description = "Binary objects, addressed by their identifier")
public interface ObjectApi {

    /** Dépose un nouvel objet dans un bucket. */
    @Operation(
            summary = "Upload a new object",
            description = "The body is streamed. Content-Length is required and checked. User metadata are sent as"
                    + " repeated X-OStore-Meta: name=value headers (percent-encoded values).",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            mediaType = "application/octet-stream",
                                            schema = @Schema(type = "string", format = "binary"))))
    @ApiResponse(responseCode = "201", description = "Object stored, identifier returned")
    @ApiResponse(responseCode = "400", description = "INVALID_OBJECT_NAME, INVALID_METADATA, CONTENT_LENGTH_MISMATCH")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @PostMapping(path = ApiPaths.BUCKET_OBJECTS, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ObjectResponse create(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Free, optional name (e.g. file name)")
                    @RequestParam(value = "name", required = false)
                    @Nullable
                    String name,
            @RequestHeader(HttpHeaders.CONTENT_LENGTH) long contentLength,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) @Nullable String contentType,
            @Parameter(description = "User metadata, name=value (percent-encoded value), repeatable")
                    @RequestHeader(value = OStoreHeaders.META, required = false)
                    @Nullable
                    List<String> metadata,
            @RequestBody InputStreamResource content);

    /** Liste les objets d'un bucket, par ordre de création. */
    @Operation(summary = "List the objects of a bucket, in creation order, optionally filtered by name prefix")
    @ApiResponse(responseCode = "404", description = "BUCKET_NOT_FOUND")
    @GetMapping(path = ApiPaths.BUCKET_OBJECTS, produces = APPLICATION_JSON_VALUE)
    ObjectListResponse list(
            @PathVariable("bucket") String bucket,
            @Parameter(description = "Name prefix") @RequestParam(value = "namePrefix", required = false) @Nullable
                    String namePrefix,
            @Parameter(description = "Page size (1-1000)")
                    @RequestParam(value = "limit", defaultValue = "1000")
                    @Min(1)
                    @Max(1000)
                    int limit,
            @Parameter(description = "Token returned by the previous page")
                    @RequestParam(value = "continuationToken", required = false)
                    @Nullable
                    String continuationToken);

    /** Lit les métadonnées d'un objet. */
    @Operation(summary = "Get an object metadata")
    @ApiResponse(responseCode = "404", description = "OBJECT_NOT_FOUND")
    @GetMapping(path = ApiPaths.OBJECT, produces = APPLICATION_JSON_VALUE)
    ObjectResponse metadata(@PathVariable("id") UUID id);

    /** Lit le contenu d'un objet, entier ou partiel ({@code Range}). */
    @Operation(summary = "Download an object content, entirely or partially (single Range)")
    @ApiResponse(responseCode = "200", description = "Whole content")
    @ApiResponse(responseCode = "206", description = "Requested range")
    @ApiResponse(responseCode = "404", description = "OBJECT_NOT_FOUND")
    @ApiResponse(responseCode = "416", description = "RANGE_NOT_SATISFIABLE")
    @GetMapping(ApiPaths.OBJECT_CONTENT)
    ResponseEntity<Resource> content(
            @PathVariable("id") UUID id,
            @Parameter(description = "Single byte range, e.g. bytes=0-1023")
                    @RequestHeader(value = HttpHeaders.RANGE, required = false)
                    @Nullable
                    String range);

    /** Remplace le contenu d'un objet ; son identifiant ne change pas. */
    @Operation(
            summary = "Replace an object content (same identifier)",
            description = "Content type and metadata are replaced too; the name is kept unless a new one is given.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            mediaType = "application/octet-stream",
                                            schema = @Schema(type = "string", format = "binary"))))
    @ApiResponse(responseCode = "200", description = "Content replaced")
    @ApiResponse(responseCode = "404", description = "OBJECT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "CONCURRENT_UPDATE")
    @PutMapping(path = ApiPaths.OBJECT_CONTENT, produces = APPLICATION_JSON_VALUE)
    ObjectResponse replace(
            @PathVariable("id") UUID id,
            @Parameter(description = "New name, current name kept when absent")
                    @RequestParam(value = "name", required = false)
                    @Nullable
                    String name,
            @RequestHeader(HttpHeaders.CONTENT_LENGTH) long contentLength,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) @Nullable String contentType,
            @RequestHeader(value = OStoreHeaders.META, required = false) @Nullable List<String> metadata,
            @RequestBody InputStreamResource content);

    /** Copie un objet en un nouvel objet, éventuellement dans un autre bucket. */
    @Operation(summary = "Copy an object into a new object")
    @ApiResponse(responseCode = "201", description = "Copy stored, new identifier returned")
    @ApiResponse(responseCode = "404", description = "OBJECT_NOT_FOUND, BUCKET_NOT_FOUND")
    @PostMapping(path = ApiPaths.OBJECT_COPY, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ObjectResponse copy(@PathVariable("id") UUID id, @Valid @RequestBody CopyObjectRequest request);

    /** Supprime un objet ; son contenu est purgé ensuite. */
    @Operation(summary = "Delete an object")
    @ApiResponse(responseCode = "204", description = "Object deleted")
    @ApiResponse(responseCode = "404", description = "OBJECT_NOT_FOUND")
    @DeleteMapping(ApiPaths.OBJECT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") UUID id);
}
