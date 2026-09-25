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
package io.github.doriangrelu.ostore.api.rest.controller;

import static io.github.doriangrelu.ostore.api.rest.mapper.BucketDtoMapper.toListResponse;
import static io.github.doriangrelu.ostore.api.rest.mapper.BucketDtoMapper.toResponse;

import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.contract.api.BucketApi;
import io.github.doriangrelu.ostore.contract.dto.BucketListResponse;
import io.github.doriangrelu.ostore.contract.dto.BucketResponse;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.domain.model.BucketName;
import org.springframework.web.bind.annotation.RestController;

/** Adaptateur REST des buckets : mapping HTTP, validation et documentation viennent du contrat. */
@RestController
public class BucketController implements BucketApi {

    private final BucketUseCases buckets;

    public BucketController(BucketUseCases buckets) {
        this.buckets = buckets;
    }

    @Override
    public BucketResponse create(CreateBucketRequest request) {
        return toResponse(buckets.create(new BucketName(request.name())));
    }

    @Override
    public BucketListResponse list() {
        return toListResponse(buckets.list());
    }

    @Override
    public BucketResponse get(String name) {
        return toResponse(buckets.get(new BucketName(name)));
    }

    @Override
    public void delete(String name) {
        buckets.delete(new BucketName(name));
    }
}
