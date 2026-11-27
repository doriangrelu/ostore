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
package io.github.doriangrelu.ostore.it.support;

import io.github.doriangrelu.ostore.application.command.ReplaceObjectCommand;
import io.github.doriangrelu.ostore.application.command.TransactionMode;
import io.github.doriangrelu.ostore.application.port.in.ObjectUseCases;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectMetadata;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Remplacement préparé par le cas d'usage dans une transaction SQL du test, laissée ouverte jusqu'à
 * {@link #close()} : comme une requête concurrente encore en cours, il tient ses verrous et reste invisible aux
 * autres requêtes, puis est validé à la fermeture.
 */
public final class UncommittedStaging implements AutoCloseable {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final CountDownLatch release = new CountDownLatch(1);
    private final Thread holder;

    private UncommittedStaging(
            TransactionTemplate sql, ObjectUseCases objects, UUID objectId, UUID transactionId, byte[] content)
            throws InterruptedException {
        var staged = new CountDownLatch(1);
        var command = new ReplaceObjectCommand(
                objectId,
                null,
                content.length,
                "text/plain",
                ObjectMetadata.EMPTY,
                new TransactionMode.Join(transactionId),
                new ByteArrayInputStream(content));
        holder = Thread.ofVirtual()
                .start(() -> sql.executeWithoutResult(status -> {
                    objects.replace(command);
                    staged.countDown();
                    awaitRelease();
                }));
        if (!staged.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("replacement of object %s could not be staged".formatted(objectId));
        }
    }

    /** Prépare le remplacement et rend la main sans valider la transaction SQL. */
    static UncommittedStaging start(
            TransactionTemplate sql, ObjectUseCases objects, UUID objectId, UUID transactionId, byte[] content) {
        try {
            return new UncommittedStaging(sql, objects, objectId, transactionId, content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while staging object " + objectId, e);
        }
    }

    /** Valide la transaction SQL : le remplacement en attente devient visible, ses verrous sont libérés. */
    @Override
    public void close() {
        release.countDown();
        try {
            holder.join(TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitRelease() {
        try {
            if (!release.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("staging was never released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
