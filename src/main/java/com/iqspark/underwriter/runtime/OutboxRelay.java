package com.iqspark.underwriter.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relays unpublished outbox rows. Today it marks them published (the in-process "bus"); when a broker
 * is added this is where rows are published to Kafka — the outbox guarantees the DB write and the
 * event never diverge.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;

    public OutboxRelay(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelayString = "${underwriter.runtime.outbox-poll-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = outbox.findByPublishedFalseOrderByIdAsc();
        if (pending.isEmpty()) {
            return;
        }
        pending.forEach(e -> e.setPublished(true));
        outbox.saveAll(pending);
        log.debug("Relayed {} outbox event(s)", pending.size());
    }
}
