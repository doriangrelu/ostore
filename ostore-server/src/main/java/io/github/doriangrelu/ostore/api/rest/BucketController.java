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
package io.github.doriangrelu.ostore.api.rest;

import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.contract.bucket.BucketApi;
import io.github.doriangrelu.ostore.contract.bucket.BucketListResponse;
import io.github.doriangrelu.ostore.contract.bucket.BucketResponse;
import io.github.doriangrelu.ostore.contract.bucket.CreateBucketRequest;
import io.github.doriangrelu.ostore.domain.bucket.Bucket;
import io.github.doriangrelu.ostore.domain.bucket.BucketName;
import org.springframework.web.bind.annotation.RestController;

/** Adaptateur REST des buckets : mapping HTTP, validation et documentation viennent du contrat. */
@RestController
class BucketController implements BucketApi {

    private final BucketUseCases buckets;

    BucketController(BucketUseCases buckets) {
        this.buckets = buckets;
    }

    @Override
    public BucketResponse create(CreateBucketRequest request) {
        return toResponse(buckets.create(new BucketName(request.name())));
    }

    @Override
    public BucketListResponse list() {
        return new BucketListResponse(
                buckets.list().stream().map(BucketController::toResponse).toList());
    }

    @Override
    public BucketResponse get(String name) {
        return toResponse(buckets.get(new BucketName(name)));
    }

    @Override
    public void delete(String name) {
        buckets.delete(new BucketName(name));
    }

    private static BucketResponse toResponse(Bucket bucket) {
        return new BucketResponse(bucket.id(), bucket.name().value(), bucket.createdAt());
    }
}
