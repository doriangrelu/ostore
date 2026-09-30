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
package io.github.doriangrelu.ostore.domain.model.vo;

import io.github.doriangrelu.ostore.domain.exception.InvalidObjectNameException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Nom libre d'un objet (ex. nom de fichier) : 1 à 1024 octets UTF-8, sans caractère de contrôle.
 *
 * <p>Le nom est <b>descriptif</b> : il n'est pas unique et ne désigne pas l'objet, toujours adressé par son
 * identifiant (ADR-0015). Il sert au filtrage des listes et au nom de fichier proposé au téléchargement.
 *
 * @param value nom brut, déjà validé
 * @throws InvalidObjectNameException si le nom ne respecte pas ces règles
 */
public record ObjectName(String value) {

    /** Taille maximale en octets UTF-8. */
    public static final int MAX_BYTES = 1024;

    public ObjectName {
        Objects.requireNonNull(value, "value");
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        if (bytes == 0 || bytes > MAX_BYTES || value.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidObjectNameException(value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
