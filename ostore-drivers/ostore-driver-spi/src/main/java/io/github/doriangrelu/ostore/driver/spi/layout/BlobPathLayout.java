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
 * Stratégie d'organisation des blobs sur leur support : calcule le chemin d'un nouveau blob.
 *
 * <p>Le chemin calculé est <b>stocké en base</b> avec l'objet : changer de stratégie ne concerne que les
 * nouveaux blobs, les anciens restent lisibles et purgeables à leur chemin d'origine.
 *
 * <p>Choix par instance de driver : propriété {@code ostore.storage.drivers.<id>.layout}, sinon
 * {@link io.github.doriangrelu.ostore.driver.spi.StorageDriver#defaultLayout()}. Stratégies fournies :
 * {@code date}, {@code hashed}, {@code flat}. D'autres peuvent être ajoutées par {@link
 * java.util.ServiceLoader} ({@code META-INF/services/io.github.doriangrelu.ostore.driver.spi.layout.BlobPathLayout}).
 */
public interface BlobPathLayout {

    /** Nom de la stratégie, utilisé en configuration. */
    String name();

    /**
     * Chemin du blob.
     *
     * @param blobId identifiant unique du blob (UUID v7)
     * @param createdAt date de dépôt
     */
    BlobPath pathOf(UUID blobId, Instant createdAt);
}
