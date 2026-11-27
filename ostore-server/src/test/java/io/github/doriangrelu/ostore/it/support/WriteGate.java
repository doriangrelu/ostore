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

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

/**
 * Portillon des écritures du {@link GatedStorageDriver} : une fois fermé, chaque écriture de blob attend son
 * ouverture. Des requêtes concurrentes entrent ainsi ensemble dans leur phase en base, là où se jouent les
 * conflits.
 */
public final class WriteGate {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final AtomicInteger ARRIVALS = new AtomicInteger();
    private static volatile @Nullable CountDownLatch opening;

    private WriteGate() {}

    /** Ferme le portillon : les écritures suivantes attendent {@link #open()}. */
    public static void close() {
        ARRIVALS.set(0);
        opening = new CountDownLatch(1);
    }

    /** Attend que {@code writers} écritures soient arrivées au portillon. */
    public static void awaitArrivals(int writers) {
        await().atMost(TIMEOUT).until(() -> ARRIVALS.get() >= writers);
    }

    /** Ouvre le portillon : les écritures retenues repartent ensemble. */
    public static void open() {
        var latch = opening;
        opening = null;
        if (latch != null) {
            latch.countDown();
        }
    }

    /** Point de passage d'une écriture : immédiat si le portillon est ouvert. */
    static void pass() throws InterruptedException {
        var latch = opening;
        if (latch == null) {
            return;
        }
        ARRIVALS.incrementAndGet();
        if (!latch.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("write gate was never opened");
        }
    }
}
