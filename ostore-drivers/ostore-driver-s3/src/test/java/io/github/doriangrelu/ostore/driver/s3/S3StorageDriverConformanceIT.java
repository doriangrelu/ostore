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
package io.github.doriangrelu.ostore.driver.s3;

import io.github.doriangrelu.ostore.driver.s3.factory.S3StorageDriverFactory;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.conformance.StorageDriverConformanceTest;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Kit de conformité de la SPI appliqué au driver {@code s3}, sur Adobe S3Mock. */
@Testcontainers
class S3StorageDriverConformanceIT extends StorageDriverConformanceTest {

    private static final String BUCKET = "ostore-blobs";
    private static final int S3MOCK_PORT = 9090;

    @Container
    private static final GenericContainer<?> S3 =
            new GenericContainer<>("adobe/s3mock:5.2.3").withExposedPorts(S3MOCK_PORT);

    private static S3StorageDriver driver;

    @BeforeAll
    static void createBucketAndDriver() {
        var endpoint = "http://%s:%d".formatted(S3.getHost(), S3.getMappedPort(S3MOCK_PORT));
        try (var admin = S3Client.builder()
                .endpointOverride(java.net.URI.create(endpoint))
                .forcePathStyle(true)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            admin.createBucket(request -> request.bucket(BUCKET));
        }
        driver = (S3StorageDriver) new S3StorageDriverFactory()
                .create(
                        "test",
                        Map.of(
                                "bucket", BUCKET,
                                "prefix", "blobs/",
                                "endpoint", endpoint,
                                "path-style", "true",
                                "access-key", "test",
                                "secret-key", "test"));
    }

    @AfterAll
    static void closeDriver() {
        driver.close();
    }

    @Override
    protected StorageDriver driver() {
        return driver;
    }
}
