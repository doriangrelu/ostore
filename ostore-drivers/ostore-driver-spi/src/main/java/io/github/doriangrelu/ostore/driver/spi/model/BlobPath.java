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
package io.github.doriangrelu.ostore.driver.spi.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Chemin relatif d'un blob sur son support, calculé par une {@link
 * io.github.doriangrelu.ostore.driver.spi.layout.BlobPathLayout} et <b>stocké en base</b> avec l'objet.
 *
 * <p>Le driver range le blob exactement à ce chemin, sous sa racine (répertoire, préfixe S3…). Segments
 * séparés par {@code /}, chacun en {@code [A-Za-z0-9._-]}, sans segment {@code .} ni {@code ..} : le
 * chemin est sûr comme chemin de fichier et comme clé d'objet, et ne peut pas sortir de la racine.
 *
 * @param value chemin relatif (512 caractères au plus), ex. {@code 2026/09/25/01a0d8ba-…}
 */
public record BlobPath(String value) {

    /** Longueur maximale. */
    public static final int MAX_LENGTH = 512;

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");

    public BlobPath {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()
                || value.length() > MAX_LENGTH
                || !Arrays.stream(value.split("/", -1)).allMatch(BlobPath::isSafeSegment)) {
            throw new IllegalArgumentException("Invalid blob path: " + value);
        }
    }

    /** Dernier segment du chemin (nom du blob). */
    public String fileName() {
        return value.substring(value.lastIndexOf('/') + 1);
    }

    private static boolean isSafeSegment(String segment) {
        return SEGMENT.matcher(segment).matches() && !segment.equals(".") && !segment.equals("..");
    }

    @Override
    public String toString() {
        return value;
    }
}
