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

import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.InputStream;
import java.util.Optional;

/**
 * Driver de test : délègue au driver {@code filesystem}, et retient chaque écriture au {@link WriteGate} une fois
 * le blob écrit, avant que le serveur n'enregistre l'objet en base.
 */
final class GatedStorageDriver implements StorageDriver {

    private final StorageDriver delegate;

    GatedStorageDriver(StorageDriver delegate) {
        this.delegate = delegate;
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public void write(BlobPath key, InputStream content, long size) {
        delegate.write(key, content, size);
        try {
            WriteGate.pass();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted at the write gate", e);
        }
    }

    @Override
    public InputStream read(BlobPath key, Optional<ByteRange> range) {
        return delegate.read(key, range);
    }

    @Override
    public void delete(BlobPath key) {
        delegate.delete(key);
    }

    @Override
    public boolean exists(BlobPath key) {
        return delegate.exists(key);
    }
}
