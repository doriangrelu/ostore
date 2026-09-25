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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.driver.spi.layout.DateBlobPathLayout;
import io.github.doriangrelu.ostore.driver.spi.layout.HashedBlobPathLayout;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sûreté des chemins de blob et stratégies de chemins : règles pures (ADR-0011). */
class BlobPathTest {

    private static final UUID ID = UUID.fromString("01a0d8ba-a089-7c47-bfec-781cc0025098");

    @ParameterizedTest
    @ValueSource(strings = {"blob", "2026/09/25/01a0d8ba-a089", "a/b.c/d_e-f"})
    void should_accept_safe_relative_paths(String path) {
        assertThatCode(() -> new BlobPath(path)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", "/absolute", "trailing/", "a//b", "../escape", "a/../b", ".", "with space", "back\\slash"})
    void should_reject_paths_that_could_escape_or_break_the_root(String path) {
        assertThatThrownBy(() -> new BlobPath(path)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_organize_paths_by_utc_day_or_by_hash() {
        // 23h30 à UTC-2 = le lendemain en UTC : le répertoire suit la date UTC.
        var createdAt = OffsetDateTime.parse("2026-09-25T23:30:00-02:00").toInstant();

        assertThat(new DateBlobPathLayout().pathOf(ID, createdAt).value())
                .isEqualTo("2026/09/26/01a0d8ba-a089-7c47-bfec-781cc0025098");
        assertThat(new HashedBlobPathLayout().pathOf(ID, createdAt).value())
                .isEqualTo("98/50/01a0d8ba-a089-7c47-bfec-781cc0025098");
    }
}
