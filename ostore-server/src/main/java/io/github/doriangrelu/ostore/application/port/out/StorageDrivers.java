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
package io.github.doriangrelu.ostore.application.port.out;

import io.github.doriangrelu.ostore.domain.exception.StorageDriverNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.layout.BlobPathLayout;

/** Instances de drivers configurées, avec leur stratégie de chemins. */
public interface StorageDrivers {

    /**
     * Driver d'une instance.
     *
     * @throws StorageDriverNotFoundException si l'instance n'est pas configurée
     */
    StorageDriver driver(String driverId);

    /**
     * Stratégie de chemins d'une instance : celle de la configuration, sinon celle recommandée par le driver.
     *
     * @throws StorageDriverNotFoundException si l'instance n'est pas configurée
     */
    BlobPathLayout layout(String driverId);
}
