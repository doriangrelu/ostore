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
package io.github.doriangrelu.ostore.driver.s3.factory;

import io.github.doriangrelu.ostore.driver.s3.S3StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.StorageDriverFactory;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Fabrique du driver {@code s3}.
 *
 * <table>
 *   <caption>Propriétés</caption>
 *   <tr><th>Propriété</th><th>Rôle</th></tr>
 *   <tr><td>{@code bucket}</td><td>bucket S3 des blobs (obligatoire)</td></tr>
 *   <tr><td>{@code prefix}</td><td>préfixe des clés (défaut : aucun)</td></tr>
 *   <tr><td>{@code region}</td><td>région (défaut : {@code us-east-1})</td></tr>
 *   <tr><td>{@code endpoint}</td><td>URL d'un service compatible S3 (défaut : AWS)</td></tr>
 *   <tr><td>{@code path-style}</td><td>adressage path-style, requis par la plupart des services compatibles</td></tr>
 *   <tr><td>{@code access-key}, {@code secret-key}</td><td>identifiants statiques (défaut : chaîne AWS standard)</td></tr>
 * </table>
 *
 * <p>Les checksums ne sont calculés que lorsque S3 les exige : les checksums systématiques des SDK récents
 * ne sont pas supportés par tous les services compatibles.
 */
public final class S3StorageDriverFactory implements StorageDriverFactory {

    public static final String TYPE = "s3";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public StorageDriver create(String id, Map<String, String> properties) {
        var bucket = Optional.ofNullable(properties.get("bucket"))
                .filter(value -> !value.isBlank())
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver '%s' (s3): property 'bucket' is required".formatted(id)));
        var builder = S3Client.builder()
                .region(Region.of(properties.getOrDefault("region", "us-east-1")))
                .forcePathStyle(Boolean.parseBoolean(properties.getOrDefault("path-style", "false")))
                .credentialsProvider(credentials(properties))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        Optional.ofNullable(properties.get("endpoint")).map(URI::create).ifPresent(builder::endpointOverride);
        return new S3StorageDriver(id, builder.build(), bucket, properties.getOrDefault("prefix", ""));
    }

    private static AwsCredentialsProvider credentials(Map<String, String> properties) {
        var accessKey = properties.get("access-key");
        var secretKey = properties.get("secret-key");
        return accessKey != null && secretKey != null
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.builder().build();
    }
}
