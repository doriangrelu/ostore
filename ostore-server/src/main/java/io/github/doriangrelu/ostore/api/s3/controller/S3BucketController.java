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
package io.github.doriangrelu.ostore.api.s3.controller;

import io.github.doriangrelu.ostore.api.s3.document.BucketEntry;
import io.github.doriangrelu.ostore.api.s3.document.ListAllMyBucketsResult;
import io.github.doriangrelu.ostore.api.s3.document.Owner;
import io.github.doriangrelu.ostore.api.s3.serializer.S3XmlSerializer;
import io.github.doriangrelu.ostore.application.port.in.BucketUseCases;
import io.github.doriangrelu.ostore.domain.model.BucketName;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Opérations S3 sur les buckets, en adressage « path-style » sous {@code ostore.s3.base-path}
 * (ADR-0013) : ListBuckets, CreateBucket, HeadBucket, DeleteBucket.
 */
@RestController
@RequestMapping("${ostore.s3.base-path}")
public class S3BucketController {

    private final BucketUseCases buckets;
    private final S3XmlSerializer xml;

    public S3BucketController(BucketUseCases buckets, S3XmlSerializer xml) {
        this.buckets = buckets;
        this.xml = xml;
    }

    @GetMapping(path = {"", "/"})
    ResponseEntity<String> listBuckets() {
        var entries = buckets.list().stream()
                .map(bucket -> new BucketEntry(bucket.name().value(), bucket.createdAt()))
                .toList();
        return xml.response(HttpStatus.OK, new ListAllMyBucketsResult(Owner.DEFAULT, entries));
    }

    @PutMapping("/{bucket}")
    ResponseEntity<Void> createBucket(@PathVariable String bucket) {
        var created = buckets.create(new BucketName(bucket));
        return ResponseEntity.ok()
                .location(URI.create("/" + created.name().value()))
                .build();
    }

    @RequestMapping(path = "/{bucket}", method = RequestMethod.HEAD)
    ResponseEntity<Void> headBucket(@PathVariable String bucket) {
        buckets.get(new BucketName(bucket));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bucket}")
    ResponseEntity<Void> deleteBucket(@PathVariable String bucket) {
        buckets.delete(new BucketName(bucket));
        return ResponseEntity.noContent().build();
    }
}
