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

import io.github.doriangrelu.ostore.infrastructure.persistence.converter.BytesToUuidConverter;
import io.github.doriangrelu.ostore.infrastructure.persistence.converter.UuidToBytesConverter;
import io.github.doriangrelu.ostore.infrastructure.persistence.entity.BucketEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.dialect.OracleDialect;
import org.springframework.data.relational.core.mapping.NamingStrategy;

/**
 * Configuration Spring Data JDBC commune à PostgreSQL et Oracle (ADR-0004).
 *
 * <p>Remplace la configuration par défaut de Spring Boot pour deux raisons :
 *
 * <ul>
 *   <li>les identifiants ne sont <b>pas</b> mis entre guillemets : les scripts créent des noms non
 *       quotés, stockés en minuscules par PostgreSQL et en majuscules par Oracle ; seuls des noms non
 *       quotés sont résolus correctement sur les deux ;
 *   <li>Oracle n'a pas de type UUID : les identifiants y sont stockés en {@code RAW(16)}, ce qui
 *       impose une conversion {@link UUID} ⇄ {@code byte[]} propre à ce SGBD.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class PersistenceConfiguration extends AbstractJdbcConfiguration {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.context = applicationContext;
    }

    @Bean
    @Override
    public JdbcMappingContext jdbcMappingContext(
            Optional<NamingStrategy> namingStrategy,
            JdbcCustomConversions customConversions,
            RelationalManagedTypes jdbcManagedTypes) {
        var mappingContext = super.jdbcMappingContext(namingStrategy, customConversions, jdbcManagedTypes);
        mappingContext.setForceQuote(false);
        return mappingContext;
    }

    /** Les entités ne sont pas dans le package de cette configuration : il faut l'indiquer. */
    @Override
    protected Collection<String> getMappingBasePackages() {
        return List.of(BucketEntity.class.getPackageName());
    }

    @Override
    protected List<?> userConverters() {
        return context.getBean(JdbcDialect.class) instanceof OracleDialect
                ? List.of(UuidToBytesConverter.INSTANCE, BytesToUuidConverter.INSTANCE)
                : List.of();
    }
}
