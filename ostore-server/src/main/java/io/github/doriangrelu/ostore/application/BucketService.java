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
package io.github.doriangrelu.ostore.application;

import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.application.port.out.BucketRepository;
import io.github.doriangrelu.ostore.domain.bucket.Bucket;
import io.github.doriangrelu.ostore.domain.bucket.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.bucket.BucketName;
import io.github.doriangrelu.ostore.domain.bucket.BucketNotFoundException;
import java.time.Clock;
import java.util.List;

/**
 * Implémentation des cas d'usage des buckets.
 *
 * <p>La vérification d'existence avant création donne une erreur claire dans le cas courant ; la
 * contrainte d'unicité en base reste l'arbitre en cas de création concurrente.
 *
 * @param buckets stockage des métadonnées
 * @param clock horloge (injectée pour rester testable)
 * @param defaultDriverId driver attribué aux nouveaux buckets
 */
public record BucketService(BucketRepository buckets, Clock clock, String defaultDriverId) implements BucketUseCases {

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
