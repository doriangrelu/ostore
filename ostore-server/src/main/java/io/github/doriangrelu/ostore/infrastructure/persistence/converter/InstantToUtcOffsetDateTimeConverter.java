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
package io.github.doriangrelu.ostore.infrastructure.persistence.converter;

import java.sql.JDBCType;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;

/**
 * Écrit chaque date en <b>UTC</b>, quel que soit le fuseau de la JVM.
 *
 * <p>Sans ce convertisseur, un {@link Instant} devient un {@code java.sql.Timestamp}, que le driver JDBC
 * écrit avec le fuseau de la JVM ; Oracle ({@code TIMESTAMP WITH TIME ZONE}) conserve alors ce décalage
 * (ex. {@code +02:00}). Le type SQL est imposé via {@link JdbcValue} : le driver reçoit un
 * {@code OffsetDateTime} en {@code +00:00}, transmis tel quel.
 */
@WritingConverter
public enum InstantToUtcOffsetDateTimeConverter implements Converter<Instant, JdbcValue> {
    INSTANCE;

    @Override
    public JdbcValue convert(Instant instant) {
        return JdbcValue.of(instant.atOffset(ZoneOffset.UTC), JDBCType.TIMESTAMP_WITH_TIMEZONE);
    }
}
