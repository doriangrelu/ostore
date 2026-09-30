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
package io.github.doriangrelu.ostore.application.command;

import java.time.Duration;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Mode transactionnel d'une écriture d'objet (ADR-0016). */
public sealed interface TransactionMode {

    /** Écriture immédiatement validée. */
    record None() implements TransactionMode {}

    /** L'écriture rejoint une transaction ouverte ({@code X-OStore-Transaction-Id}). */
    record Join(UUID transactionId) implements TransactionMode {}

    /** Une transaction est créée pour cette seule écriture ({@code X-OStore-Pending-Ttl}). */
    record Pending(Duration ttl) implements TransactionMode {}

    TransactionMode NONE = new None();

    /** Mode correspondant aux en-têtes reçus, déjà décodés ; au plus un des deux est renseigné. */
    static TransactionMode of(@Nullable UUID transactionId, @Nullable Duration pendingTtl) {
        if (transactionId != null) {
            return new Join(transactionId);
        }
        return pendingTtl != null ? new Pending(pendingTtl) : NONE;
    }
}
