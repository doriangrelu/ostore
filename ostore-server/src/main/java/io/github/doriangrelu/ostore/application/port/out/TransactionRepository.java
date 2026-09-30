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

import io.github.doriangrelu.ostore.domain.model.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des transactions. Les méthodes de verrouillage s'utilisent dans une {@link UnitOfWork}. */
public interface TransactionRepository {

    Transaction insert(Transaction transaction);

    Optional<Transaction> find(UUID id);

    /** Transaction verrouillée jusqu'à la fin de la transaction SQL courante ({@code FOR UPDATE}). */
    Optional<Transaction> lock(UUID id);

    /**
     * Transaction encore ouverte, verrouillée sans attendre ({@code FOR UPDATE SKIP LOCKED}) : vide si elle est
     * close ou déjà verrouillée par une autre instance.
     */
    Optional<Transaction> tryLockOpen(UUID id);

    /** Identifiants de transactions ouvertes dont l'échéance est passée, les plus anciennes d'abord. */
    List<UUID> findDue(Instant now, int limit);

    Transaction update(Transaction transaction);
}
