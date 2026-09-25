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
package io.github.doriangrelu.ostore.application.port.in;

import io.github.doriangrelu.ostore.application.command.CopyObjectCommand;
import io.github.doriangrelu.ostore.application.command.PutObjectCommand;
import io.github.doriangrelu.ostore.application.result.ObjectContent;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.ContentLengthMismatchException;
import io.github.doriangrelu.ostore.domain.exception.ObjectNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.RangeNotSatisfiableException;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import io.github.doriangrelu.ostore.domain.model.vo.RangeRequest;
import java.util.Optional;

/**
 * Cas d'usage des objets, appelés par l'API REST. Un bucket inconnu lève {@link BucketNotFoundException},
 * un objet inconnu {@link ObjectNotFoundException}.
 */
public interface ObjectUseCases {

    /** Taille maximale d'une page de liste. */
    int MAX_PAGE_SIZE = 1000;

    /**
     * Dépose un objet : le contenu est écrit en flux dans un nouveau blob, puis l'objet remplace atomiquement
     * la version précédente de sa clé.
     *
     * @throws ContentLengthMismatchException si la taille reçue diffère de celle annoncée
     */
    StoredObject put(PutObjectCommand command);

    /** Copie un objet ; le contenu est dupliqué dans un nouveau blob. */
    StoredObject copy(CopyObjectCommand command);

    /** Métadonnées d'un objet. */
    StoredObject get(BucketName bucket, ObjectKey key);

    /**
     * Ouvre le contenu d'un objet, entier ou limité à une plage.
     *
     * @throws RangeNotSatisfiableException si la plage ne recouvre aucun octet
     */
    ObjectContent open(BucketName bucket, ObjectKey key, Optional<RangeRequest> range);

    /**
     * Liste les objets d'un bucket en ordre binaire des clés.
     *
     * @param prefix préfixe des clés (vide = toutes)
     * @param after reprise après cette clé (pagination)
     * @param maxKeys taille de page, bornée à {@link #MAX_PAGE_SIZE}
     */
    ObjectPage list(BucketName bucket, String prefix, Optional<ObjectKey> after, int maxKeys);

    /** Supprime un objet ; son blob est purgé ensuite. */
    void delete(BucketName bucket, ObjectKey key);
}
