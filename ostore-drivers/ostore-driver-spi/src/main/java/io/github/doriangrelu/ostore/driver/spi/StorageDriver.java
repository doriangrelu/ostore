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
package io.github.doriangrelu.ostore.driver.spi;

import io.github.doriangrelu.ostore.driver.spi.exception.BlobNotFoundException;
import io.github.doriangrelu.ostore.driver.spi.exception.StorageException;
import io.github.doriangrelu.ostore.driver.spi.model.BlobKey;
import io.github.doriangrelu.ostore.driver.spi.model.ByteRange;
import java.io.InputStream;
import java.util.Optional;

/**
 * Support physique de stockage des blobs (contenus binaires) d'OStore.
 *
 * <p>Un driver ne connaît ni les buckets, ni les clés utilisateur, ni les transactions : il manipule des
 * {@link BlobKey} opaques générées par le serveur, jamais réutilisées. Toutes les opérations travaillent
 * <b>en flux</b> : aucune implémentation ne doit charger un blob entier en mémoire.
 *
 * <p>Toute implémentation doit passer le kit de conformité fourni par le {@code test-jar} de ce module.
 */
public interface StorageDriver {

    /** Identifiant de l'instance configurée (ex. {@code local}), référencé par les buckets. */
    String id();

    /**
     * Écrit un nouveau blob.
     *
     * <p>L'écriture est atomique du point de vue des lecteurs : un blob partiellement écrit n'est
     * jamais visible. En cas d'échec, aucun blob n'est laissé sous cette clé.
     *
     * @param key clé du blob, inédite
     * @param content contenu, lu jusqu'à la fin du flux ; non fermé par le driver
     * @param size taille annoncée en octets (certains backends l'exigent avant l'envoi)
     * @throws StorageException si l'écriture échoue
     */
    void write(BlobKey key, InputStream content, long size);

    /**
     * Ouvre un blob en lecture ; l'appelant ferme le flux.
     *
     * @param range plage d'octets, déjà résolue et comprise dans le blob, ou vide pour tout le contenu
     * @throws BlobNotFoundException si le blob n'existe pas
     */
    InputStream read(BlobKey key, Optional<ByteRange> range);

    /** Supprime un blob ; sans effet s'il n'existe pas (idempotent). */
    void delete(BlobKey key);

    /** Indique si le blob existe. */
    boolean exists(BlobKey key);
}
