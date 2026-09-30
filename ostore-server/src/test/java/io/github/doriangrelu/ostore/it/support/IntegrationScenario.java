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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.doriangrelu.ostore.it.client.OStoreRestClient;
import io.github.doriangrelu.ostore.it.client.ObjectRestClient;
import io.github.doriangrelu.ostore.it.client.TransactionRestClient;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Socle des scénarios traversants (ADR-0011) : le client réel d'OStore et quelques helpers.
 *
 * <p>Un scénario est une interface dont les méthodes {@code @Test} sont des {@code default} : il est écrit
 * une seule fois et exécuté sur chaque SGBD par une classe vide qui étend {@link PostgresIntegrationTest}
 * ou {@link OracleIntegrationTest}.
 */
public interface IntegrationScenario {

    /** Client REST des buckets, construit sur l'interface {@code BucketApi} du contrat. */
    OStoreRestClient rest();

    /** Client REST des objets, construit sur l'interface {@code ObjectApi} du contrat (flux binaires). */
    ObjectRestClient objects();

    /** Client REST des transactions, construit sur l'interface {@code TransactionApi} du contrat. */
    TransactionRestClient transactions();

    /** Accès au stockage physique (chemins enregistrés, présence des blobs). */
    StorageProbe storage();

    /** Nom de bucket unique : isole les scénarios sans nettoyer la base. */
    default String uniqueBucketName() {
        return "it-" + UUID.randomUUID().toString().substring(0, 18);
    }

    /** Erreur REST (ProblemDetail) levée par l'appel ; échec d'assertion si l'appel réussit. */
    static Problem problemOf(ThrowingCallable call) {
        var error = catchThrowableOfType(HttpClientErrorException.class, call);
        assertThat(error).as("expected an HTTP client error").isNotNull();
        return error.getResponseBodyAs(Problem.class);
    }

    /** Vue simplifiée d'un ProblemDetail, de son code métier et, pour TRANSACTION_CLOSED, du statut réel. */
    record Problem(
            int status,
            String code,
            String detail,
            @Nullable String transactionStatus) {}
}
