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
package io.github.doriangrelu.ostore.api.rest.mapper;

import io.github.doriangrelu.ostore.contract.dto.BucketListResponse;
import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.domain.model.Bucket;
import java.util.List;

/** Conversion des buckets du domaine en DTO du contrat. */
public final class BucketDtoMapper {

    private BucketDtoMapper() {}

    public static BucketResponse toResponse(Bucket bucket) {
        return new BucketResponse(bucket.id(), bucket.name().value(), bucket.createdAt());
    }

    public static BucketListResponse toListResponse(List<Bucket> buckets) {
        return new BucketListResponse(
                buckets.stream().map(BucketDtoMapper::toResponse).toList());
    }
}
