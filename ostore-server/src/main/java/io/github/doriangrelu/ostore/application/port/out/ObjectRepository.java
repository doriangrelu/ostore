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

    /** Enregistre un nouvel objet. */
    StoredObject insert(StoredObject object);

    /**
     * Remplace le contenu d'un objet existant <b>atomiquement</b> : l'objet mis à jour est enregistré et le blob
     * précédent planifié pour purge, dans la même transaction.
     *
     * @throws ConcurrentObjectUpdateException si l'objet a été modifié ou supprimé entre-temps
     */
    StoredObject replace(StoredObject updated);

    Optional<StoredObject> find(UUID id);

    /**
     * Objets d'un bucket par identifiant croissant (ordre de création), à partir de l'identifiant suivant
     * {@code after}.
     *
     * @param namePrefix préfixe de nom, {@code null} = tous les objets, y compris sans nom
     * @param limit nombre maximal d'éléments
     */
    List<ObjectSummary> list(UUID bucketId, @Nullable String namePrefix, Optional<UUID> after, int limit);

    /** Supprime un objet et planifie la purge de son blob, dans la même transaction. */
    void delete(StoredObject object);

    /** Indique si le bucket contient au moins un objet. */
    boolean existsInBucket(UUID bucketId);
}
