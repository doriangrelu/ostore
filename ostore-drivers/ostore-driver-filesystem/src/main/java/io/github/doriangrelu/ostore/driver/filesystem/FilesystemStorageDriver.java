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
package io.github.doriangrelu.ostore.driver.filesystem;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import io.github.doriangrelu.ostore.driver.filesystem.io.BoundedInputStream;
import io.github.doriangrelu.ostore.driver.spi.StorageDriver;
import io.github.doriangrelu.ostore.driver.spi.exception.BlobNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.exception.StorageException;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Driver de stockage sur système de fichiers (local ou partagé).
 *
 * <ul>
 *   <li>Chaque blob est rangé à {@code <root>/<chemin>} : l'arborescence (par date, répartie…) est décidée
 *       par la stratégie de chemins, pas par le driver.
 *   <li>Écriture atomique : fichier temporaire dans le même répertoire, {@code fsync}, puis
 *       {@code ATOMIC_MOVE}. Un blob partiel n'est jamais visible.
 * </ul>
 */
public final class FilesystemStorageDriver implements StorageDriver {

    private final String id;
    private final Path root;

    public FilesystemStorageDriver(String id, Path root) {
        this.id = id;
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void write(BlobPath key, InputStream content, long size) {
        var target = pathOf(key);
        var temporary = target.resolveSibling(key.fileName() + ".tmp-" + UUID.randomUUID());
        try {
            Files.createDirectories(target.getParent());
            try (var channel = FileChannel.open(temporary, CREATE_NEW, WRITE)) {
                content.transferTo(Channels.newOutputStream(channel));
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporary);
            throw new StorageException("Unable to write blob " + key + " in " + root, e);
        }
    }

    @Override
    public InputStream read(BlobPath key, Optional<ByteRange> range) {
        try {
            var channel = FileChannel.open(pathOf(key), READ);
            if (range.isEmpty()) {
                return Channels.newInputStream(channel);
            }
            channel.position(range.get().start());
            return new BoundedInputStream(
                    Channels.newInputStream(channel), range.get().length());
        } catch (NoSuchFileException e) {
            throw new BlobNotFoundException(key);
        } catch (IOException e) {
            throw new StorageException("Unable to read blob " + key, e);
        }
    }

    @Override
    public void delete(BlobPath key) {
        try {
            Files.deleteIfExists(pathOf(key));
        } catch (IOException e) {
            throw new StorageException("Unable to delete blob " + key, e);
        }
    }

    @Override
    public boolean exists(BlobPath key) {
        return Files.isRegularFile(pathOf(key));
    }

    private Path pathOf(BlobPath key) {
        var path = root.resolve(key.value()).normalize();
        if (!path.startsWith(root)) {
            // Impossible avec un BlobPath valide ; garde-fou contre toute évasion de la racine.
            throw new StorageException("Blob path escapes the storage root: " + key);
        }
        return path;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Nettoyage au mieux : l'erreur d'origine est remontée par l'appelant.
        }
    }
}
