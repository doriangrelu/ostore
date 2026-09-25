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
package io.github.doriangrelu.ostore.infrastructure.persistence.adapter;

import io.github.doriangrelu.ostore.application.port.out.BlobPurgeQueue;
import io.github.doriangrelu.ostore.domain.model.vo.BlobLocation;
import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * File de purge en base ({@code OST_BLOB_PURGE}). Appelée dans une transaction existante, elle en fait partie :
 * la purge n'est planifiée que si le remplacement ou la suppression est validé.
 */
@Repository
public class JdbcBlobPurgeQueue implements BlobPurgeQueue {

    private final JdbcClient jdbc;
    private final Clock clock;

    public JdbcBlobPurgeQueue(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void schedule(BlobLocation location) {
        var now = clock.instant().atOffset(ZoneOffset.UTC);
        jdbc.sql("""
                        INSERT INTO OST_BLOB_PURGE (DRIVER_ID, BLOB_PATH, NOT_BEFORE, ATTEMPTS, CREATED_AT)
                        VALUES (:driverId, :path, :now, 0, :now)""")
                .param("driverId", location.driverId())
                .param("path", location.path())
                .param("now", now)
                .update();
    }
}
