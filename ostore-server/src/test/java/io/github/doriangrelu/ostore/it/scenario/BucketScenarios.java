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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.it.support.IntegrationScenario;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/** Scénarios « buckets » : l'API REST et l'API S3 partagent les mêmes buckets. */
public interface BucketScenarios extends IntegrationScenario {

    @Test
    default void should_share_bucket_lifecycle_between_rest_and_s3() {
        var name = uniqueBucketName();

        // Given : un bucket créé par l'API REST
        var created = rest().create(new CreateBucketRequest(name));
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo(name);

        // Then : il est vu par un client S3 standard
        assertThat(s3().listBuckets().buckets()).extracting(Bucket::name).contains(name);
        s3().headBucket(request -> request.bucket(name));
        assertThat(rest().list().buckets()).extracting(BucketResponse::name).contains(name);

        // When : il est supprimé par le client S3
        s3().deleteBucket(request -> request.bucket(name));

        // Then : il a disparu des deux API
        assertThat(problemOf(() -> rest().get(name)).code()).isEqualTo("BUCKET_NOT_FOUND");
        assertThatThrownBy(() -> s3().headBucket(request -> request.bucket(name)))
                .isInstanceOf(NoSuchBucketException.class);
    }

    @Test
    default void should_reject_duplicate_and_invalid_bucket_names() {
        var name = uniqueBucketName();

        // Given : un bucket créé par le client S3, visible en REST
        s3().createBucket(request -> request.bucket(name));
        assertThat(rest().get(name).name()).isEqualTo(name);

        // Then : le nom ne peut plus être pris, quelle que soit l'API
        assertThat(problemOf(() -> rest().create(new CreateBucketRequest(name))))
                .extracting(Problem::status, Problem::code)
                .containsExactly(409, "BUCKET_ALREADY_EXISTS");
        assertThatThrownBy(() -> s3().createBucket(request -> request.bucket(name)))
                .isInstanceOf(BucketAlreadyOwnedByYouException.class);

        // And : un nom hors règles S3 est refusé avec un code métier
        assertThat(problemOf(() -> rest().create(new CreateBucketRequest("Invalid_Bucket"))))
                .extracting(Problem::status, Problem::code)
                .containsExactly(400, "INVALID_BUCKET_NAME");
    }
}
