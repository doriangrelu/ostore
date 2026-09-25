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

import io.github.doriangrelu.ostore.domain.exception.InvalidMetadataException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Métadonnées utilisateur d'un objet : couples nom → valeur, triés par nom.
 *
 * <p>Noms : 1 à 64 caractères {@code [a-z0-9][a-z0-9_-]*}. Taille totale (noms + valeurs, en octets UTF-8)
 * limitée à {@value #MAX_TOTAL_BYTES} octets, comme S3.
 *
 * @param entries métadonnées, triées par nom
 * @throws InvalidMetadataException si une règle est violée
 */
public record ObjectMetadata(SortedMap<String, String> entries) {

    /** Taille totale maximale, en octets UTF-8. */
    public static final int MAX_TOTAL_BYTES = 2048;

    /** Aucune métadonnée. */
    public static final ObjectMetadata EMPTY = new ObjectMetadata(new TreeMap<>());

    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    public ObjectMetadata {
        entries.keySet().stream()
                .filter(name -> !NAME.matcher(name).matches())
                .findFirst()
                .ifPresent(name -> {
                    throw new InvalidMetadataException("invalid name '%s'".formatted(name));
                });
        int total = entries.entrySet().stream()
                .mapToInt(entry -> utf8Length(entry.getKey()) + utf8Length(entry.getValue()))
                .sum();
        if (total > MAX_TOTAL_BYTES) {
            throw new InvalidMetadataException("total size %d exceeds %d bytes".formatted(total, MAX_TOTAL_BYTES));
        }
        entries = new TreeMap<>(entries);
    }

    /** Métadonnées à partir d'un dictionnaire quelconque (les doublons sont impossibles). */
    public static ObjectMetadata of(Map<String, String> entries) {
        return new ObjectMetadata(new TreeMap<>(entries));
    }

    /**
     * Métadonnées à partir d'entrées {@code nom=valeur} déjà décodées.
     *
     * @throws InvalidMetadataException si une entrée est mal formée ou si un nom est répété
     */
    public static ObjectMetadata parse(Collection<String> nameValuePairs) {
        var entries = new TreeMap<String, String>();
        for (String pair : nameValuePairs) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                throw new InvalidMetadataException("expected 'name=value' but got '%s'".formatted(pair));
            }
            var name = pair.substring(0, separator).trim();
            if (entries.put(name, pair.substring(separator + 1)) != null) {
                throw new InvalidMetadataException("duplicate name '%s'".formatted(name));
            }
        }
        return new ObjectMetadata(entries);
    }

    @Override
    public SortedMap<String, String> entries() {
        return new TreeMap<>(entries);
    }

    private static int utf8Length(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }
}
