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
package io.github.doriangrelu.ostore.application.port.out;

import io.github.doriangrelu.ostore.domain.exception.ConcurrentObjectUpdateException;
import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Stockage des métadonnées d'objets. */
public interface ObjectRepository {

    /** Enregistre un nouvel objet (ou un remplacement en attente). */
    StoredObject insert(StoredObject object);

    /**
     * Remplace le contenu d'une ligne existante <b>atomiquement</b> : la ligne mise à jour est enregistrée et le
     * blob précédent planifié pour purge, dans la même transaction.
     *
     * @throws ConcurrentObjectUpdateException si la ligne a été modifiée ou supprimée entre-temps
     */
    StoredObject replace(StoredObject updated);

    /** Ligne par identifiant (objet, ou remplacement en attente). */
    Optional<StoredObject> find(UUID id);

    /** Remplacement en attente de l'objet donné, s'il existe. */
    Optional<StoredObject> findPendingReplacement(UUID objectId);

    /**
     * Objets validés d'un bucket par identifiant croissant (ordre de création), après la position {@code after}.
     *
     * @param namePrefix préfixe de nom, {@code null} = tous les objets, y compris sans nom
     */
    List<ObjectSummary> list(UUID bucketId, @Nullable String namePrefix, Optional<UUID> after, int limit);

    /** Objets d'une transaction (tous buckets, remplacements compris), après la position {@code after}. */
    List<ObjectSummary> listByTransaction(UUID transactionId, Optional<UUID> after, int limit);

    long countByTransaction(UUID transactionId);

    /** Commit : applique les remplacements en attente aux objets visés (anciens blobs purgés). */
    void applyReplacements(UUID transactionId);

    /** Commit : les nouveaux objets en attente de la transaction deviennent actifs. */
    void activatePending(UUID transactionId);

    /** Rollback, expiration : supprime les écritures en attente de la transaction et purge leurs blobs. */
    void deletePending(UUID transactionId);

    /** Supprime un objet et planifie la purge de son blob, dans la même transaction. */
    void delete(StoredObject object);

    /** Indique si le bucket contient au moins un objet (en attente compris). */
    boolean existsInBucket(UUID bucketId);
}
