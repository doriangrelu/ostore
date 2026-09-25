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
package io.github.doriangrelu.ostore.driver.spi;

import java.util.Map;

/**
 * Fabrique d'instances d'un type de driver, découverte par {@link java.util.ServiceLoader}.
 *
 * <p>Déclarée dans {@code META-INF/services/io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory}.
 * Le serveur crée une instance par entrée {@code ostore.storage.drivers.<id>} dont le {@code type}
 * correspond à {@link #type()}.
 */
public interface StorageDriverFactory {

    /** Type de driver (ex. {@code filesystem}, {@code s3}). */
    String type();

    /**
     * Crée une instance configurée.
     *
     * @param id identifiant de l'instance
     * @param properties propriétés propres au type (ex. {@code root} pour {@code filesystem})
     * @throws IllegalArgumentException si une propriété est absente ou invalide
     */
    StorageDriver create(String id, Map<String, String> properties);
}
