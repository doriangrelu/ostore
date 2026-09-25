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
package io.github.doriangrelu.ostore.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.doriangrelu.ostore.driver.spi.testing.SyntheticContent;
import io.github.doriangrelu.ostore.it.scenario.ObjectScenarios;
import io.github.doriangrelu.ostore.it.support.PostgresIntegrationTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;

class ObjectPostgresIT extends PostgresIntegrationTest implements ObjectScenarios {

    private static final long THREE_GIB = 3L * 1024 * 1024 * 1024;

    /**
     * Exigence ADR-0003 : un fichier de plusieurs Go traverse OStore (dépôt puis lecture) avec un tas JVM bien
     * plus petit que le fichier (voir {@code -Xmx} des TI dans le POM parent). Client et serveur partagent ce
     * tas : tout chargement en mémoire ferait échouer le test.
     */
    @Test
    void should_stream_a_multi_gigabyte_object_with_bounded_memory() throws IOException {
        assertThat(Runtime.getRuntime().maxMemory()).isLessThan(THREE_GIB / 4);
        var bucket = newBucket();

        var stored = objects()
                .put(
                        bucket,
                        "backups/huge.bin",
                        THREE_GIB,
                        null,
                        null,
                        new InputStreamResource(SyntheticContent.stream(THREE_GIB, 42)));

        assertThat(stored.size()).isEqualTo(THREE_GIB);
        var content = objects().content(bucket, "backups/huge.bin", null);
        assertThat(SyntheticContent.sha256(content.getBody().getInputStream()))
                .isEqualTo(SyntheticContent.sha256(THREE_GIB, 42));
        objects().delete(bucket, "backups/huge.bin");
    }
}
