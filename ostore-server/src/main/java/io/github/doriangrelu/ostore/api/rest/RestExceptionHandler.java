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
package io.github.doriangrelu.ostore.api.rest;

import io.github.doriangrelu.ostore.contract.ErrorCode;
import io.github.doriangrelu.ostore.domain.DomainException;
import io.github.doriangrelu.ostore.domain.bucket.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.bucket.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.bucket.InvalidBucketNameException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier en {@link ProblemDetail} (RFC 9457) portant un {@link ErrorCode}.
 *
 * <p>Limité aux contrôleurs REST : l'API S3 a son propre format d'erreur.
 */
@RestControllerAdvice(basePackageClasses = RestExceptionHandler.class)
class RestExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handle(DomainException exception) {
        var code = codeOf(exception);
        var problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(code.httpStatus()), exception.getMessage());
        problem.setProperty(ErrorCode.PROPERTY, code.name());
        return ResponseEntity.of(problem).build();
    }

    private static ErrorCode codeOf(DomainException exception) {
        return switch (exception) {
            case InvalidBucketNameException _ -> ErrorCode.INVALID_BUCKET_NAME;
            case BucketNotFoundException _ -> ErrorCode.BUCKET_NOT_FOUND;
            case BucketAlreadyExistsException _ -> ErrorCode.BUCKET_ALREADY_EXISTS;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
