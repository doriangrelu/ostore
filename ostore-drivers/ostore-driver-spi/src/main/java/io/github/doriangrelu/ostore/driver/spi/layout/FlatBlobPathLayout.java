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
package io.github.doriangrelu.ostore.driver.spi.layout;

import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import java.time.Instant;
import java.util.UUID;

/** Stratégie {@code flat} : {@code <id>} à la racine, adaptée aux stockages objet sans notion de répertoire. */
public final class FlatBlobPathLayout implements BlobPathLayout {

    public static final String NAME = "flat";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BlobPath pathOf(UUID blobId, Instant createdAt) {
        return new BlobPath(blobId.toString());
    }
}
