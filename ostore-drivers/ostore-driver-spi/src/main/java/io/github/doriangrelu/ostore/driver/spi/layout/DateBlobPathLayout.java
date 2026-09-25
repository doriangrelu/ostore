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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Stratégie {@code date} (par défaut) : {@code année/mois/jour/<id>}, date de dépôt en <b>UTC</b>.
 *
 * <p>Facilite l'exploitation : sauvegardes incrémentales, archivage ou inspection par période.
 */
public final class DateBlobPathLayout implements BlobPathLayout {

    public static final String NAME = "date";

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BlobPath pathOf(UUID blobId, Instant createdAt) {
        return new BlobPath(DAY.format(createdAt) + "/" + blobId);
    }
}
