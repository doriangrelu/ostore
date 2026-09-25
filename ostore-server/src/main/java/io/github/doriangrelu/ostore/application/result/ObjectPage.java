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
package io.github.doriangrelu.ostore.application.result;

import io.github.doriangrelu.ostore.domain.model.ObjectSummary;
import io.github.doriangrelu.ostore.domain.model.vo.ObjectKey;
import java.util.List;
import java.util.Optional;

/**
 * Page d'une liste d'objets.
 *
 * @param objects objets de la page, en ordre binaire des clés
 * @param lastKey dernière clé de la page s'il en reste d'autres (point de reprise), sinon vide
 */
public record ObjectPage(List<ObjectSummary> objects, Optional<ObjectKey> lastKey) {

    public ObjectPage {
        objects = List.copyOf(objects);
    }
}
