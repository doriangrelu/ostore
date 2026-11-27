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
package io.github.doriangrelu.ostore.it.support;

import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Fabrique du driver de test {@code gated} : mêmes propriétés que {@code filesystem}, dont le driver est retrouvé
 * par {@link ServiceLoader} (dépendance d'exécution du serveur).
 */
public final class GatedStorageDriverFactory implements StorageDriverFactory {

    public static final String TYPE = "gated";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public StorageDriver create(String id, Map<String, String> properties) {
        var filesystem = ServiceLoader.load(StorageDriverFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(factory -> factory.type().equals("filesystem"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("filesystem driver not on the classpath"));
        return new GatedStorageDriver(filesystem.create(id, properties));
    }
}
