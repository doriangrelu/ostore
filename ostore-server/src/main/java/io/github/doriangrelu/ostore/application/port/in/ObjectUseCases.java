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
import io.github.doriangrelu.ostore.application.command.CreateObjectCommand;
import io.github.doriangrelu.ostore.application.command.ReplaceObjectCommand;
import io.github.doriangrelu.ostore.application.result.ObjectContent;
import io.github.doriangrelu.ostore.application.result.ObjectPage;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.ConcurrentObjectUpdateException;
import io.github.doriangrelu.ostore.domain.exception.ContentLengthMismatchException;
import io.github.doriangrelu.ostore.domain.exception.ObjectNotFoundException;
import io.github.doriangrelu.ostore.domain.exception.RangeNotSatisfiableException;
import io.github.doriangrelu.ostore.domain.model.StoredObject;
import io.github.doriangrelu.ostore.domain.model.vo.BucketName;
import io.github.doriangrelu.ostore.domain.model.vo.RangeRequest;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage des objets, désignés par leur identifiant (ADR-0015). Un bucket inconnu lève
 * {@link BucketNotFoundException}, un objet inconnu {@link ObjectNotFoundException}.
 */
public interface ObjectUseCases {

    /** Taille maximale d'une page de liste. */
    int MAX_PAGE_SIZE = 1000;

    /**
     * Dépose un nouvel objet : le contenu est écrit en flux dans un nouveau blob.
     *
     * @throws ContentLengthMismatchException si la taille reçue diffère de celle annoncée
     */
    StoredObject create(CreateObjectCommand command);

    /**
     * Remplace le contenu d'un objet (même identifiant) ; l'ancien blob est purgé ensuite.
     *
     * @throws ContentLengthMismatchException si la taille reçue diffère de celle annoncée
     * @throws ConcurrentObjectUpdateException si un autre remplacement du même objet l'a emporté
     */
    StoredObject replace(ReplaceObjectCommand command);

    /** Copie un objet en un nouvel objet ; le contenu est dupliqué dans un nouveau blob. */
    StoredObject copy(CopyObjectCommand command);

    /** Métadonnées d'un objet. */
    StoredObject get(UUID id);

    /**
     * Ouvre le contenu d'un objet, entier ou limité à une plage.
     *
     * @throws RangeNotSatisfiableException si la plage ne recouvre aucun octet
     */
    ObjectContent open(UUID id, Optional<RangeRequest> range);

    /**
     * Liste les objets d'un bucket par ordre de création.
     *
     * @param namePrefix préfixe de nom, {@code null} = tous les objets (y compris sans nom)
     * @param after reprise après cet identifiant (pagination)
     * @param limit taille de page, bornée à {@link #MAX_PAGE_SIZE}
     */
    ObjectPage list(BucketName bucket, @Nullable String namePrefix, Optional<UUID> after, int limit);

    /** Supprime un objet ; son blob est purgé ensuite. */
    void delete(UUID id);
}
