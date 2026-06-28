package com.iqspark.underwriter.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxRelayTest {

    @Autowired
    private OutboxEventRepository outbox;

    @Test
    void marksPendingEventsAsPublished() {
        outbox.save(new OutboxEventEntity("c1", "SubmissionReceived", "c1"));
        outbox.save(new OutboxEventEntity("c1", "DecisionMade", "APPROVE"));

        new OutboxRelay(outbox).relay();

        assertThat(outbox.findByPublishedFalseOrderByIdAsc()).isEmpty();
        assertThat(outbox.findAll()).allMatch(OutboxEventEntity::isPublished);
    }

    @Test
    void relayIsANoOpWhenNothingPending() {
        new OutboxRelay(outbox).relay(); // must not throw on an empty outbox
        assertThat(outbox.findByPublishedFalseOrderByIdAsc()).isEmpty();
    }
}
