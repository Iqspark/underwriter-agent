package com.iqspark.underwriter.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iqspark.underwriter.agent.DecisionOrchestrator;
import com.iqspark.underwriter.domain.decision.AutonomyAssessment;
import com.iqspark.underwriter.domain.decision.AutonomyTier;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
class CaseServiceTest {

    @Autowired
    private SubmissionCaseRepository cases;
    @Autowired
    private OutboxEventRepository outbox;

    private final DecisionOrchestrator orchestrator = mock(DecisionOrchestrator.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CaseService service;

    @BeforeEach
    void setUp() {
        RuntimeProperties props = new RuntimeProperties();
        props.setAsync(false); // process inline for deterministic assertions
        service = new CaseService(cases, outbox, orchestrator, mapper, props, event -> { });
    }

    private Decision decision(String ref, DecisionOutcome outcome, AutonomyTier tier) {
        return new Decision(ref, outcome, 0, List.of(), List.of(), Money.cad(1000), "rationale",
                null, List.of(), List.of(), new AutonomyAssessment(tier, false, List.of()),
                List.of("audit"), Instant.now());
    }

    @Test
    void autoApprovalIsAutoDecided() {
        when(orchestrator.decide(any())).thenReturn(decision("C1", DecisionOutcome.APPROVE, AutonomyTier.AUTO));
        CaseView v = service.submit(Submissions.vacantClean());
        assertThat(v.status()).isEqualTo(CaseStatus.AUTO_DECIDED);
        assertThat(v.outcome()).isEqualTo("APPROVE");
        assertThat(outbox.findByPublishedFalseOrderByIdAsc()).isNotEmpty();
    }

    @Test
    void declineIsReferredForHumanReview() {
        when(orchestrator.decide(any())).thenReturn(decision("C2", DecisionOutcome.DECLINE, AutonomyTier.SPECIALIST));
        CaseView v = service.submit(Submissions.vacantKnockout());
        assertThat(v.status()).isEqualTo(CaseStatus.REFERRED);
    }

    @Test
    void resubmittingTheSameReferenceIsIdempotent() {
        when(orchestrator.decide(any())).thenReturn(decision("DUP", DecisionOutcome.APPROVE, AutonomyTier.ASSISTED));
        CaseView first = service.submit(Submissions.vacantClean());   // reference T-CLEAN-1
        CaseView second = service.submit(Submissions.vacantClean());
        assertThat(second.caseId()).isEqualTo(first.caseId());
        assertThat(cases.count()).isEqualTo(1);
    }

    @Test
    void exhaustedRetriesDeadLetter() {
        RuntimeProperties props = new RuntimeProperties();
        props.setAsync(false);
        props.setMaxAttempts(1);
        CaseService failing = new CaseService(cases, outbox, orchestrator, mapper, props, event -> { });
        when(orchestrator.decide(any())).thenThrow(new RuntimeException("boom"));

        CaseView v = failing.submit(Submissions.vacantClean());

        assertThat(v.status()).isEqualTo(CaseStatus.FAILED);
        assertThat(outbox.findByPublishedFalseOrderByIdAsc())
                .anyMatch(e -> e.getType().equals("DecisionFailed"));
    }
}
