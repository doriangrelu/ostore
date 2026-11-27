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
package io.github.doriangrelu.ostore.it.scenario;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.doriangrelu.ostore.it.support.IntegrationScenario;
import io.github.doriangrelu.ostore.it.support.WriteGate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

/**
 * Écritures concurrentes sur un même objet (ADR-0016) : le driver de test retient les écritures de blob au
 * {@link WriteGate}, pour que les requêtes entrent ensemble dans leur phase en base. Quel que soit l'ordre,
 * l'une gagne et l'autre reçoit une erreur métier, jamais une erreur serveur.
 */
public interface ConcurrentWriteScenarios extends IntegrationScenario {

    byte[] V1 = "version 1".getBytes(UTF_8);
    byte[] V2 = "version 2".getBytes(UTF_8);
    byte[] V3 = "version 3".getBytes(UTF_8);
    Executor CALLS = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    default void openTheGate() {
        WriteGate.open();
    }

    @RepeatedTest(5)
    default void should_stage_an_object_in_a_single_transaction_when_two_compete() {
        var document = objects().create(newBucket(), "contract.pdf", "application/pdf", V1);
        var first = transactions().open(Duration.ofMinutes(5));
        var second = transactions().open(Duration.ofMinutes(5));

        // When : deux transactions préparent le remplacement du même objet au même instant
        WriteGate.close();
        var staging = outcomeOf(() -> objects().replaceIn(first.id(), document.id(), V2));
        var competing = outcomeOf(() -> objects().replaceIn(second.id(), document.id(), V3));
        WriteGate.awaitArrivals(2);
        WriteGate.open();

        // Then : une seule réussit, l'autre trouve l'objet verrouillé
        assertThat(List.of(staging.join(), competing.join()))
                .containsExactlyInAnyOrder(new Outcome(200, null), new Outcome(409, "OBJECT_LOCKED"));
        transactions().rollback(first.id());
        transactions().rollback(second.id());
    }

    @Test
    default void should_refuse_a_replacement_while_a_concurrent_staging_is_not_yet_committed() {
        var document = objects().create(newBucket(), "contract.pdf", "application/pdf", V1);
        var transaction = transactions().open(Duration.ofMinutes(5));
        CompletableFuture<Outcome> replacing;

        // Given : une transaction prépare le remplacement de l'objet, et sa requête n'est pas encore terminée
        try (var staging = stageWithoutCommitting(document.id(), transaction.id(), V2)) {

            // When : un remplacement hors transaction arrive pendant ce temps
            replacing = outcomeOf(() -> objects().replace(document.id(), "text/plain", V3));

            // Then : il attend que la mise en attente soit enregistrée
            await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).until(() -> !replacing.isDone());
        }

        // And : il trouve alors l'objet verrouillé, et le commit applique bien la version préparée
        assertThat(replacing.join()).isEqualTo(new Outcome(409, "OBJECT_LOCKED"));
        transactions().commit(transaction.id());
        assertThat(objects().read(document.id())).isEqualTo(V2);
    }

    @RepeatedTest(5)
    default void should_either_delete_or_stage_an_object_when_both_compete() {
        var document = objects().create(newBucket(), "contract.pdf", "application/pdf", V1);
        var transaction = transactions().open(Duration.ofMinutes(5));

        // When : l'objet est supprimé pendant qu'une transaction prépare son remplacement
        WriteGate.close();
        var staging = outcomeOf(() -> objects().replaceIn(transaction.id(), document.id(), V2));
        WriteGate.awaitArrivals(1);
        var deletion = outcomeOf(() -> objects().delete(document.id()));
        WriteGate.open();

        // Then : soit la suppression passe et le remplacement ne trouve plus l'objet, soit l'inverse
        assertThat(List.of(deletion.join(), staging.join()))
                .isIn(
                        List.of(new Outcome(200, null), new Outcome(404, "OBJECT_NOT_FOUND")),
                        List.of(new Outcome(409, "OBJECT_LOCKED"), new Outcome(200, null)));
        transactions().rollback(transaction.id());
    }

    /** Statut HTTP d'un appel lancé en parallèle, et code métier en cas d'erreur. */
    private static CompletableFuture<Outcome> outcomeOf(Runnable call) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        call.run();
                        return new Outcome(200, null);
                    } catch (RestClientResponseException e) {
                        var problem = e.getResponseBodyAs(Problem.class);
                        return new Outcome(e.getStatusCode().value(), problem == null ? null : problem.code());
                    }
                },
                CALLS);
    }

    /**
     * Issue d'un appel : succès ({@code 200}, quel que soit le code 2xx) ou erreur avec son code métier.
     *
     * @param status {@code 200} pour un succès, sinon le statut HTTP de l'erreur
     * @param code code métier de l'erreur, {@code null} pour un succès
     */
    record Outcome(int status, @Nullable String code) {}
}
