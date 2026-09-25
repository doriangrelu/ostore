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
package io.github.doriangrelu.ostore.api.s3.document;

import io.github.doriangrelu.ostore.api.s3.constant.S3Constants;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Réponse S3 de ListBuckets.
 *
 * @param owner propriétaire des buckets
 * @param buckets buckets triés par nom
 */
@JacksonXmlRootElement(localName = "ListAllMyBucketsResult", namespace = S3Constants.NAMESPACE)
public record ListAllMyBucketsResult(
        Owner owner,

        @JacksonXmlElementWrapper(localName = "Buckets") @JacksonXmlProperty(localName = "Bucket")
        List<BucketEntry> buckets) {}
