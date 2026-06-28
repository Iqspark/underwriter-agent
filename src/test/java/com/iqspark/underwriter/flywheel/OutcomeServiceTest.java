package com.iqspark.underwriter.flywheel;

import com.iqspark.underwriter.metrics.DecisionMetrics;
import com.iqspark.underwriter.runtime.OutboxEventEntity;
import com.iqspark.underwriter.runtime.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutcomeServiceTest {

    private final RealizedOutcomeRepository outcomes = mock(RealizedOutcomeRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final DecisionMetrics metrics = new DecisionMetrics(new SimpleMeterRegistry());
    private final OutcomeService service = new OutcomeService(outcomes, outbox, metrics);

    @Test
    void recordsOutcomeAndEmitsOutboxEvent() {
        when(outcomes.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(new OutcomeRequest("REF-1", true, 1.2));

        verify(outcomes).save(any(RealizedOutcomeEntity.class));
        verify(outbox).save(any(OutboxEventEntity.class));
    }
}
