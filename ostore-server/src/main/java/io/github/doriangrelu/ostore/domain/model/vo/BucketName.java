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

import io.github.doriangrelu.ostore.domain.exception.InvalidBucketNameException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Nom de bucket, utilisable tel quel dans une URL, un nom DNS ou un chemin de fichier.
 *
 * <p>Un nom fait 3 à 63 caractères (minuscules, chiffres, points, tirets). Il commence et finit par une
 * lettre ou un chiffre, ne contient pas deux points consécutifs et n'a pas la forme d'une adresse IP.
 *
 * @param value nom brut, déjà validé
 */
public record BucketName(String value) implements Comparable<BucketName> {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]");
    private static final Pattern IP_ADDRESS = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    public BucketName {
        Objects.requireNonNull(value, "value");
        if (!isValid(value)) {
            throw new InvalidBucketNameException(value);
        }
    }

    private static boolean isValid(String name) {
        return FORMAT.matcher(name).matches()
                && !name.contains("..")
                && !IP_ADDRESS.matcher(name).matches();
    }

    @Override
    public int compareTo(BucketName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
