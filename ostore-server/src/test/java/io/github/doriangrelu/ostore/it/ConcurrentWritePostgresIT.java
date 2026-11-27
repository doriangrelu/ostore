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

import io.github.doriangrelu.ostore.it.scenario.ConcurrentWriteScenarios;
import io.github.doriangrelu.ostore.it.support.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Écritures concurrentes, avec le driver de test {@code gated} comme stockage par défaut. */
class ConcurrentWritePostgresIT extends PostgresIntegrationTest implements ConcurrentWriteScenarios {

    @DynamicPropertySource
    static void useGatedDriver(DynamicPropertyRegistry registry) {
        registry.add("ostore.storage.default-driver", () -> "gated");
        registry.add("ostore.storage.drivers.gated.type", () -> "gated");
        registry.add("ostore.storage.drivers.gated.properties.root", () -> "target/it-storage");
    }
}
