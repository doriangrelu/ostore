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

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Sérialisation XML des réponses S3.
 *
 * <p>Le XML est produit ici, et non par un convertisseur HTTP global, pour que l'API REST reste en JSON
 * quelle que soit la négociation de contenu. Les éléments S3 sont en « UpperCamelCase ».
 */
@Component
class S3Xml {

    private final XmlMapper mapper = XmlMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .build();

    /** Réponse XML avec le statut donné. */
    ResponseEntity<String> response(HttpStatusCode status, Object body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(mapper.writeValueAsString(body));
    }
}
