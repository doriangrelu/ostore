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

import static io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus.COMMITTED;
import static io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus.EXPIRED;
import static io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus.OPEN;
import static io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus.ROLLED_BACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.doriangrelu.ostore.domain.exception.InvalidTtlException;
import io.github.doriangrelu.ostore.domain.exception.TransactionClosedException;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionPolicy;
import io.github.doriangrelu.ostore.domain.model.vo.TransactionStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Machine à états des transactions : règle pure et combinatoire (ADR-0011). */
class TransactionTest {

    private static final Instant OPENED = Instant.parse("2026-09-30T10:00:00Z");
    private static final Instant BEFORE_EXPIRY = OPENED.plusSeconds(60);
    private static final Instant AFTER_EXPIRY = OPENED.plusSeconds(3600);
    private static final TransactionPolicy POLICY = new TransactionPolicy(Duration.ofMinutes(15), Duration.ofDays(1));

    @Test
    void should_be_expired_once_due_even_if_not_yet_processed() {
        var open = open();

        assertThat(open.statusAt(BEFORE_EXPIRY)).isEqualTo(OPEN);
        assertThat(open.statusAt(OPENED.plus(Duration.ofMinutes(15)))).isEqualTo(EXPIRED);
    }

    @Test
    void should_commit_an_open_transaction_idempotently() {
        var committed = open().commit(BEFORE_EXPIRY);

        assertThat(committed.status()).isEqualTo(COMMITTED);
        assertThat(committed.closedAt()).isEqualTo(BEFORE_EXPIRY);
        assertThat(committed.commit(AFTER_EXPIRY)).isSameAs(committed);
    }

    @Test
    void should_roll_back_an_open_or_already_closed_without_commit_transaction_idempotently() {
        var rolledBack = open().rollback(BEFORE_EXPIRY);

        assertThat(rolledBack.status()).isEqualTo(ROLLED_BACK);
        assertThat(rolledBack.rollback(AFTER_EXPIRY)).isSameAs(rolledBack);
        assertThat(open().rollback(AFTER_EXPIRY).status()).isEqualTo(EXPIRED);
    }

    @ParameterizedTest
    @EnumSource(names = {"ROLLED_BACK", "EXPIRED"})
    void should_refuse_to_commit_a_transaction_closed_without_commit(TransactionStatus closedStatus) {
        var closed = closedStatus == ROLLED_BACK ? open().rollback(BEFORE_EXPIRY) : open();
        var when = closedStatus == ROLLED_BACK ? BEFORE_EXPIRY : AFTER_EXPIRY;

        assertThatThrownBy(() -> closed.commit(when))
                .isInstanceOfSatisfying(TransactionClosedException.class, e -> assertThat(e.status())
                        .isEqualTo(closedStatus));
    }

    @Test
    void should_refuse_to_roll_back_or_extend_a_committed_transaction() {
        var committed = open().commit(BEFORE_EXPIRY);

        assertThatThrownBy(() -> committed.rollback(BEFORE_EXPIRY)).isInstanceOf(TransactionClosedException.class);
        assertThatThrownBy(() -> committed.extend(Duration.ofMinutes(5), BEFORE_EXPIRY, POLICY))
                .isInstanceOf(TransactionClosedException.class);
    }

    @Test
    void should_extend_within_the_maximum_lifetime_only() {
        var extended = open().extend(Duration.ofHours(2), BEFORE_EXPIRY, POLICY);

        assertThat(extended.expiresAt()).isEqualTo(BEFORE_EXPIRY.plus(Duration.ofHours(2)));
        assertThatThrownBy(() -> extended.extend(Duration.ofHours(23), OPENED.plus(Duration.ofHours(2)), POLICY))
                .isInstanceOf(InvalidTtlException.class);
    }

    private static Transaction open() {
        return Transaction.open("case-42", POLICY.resolve(null), OPENED);
    }
}
