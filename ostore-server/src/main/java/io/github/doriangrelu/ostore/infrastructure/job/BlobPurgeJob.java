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
package io.github.doriangrelu.ostore.infrastructure.job;

import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import io.github.doriangrelu.ostore.infrastructure.properties.PurgeProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Supprime physiquement les blobs planifiés dans {@code OST_BLOB_PURGE} (ADR-0006).
 *
 * <ul>
 *   <li><b>Sûr en cluster</b> : chaque lot est verrouillé par {@code FOR UPDATE SKIP LOCKED} ; plusieurs
 *       instances se partagent la file sans coordination. La taille du lot est bornée par {@code maxRows} (Oracle
 *       refuse {@code FETCH FIRST} avec {@code FOR UPDATE}).
 *   <li><b>Idempotent</b> : supprimer un blob absent n'est pas une erreur.
 *   <li><b>Rejoué</b> : un échec repousse la tentative (backoff exponentiel borné) et conserve l'erreur.
 * </ul>
 */
@Component
public class BlobPurgeJob {

    private static final Logger LOG = LoggerFactory.getLogger(BlobPurgeJob.class);
    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(30);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final JdbcTemplate batch;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final StorageDrivers drivers;
    private final PurgeProperties properties;
    private final Clock clock;

    public BlobPurgeJob(
            DataSource dataSource,
            TransactionTemplate transaction,
            StorageDrivers drivers,
            PurgeProperties properties,
            Clock clock) {
        this.batch = new JdbcTemplate(dataSource);
        this.batch.setMaxRows(properties.batchSize());
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = transaction;
        this.drivers = drivers;
        this.properties = properties;
        this.clock = clock;
    }

    /** Traite les lots échus jusqu'à épuisement de la file. */
    @Scheduled(
            fixedDelayString = "${ostore.purge.interval:PT30S}",
            initialDelayString = "${ostore.purge.interval:PT30S}")
    public void purgeDueBlobs() {
        int processed;
        do {
            processed =
                    Optional.ofNullable(transaction.execute(_ -> purgeBatch())).orElse(0);
        } while (processed == properties.batchSize());
    }

    private int purgeBatch() {
        var now = clock.instant();
        List<PurgeTask> due = batch.query(
                """
                SELECT ID, DRIVER_ID, BLOB_PATH, ATTEMPTS FROM OST_BLOB_PURGE
                WHERE NOT_BEFORE <= ? ORDER BY NOT_BEFORE FOR UPDATE SKIP LOCKED""",
                (row, _) -> new PurgeTask(
                        row.getLong("ID"),
                        row.getString("DRIVER_ID"),
                        row.getString("BLOB_PATH"),
                        row.getInt("ATTEMPTS")),
                utc(now));
        due.forEach(task -> purge(task, now));
        return due.size();
    }

    private void purge(PurgeTask task, Instant now) {
        try {
            drivers.driver(task.driverId()).delete(new BlobPath(task.path()));
            jdbc.update("DELETE FROM OST_BLOB_PURGE WHERE ID = ?", task.id());
        } catch (RuntimeException e) {
            int attempts = task.attempts() + 1;
            var retryAt = now.plus(backoff(attempts));
            LOG.warn(
                    "Purge of blob {} on driver {} failed (attempt {}), retry at {}",
                    task.path(),
                    task.driverId(),
                    attempts,
                    retryAt,
                    e);
            jdbc.update(
                    "UPDATE OST_BLOB_PURGE SET ATTEMPTS = ?, NOT_BEFORE = ?, LAST_ERROR = ? WHERE ID = ?",
                    attempts,
                    utc(retryAt),
                    truncate(String.valueOf(e.getMessage())),
                    task.id());
        }
    }

    private Duration backoff(int attempts) {
        var delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(attempts - 1, 20));
        return delay.compareTo(properties.maxBackoff()) > 0 ? properties.maxBackoff() : delay;
    }

    private static java.time.OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static String truncate(String message) {
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    /** Ligne de la file de purge. */
    private record PurgeTask(long id, String driverId, String path, int attempts) {}
}
