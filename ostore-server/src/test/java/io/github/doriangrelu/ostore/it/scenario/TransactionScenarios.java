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

import static io.github.doriangrelu.ostore.it.support.IntegrationScenario.problemOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.contract.dto.ExtendTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectStatus;
import io.github.doriangrelu.ostore.contract.dto.ObjectSummaryResponse;
import io.github.doriangrelu.ostore.contract.dto.OpenTransactionRequest;
import io.github.doriangrelu.ostore.contract.dto.TransactionStatus;
import io.github.doriangrelu.ostore.it.support.IntegrationScenario;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;

/** Scénarios « transactions » de bout en bout (ADR-0016). */
public interface TransactionScenarios extends IntegrationScenario {

    byte[] V1 = "version 1".getBytes(UTF_8);
    byte[] V2 = "version 2".getBytes(UTF_8);
    byte[] V3 = "version 3".getBytes(UTF_8);
    Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    default void should_commit_the_objects_written_in_a_transaction() {
        var invoices = newBucket();
        var archive = newBucket();

        // Given : une transaction et deux dépôts, dans deux buckets
        var transaction = transactions().open(new OpenTransactionRequest(Duration.ofMinutes(5), "case-42"));
        assertThat(transaction.status()).isEqualTo(TransactionStatus.OPEN);
        assertThat(transaction.reference()).isEqualTo("case-42");
        var invoice = objects().createIn(transaction.id(), invoices, "invoice.pdf", V1);
        var copy = objects().createIn(transaction.id(), archive, "invoice-copy.pdf", V1);
        var discarded = objects().createIn(transaction.id(), invoices, "draft.pdf", V2);

        // Then : en attente, absents des listes mais lisibles par identifiant
        assertThat(invoice.status()).isEqualTo(ObjectStatus.PENDING);
        assertThat(invoice.transactionId()).isEqualTo(transaction.id());
        assertThat(objects().list(invoices, null, 1000, null).objects()).isEmpty();
        assertThat(objects().read(invoice.id())).isEqualTo(V1);

        // And : un objet en attente peut être retiré de la transaction
        objects().delete(discarded.id());
        assertThat(transactions().get(transaction.id()).objectCount()).isEqualTo(2);
        assertThat(transactions().objects(transaction.id(), 1000, null).objects())
                .extracting(ObjectSummaryResponse::id)
                .containsExactly(invoice.id(), copy.id());

        // When : la transaction est validée
        var committed = transactions().commit(transaction.id());

        // Then : les objets sont actifs et listés ; valider à nouveau est sans effet
        assertThat(committed.status()).isEqualTo(TransactionStatus.COMMITTED);
        assertThat(committed.closedAt()).isNotNull();
        assertThat(objects().metadata(invoice.id()).status()).isEqualTo(ObjectStatus.ACTIVE);
        assertThat(objects().list(archive, null, 1000, null).objects())
                .extracting(ObjectSummaryResponse::id)
                .containsExactly(copy.id());
        assertThat(transactions().commit(transaction.id()).status()).isEqualTo(TransactionStatus.COMMITTED);

        // And : une transaction validée n'accepte plus ni annulation ni écriture
        assertThat(problemOf(() -> transactions().rollback(transaction.id())))
                .extracting(Problem::code, Problem::transactionStatus)
                .containsExactly("TRANSACTION_CLOSED", "COMMITTED");
        assertThat(problemOf(() -> objects().createIn(transaction.id(), invoices, "late.pdf", V1))
                        .code())
                .isEqualTo("TRANSACTION_CLOSED");
    }

    @Test
    default void should_discard_pending_writes_on_rollback() {
        var bucket = newBucket();
        var transaction = transactions().open(Duration.ofMinutes(5));
        var pending = objects().createIn(transaction.id(), bucket, "tmp.bin", V1);
        var blob = storage().blobOf(pending.id());

        // Then : un bucket contenant un objet en attente n'est pas vide
        assertThat(problemOf(() -> rest().delete(bucket)).code()).isEqualTo("BUCKET_NOT_EMPTY");

        // When : la transaction est annulée
        var rolledBack = transactions().rollback(transaction.id());

        // Then : l'objet a disparu, son blob est purgé ; annuler à nouveau est sans effet, valider est refusé
        assertThat(rolledBack.status()).isEqualTo(TransactionStatus.ROLLED_BACK);
        assertThat(problemOf(() -> objects().metadata(pending.id())).code()).isEqualTo("OBJECT_NOT_FOUND");
        await().atMost(TIMEOUT).until(() -> !storage().exists(blob));
        assertThat(transactions().rollback(transaction.id()).status()).isEqualTo(TransactionStatus.ROLLED_BACK);
        assertThat(problemOf(() -> transactions().commit(transaction.id())))
                .extracting(Problem::status, Problem::transactionStatus)
                .containsExactly(409, "ROLLED_BACK");
        rest().delete(bucket);
    }

