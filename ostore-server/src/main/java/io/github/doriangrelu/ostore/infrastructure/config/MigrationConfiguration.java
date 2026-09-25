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
package io.github.doriangrelu.ostore.infrastructure.config;

import io.github.doriangrelu.ostore.infrastructure.properties.DatabaseProperties;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * Configuration de Flyway pour les scripts OStore (ADR-0005).
 *
 * <ul>
 *   <li>Placeholders de tablespace : ils contiennent la clause complète ({@code TABLESPACE xxx}) et valent
 *       une chaîne vide par défaut, donc les scripts restent valides sans tablespace dédié. Les scripts
 *       PostgreSQL ne les utilisent pas.
 *   <li>Table d'historique préfixée comme les autres ({@code OST_SCHEMA_HISTORY}). Flyway met ce nom entre
 *       guillemets : il est donc écrit dans la casse native du SGBD (majuscules pour Oracle, minuscules
 *       pour PostgreSQL), afin que les DBA puissent l'interroger sans guillemets.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DatabaseProperties.class)
class MigrationConfiguration {

    private static final String HISTORY_TABLE = "OST_SCHEMA_HISTORY";

    @Bean
    FlywayConfigurationCustomizer ostoreFlywayConventions(DatabaseProperties database) {
        return flyway -> {
            Map<String, String> placeholders = new HashMap<>(flyway.getPlaceholders());
            placeholders.put("ostore_table_tablespace_clause", database.tableTablespaceClause());
            placeholders.put("ostore_index_tablespace_clause", database.indexTablespaceClause());
            flyway.placeholders(placeholders);
            flyway.table(isOracle(flyway.getDataSource()) ? HISTORY_TABLE : HISTORY_TABLE.toLowerCase(Locale.ROOT));
        };
    }

    private static boolean isOracle(DataSource dataSource) {
        try {
            String product = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
            return product.toLowerCase(Locale.ROOT).contains("oracle");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to detect the database vendor for migrations", e);
        }
    }
}
