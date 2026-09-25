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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/** Scénarios « objets » de bout en bout, via le client construit sur le contrat. */
public interface ObjectScenarios extends IntegrationScenario {

    byte[] INVOICE = SyntheticContent.slice(7, 0, 999);
    byte[] REPORT = SyntheticContent.slice(8, 0, 499);
    Duration PURGE_TIMEOUT = Duration.ofSeconds(15);

    @Test
    default void should_store_read_and_delete_an_object_with_metadata() throws IOException {
        var bucket = newBucket();
        var key = "invoices/2026/invoice-42.pdf";

        // Given : un objet déposé avec son type et des métadonnées (valeur accentuée contenant une virgule)
        var stored = objects()
                .put(bucket, key, "application/pdf", INVOICE, "invoice-id=42", "customer=Soci%C3%A9t%C3%A9%2C%20Paris");
        assertThat(stored.resourceId()).isNotNull();
        assertThat(stored.size()).isEqualTo(INVOICE.length);
        assertThat(stored.etag()).isEqualTo(md5(INVOICE));
        assertThat(stored.metadata()).isEqualTo(Map.of("invoice-id", "42", "customer", "Société, Paris"));

        // And : son chemin physique est enregistré en base, rangé par jour de dépôt (UTC)
        var blob = storage().blobOf(bucket, key);
        var day = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .withZone(ZoneOffset.UTC)
                .format(stored.lastModified());
        assertThat(blob.path()).matches(day + "/[0-9a-f-]{36}");
        assertThat(storage().exists(blob)).isTrue();

        // Then : métadonnées et contenu sont relus à l'identique
        assertThat(objects().metadata(bucket, key)).isEqualTo(stored);
        var full = objects().content(bucket, key, null);
        assertThat(full.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(full.getHeaders().getETag()).isEqualTo("\"" + md5(INVOICE) + "\"");
        assertThat(full.getHeaders().getFirst(OStoreHeaders.RESOURCE_ID))
                .isEqualTo(stored.resourceId().toString());
        assertThat(full.getHeaders().get(OStoreHeaders.META)).contains("customer=Soci%C3%A9t%C3%A9%2C%20Paris");
        try (var body = full.getBody().getInputStream()) {
            assertThat(body.readAllBytes()).isEqualTo(INVOICE);
        }

        // And : une plage est servie seule (206), une plage hors contenu est refusée (416)
        var part = objects().content(bucket, key, "bytes=10-19");
        assertThat(part.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(part.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 10-19/1000");
        try (var body = part.getBody().getInputStream()) {
            assertThat(body.readAllBytes()).isEqualTo(SyntheticContent.slice(7, 10, 19));
        }
        assertThat(problemOf(() -> objects().content(bucket, key, "bytes=5000-")))
                .extracting(Problem::status, Problem::code)
                .containsExactly(416, "RANGE_NOT_SATISFIABLE");

        // When : l'objet est supprimé
        objects().delete(bucket, key);

        // Then : il est introuvable, et son blob est purgé du stockage
        assertThat(problemOf(() -> objects().metadata(bucket, key)).code()).isEqualTo("OBJECT_NOT_FOUND");
        await().atMost(PURGE_TIMEOUT).until(() -> !storage().exists(blob));
    }

    @Test
    default void should_replace_an_object_and_purge_its_previous_blob() {
        var bucket = newBucket();
        var first = objects().put(bucket, "report.csv", "text/csv", REPORT);
        var firstBlob = storage().blobOf(bucket, "report.csv");

        // When : la même clé est déposée à nouveau
        var second = objects().put(bucket, "report.csv", "application/pdf", INVOICE);

        // Then : la nouvelle version est servie, l'ancien blob est purgé
        assertThat(second.resourceId()).isNotEqualTo(first.resourceId());
        assertThat(objects().metadata(bucket, "report.csv").etag()).isEqualTo(md5(INVOICE));
        assertThat(objects().read(bucket, "report.csv")).isEqualTo(INVOICE);
        await().atMost(PURGE_TIMEOUT).until(() -> !storage().exists(firstBlob));
        assertThat(storage().exists(storage().blobOf(bucket, "report.csv"))).isTrue();
    }

    @Test
    default void should_list_objects_by_prefix_in_binary_order_page_by_page() {
        var bucket = newBucket();
        for (var key : new String[] {"dir/two", "a.txt", "dir_x", "dir/one", "B.txt", "dir/sub/three"}) {
            objects().put(bucket, key, "text/plain", REPORT);
        }

        // Then : ordre binaire UTF-8, identique sur tous les SGBD ("B" < "a", "/" < "_")
        assertThat(objects().list(bucket, null, 1000, null).objects())
                .extracting(ObjectSummaryResponse::key)
                .containsExactly("B.txt", "a.txt", "dir/one", "dir/sub/three", "dir/two", "dir_x");

        // And : pagination par préfixe
        var first = objects().list(bucket, "dir/", 2, null);
        assertThat(first.objects()).extracting(ObjectSummaryResponse::key).containsExactly("dir/one", "dir/sub/three");
        var second = objects().list(bucket, "dir/", 2, first.nextContinuationToken());
        assertThat(second.objects()).extracting(ObjectSummaryResponse::key).containsExactly("dir/two");
        assertThat(second.nextContinuationToken()).isNull();

        // And : "_" est un caractère de préfixe ordinaire, pas un joker SQL
        assertThat(objects().list(bucket, "dir_", 1000, null).objects())
                .extracting(ObjectSummaryResponse::key)
                .containsExactly("dir_x");
    }

    @Test
    default void should_copy_objects_and_protect_buckets_and_inputs() {
        var bucket = newBucket();
        var archive = newBucket();
        var original = objects().put(bucket, "report.csv", "text/csv", REPORT, "owner=finance");

        // When : l'objet est copié dans un autre bucket
        var copy = objects().copy(bucket, new CopyObjectRequest("report.csv", archive, "2026/report.csv"));

        // Then : la copie est une ressource distincte, au contenu et aux métadonnées identiques
        assertThat(copy.resourceId()).isNotEqualTo(original.resourceId());
        assertThat(copy.bucket()).isEqualTo(archive);
        assertThat(copy.etag()).isEqualTo(original.etag());
        assertThat(copy.metadata()).isEqualTo(original.metadata());
        assertThat(objects().read(archive, "2026/report.csv")).isEqualTo(REPORT);

        // And : un bucket non vide ne peut pas être supprimé
        assertThat(problemOf(() -> rest().delete(bucket)))
                .extracting(Problem::status, Problem::code)
                .containsExactly(409, "BUCKET_NOT_EMPTY");

        // And : les entrées invalides sont refusées avec un code métier
        assertThat(problemOf(() -> objects().put(bucket, "bad.txt", "text/plain", REPORT, "Bad Name=1"))
                        .code())
                .isEqualTo("INVALID_METADATA");
        assertThat(problemOf(() -> objects().put(uniqueBucketName(), "x.txt", "text/plain", REPORT))
                        .code())
                .isEqualTo("BUCKET_NOT_FOUND");

        // When : le bucket est vidé, Then : il peut être supprimé
        objects().delete(bucket, "report.csv");
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
