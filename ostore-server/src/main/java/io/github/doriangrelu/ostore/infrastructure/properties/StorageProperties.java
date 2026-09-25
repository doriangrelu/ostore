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
package io.github.doriangrelu.ostore.infrastructure.properties;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Instances de drivers de stockage, préfixe {@code ostore.storage} (ADR-0007, ADR-0015).
 *
 * <pre>{@code
 * ostore.storage:
 *   default-driver: local
 *   drivers:
 *     local:
 *       type: filesystem
 *       layout: date          # optionnel : sinon la stratégie recommandée par le driver
 *       properties: { root: ./data/blobs }
 * }</pre>
 *
 * @param defaultDriver instance attribuée aux nouveaux buckets
 * @param drivers instances configurées, par identifiant
 */
@ConfigurationProperties("ostore.storage")
public record StorageProperties(String defaultDriver, Map<String, Driver> drivers) {

    public StorageProperties {
        drivers = Map.copyOf(drivers);
    }

    /**
     * Une instance de driver.
     *
     * @param type type de driver ({@code filesystem}, {@code s3}…)
     * @param layout stratégie de chemins ({@code date}, {@code hashed}, {@code flat}…), absente = celle du driver
     * @param properties propriétés propres au type
     */
    public record Driver(
            String type,
            @Nullable String layout,
            @DefaultValue Map<String, String> properties) {

        public Driver {
            properties = Map.copyOf(properties);
        }
    }
}
