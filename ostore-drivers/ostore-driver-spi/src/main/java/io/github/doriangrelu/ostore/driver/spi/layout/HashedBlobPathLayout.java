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
package io.github.doriangrelu.ostore.driver.spi.layout;

import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import java.time.Instant;
import java.util.UUID;

/**
 * Stratégie {@code hashed} : {@code xx/yy/<id>}, où {@code xx} et {@code yy} viennent des derniers caractères
 * de l'identifiant (partie aléatoire d'un UUID v7). Répartit uniformément les blobs, pour les systèmes de
 * fichiers qui supportent mal les répertoires très peuplés.
 */
public final class HashedBlobPathLayout implements BlobPathLayout {

    public static final String NAME = "hashed";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BlobPath pathOf(UUID blobId, Instant createdAt) {
        var id = blobId.toString();
        int length = id.length();
        return new BlobPath(id.substring(length - 2) + "/" + id.substring(length - 4, length - 2) + "/" + id);
    }
}
