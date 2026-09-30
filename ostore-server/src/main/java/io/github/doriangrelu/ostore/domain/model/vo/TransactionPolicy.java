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
package io.github.doriangrelu.ostore.domain.model.vo;

import io.github.doriangrelu.ostore.domain.exception.InvalidTtlException;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Durées de vie des transactions : valeur par défaut et durée totale maximale, prolongations comprises.
 *
 * @param defaultTtl durée appliquée quand le client n'en demande pas
 * @param maxTtl durée maximale entre l'ouverture et l'échéance
 */
public record TransactionPolicy(Duration defaultTtl, Duration maxTtl) {

    public TransactionPolicy {
        Objects.requireNonNull(defaultTtl, "defaultTtl");
        Objects.requireNonNull(maxTtl, "maxTtl");
        if (defaultTtl.isNegative() || defaultTtl.isZero() || defaultTtl.compareTo(maxTtl) > 0) {
            throw new IllegalArgumentException("default TTL must be positive and not exceed the maximum TTL");
        }
    }

    /**
     * Durée de vie effective d'une demande.
     *
     * @param requested durée demandée, {@code null} = durée par défaut
     * @throws InvalidTtlException si la durée est nulle, négative ou dépasse le maximum
     */
    public Duration resolve(@Nullable Duration requested) {
        if (requested == null) {
            return defaultTtl;
        }
        if (requested.isNegative() || requested.isZero() || requested.compareTo(maxTtl) > 0) {
            throw new InvalidTtlException("TTL %s must be positive and at most %s".formatted(requested, maxTtl));
        }
        return requested;
    }
}
