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
package io.github.doriangrelu.ostore.it.support;

import io.github.doriangrelu.ostore.application.port.out.StorageDrivers;
import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import io.github.doriangrelu.ostore.driver.spi.model.BlobPath;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Regard sur le stockage physique, pour vérifier ce que l'API ne montre pas : chemin enregistré en base et
 * présence réelle du blob sur son driver (purge).
 */
public final class StorageProbe {

    private final JdbcClient jdbc;
    private final StorageDrivers drivers;

    StorageProbe(JdbcClient jdbc, StorageDrivers drivers) {
        this.jdbc = jdbc;
        this.drivers = drivers;
    }

    /** Emplacement enregistré en base pour l'objet actif de cette clé. */
    public BlobLocation blobOf(String bucket, String key) {
        return jdbc.sql("""
                        SELECT o.DRIVER_ID, o.BLOB_PATH FROM OST_OBJECT o JOIN OST_BUCKET b ON b.ID = o.BUCKET_ID
                        WHERE b.NAME = ? AND o.OBJECT_KEY = ?""")
                .params(bucket, key)
                .query((row, _) -> new BlobLocation(row.getString("DRIVER_ID"), row.getString("BLOB_PATH")))
                .single();
    }

    /** Indique si le blob existe encore sur son driver. */
    public boolean exists(BlobLocation location) {
        return drivers.driver(location.driverId()).exists(new BlobPath(location.path()));
    }
}
