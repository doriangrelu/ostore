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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Page d'une liste d'objets.
 *
 * @param objects objets de la page, par ordre de création (identifiants UUID v7)
 * @param resumeAfter position de reprise s'il reste d'autres pages, sinon vide
 */
public record ObjectPage(List<ObjectSummary> objects, Optional<UUID> resumeAfter) {

    public ObjectPage {
        objects = List.copyOf(objects);
    }

    /** Page construite à partir d'un élément de plus que la taille demandée (indique s'il en reste). */
    public static ObjectPage of(List<ObjectSummary> found, int pageSize) {
        if (found.size() <= pageSize) {
            return new ObjectPage(found, Optional.empty());
        }
        var page = found.subList(0, pageSize);
        return new ObjectPage(page, Optional.of(page.getLast().rowId()));
    }
}
