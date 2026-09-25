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

import io.github.doriangrelu.ostore.domain.bucket.Bucket;
import io.github.doriangrelu.ostore.domain.bucket.BucketAlreadyExistsException;
import io.github.doriangrelu.ostore.domain.bucket.BucketName;
import java.util.List;
import java.util.Optional;

/** Stockage des métadonnées de buckets. */
public interface BucketRepository {

    /**
     * Enregistre un nouveau bucket.
     *
     * @throws BucketAlreadyExistsException si le nom est déjà pris, y compris en cas de création
     *     concurrente (garantie par la contrainte d'unicité en base)
     */
    Bucket insert(Bucket bucket);

    Optional<Bucket> findByName(BucketName name);

    /** Tous les buckets, triés par nom. */
    List<Bucket> findAll();

    void delete(Bucket bucket);
}
