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
package io.github.doriangrelu.ostore.infrastructure.config;

import io.github.doriangrelu.ostore.infrastructure.properties.PurgeProperties;
import io.github.doriangrelu.ostore.infrastructure.properties.StorageProperties;
import io.github.doriangrelu.ostore.infrastructure.storage.adapter.ConfiguredStorageDrivers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Drivers de stockage configurés et purge planifiée des blobs. Les tâches planifiées s'exécutent sur des
 * virtual threads ({@code spring.threads.virtual.enabled}).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({StorageProperties.class, PurgeProperties.class})
class StorageConfiguration {

    /** Fermé à l'arrêt de l'application (libère les clients des backends). */
    @Bean
    ConfiguredStorageDrivers storageDrivers(StorageProperties properties) {
        return new ConfiguredStorageDrivers(properties);
    }
}
