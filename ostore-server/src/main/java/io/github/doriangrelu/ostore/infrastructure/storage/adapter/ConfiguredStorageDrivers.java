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
package io.github.doriangrelu.ostore.infrastructure.storage.adapter;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.domain.exception.StorageDriverNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory;
import io.github.doriangrelu.ostore.driver.spi.layout.BlobPathLayout;
import io.github.doriangrelu.ostore.infrastructure.properties.StorageProperties;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Instances de drivers créées au démarrage à partir de {@code ostore.storage}.
 *
 * <p>Types de drivers et stratégies de chemins sont découverts par {@link ServiceLoader} : un driver ou une
 * stratégie tiers s'ajoute simplement au classpath. Toute incohérence de configuration (type ou stratégie
 * inconnus, driver par défaut absent) fait échouer le démarrage.
 */
public final class ConfiguredStorageDrivers implements StorageDrivers, AutoCloseable {

    private final Map<String, Instance> instances;

    public ConfiguredStorageDrivers(StorageProperties properties) {
        var classLoader = getClass().getClassLoader();
        var factories = ServiceLoader.load(StorageDriverFactory.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .collect(toMap(StorageDriverFactory::type, identity()));
        var layouts = ServiceLoader.load(BlobPathLayout.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .collect(toMap(BlobPathLayout::name, identity()));
        this.instances = properties.drivers().entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey, entry -> create(entry.getKey(), entry.getValue(), factories, layouts)));
        if (!instances.containsKey(properties.defaultDriver())) {
            throw new IllegalStateException(
                    "ostore.storage.default-driver '%s' is not configured".formatted(properties.defaultDriver()));
        }
    }

    @Override
    public StorageDriver driver(String driverId) {
        return instance(driverId).driver();
    }

    @Override
    public BlobPathLayout layout(String driverId) {
        return instance(driverId).layout();
    }

    @Override
    public void close() throws Exception {
        for (var instance : instances.values()) {
            if (instance.driver() instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }

    private Instance instance(String driverId) {
        return Optional.ofNullable(instances.get(driverId))
                .orElseThrow(() -> new StorageDriverNotFoundException(driverId));
    }

    private static Instance create(
            String id,
            StorageProperties.Driver config,
            Map<String, StorageDriverFactory> factories,
            Map<String, BlobPathLayout> layouts) {
        var factory = Optional.ofNullable(factories.get(config.type()))
                .orElseThrow(() -> new IllegalStateException("Storage driver '%s': unknown type '%s' (available: %s)"
                        .formatted(id, config.type(), factories.keySet())));
        var driver = factory.create(id, config.properties());
        var layoutName = Optional.ofNullable(config.layout()).orElseGet(driver::defaultLayout);
        var layout = Optional.ofNullable(layouts.get(layoutName))
                .orElseThrow(() -> new IllegalStateException("Storage driver '%s': unknown layout '%s' (available: %s)"
                        .formatted(id, layoutName, layouts.keySet())));
        return new Instance(driver, layout);
    }

    /** Driver configuré et sa stratégie de chemins. */
    private record Instance(StorageDriver driver, BlobPathLayout layout) {}
}
