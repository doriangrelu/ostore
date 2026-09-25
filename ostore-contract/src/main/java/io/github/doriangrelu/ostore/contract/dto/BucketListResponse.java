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
package io.github.doriangrelu.ostore.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Liste des buckets, triée par nom.
 *
 * <p>Enveloppe plutôt que tableau nu : permet d'ajouter une pagination sans rupture de contrat.
 *
 * @param buckets buckets triés par nom
 */
@Schema(description = "Buckets sorted by name")
public record BucketListResponse(List<BucketResponse> buckets) {

    public BucketListResponse {
        buckets = List.copyOf(buckets);
    }
}
