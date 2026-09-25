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

import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Corps d'erreur S3 ({@code <Error>}), sans espace de noms, conforme à AWS.
 *
 * @param code code d'erreur AWS (ex. {@code NoSuchBucket})
 * @param message message lisible
 * @param resource ressource concernée
 * @param requestId identifiant de la requête
 */
@JacksonXmlRootElement(localName = "Error")
public record ErrorDocument(String code, String message, String resource, String requestId) {}
