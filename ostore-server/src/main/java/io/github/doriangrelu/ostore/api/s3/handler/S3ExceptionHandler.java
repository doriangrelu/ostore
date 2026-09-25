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
package io.github.doriangrelu.ostore.api.s3.handler;

import io.github.doriangrelu.ostore.api.s3.controller.S3BucketController;
import io.github.doriangrelu.ostore.api.s3.document.ErrorDocument;
import io.github.doriangrelu.ostore.api.s3.serializer.S3XmlSerializer;
import io.github.doriangrelu.ostore.domain.exception.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.DomainException;
import io.github.doriangrelu.ostore.domain.exception.InvalidBucketNameException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs en réponses d'erreur S3 (XML {@code <Error>}) avec les codes AWS, que les SDK
 * convertissent en exceptions typées (ex. {@code NoSuchBucketException}).
 */
@RestControllerAdvice(basePackageClasses = S3BucketController.class)
public class S3ExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(S3ExceptionHandler.class);

    private final S3XmlSerializer xml;

    public S3ExceptionHandler(S3XmlSerializer xml) {
        this.xml = xml;
    }

    /** Code et statut S3 d'une erreur. */
    private record S3ErrorCode(HttpStatus status, String code) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<String> handle(DomainException exception, HttpServletRequest request) {
        var error =
                switch (exception) {
                    case InvalidBucketNameException _ -> new S3ErrorCode(HttpStatus.BAD_REQUEST, "InvalidBucketName");
                    case BucketNotFoundException _ -> new S3ErrorCode(HttpStatus.NOT_FOUND, "NoSuchBucket");
                    case BucketAlreadyExistsException _ ->
                        new S3ErrorCode(HttpStatus.CONFLICT, "BucketAlreadyOwnedByYou");
                    default -> new S3ErrorCode(HttpStatus.INTERNAL_SERVER_ERROR, "InternalError");
                };
        return error(error, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<String> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOG.error("Unexpected error on S3 request {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(
                new S3ErrorCode(HttpStatus.INTERNAL_SERVER_ERROR, "InternalError"),
                "We encountered an internal error. Please try again.",
                request);
    }

    private ResponseEntity<String> error(S3ErrorCode error, String message, HttpServletRequest request) {
        var body = new ErrorDocument(
                error.code(),
                message,
                request.getRequestURI(),
                UUID.randomUUID().toString());
        return xml.response(error.status(), body);
    }
}
