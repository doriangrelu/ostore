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

/** Le nom d'objet est vide, trop long (plus de 1024 octets UTF-8) ou contient un caractère de contrôle. */
public final class InvalidObjectNameException extends DomainException {

    public InvalidObjectNameException(String name) {
        super("Invalid object name: '%s'".formatted(name));
    }
}
