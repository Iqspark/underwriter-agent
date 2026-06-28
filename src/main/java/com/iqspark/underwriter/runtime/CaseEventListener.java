package com.iqspark.underwriter.runtime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles {@link CaseReceivedEvent} <b>after the submit transaction commits</b> (so the case row is
 * visible) and <b>asynchronously</b> (so intake returns immediately). This is the in-process stand-in
 * for a Kafka consumer; the seam doesn't change when a broker is introduced.
 */
@Component
public class CaseEventListener {

    private final CaseService caseService;

    public CaseEventListener(CaseService caseService) {
        this.caseService = caseService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCaseReceived(CaseReceivedEvent event) {
        caseService.process(event.caseId());
    }
}
