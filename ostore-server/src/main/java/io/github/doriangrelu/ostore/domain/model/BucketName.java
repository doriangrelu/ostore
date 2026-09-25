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
package io.github.doriangrelu.ostore.domain.model;

import io.github.doriangrelu.ostore.domain.exception.InvalidBucketNameException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Nom de bucket, valide selon les
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">règles de
 * nommage S3</a>.
 *
 * <p>Un nom fait 3 à 63 caractères (minuscules, chiffres, points, tirets). Il commence et finit par une
 * lettre ou un chiffre, ne contient pas deux points consécutifs, n'a pas la forme d'une adresse IP et
 * n'utilise aucun préfixe ni suffixe réservé par AWS.
 *
 * @param value nom brut, déjà validé
 * @throws InvalidBucketNameException si le nom ne respecte pas ces règles
 */
public record BucketName(String value) implements Comparable<BucketName> {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]");
    private static final Pattern IP_ADDRESS = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final List<String> RESERVED_PREFIXES = List.of("xn--", "sthree-", "amzn-s3-demo-");
    private static final List<String> RESERVED_SUFFIXES =
            List.of("-s3alias", "--ol-s3", ".mrap", "--x-s3", "--table-s3");

    public BucketName {
        Objects.requireNonNull(value, "value");
        if (!isValid(value)) {
            throw new InvalidBucketNameException(value);
        }
    }

    private static boolean isValid(String name) {
        return FORMAT.matcher(name).matches()
                && !name.contains("..")
                && !IP_ADDRESS.matcher(name).matches()
                && RESERVED_PREFIXES.stream().noneMatch(name::startsWith)
                && RESERVED_SUFFIXES.stream().noneMatch(name::endsWith);
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
