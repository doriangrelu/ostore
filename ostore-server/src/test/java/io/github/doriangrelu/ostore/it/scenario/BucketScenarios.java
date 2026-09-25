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
package io.github.doriangrelu.ostore.it.scenario;

import static io.github.doriangrelu.ostore.it.support.IntegrationScenario.problemOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.it.support.IntegrationScenario;
import org.junit.jupiter.api.Test;

/** Scénarios « buckets » de bout en bout, via le client construit sur le contrat. */
public interface BucketScenarios extends IntegrationScenario {

    @Test
    default void should_create_list_and_delete_a_bucket() {
        var name = uniqueBucketName();

        // Given : un bucket créé
        var created = rest().create(new CreateBucketRequest(name));
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo(name);

        // Then : il est lisible et listé
        assertThat(rest().get(name)).isEqualTo(created);
        assertThat(rest().list().buckets()).extracting(BucketResponse::name).contains(name);

        // When : il est supprimé
        rest().delete(name);

        // Then : il a disparu, et une seconde suppression est signalée
        assertThat(rest().list().buckets()).extracting(BucketResponse::name).doesNotContain(name);
        assertThat(problemOf(() -> rest().get(name)))
                .extracting(Problem::status, Problem::code)
                .containsExactly(404, "BUCKET_NOT_FOUND");
        assertThat(problemOf(() -> rest().delete(name)).code()).isEqualTo("BUCKET_NOT_FOUND");
    }

    @Test
    default void should_reject_duplicate_and_invalid_bucket_names() {
        var name = uniqueBucketName();
        rest().create(new CreateBucketRequest(name));

        // Then : le nom ne peut plus être pris
        assertThat(problemOf(() -> rest().create(new CreateBucketRequest(name))))
                .extracting(Problem::status, Problem::code)
                .containsExactly(409, "BUCKET_ALREADY_EXISTS");

        // And : un nom hors règles de nommage est refusé avec un code métier
        assertThat(problemOf(() -> rest().create(new CreateBucketRequest("Invalid_Bucket"))))
                .extracting(Problem::status, Problem::code)
                .containsExactly(400, "INVALID_BUCKET_NAME");
    }
}
