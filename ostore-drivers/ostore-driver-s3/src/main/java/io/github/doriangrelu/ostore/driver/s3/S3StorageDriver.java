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

import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.exception.BlobNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.exception.StorageException;
import io.github.doriangrelu.ostore.driver.spi.model.BlobKey;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Driver de stockage vers Amazon S3 ou tout service compatible (Ceph, Garage, SeaweedFS…).
 *
 * <p>Chaque blob est un objet {@code <prefix><clé du blob>} du bucket configuré. OStore n'utilise ici S3
 * que comme <b>backend</b> ; son API reste en JSON (ADR-0014).
 */
public final class S3StorageDriver implements StorageDriver, AutoCloseable {

    private static final int NOT_FOUND = 404;

    private final String id;
    private final S3Client client;
    private final String bucket;
    private final String prefix;

    public S3StorageDriver(String id, S3Client client, String bucket, String prefix) {
        this.id = id;
        this.client = client;
        this.bucket = bucket;
        this.prefix = prefix;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void write(BlobKey key, InputStream content, long size) {
        try {
            client.putObject(
                    request -> request.bucket(bucket).key(objectKey(key)).contentLength(size),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            throw new StorageException("Unable to write blob " + key + " to s3://" + bucket, e);
        }
    }

    @Override
    public InputStream read(BlobKey key, Optional<ByteRange> range) {
        try {
            return client.getObject(request -> {
                request.bucket(bucket).key(objectKey(key));
                range.ifPresent(r -> request.range("bytes=%d-%d".formatted(r.start(), r.end())));
            });
        } catch (NoSuchKeyException e) {
            throw new BlobNotFoundException(key);
        } catch (SdkException e) {
            throw new StorageException("Unable to read blob " + key + " from s3://" + bucket, e);
        }
    }

    @Override
    public void delete(BlobKey key) {
        try {
            client.deleteObject(request -> request.bucket(bucket).key(objectKey(key)));
        } catch (SdkException e) {
            throw new StorageException("Unable to delete blob " + key + " from s3://" + bucket, e);
        }
    }

    @Override
    public boolean exists(BlobKey key) {
        try {
            client.headObject(request -> request.bucket(bucket).key(objectKey(key)));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == NOT_FOUND) {
                return false;
            }
            throw new StorageException("Unable to check blob " + key + " in s3://" + bucket, e);
        } catch (SdkException e) {
            throw new StorageException("Unable to check blob " + key + " in s3://" + bucket, e);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private String objectKey(BlobKey key) {
        return prefix + key.value();
    }
}
