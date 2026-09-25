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

import io.github.doriangrelu.ostore.it.scenario.ObjectScenarios;
import io.github.doriangrelu.ostore.it.support.PostgresIntegrationTest;
import java.net.URI;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Les mêmes scénarios « objets », avec le driver {@code s3} comme stockage par défaut (Adobe S3Mock) : le
 * comportement d'OStore ne dépend pas du support.
 */
class ObjectS3IT extends PostgresIntegrationTest implements ObjectScenarios {

    private static final String BLOB_BUCKET = "ostore-blobs";
    private static final GenericContainer<?> S3 = new GenericContainer<>("adobe/s3mock:5.2.3").withExposedPorts(9090);

    @DynamicPropertySource
    static void useS3Driver(DynamicPropertyRegistry registry) {
        S3.start();
        var endpoint = "http://%s:%d".formatted(S3.getHost(), S3.getMappedPort(9090));
        try (var admin = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            admin.createBucket(request -> request.bucket(BLOB_BUCKET));
        }
        registry.add("ostore.storage.default-driver", () -> "remote");
        registry.add("ostore.storage.drivers.remote.type", () -> "s3");
        registry.add("ostore.storage.drivers.remote.properties.bucket", () -> BLOB_BUCKET);
        registry.add("ostore.storage.drivers.remote.properties.endpoint", () -> endpoint);
        registry.add("ostore.storage.drivers.remote.properties.path-style", () -> "true");
        registry.add("ostore.storage.drivers.remote.properties.access-key", () -> "test");
        registry.add("ostore.storage.drivers.remote.properties.secret-key", () -> "test");
    }
}
