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
package io.github.doriangrelu.ostore.application.service;

import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import io.github.doriangrelu.ostore.domain.exception.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.exception.BucketNotFoundException;
import io.github.doriangrelu.ostore.domain.model.Bucket;
import io.github.doriangrelu.ostore.domain.model.BucketName;
import java.time.Clock;
import java.util.List;

/**
 * Implémentation des cas d'usage des buckets.
 *
 * <p>Classe à champs privés, et non {@code record} : un service ne doit exposer aucun accesseur vers ses
 * ports de sortie.
 *
 * <p>La vérification d'existence avant création donne une erreur claire dans le cas courant ; la
 * contrainte d'unicité en base reste l'arbitre en cas de création concurrente.
 */
public final class BucketService implements BucketUseCases {

    private final BucketRepository buckets;
    private final Clock clock;
    private final String defaultDriverId;

    /**
     * @param buckets stockage des métadonnées
     * @param clock horloge (injectée pour rester testable)
     * @param defaultDriverId driver attribué aux nouveaux buckets
     */
    public BucketService(BucketRepository buckets, Clock clock, String defaultDriverId) {
        this.buckets = buckets;
        this.clock = clock;
        this.defaultDriverId = defaultDriverId;
    }

    @Override
    public Bucket create(BucketName name) {
        if (buckets.findByName(name).isPresent()) {
            throw new BucketAlreadyExistsException(name);
        }
        return buckets.insert(Bucket.create(name, defaultDriverId, clock.instant()));
    }

    @Override
    public Bucket get(BucketName name) {
        return buckets.findByName(name).orElseThrow(() -> new BucketNotFoundException(name));
    }

    @Override
    public List<Bucket> list() {
        return buckets.findAll();
    }

    @Override
    public void delete(BucketName name) {
        // La vérification « bucket vide » arrive avec les objets (tranche M2).
        buckets.delete(get(name));
    }
}
