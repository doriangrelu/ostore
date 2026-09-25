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

import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.it.client.OStoreRestClient;
import io.github.doriangrelu.ostore.it.client.ObjectRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Application OStore complète démarrée sur un port aléatoire, appelée par de vrais clients HTTP.
 *
 * <p>Le contexte Spring et les conteneurs sont partagés entre toutes les classes d'un même SGBD. Les blobs
 * vont dans {@code target/it-storage} ; la purge tourne toutes les 500 ms pour être observable.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ostore.storage.drivers.local.properties.root=target/it-storage", "ostore.purge.interval=PT0.5S"})
abstract class IntegrationTest implements IntegrationScenario {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private StorageDrivers drivers;

    @Override
    public OStoreRestClient rest() {
        return new OStoreRestClient(baseUrl());
    }

    @Override
    public ObjectRestClient objects() {
        return new ObjectRestClient(baseUrl());
    }

    @Override
    public StorageProbe storage() {
        return new StorageProbe(jdbc, drivers);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
