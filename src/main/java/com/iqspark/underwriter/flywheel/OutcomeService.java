package com.iqspark.underwriter.flywheel;

import com.iqspark.underwriter.metrics.DecisionMetrics;
import com.iqspark.underwriter.runtime.OutboxEventEntity;
import com.iqspark.underwriter.runtime.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records realized outcomes (the data-flywheel input) and emits an {@code OutcomeRecorded} outbox
 * event. Outcomes feed predicted-vs-realized dashboards now and (later) refresh the book/RAG/eval
 * sets — the loop that lets the agent improve with experience (doc 17 §3).
 */
@Service
public class OutcomeService {

    private final RealizedOutcomeRepository outcomes;
    private final OutboxEventRepository outbox;
    private final DecisionMetrics metrics;

    public OutcomeService(RealizedOutcomeRepository outcomes, OutboxEventRepository outbox,
                          DecisionMetrics metrics) {
        this.outcomes = outcomes;
        this.outbox = outbox;
        this.metrics = metrics;
    }

    @Transactional
    public RealizedOutcomeEntity record(OutcomeRequest request) {
        RealizedOutcomeEntity saved = outcomes.save(new RealizedOutcomeEntity(
                request.reference(), request.hadClaim(), request.lossRatio()));
        outbox.save(new OutboxEventEntity(request.reference(), "OutcomeRecorded", request.reference()));
        metrics.recordOutcome(request.hadClaim());
        return saved;
    }
}
