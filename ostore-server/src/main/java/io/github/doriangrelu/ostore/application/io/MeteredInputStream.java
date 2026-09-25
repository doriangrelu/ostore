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
package io.github.doriangrelu.ostore.application.io;

import io.github.doriangrelu.ostore.domain.exception.ContentLengthMismatchException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Flux qui compte les octets lus et calcule leur MD5 (ETag) au passage, sans rien mettre en mémoire.
 *
 * <p>La fermeture n'est <b>pas</b> propagée : le flux d'origine (requête HTTP) reste sous le contrôle de
 * l'appelant, même si un driver ferme ce qu'on lui confie.
 */
public final class MeteredInputStream extends FilterInputStream {

    private final MessageDigest md5;
    private long count;

    public MeteredInputStream(InputStream in) {
        super(in);
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is required by the Java platform", e);
        }
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value >= 0) {
            md5.update((byte) value);
            count++;
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            md5.update(buffer, offset, read);
            count += read;
        }
        return read;
    }

    @Override
    public long skip(long n) {
        return 0; // Sauter des octets fausserait l'empreinte : on lit tout.
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() {
        // Volontairement sans effet (voir la Javadoc de la classe).
    }

    /** Nombre d'octets lus. */
    public long count() {
        return count;
    }

    /** Empreinte MD5 hexadécimale des octets lus ; à n'appeler qu'une fois la lecture terminée. */
    public String md5Hex() {
        return HexFormat.of().formatHex(md5.digest());
    }

    /**
     * Vérifie que exactement {@code expected} octets ont été lus et que le flux d'origine est épuisé.
     *
     * @throws ContentLengthMismatchException sinon
     */
    public void requireExactly(long expected) {
        try {
            boolean exhausted = in.read() == -1;
            if (count != expected || !exhausted) {
                throw new ContentLengthMismatchException(expected, exhausted ? count : count + 1);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
