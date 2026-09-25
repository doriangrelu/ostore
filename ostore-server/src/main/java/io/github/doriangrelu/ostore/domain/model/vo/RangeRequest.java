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

import io.github.doriangrelu.ostore.domain.exception.RangeNotSatisfiableException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Plage d'octets demandée par un client, au sens de l'en-tête HTTP {@code Range} (RFC 9110), limitée à une
 * seule plage.
 *
 * <p>Formes acceptées : {@code bytes=a-b}, {@code bytes=a-} (jusqu'à la fin), {@code bytes=-n} (les
 * {@code n} derniers octets).
 */
public sealed interface RangeRequest {

    /** Plage {@code first-last} ; {@code last} absent = jusqu'à la fin. */
    record FromTo(long first, Optional<Long> last) implements RangeRequest {}

    /** Les {@code length} derniers octets. */
    record Suffix(long length) implements RangeRequest {}

    /** Plage résolue sur un contenu : octets {@code [start, end]}, bornes incluses. */
    record Resolved(long start, long end, long totalSize) {

        public long length() {
            return end - start + 1;
        }
    }

    Pattern SINGLE_RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    /**
     * Interprète un en-tête {@code Range}. Un en-tête absent, multi-plages ou d'une unité inconnue est
     * ignoré (contenu complet), comme la RFC 9110 l'autorise.
     */
    static Optional<RangeRequest> parse(String header) {
        if (header == null) {
            return Optional.empty();
        }
        var matcher = SINGLE_RANGE.matcher(header.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        var first = matcher.group(1);
        var last = matcher.group(2);
        try {
            if (first.isEmpty()) {
                return last.isEmpty() ? Optional.empty() : Optional.of(new Suffix(Long.parseLong(last)));
            }
            var start = Long.parseLong(first);
            var end = last.isEmpty() ? Optional.<Long>empty() : Optional.of(Long.parseLong(last));
            return end.filter(e -> e < start).isPresent() ? Optional.empty() : Optional.of(new FromTo(start, end));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Résout la plage sur un contenu de {@code size} octets ; la fin est tronquée à la taille.
     *
     * @throws RangeNotSatisfiableException si la plage ne recouvre aucun octet
     */
    default Resolved resolve(long size) {
        return switch (this) {
            case FromTo(long first, Optional<Long> last)
            when first < size -> new Resolved(first, Math.min(last.orElse(size - 1), size - 1), size);
            case Suffix(long length)
            when length > 0 && size > 0 -> new Resolved(Math.max(0, size - length), size - 1, size);
            default -> throw new RangeNotSatisfiableException(size);
        };
    }
}
