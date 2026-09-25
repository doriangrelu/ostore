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
import org.jspecify.annotations.Nullable;

/**
 * Page d'objets, en ordre binaire (UTF-8) des clés.
 *
 * @param bucket nom du bucket
 * @param prefix préfixe demandé ({@code ""} = tous les objets)
 * @param objects objets de la page
 * @param nextContinuationToken jeton à renvoyer pour la page suivante, {@code null} sur la dernière page
 */
@Schema(description = "Page of objects, sorted by key (binary UTF-8 order)")
public record ObjectListResponse(
        String bucket,
        String prefix,
        List<ObjectSummaryResponse> objects,

        @Schema(description = "Token of the next page, absent on the last page") @Nullable
        String nextContinuationToken) {

    public ObjectListResponse {
        objects = List.copyOf(objects);
    }
}
