package com.iqspark.underwriter.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqspark.underwriter.agent.DecisionOrchestrator;
import com.iqspark.underwriter.domain.decision.AutonomyTier;
import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The lean event-driven runtime (doc 10 §2). Accepts a submission as a durable case, then processes
 * it asynchronously through a persisted state machine with an outbox, idempotency and retries→DLQ.
 * The synchronous {@code /submissions} fast-path is unchanged; this adds the accept-then-poll path
 * for work that should survive spikes, retries and (later) human-in-the-loop pauses.
 */
@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final SubmissionCaseRepository cases;
    private final OutboxEventRepository outbox;
    private final DecisionOrchestrator orchestrator;
    private final ObjectMapper mapper;
    private final RuntimeProperties props;
    private final ApplicationEventPublisher events;

    public CaseService(SubmissionCaseRepository cases, OutboxEventRepository outbox,
                       DecisionOrchestrator orchestrator, ObjectMapper mapper,
                       RuntimeProperties props, ApplicationEventPublisher events) {
        this.cases = cases;
        this.outbox = outbox;
        this.orchestrator = orchestrator;
        this.mapper = mapper;
        this.props = props;
        this.events = events;
    }

    /** Accept a submission as a case. Idempotent on the submission reference. */
    @Transactional
    public CaseView submit(Submission submission) {
        String key = submission.reference() != null && !submission.reference().isBlank()
                ? submission.reference() : null;
        if (key != null) {
            Optional<SubmissionCaseEntity> existing = cases.findFirstByIdempotencyKeyOrderByCreatedAtDesc(key);
            if (existing.isPresent()) {
                return view(existing.get()); // dedupe: return the in-flight/completed case
            }
        }

        String caseId = UUID.randomUUID().toString();
        SubmissionCaseEntity e = new SubmissionCaseEntity();
        e.setCaseId(caseId);
        e.setIdempotencyKey(key != null ? key : caseId);
        e.setReference(submission.reference());
        e.setLine(submission.effectiveLine().name());
        e.setStatus(CaseStatus.RECEIVED);
        e.setAttempts(0);
        e.setSubmissionJson(toJson(submission));
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        cases.save(e);
        outbox.save(new OutboxEventEntity(caseId, "SubmissionReceived", caseId));

        if (props.isAsync()) {
            events.publishEvent(new CaseReceivedEvent(caseId)); // handled after commit, async
        } else {
            process(caseId); // inline (deterministic for tests)
        }
        return view(cases.findById(caseId).orElse(e));
    }

    /** Process a received case: assess, decide, route, persist — with retries and a dead-letter on exhaustion. */
    @Transactional
    public void process(String caseId) {
        SubmissionCaseEntity e = cases.findById(caseId).orElse(null);
        if (e == null || e.getStatus() != CaseStatus.RECEIVED) {
            return; // idempotent: only RECEIVED cases are processed
        }
        e.setStatus(CaseStatus.ASSESSING);
        e.setUpdatedAt(Instant.now());
        cases.save(e);

        try {
            Submission submission = mapper.readValue(e.getSubmissionJson(), Submission.class);
            Decision decision = orchestrator.decide(submission);
            e.setOutcome(decision.outcome().name());
            e.setTier(decision.autonomy() != null ? decision.autonomy().tier().name() : null);
            e.setDecisionReference(decision.reference());
            e.setDecisionJson(toJson(decision));
            e.setStatus(mapStatus(decision));
            e.setUpdatedAt(Instant.now());
            cases.save(e);
            outbox.save(new OutboxEventEntity(caseId, "DecisionMade", decision.outcome().name()));
        } catch (Exception ex) {
            handleFailure(e, ex);
        }
    }

    private void handleFailure(SubmissionCaseEntity e, Exception ex) {
        e.setAttempts(e.getAttempts() + 1);
        e.setLastError(ex.getMessage());
        if (e.getAttempts() < props.getMaxAttempts()) {
            e.setStatus(CaseStatus.RECEIVED); // eligible for another attempt
            e.setUpdatedAt(Instant.now());
            cases.save(e);
            log.warn("Case {} failed (attempt {}/{}); will retry: {}",
                    e.getCaseId(), e.getAttempts(), props.getMaxAttempts(), ex.getMessage());
            if (props.isAsync()) {
                events.publishEvent(new CaseReceivedEvent(e.getCaseId()));
            }
        } else {
            e.setStatus(CaseStatus.FAILED); // dead-letter
            e.setUpdatedAt(Instant.now());
            cases.save(e);
            outbox.save(new OutboxEventEntity(e.getCaseId(), "DecisionFailed", ex.getMessage()));
            log.error("Case {} dead-lettered after {} attempts: {}",
                    e.getCaseId(), e.getAttempts(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<CaseView> find(String caseId) {
        return cases.findById(caseId).map(this::view);
    }

    private CaseStatus mapStatus(Decision decision) {
        if (decision.outcome() == DecisionOutcome.DECLINE) {
            return CaseStatus.REFERRED; // a human reviews/overrides a decline
        }
        boolean auto = decision.outcome() == DecisionOutcome.APPROVE
                && decision.autonomy() != null && decision.autonomy().tier() == AutonomyTier.AUTO;
        return auto ? CaseStatus.AUTO_DECIDED : CaseStatus.REFERRED;
    }

    private CaseView view(SubmissionCaseEntity e) {
        Decision decision = null;
        if (e.getDecisionJson() != null) {
            try {
                decision = mapper.readValue(e.getDecisionJson(), Decision.class);
            } catch (Exception ignored) {
                // view is best-effort; status fields still returned
            }
        }
        return new CaseView(e.getCaseId(), e.getReference(), e.getStatus(), e.getOutcome(), e.getTier(),
                e.getDecisionReference(), e.getAttempts(), decision, e.getCreatedAt(), e.getUpdatedAt());
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize case payload", ex);
        }
    }
}
