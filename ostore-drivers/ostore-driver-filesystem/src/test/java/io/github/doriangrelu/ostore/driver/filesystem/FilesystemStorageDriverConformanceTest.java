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
package io.github.doriangrelu.ostore.driver.filesystem;

import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.conformance.StorageDriverConformanceTest;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/** Kit de conformité de la SPI appliqué au driver {@code filesystem}. */
class FilesystemStorageDriverConformanceTest extends StorageDriverConformanceTest {

    @TempDir
    private Path root;

    private StorageDriver driver;

    @BeforeEach
    void createDriver() {
        driver = new FilesystemStorageDriver("test", root);
    }

    @Override
    protected StorageDriver driver() {
        return driver;
    }
}
