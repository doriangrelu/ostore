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
package io.github.doriangrelu.ostore.domain.model.vo;

import java.util.Objects;

/**
 * Emplacement physique d'un contenu : instance de driver et chemin du blob sous sa racine.
 *
 * <p>Le chemin est calculé à l'écriture par la stratégie de chemins du driver (ex. {@code 2026/09/25/<uuid>})
 * puis <b>conservé en base</b> : lecture et purge l'utilisent tel quel, même si la stratégie change ensuite.
 * Un remplacement écrit toujours un nouveau blob ; l'ancien est purgé (ADR-0006).
 *
 * @param driverId instance de driver
 * @param path chemin relatif du blob
 */
public record BlobLocation(String driverId, String path) {

    public BlobLocation {
        Objects.requireNonNull(driverId, "driverId");
        Objects.requireNonNull(path, "path");
    }
}
