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

import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.doriangrelu.ostore.it.client.OStoreRestClient;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.web.client.HttpClientErrorException;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Socle des scénarios traversants (ADR-0011) : les clients réels d'OStore et quelques helpers.
 *
 * <p>Un scénario est une interface dont les méthodes {@code @Test} sont des {@code default} : il est écrit
 * une seule fois et exécuté sur chaque SGBD par une classe vide qui étend {@link PostgresIntegrationTest}
 * ou {@link OracleIntegrationTest}.
 */
public interface IntegrationScenario {

    /** Client de l'API REST, construit sur les interfaces du contrat. */
    OStoreRestClient rest();

    /** Client S3 officiel d'AWS, pointé sur OStore. */
    S3Client s3();

    /** Nom de bucket unique : isole les scénarios sans nettoyer la base. */
    default String uniqueBucketName() {
        return "it-" + UUID.randomUUID().toString().substring(0, 18);
    }

    /** Erreur REST (ProblemDetail) levée par l'appel. */
    static Problem problemOf(ThrowingCallable call) {
        return catchThrowableOfType(HttpClientErrorException.class, call).getResponseBodyAs(Problem.class);
    }

    /** Vue simplifiée d'un ProblemDetail et de son code métier. */
    record Problem(int status, String code, String detail) {}
}