    @Test
    default void should_expire_an_implicit_transaction_and_delete_its_object() {
        var bucket = newBucket();

        // Given : un dépôt qui crée sa propre transaction, d'une seconde
        var pending = objects().createPending(bucket, "upload.tmp", V1, "PT1S");
        assertThat(pending.status()).isEqualTo(ObjectStatus.PENDING);
        var transactionId = pending.transactionId();
        assertThat(transactionId).isNotNull();
        var blob = storage().blobOf(pending.id());

        // Then : sans validation, la transaction expire, l'objet et son blob disparaissent
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(
                        problemOf(() -> objects().metadata(pending.id())).code())
                .isEqualTo("OBJECT_NOT_FOUND"));
        await().atMost(TIMEOUT).until(() -> !storage().exists(blob));
        assertThat(transactions().get(transactionId).status()).isEqualTo(TransactionStatus.EXPIRED);
        assertThat(problemOf(() -> transactions().commit(transactionId)).transactionStatus())
                .isEqualTo("EXPIRED");
    }

    @Test
    default void should_stage_the_replacement_of_an_active_object_until_commit() {
        var bucket = newBucket();
        var document = objects().create(bucket, "contract.pdf", "application/pdf", V1);
        var originalBlob = storage().blobOf(document.id());
        var transaction = transactions().open(Duration.ofMinutes(5));

        // When : le contenu est remplacé dans la transaction
        var staged = objects().replaceIn(transaction.id(), document.id(), V2);

        // Then : même identifiant, version en attente lisible avec la transaction, version validée inchangée
        assertThat(staged.id()).isEqualTo(document.id());
        assertThat(staged.status()).isEqualTo(ObjectStatus.PENDING);
        assertThat(objects().read(document.id(), transaction.id())).isEqualTo(V2);
        assertThat(objects().read(document.id())).isEqualTo(V1);
        assertThat(objects().metadata(document.id()).status()).isEqualTo(ObjectStatus.ACTIVE);

        // And : l'objet est verrouillé pour toute autre écriture
        assertThat(problemOf(() -> objects().replace(document.id(), "text/plain", V3))
                        .code())
                .isEqualTo("OBJECT_LOCKED");
        assertThat(problemOf(() -> objects().delete(document.id())).code()).isEqualTo("OBJECT_LOCKED");
        var other = transactions().open(Duration.ofMinutes(5));
        assertThat(problemOf(() -> objects().replaceIn(other.id(), document.id(), V3))
                        .code())
                .isEqualTo("OBJECT_LOCKED");

        // And : la même transaction peut corriger sa version en attente
        objects().replaceIn(transaction.id(), document.id(), V3);
        assertThat(objects().read(document.id(), transaction.id())).isEqualTo(V3);
        assertThat(transactions().objects(transaction.id(), 1000, null).objects())
                .extracting(ObjectSummaryResponse::id)
                .containsExactly(document.id());

        // When : la transaction est validée
        transactions().commit(transaction.id());

        // Then : le nouveau contenu est actif sous le même identifiant, l'ancien blob est purgé
        var committed = objects().metadata(document.id());
        assertThat(committed.status()).isEqualTo(ObjectStatus.ACTIVE);
        assertThat(committed.createdAt()).isEqualTo(document.createdAt());
        assertThat(objects().read(document.id())).isEqualTo(V3);
        await().atMost(TIMEOUT).until(() -> !storage().exists(originalBlob));
        objects().replace(document.id(), "text/plain", V1);
        transactions().rollback(other.id());
    }

    @Test
    default void should_discard_a_staged_replacement_and_bound_transaction_lifetimes() {
        var bucket = newBucket();
        var document = objects().create(bucket, "contract.pdf", "application/pdf", V1);
        var transaction = transactions().open(Duration.ofMinutes(1));
        objects().replaceIn(transaction.id(), document.id(), V2);

        // When : la transaction est prolongée
        var extended = transactions().extend(transaction.id(), new ExtendTransactionRequest(Duration.ofMinutes(10)));

        // Then : l'échéance est repoussée, mais jamais au-delà de la durée maximale (24 h)
        assertThat(extended.expiresAt()).isAfter(transaction.expiresAt());
        assertThat(problemOf(() -> transactions()
                                .extend(transaction.id(), new ExtendTransactionRequest(Duration.ofDays(2))))
                        .code())
                .isEqualTo("INVALID_TTL");

        // When : la transaction est annulée
        transactions().rollback(transaction.id());

        // Then : l'objet garde sa version validée et redevient modifiable ; plus de prolongation possible
        assertThat(objects().read(document.id())).isEqualTo(V1);
        assertThat(objects().read(document.id(), transaction.id())).isEqualTo(V1);
        objects().replace(document.id(), "text/plain", V3);
        assertThat(problemOf(() -> transactions()
                                .extend(transaction.id(), new ExtendTransactionRequest(Duration.ofMinutes(1))))
                        .code())
                .isEqualTo("TRANSACTION_CLOSED");

        // And : les demandes invalides sont refusées avec un code métier
        assertThat(problemOf(() -> transactions().open(Duration.ofDays(2))).code())
                .isEqualTo("INVALID_TTL");
        assertThat(problemOf(() -> transactions().get(UUID.randomUUID())).code())
                .isEqualTo("TRANSACTION_NOT_FOUND");
        assertThat(problemOf(() -> objects().createPending(bucket, "x", V1, "tomorrow"))
                        .code())
                .isEqualTo("INVALID_TTL");
        assertThat(problemOf(() -> objects()
                                .create(
                                        bucket,
                                        "x",
                                        V1.length,
                                        "text/plain",
                                        List.of(),
                                        UUID.randomUUID(),
                                        "PT1M",
                                        new InputStreamResource(new ByteArrayInputStream(V1))))
                        .code())
                .isEqualTo("CONFLICTING_TRANSACTION_HEADERS");
    }

    /** Crée un bucket au nom unique. */
    default String newBucket() {
        var name = uniqueBucketName();
        rest().create(new CreateBucketRequest(name));
        return name;
    }
}
