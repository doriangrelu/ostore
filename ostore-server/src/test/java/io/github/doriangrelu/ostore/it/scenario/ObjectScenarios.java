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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.doriangrelu.ostore.contract.constant.OStoreHeaders;
import io.github.doriangrelu.ostore.contract.dto.CopyObjectRequest;
import io.github.doriangrelu.ostore.contract.dto.CreateBucketRequest;
import io.github.doriangrelu.ostore.contract.dto.ObjectSummaryResponse;
import io.github.doriangrelu.ostore.driver.spi.testing.SyntheticContent;
import io.github.doriangrelu.ostore.it.support.IntegrationScenario;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** Scénarios « objets » de bout en bout : objets désignés par leur identifiant, nom libre (ADR-0015). */
public interface ObjectScenarios extends IntegrationScenario {

    byte[] INVOICE = SyntheticContent.slice(7, 0, 999);
    byte[] REPORT = SyntheticContent.slice(8, 0, 499);
    Duration PURGE_TIMEOUT = Duration.ofSeconds(15);

    @Test
    default void should_store_read_and_delete_an_object_by_its_identifier() throws IOException {
        var bucket = newBucket();

        // Given : un objet déposé avec un nom, un type et des métadonnées (valeur accentuée avec une virgule)
        var stored = objects()
                .create(
                        bucket,
                        "invoices/2026/invoice-42.pdf",
                        "application/pdf",
                        INVOICE,
                        "invoice-id=42",
                        "customer=Soci%C3%A9t%C3%A9%2C%20Paris");
        assertThat(stored.id()).isNotNull();
        assertThat(stored.name()).isEqualTo("invoices/2026/invoice-42.pdf");
        assertThat(stored.size()).isEqualTo(INVOICE.length);
        assertThat(stored.etag()).isEqualTo(md5(INVOICE));
        assertThat(stored.metadata()).isEqualTo(Map.of("invoice-id", "42", "customer", "Société, Paris"));

        // And : son chemin physique est enregistré en base, rangé par jour de dépôt (UTC)
        var blob = storage().blobOf(stored.id());
        var day = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .withZone(ZoneOffset.UTC)
                .format(stored.createdAt());
        assertThat(blob.path()).matches(day + "/[0-9a-f-]{36}");
        assertThat(storage().exists(blob)).isTrue();

        // Then : métadonnées et contenu sont relus par l'identifiant, le nom devient le nom de fichier proposé
        assertThat(objects().metadata(stored.id())).isEqualTo(stored);
        var full = objects().content(stored.id(), null);
        assertThat(full.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(full.getHeaders().getETag()).isEqualTo("\"" + md5(INVOICE) + "\"");
        assertThat(full.getHeaders().getFirst(OStoreHeaders.RESOURCE_ID))
                .isEqualTo(stored.id().toString());
        assertThat(full.getHeaders().getContentDisposition().getFilename()).isEqualTo("invoices/2026/invoice-42.pdf");
        assertThat(full.getHeaders().get(OStoreHeaders.META)).contains("customer=Soci%C3%A9t%C3%A9%2C%20Paris");
        try (var body = full.getBody().getInputStream()) {
            assertThat(body.readAllBytes()).isEqualTo(INVOICE);
        }

        // And : une plage est servie seule (206), une plage hors contenu est refusée (416)
        var part = objects().content(stored.id(), "bytes=10-19");
        assertThat(part.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(part.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 10-19/1000");
        try (var body = part.getBody().getInputStream()) {
            assertThat(body.readAllBytes()).isEqualTo(SyntheticContent.slice(7, 10, 19));
        }
        assertThat(problemOf(() -> objects().content(stored.id(), "bytes=5000-")))
                .extracting(Problem::status, Problem::code)
                .containsExactly(416, "RANGE_NOT_SATISFIABLE");

        // When : l'objet est supprimé
        objects().delete(stored.id());

        // Then : il est introuvable, et son blob est purgé du stockage
        assertThat(problemOf(() -> objects().metadata(stored.id())).code()).isEqualTo("OBJECT_NOT_FOUND");
        await().atMost(PURGE_TIMEOUT).until(() -> !storage().exists(blob));
    }

    @Test
    default void should_replace_content_keeping_the_identifier_and_purge_the_previous_blob() {
        var bucket = newBucket();
        var first = objects().create(bucket, "report.csv", "text/csv", REPORT, "version=1");
        var firstBlob = storage().blobOf(first.id());

        // When : le contenu est remplacé
        var second = objects().replace(first.id(), "application/pdf", INVOICE, "version=2");

        // Then : même identifiant et même nom, nouveau contenu, ancien blob purgé
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.name()).isEqualTo("report.csv");
        assertThat(second.etag()).isEqualTo(md5(INVOICE));
        assertThat(second.metadata()).isEqualTo(Map.of("version", "2"));
        assertThat(second.createdAt()).isEqualTo(first.createdAt());
        assertThat(second.updatedAt()).isAfterOrEqualTo(first.updatedAt());
        assertThat(objects().read(first.id())).isEqualTo(INVOICE);
        await().atMost(PURGE_TIMEOUT).until(() -> !storage().exists(firstBlob));
        assertThat(storage().exists(storage().blobOf(first.id()))).isTrue();
    }

    @Test
    default void should_list_objects_in_creation_order_page_by_page_and_filter_by_name() {
        var bucket = newBucket();
        var unnamed = objects().create(bucket, null, "text/plain", REPORT);
        var one = objects().create(bucket, "dir/one", "text/plain", REPORT);
        var two = objects().create(bucket, "dir/two", "text/plain", REPORT);
        var underscore = objects().create(bucket, "dir_x", "text/plain", REPORT);

        // Then : tous les objets, y compris sans nom, par ordre de création
        assertThat(objects().list(bucket, null, 1000, null).objects())
                .extracting(ObjectSummaryResponse::id)
                .containsExactly(unnamed.id(), one.id(), two.id(), underscore.id());

        // And : pagination avec jeton de continuation
        var firstPage = objects().list(bucket, null, 3, null);
        assertThat(firstPage.objects()).hasSize(3);
        var secondPage = objects().list(bucket, null, 3, firstPage.nextContinuationToken());
        assertThat(secondPage.objects()).extracting(ObjectSummaryResponse::id).containsExactly(underscore.id());
        assertThat(secondPage.nextContinuationToken()).isNull();

        // And : filtre par préfixe de nom, où "_" est un caractère ordinaire et non un joker SQL
        assertThat(objects().list(bucket, "dir/", 1000, null).objects())
                .extracting(ObjectSummaryResponse::name)
                .containsExactly("dir/one", "dir/two");
        assertThat(objects().list(bucket, "dir_", 1000, null).objects())
                .extracting(ObjectSummaryResponse::name)
                .containsExactly("dir_x");
    }

    @Test
    default void should_copy_objects_and_protect_buckets_and_inputs() {
        var bucket = newBucket();
        var archive = newBucket();
        var original = objects().create(bucket, "report.csv", "text/csv", REPORT, "owner=finance");

        // When : l'objet est copié dans un autre bucket, sous un autre nom
        var copy = objects().copy(original.id(), new CopyObjectRequest(archive, "2026-report.csv"));

        // Then : la copie est un nouvel objet, au contenu et aux métadonnées identiques
        assertThat(copy.id()).isNotEqualTo(original.id());
        assertThat(copy.name()).isEqualTo("2026-report.csv");
        assertThat(copy.etag()).isEqualTo(original.etag());
        assertThat(copy.metadata()).isEqualTo(original.metadata());
        assertThat(objects().read(copy.id())).isEqualTo(REPORT);
        assertThat(objects().list(archive, null, 1000, null).objects())
                .extracting(ObjectSummaryResponse::id)
                .containsExactly(copy.id());

        // And : un bucket non vide ne peut pas être supprimé
        assertThat(problemOf(() -> rest().delete(bucket)))
                .extracting(Problem::status, Problem::code)
                .containsExactly(409, "BUCKET_NOT_EMPTY");

        // And : les entrées invalides ou inconnues sont refusées avec un code métier
        assertThat(problemOf(() -> objects().create(bucket, "bad.txt", "text/plain", REPORT, "Bad Name=1"))
                        .code())
                .isEqualTo("INVALID_METADATA");
        assertThat(problemOf(() -> objects().create(uniqueBucketName(), null, "text/plain", REPORT))
                        .code())
                .isEqualTo("BUCKET_NOT_FOUND");
        assertThat(problemOf(() -> objects().metadata(UUID.randomUUID())).code())
                .isEqualTo("OBJECT_NOT_FOUND");

        // When : le bucket est vidé, Then : il peut être supprimé
        objects().delete(original.id());
        rest().delete(bucket);
    }

    /** Crée un bucket au nom unique. */
    default String newBucket() {
        var name = uniqueBucketName();
        rest().create(new CreateBucketRequest(name));
        return name;
    }

    static String md5(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
