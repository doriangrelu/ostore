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
package io.github.doriangrelu.ostore.application.port.in;

import io.github.doriangrelu.ostore.application.command.OpenTransactionCommand;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.application.result.TransactionDetails;
import io.github.doriangrelu.ostore.domain.exception.InvalidTtlException;
import io.github.doriangrelu.ostore.domain.exception.TransactionClosedException;
import io.github.doriangrelu.ostore.domain.exception.TransactionNotFoundException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage des transactions (ADR-0016). Une transaction inconnue lève {@link TransactionNotFoundException}.
 */
public interface TransactionUseCases {

    /**
     * Ouvre une transaction.
     *
     * @throws InvalidTtlException si la durée demandée est invalide
     */
    TransactionDetails open(OpenTransactionCommand command);

    TransactionDetails get(UUID id);

    /** Objets de la transaction, paginés. */
    ObjectPage listObjects(UUID id, Optional<UUID> after, int limit);

    /**
     * Valide la transaction : ses écritures en attente deviennent effectives. Sans effet si elle l'est déjà.
     *
     * @throws TransactionClosedException si elle est annulée ou expirée
     */
    TransactionDetails commit(UUID id);

    /**
     * Annule la transaction : ses écritures en attente sont supprimées. Sans effet si elle est déjà annulée ou
     * expirée.
     *
     * @throws TransactionClosedException si elle est validée
     */
    TransactionDetails rollback(UUID id);

    /**
     * Repousse l'échéance à maintenant + {@code ttl}.
     *
     * @throws TransactionClosedException si elle n'est plus ouverte
     * @throws InvalidTtlException si la durée totale dépasserait le maximum
     */
    TransactionDetails extend(UUID id, Duration ttl);

    /**
     * Expire un lot de transactions échues (job planifié) : leurs écritures en attente sont supprimées.
     *
     * @return nombre de transactions expirées
     */
    int expireDue(int batchSize);
}
