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

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Purge asynchrone des blobs, préfixe {@code ostore.purge} (ADR-0006).
 *
 * @param interval délai entre deux passages du job (format ISO-8601, ex. {@code PT30S})
 * @param batchSize nombre de blobs traités par transaction
 * @param maxBackoff délai maximal avant une nouvelle tentative après échecs répétés
 */
@ConfigurationProperties("ostore.purge")
public record PurgeProperties(
        @DefaultValue("PT30S") Duration interval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("PT1H") Duration maxBackoff) {}
