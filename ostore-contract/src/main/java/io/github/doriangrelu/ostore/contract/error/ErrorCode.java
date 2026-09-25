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
package io.github.doriangrelu.ostore.contract.error;

/**
 * Codes d'erreur métier de l'API REST.
 *
 * <p>Chaque réponse d'erreur est un {@code ProblemDetail} (RFC 9457) dont la propriété {@value #PROPERTY}
 * contient le nom de l'une de ces constantes : un client peut réagir au code sans analyser le message.
 */
public enum ErrorCode {
    INVALID_BUCKET_NAME(400),
    INVALID_OBJECT_KEY(400),
    INVALID_METADATA(400),
    /** Le nombre d'octets reçus diffère du {@code Content-Length} annoncé ; rien n'est enregistré. */
    CONTENT_LENGTH_MISMATCH(400),
    BUCKET_NOT_FOUND(404),
    OBJECT_NOT_FOUND(404),
    BUCKET_ALREADY_EXISTS(409),
    BUCKET_NOT_EMPTY(409),
    /** Écriture simultanée sur la même clé : la requête peut être rejouée. */
    CONCURRENT_UPDATE(409),
    RANGE_NOT_SATISFIABLE(416),
    /** Le support de stockage (disque, backend S3…) a échoué. */
    STORAGE_ERROR(500),
    INTERNAL_ERROR(500);

    /** Nom de la propriété du {@code ProblemDetail} qui porte le code. */
    public static final String PROPERTY = "code";

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /** Statut HTTP associé au code. */
    public int httpStatus() {
        return httpStatus;
    }
}
