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
package io.github.doriangrelu.ostore.domain.exception;

import io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus;
import java.util.UUID;

/** L'opération exige une transaction ouverte (ou dans un autre état) : son statut réel l'interdit. */
public final class TransactionClosedException extends DomainException {

    private final TransactionStatus status;

    public TransactionClosedException(UUID id, TransactionStatus status) {
        super("Transaction %s is %s".formatted(id, status));
        this.status = status;
    }

    /** Statut réel de la transaction. */
    public TransactionStatus status() {
        return status;
    }
}
