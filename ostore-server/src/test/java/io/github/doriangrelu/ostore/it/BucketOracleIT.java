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

import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.it.scenario.BucketScenarios;
import io.github.doriangrelu.ostore.it.support.OracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class BucketOracleIT extends OracleIntegrationTest implements BucketScenarios {

    @Autowired
    private JdbcClient jdbc;

    /**
     * Oracle conserve le décalage horaire reçu : les dates doivent y être écrites en UTC, quel que soit
     * le fuseau de la JVM (forcé à Europe/Paris pour les TI, voir le POM parent).
     */
    @Test
    void should_store_dates_in_utc() {
        var name = uniqueBucketName();
        rest().create(new CreateBucketRequest(name));

        var storedOffset = jdbc.sql("SELECT TO_CHAR(CREATED_AT, 'TZH:TZM') FROM OST_BUCKET WHERE NAME = ?")
                .param(name)
                .query(String.class)
                .single();

        assertThat(storedOffset).isEqualTo("+00:00");
    }
}
