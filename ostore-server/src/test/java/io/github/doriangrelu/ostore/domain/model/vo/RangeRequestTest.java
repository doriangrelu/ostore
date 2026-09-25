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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.domain.exception.RangeNotSatisfiableException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Plages HTTP (RFC 9110) : règle pure et combinatoire (ADR-0011). */
class RangeRequestTest {

    private static final long SIZE = 100;

    @ParameterizedTest
    @CsvSource({
        "bytes=0-9,     0,  9", // plage explicite
        "bytes=90-,     90, 99", // jusqu'à la fin
        "bytes=-10,     90, 99", // 10 derniers octets
        "bytes=95-200,  95, 99", // fin tronquée à la taille
        "bytes=-200,    0,  99" // suffixe plus long que le contenu
    })
    void should_resolve_satisfiable_ranges(String header, long start, long end) {
        var resolved = RangeRequest.parse(header).orElseThrow().resolve(SIZE);

        assertThat(resolved).isEqualTo(new RangeRequest.Resolved(start, end, SIZE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bytes=100-", "bytes=-0"})
    void should_reject_ranges_outside_the_content(String header) {
        var range = RangeRequest.parse(header).orElseThrow();

        assertThatThrownBy(() -> range.resolve(SIZE)).isInstanceOf(RangeNotSatisfiableException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"items=0-1", "bytes=0-1,5-6", "bytes=5-1", "bytes=-", "bytes=abc"})
    void should_ignore_absent_multiple_or_malformed_ranges(String header) {
        assertThat(RangeRequest.parse(header)).isEmpty();
    }
}
