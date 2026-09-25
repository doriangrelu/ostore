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
package io.github.doriangrelu.ostore.it;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Application OStore complète démarrée sur un port aléatoire, appelée par de vrais clients HTTP.
 *
 * <p>Le contexte Spring et les conteneurs sont partagés entre toutes les classes d'un même SGBD.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTest implements IntegrationScenario {

    /** Clients S3 par port : un par contexte Spring, réutilisés entre les tests. */
    private static final Map<Integer, S3Client> S3_CLIENTS = new ConcurrentHashMap<>();

    @LocalServerPort
    private int port;

    @Override
    public OStoreRestClient rest() {
        return new OStoreRestClient(baseUrl());
    }

    @Override
    public S3Client s3() {
        return S3_CLIENTS.computeIfAbsent(port, _ -> S3Client.builder()
                .endpointOverride(URI.create(baseUrl() + "/s3"))
                .forcePathStyle(true)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("ostore", "ostore")))
                .build());
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
