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
package io.github.doriangrelu.ostore.api.rest.handler;

import io.github.doriangrelu.ostore.api.rest.controller.BucketController;
import io.github.doriangrelu.ostore.contract.error.ErrorCode;
import io.github.doriangrelu.ostore.domain.exception.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.exception.BucketNotEmptyException;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.ConcurrentObjectUpdateException;
import io.github.doriangrelu.ostore.domain.exception.ContentLengthMismatchException;
import io.github.doriangrelu.ostore.domain.exception.DomainException;
import io.github.doriangrelu.ostore.domain.exception.InvalidBucketNameException;
import io.github.doriangrelu.ostore.domain.exception.InvalidMetadataException;
import io.github.doriangrelu.ostore.domain.exception.InvalidObjectKeyException;
import io.github.doriangrelu.ostore.domain.exception.ObjectNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.RangeNotSatisfiableException;
import io.github.doriangrelu.ostore.driver.spi.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier et de stockage en {@link ProblemDetail} (RFC 9457) portant un {@link ErrorCode}.
 *
 * <p>Limité aux contrôleurs REST.
 */
@RestControllerAdvice(basePackageClasses = BucketController.class)
public class RestExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handle(DomainException exception) {
        var response = ResponseEntity.status(codeOf(exception).httpStatus());
        if (exception instanceof RangeNotSatisfiableException unsatisfiable) {
            // RFC 9110 : une réponse 416 indique la taille réelle du contenu.
            response.header(HttpHeaders.CONTENT_RANGE, "bytes */" + unsatisfiable.size());
        }
        return response.body(problem(codeOf(exception), exception.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ProblemDetail> handle(StorageException exception) {
        LOG.error("Storage failure", exception);
        return ResponseEntity.internalServerError()
                .body(problem(ErrorCode.STORAGE_ERROR, "The storage backend failed, retry later"));
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(code.httpStatus()), detail);
        problem.setProperty(ErrorCode.PROPERTY, code.name());
        return problem;
    }

    private static ErrorCode codeOf(DomainException exception) {
        return switch (exception) {
            case InvalidBucketNameException _ -> ErrorCode.INVALID_BUCKET_NAME;
            case InvalidObjectKeyException _ -> ErrorCode.INVALID_OBJECT_KEY;
            case InvalidMetadataException _ -> ErrorCode.INVALID_METADATA;
            case ContentLengthMismatchException _ -> ErrorCode.CONTENT_LENGTH_MISMATCH;
            case BucketNotFoundException _ -> ErrorCode.BUCKET_NOT_FOUND;
            case ObjectNotFoundException _ -> ErrorCode.OBJECT_NOT_FOUND;
            case BucketAlreadyExistsException _ -> ErrorCode.BUCKET_ALREADY_EXISTS;
            case BucketNotEmptyException _ -> ErrorCode.BUCKET_NOT_EMPTY;
            case ConcurrentObjectUpdateException _ -> ErrorCode.CONCURRENT_UPDATE;
            case RangeNotSatisfiableException _ -> ErrorCode.RANGE_NOT_SATISFIABLE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
