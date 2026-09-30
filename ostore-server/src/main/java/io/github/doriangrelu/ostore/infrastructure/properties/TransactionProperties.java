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
 * Transactions, préfixe {@code ostore.transactions} (ADR-0016).
 *
 * @param defaultTtl durée de vie quand le client n'en demande pas
 * @param maxTtl durée maximale entre l'ouverture et l'échéance, prolongations comprises
 * @param expirationInterval délai entre deux passages du job d'expiration (ISO-8601)
 * @param expirationBatchSize nombre de transactions échues traitées par passage et par lot
 */
@ConfigurationProperties("ostore.transactions")
public record TransactionProperties(
        @DefaultValue("PT15M") Duration defaultTtl,
        @DefaultValue("P1D") Duration maxTtl,
        @DefaultValue("PT30S") Duration expirationInterval,
        @DefaultValue("100") int expirationBatchSize) {}
