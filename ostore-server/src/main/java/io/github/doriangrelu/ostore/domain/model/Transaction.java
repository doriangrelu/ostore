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
package io.github.doriangrelu.ostore.domain.model;

import io.github.doriangrelu.ostore.domain.exception.InvalidTtlException;
import io.github.doriangrelu.ostore.domain.exception.TransactionClosedException;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionPolicy;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus;
import io.github.doriangrelu.ostore.domain.util.Identifiers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Transaction de dépôt (ADR-0016) : ses écritures en attente ne sont conservées que si elle est validée avant
 * son échéance.
 *
 * <p>Une transaction {@code OPEN} dont l'échéance est passée est <b>expirée</b>, même si le job d'expiration ne
 * l'a pas encore traitée : toutes les règles s'appuient sur {@link #statusAt(Instant)}.
 *
 * @param id identifiant (UUID v7)
 * @param status statut enregistré
 * @param reference référence libre du client
 * @param createdAt date d'ouverture
 * @param expiresAt échéance
 * @param closedAt date de clôture, absente tant qu'elle est ouverte
 */
public record Transaction(
        UUID id,
        TransactionStatus status,
        @Nullable String reference,
        Instant createdAt,
        Instant expiresAt,
        @Nullable Instant closedAt) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt").truncatedTo(ChronoUnit.MICROS);
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt").truncatedTo(ChronoUnit.MICROS);
        closedAt = closedAt == null ? null : closedAt.truncatedTo(ChronoUnit.MICROS);
    }

    /** Ouvre une transaction pour la durée donnée (déjà validée par la politique). */
    public static Transaction open(@Nullable String reference, Duration ttl, Instant now) {
        return new Transaction(Identifiers.newId(now), TransactionStatus.OPEN, reference, now, now.plus(ttl), null);
    }

    /** Statut réel à l'instant donné : une transaction ouverte mais échue est expirée. */
    public TransactionStatus statusAt(Instant now) {
        return status == TransactionStatus.OPEN && !now.isBefore(expiresAt) ? TransactionStatus.EXPIRED : status;
    }

    /**
     * Vérifie qu'une écriture peut rejoindre la transaction.
     *
     * @throws TransactionClosedException si elle n'est plus ouverte
     */
    public void ensureOpen(Instant now) {
        var actual = statusAt(now);
        if (actual != TransactionStatus.OPEN) {
            throw new TransactionClosedException(id, actual);
        }
    }

    /**
     * Valide la transaction ; sans effet si elle l'est déjà.
     *
     * @throws TransactionClosedException si elle est annulée ou expirée
     */
    public Transaction commit(Instant now) {
        if (status == TransactionStatus.COMMITTED) {
            return this;
        }
        ensureOpen(now);
        return close(TransactionStatus.COMMITTED, now);
    }

    /**
     * Annule la transaction ; sans effet si elle est déjà annulée ou expirée.
     *
     * @throws TransactionClosedException si elle est validée
     */
    public Transaction rollback(Instant now) {
        return switch (statusAt(now)) {
            case OPEN -> close(TransactionStatus.ROLLED_BACK, now);
            case ROLLED_BACK -> this;
            case EXPIRED -> expire(now);
            case COMMITTED -> throw new TransactionClosedException(id, TransactionStatus.COMMITTED);
        };
    }

    /** Transaction échue, marquée expirée (sans effet si elle est déjà close). */
    public Transaction expire(Instant now) {
        return status == TransactionStatus.OPEN ? close(TransactionStatus.EXPIRED, now) : this;
    }

    /**
     * Repousse l'échéance à {@code now + ttl}, dans la limite de la durée maximale depuis l'ouverture.
     *
     * @throws TransactionClosedException si elle n'est plus ouverte
     * @throws InvalidTtlException si la nouvelle échéance dépasse la durée maximale
     */
    public Transaction extend(Duration ttl, Instant now, TransactionPolicy policy) {
        ensureOpen(now);
        var newExpiresAt = now.plus(policy.resolve(ttl));
        var limit = createdAt.plus(policy.maxTtl());
        if (newExpiresAt.isAfter(limit)) {
            throw new InvalidTtlException("extension beyond the maximum lifetime (%s)".formatted(limit));
        }
        return new Transaction(id, status, reference, createdAt, newExpiresAt, null);
    }

    private Transaction close(TransactionStatus closedStatus, Instant now) {
        return new Transaction(id, closedStatus, reference, createdAt, expiresAt, now);
    }
}
