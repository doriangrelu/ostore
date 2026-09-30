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

import io.github.doriangrelu.ostore.application.port.in.TransactionUseCases;
import io.github.doriangrelu.ostore.infrastructure.properties.TransactionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expire les transactions échues (ADR-0016) : leurs écritures en attente sont supprimées et leurs blobs purgés.
 * Sûr en cluster : chaque transaction est verrouillée sans attente par l'instance qui la traite.
 */
@Component
public class TransactionExpirationJob {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionExpirationJob.class);

    private final TransactionUseCases transactions;
    private final TransactionProperties properties;

    public TransactionExpirationJob(TransactionUseCases transactions, TransactionProperties properties) {
        this.transactions = transactions;
        this.properties = properties;
    }

    /** Traite les lots de transactions échues jusqu'à épuisement. */
    @Scheduled(
            fixedDelayString = "${ostore.transactions.expiration-interval:PT30S}",
            initialDelayString = "${ostore.transactions.expiration-interval:PT30S}")
    public void expireDueTransactions() {
        int expired;
        do {
            expired = transactions.expireDue(properties.expirationBatchSize());
            if (expired > 0) {
                LOG.info("{} transaction(s) expired", expired);
            }
        } while (expired == properties.expirationBatchSize());
    }
}
