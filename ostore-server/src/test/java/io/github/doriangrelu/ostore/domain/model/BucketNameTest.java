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
package io.github.doriangrelu.ostore.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.domain.exception.InvalidBucketNameException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Règles de nommage S3 : règle pure et combinatoire, seul cas justifiant un test unitaire (ADR-0011). */
class BucketNameTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "abc",
                "my-bucket",
                "invoices.2026",
                "a1b2c3",
                "sixty-three-characters-is-the-longest-name-accepted-by-s3-rules" // 63 caractères
            })
    void should_accept_name_when_it_follows_s3_rules(String name) {
        assertThatCode(() -> new BucketName(name)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ab", // trop court
                "Uppercase", // majuscules
                "under_score", // caractère interdit
                "-starts-with-dash", // doit commencer par une lettre ou un chiffre
                "ends-with-dot.", // doit finir par une lettre ou un chiffre
                "two..dots", // points consécutifs
                "192.168.1.10", // format d'adresse IP
                "xn--reserved-prefix", // préfixe réservé
                "reserved-suffix-s3alias" // suffixe réservé
            })
    void should_reject_name_when_it_breaks_s3_rules(String name) {
        assertThatThrownBy(() -> new BucketName(name)).isInstanceOf(InvalidBucketNameException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {64, 100})
    void should_reject_name_when_it_is_longer_than_63_characters(int length) {
        assertThatThrownBy(() -> new BucketName("b".repeat(length))).isInstanceOf(InvalidBucketNameException.class);
    }
}
