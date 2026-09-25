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
package io.github.doriangrelu.ostore.domain.exception;

/** Le nombre d'octets reçus ne correspond pas au {@code Content-Length} annoncé : le contenu est rejeté. */
public final class ContentLengthMismatchException extends DomainException {

    public ContentLengthMismatchException(long expected, long received) {
        super("Content length mismatch: %d bytes announced, %d received".formatted(expected, received));
    }
}
