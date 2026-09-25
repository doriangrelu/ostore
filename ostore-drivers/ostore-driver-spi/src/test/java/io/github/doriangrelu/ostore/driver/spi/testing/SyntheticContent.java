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
package io.github.doriangrelu.ostore.driver.spi.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Contenu de test déterministe, généré à la volée : permet d'envoyer et de vérifier des blobs de
 * plusieurs Go sans jamais les tenir en mémoire.
 *
 * <p>L'octet à la position {@code p} ne dépend que de {@code (seed, p)} : une plage peut être vérifiée
 * sans relire le début du contenu.
 */
public final class SyntheticContent {

    private SyntheticContent() {}

    /** Flux de {@code size} octets pseudo-aléatoires. */
    public static InputStream stream(long size, long seed) {
        return new InputStream() {
            private long position;

            @Override
            public int read() {
                return position < size ? byteAt(seed, position++) : -1;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (position >= size) {
                    return -1;
                }
                int count = (int) Math.min(length, size - position);
                for (int i = 0; i < count; i++) {
                    buffer[offset + i] = (byte) byteAt(seed, position++);
                }
                return count;
            }
        };
    }

    /** Octets {@code [start, end]} (inclus) du contenu, pour vérifier une lecture partielle. */
    public static byte[] slice(long seed, long start, long end) {
        var bytes = new byte[Math.toIntExact(end - start + 1)];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) byteAt(seed, start + i);
        }
        return bytes;
    }

    /** Empreinte SHA-256 (hexadécimale) d'un flux, calculée en flux ; le flux est fermé. */
    public static String sha256(InputStream content) {
        try (content) {
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[64 * 1024];
            for (int read; (read = content.read(buffer)) != -1; ) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Empreinte SHA-256 du contenu synthétique, sans le matérialiser. */
    public static String sha256(long size, long seed) {
        return sha256(stream(size, seed));
    }

    private static int byteAt(long seed, long position) {
        // SplitMix64 : dispersion rapide et déterministe de (seed, position).
        long z = seed + position * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return (int) ((z ^ (z >>> 31)) & 0xFF);
    }
}
