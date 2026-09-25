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
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des métadonnées d'objets. */
public interface ObjectRepository {

    /**
     * Enregistre un objet comme version active de sa clé, <b>atomiquement</b> : la version précédente éventuelle
     * est supprimée et son blob planifié pour purge dans la même transaction.
     *
     * @throws ConcurrentObjectUpdateException si une écriture simultanée sur la même clé l'a emporté
     */
    StoredObject putActive(StoredObject object);

    Optional<StoredObject> find(UUID bucketId, ObjectKey key);

    /**
     * Objets d'un bucket en ordre binaire des clés (UTF-8), à partir de la clé suivant {@code after}.
     *
     * @param prefix préfixe des clés (vide = toutes)
     * @param limit nombre maximal d'éléments
     */
    List<ObjectSummary> list(UUID bucketId, String prefix, Optional<ObjectKey> after, int limit);

    /** Supprime un objet et planifie la purge de son blob, dans la même transaction. */
    void delete(StoredObject object);

    /** Indique si le bucket contient au moins un objet. */
    boolean existsInBucket(UUID bucketId);
}
