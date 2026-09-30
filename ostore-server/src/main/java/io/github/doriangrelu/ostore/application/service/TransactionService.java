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
package io.github.doriangrelu.ostore.application.service;

import io.github.doriangrelu.ostore.application.command.OpenTransactionCommand;
import io.github.doriangrelu.ostore.application.port.in.ObjectUseCases;
import io.github.doriangrelu.ostore.application.port.in.TransactionUseCases;
import io.github.doriangrelu.ostore.application.port.out.ObjectRepository;
import io.github.doriangrelu.ostore.application.port.out.TransactionRepository;
import io.github.doriangrelu.ostore.application.port.out.UnitOfWork;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.application.result.TransactionDetails;
import io.github.doriangrelu.ostore.domain.exception.TransactionNotFoundException;
import io.github.doriangrelu.ostore.domain.model.Transaction;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionPolicy;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation des cas d'usage des transactions (ADR-0016).
 *
 * <p>Commit, rollback, prolongation et expiration verrouillent la ligne de la transaction dans une
 * {@link UnitOfWork} : ils s'excluent mutuellement et excluent toute écriture qui voudrait la rejoindre. Le
 * changement de statut et ses effets sur les objets sont validés ensemble, ou pas du tout.
 */
public final class TransactionService implements TransactionUseCases {

    private final TransactionRepository transactions;
    private final ObjectRepository objects;
    private final UnitOfWork unitOfWork;
    private final TransactionPolicy policy;
    private final Clock clock;

    public TransactionService(
            TransactionRepository transactions,
            ObjectRepository objects,
            UnitOfWork unitOfWork,
            TransactionPolicy policy,
            Clock clock) {
        this.transactions = transactions;
        this.objects = objects;
        this.unitOfWork = unitOfWork;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public TransactionDetails open(OpenTransactionCommand command) {
        var now = clock.instant();
        var transaction =
                transactions.insert(Transaction.open(command.reference(), policy.resolve(command.ttl()), now));
        return details(transaction);
    }

    @Override
    public TransactionDetails get(UUID id) {
        return details(find(id));
    }

    @Override
    public ObjectPage listObjects(UUID id, Optional<UUID> after, int limit) {
        find(id);
        int pageSize = Math.clamp(limit, 1, ObjectUseCases.MAX_PAGE_SIZE);
        return ObjectPage.of(objects.listByTransaction(id, after, pageSize + 1), pageSize);
    }

    @Override
    public TransactionDetails commit(UUID id) {
        return unitOfWork.execute(() -> {
            var current = lock(id);
            var committed = current.commit(clock.instant());
            if (committed != current) {
                objects.applyReplacements(id);
                objects.activatePending(id);
                return details(transactions.update(committed));
            }
            return details(current);
        });
    }

    @Override
    public TransactionDetails rollback(UUID id) {
        return unitOfWork.execute(() -> {
            var current = lock(id);
            return close(current, current.rollback(clock.instant()));
        });
    }

    @Override
    public TransactionDetails extend(UUID id, Duration ttl) {
        return unitOfWork.execute(() -> details(transactions.update(lock(id).extend(ttl, clock.instant(), policy))));
    }

    @Override
    public int expireDue(int batchSize) {
        var now = clock.instant();
        int expired = 0;
        for (var id : transactions.findDue(now, batchSize)) {
            // Verrou sans attente : une autre instance traite peut-être déjà cette transaction.
            boolean done = unitOfWork.execute(() -> transactions
                    .tryLockOpen(id)
                    .filter(transaction -> transaction.statusAt(now) == TransactionStatus.EXPIRED)
                    .map(transaction -> close(transaction, transaction.expire(now)))
                    .isPresent());
            if (done) {
                expired++;
            }
        }
        return expired;
    }

    /**
     * Clôture (annulation ou expiration) : supprime les écritures en attente si le statut change. Les règles du
     * domaine renvoient la même instance quand l'opération est sans effet (idempotence).
     */
    private TransactionDetails close(Transaction current, Transaction closed) {
        if (closed == current) {
            return details(current);
        }
        objects.deletePending(current.id());
        return details(transactions.update(closed));
    }

    private Transaction find(UUID id) {
        return transactions.find(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private Transaction lock(UUID id) {
        return transactions.lock(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private TransactionDetails details(Transaction transaction) {
        return new TransactionDetails(
                transaction, transaction.statusAt(clock.instant()), objects.countByTransaction(transaction.id()));
    }
}
