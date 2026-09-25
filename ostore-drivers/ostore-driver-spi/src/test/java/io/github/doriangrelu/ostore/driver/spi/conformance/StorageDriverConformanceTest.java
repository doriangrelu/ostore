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
package io.github.doriangrelu.ostore.driver.spi.conformance;

import static io.github.doriangrelu.ostore.driver.spi.testing.SyntheticContent.sha256;
import static io.github.doriangrelu.ostore.driver.spi.testing.SyntheticContent.slice;
import static io.github.doriangrelu.ostore.driver.spi.testing.SyntheticContent.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.exception.BlobNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.model.BlobKey;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Kit de conformité de la SPI (ADR-0007) : tout driver l'étend et fournit son instance. Un driver n'est
 * accepté que si ces scénarios passent.
 */
public abstract class StorageDriverConformanceTest {

    private static final long SMALL = 1_000;
    private static final long LARGE = 64L * 1024 * 1024;

    /** Driver à éprouver, prêt à l'emploi. */
    protected abstract StorageDriver driver();

    @Test
    void should_read_back_written_content_entirely_and_by_range() throws IOException {
        var key = newKey();
        driver().write(key, stream(SMALL, 1), SMALL);

        assertThat(driver().exists(key)).isTrue();
        assertThat(sha256(driver().read(key, Optional.empty()))).isEqualTo(sha256(SMALL, 1));
        try (InputStream middle = driver().read(key, Optional.of(new ByteRange(100, 199)))) {
            assertThat(middle.readAllBytes()).isEqualTo(slice(1, 100, 199));
        }
        try (InputStream lastByte = driver().read(key, Optional.of(new ByteRange(SMALL - 1, SMALL - 1)))) {
            assertThat(lastByte.readAllBytes()).isEqualTo(slice(1, SMALL - 1, SMALL - 1));
        }
    }

    @Test
    void should_stream_a_large_blob() {
        var key = newKey();

        driver().write(key, stream(LARGE, 2), LARGE);

        assertThat(sha256(driver().read(key, Optional.empty()))).isEqualTo(sha256(LARGE, 2));
    }

    @Test
    void should_delete_idempotently_and_report_missing_blobs() {
        var key = newKey();
        driver().write(key, stream(SMALL, 3), SMALL);

        driver().delete(key);
        driver().delete(key);

        assertThat(driver().exists(key)).isFalse();
        assertThatThrownBy(() -> driver().read(key, Optional.empty())).isInstanceOf(BlobNotFoundException.class);
        assertThatThrownBy(() -> driver().read(newKey(), Optional.empty())).isInstanceOf(BlobNotFoundException.class);
    }

    private static BlobKey newKey() {
        return new BlobKey(UUID.randomUUID().toString());
    }
}
