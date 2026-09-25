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
package io.github.doriangrelu.ostore.api.s3;

import java.time.Instant;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Documents XML du protocole S3 (réponses et erreurs). */
final class S3Documents {

    /** Espace de noms XML des réponses S3. */
    static final String NAMESPACE = "http://s3.amazonaws.com/doc/2006-03-01/";

    /** Propriétaire affiché : OStore n'a pas encore de notion de compte (ADR-0008). */
    static final Owner DEFAULT_OWNER = new Owner("ostore", "ostore");

    private S3Documents() {}

    /** Réponse de ListBuckets. */
    @JacksonXmlRootElement(localName = "ListAllMyBucketsResult", namespace = NAMESPACE)
    record ListAllMyBucketsResult(
            Owner owner,

            @JacksonXmlElementWrapper(localName = "Buckets") @JacksonXmlProperty(localName = "Bucket")
            List<BucketEntry> buckets) {}

    record Owner(@JacksonXmlProperty(localName = "ID") String id, String displayName) {}

    record BucketEntry(String name, Instant creationDate) {}

    /** Corps d'erreur S3, sans espace de noms (conforme à AWS). */
    @JacksonXmlRootElement(localName = "Error")
    record ErrorDocument(String code, String message, String resource, String requestId) {}
}
