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
package io.github.doriangrelu.ostore.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Génération des identifiants techniques au format <b>UUID v7</b> (RFC 9562).
 *
 * <p>Les 48 premiers bits portent l'horodatage en millisecondes : les identifiants sont croissants dans
 * le temps, ce qui garde les index B-tree compacts, contrairement aux UUID v4 aléatoires.
 */
public final class Identifiers {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_RFC_9562 = 0x8000_0000_0000_0000L;

    private Identifiers() {}

    /** Nouvel identifiant horodaté à l'instant donné. */
    public static UUID newId(Instant now) {
        long timestampAndVersion = (now.toEpochMilli() << 16) | VERSION_7 | (RANDOM.nextInt() & 0x0FFFL);
        long variantAndRandom = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | VARIANT_RFC_9562;
        return new UUID(timestampAndVersion, variantAndRandom);
    }
}
