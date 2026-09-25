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
package io.github.doriangrelu.ostore.driver.filesystem.factory;

import io.github.doriangrelu.ostore.driver.filesystem.FilesystemStorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Fabrique du driver {@code filesystem}.
 *
 * <p>Propriété : {@code root} (obligatoire), répertoire racine des blobs, créé au besoin.
 */
public final class FilesystemStorageDriverFactory implements StorageDriverFactory {

    public static final String TYPE = "filesystem";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public StorageDriver create(String id, Map<String, String> properties) {
        var root = Optional.ofNullable(properties.get("root"))
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver '%s' (filesystem): property 'root' is required".formatted(id)));
        return new FilesystemStorageDriver(id, Path.of(root));
    }
}
