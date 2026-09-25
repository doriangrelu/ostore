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

/**
 * Plage d'octets résolue, bornes incluses, comprise dans le blob.
 *
 * @param start premier octet (à partir de 0)
 * @param end dernier octet, inclus
 */
public record ByteRange(long start, long end) {

    public ByteRange {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid byte range: %d-%d".formatted(start, end));
        }
    }

    /** Nombre d'octets de la plage. */
    public long length() {
        return end - start + 1;
    }
}
