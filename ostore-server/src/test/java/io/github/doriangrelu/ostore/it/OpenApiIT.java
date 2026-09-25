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
package io.github.doriangrelu.ostore.it;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.doriangrelu.ostore.it.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

/** La spécification OpenAPI est générée depuis le contrat et ne décrit que l'API REST (ADR-0003). */
class OpenApiIT extends PostgresIntegrationTest {

    @Test
    void should_publish_openapi_generated_from_the_contract() {
        var openApi = rest().openApiDocument();

        assertThat(openApi)
                .contains("\"/api/v1/buckets\"", "\"/api/v1/buckets/{name}\"")
                .contains("INVALID_BUCKET_NAME", "BUCKET_ALREADY_EXISTS")
                .doesNotContain("\"/s3");
    }
}
