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
package io.github.doriangrelu.ostore.infrastructure.migration;

import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Options de déploiement du schéma, préfixe {@code ostore.database}.
 *
 * <p>Les noms de tablespace sont injectés tels quels dans le DDL Oracle : ils sont donc validés comme
 * identifiants SQL simples, ce qui interdit toute injection.
 *
 * @param tableTablespace tablespace des tables (Oracle), absent pour celui par défaut du schéma
 * @param indexTablespace tablespace des index (Oracle), absent pour celui par défaut du schéma
 */
@ConfigurationProperties("ostore.database")
public record DatabaseProperties(
        @Nullable String tableTablespace, @Nullable String indexTablespace) {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0,127}");

    public DatabaseProperties {
        tableTablespace = validate("table-tablespace", tableTablespace);
        indexTablespace = validate("index-tablespace", indexTablespace);
    }

    /** Clause {@code TABLESPACE xxx} à ajouter aux tables, ou chaîne vide. */
    public String tableTablespaceClause() {
        return clause(tableTablespace);
    }

    /** Clause {@code TABLESPACE xxx} à ajouter aux index, ou chaîne vide. */
    public String indexTablespaceClause() {
        return clause(indexTablespace);
    }

    private static String clause(@Nullable String tablespace) {
        return Optional.ofNullable(tablespace).map("TABLESPACE "::concat).orElse("");
    }

    private static @Nullable String validate(String property, @Nullable String tablespace) {
        if (tablespace == null || tablespace.isBlank()) {
            return null;
        }
        if (!SQL_IDENTIFIER.matcher(tablespace).matches()) {
            throw new IllegalArgumentException(
                    "ostore.database.%s is not a valid SQL identifier: %s".formatted(property, tablespace));
        }
        return tablespace;
    }
}
