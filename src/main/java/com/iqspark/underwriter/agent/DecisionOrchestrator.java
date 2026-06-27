package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Decision;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.llm.LlmReasoner;
import com.iqspark.underwriter.llm.LlmReasoningException;
import com.iqspark.underwriter.llm.TemplateLlmReasoner;
import com.iqspark.underwriter.metrics.DecisionMetrics;
import com.iqspark.underwriter.persistence.DecisionStore;
import com.iqspark.underwriter.security.pii.PiiRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the agents in order, derives two outcomes (guardrail + learned) and takes the most
 * conservative, derives curing conditions, requests a rationale (with offline fallback), and
 * assembles the {@link Decision}. The LLM never decides; deterministic guardrails own the veto.
 */
@Service
public class DecisionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DecisionOrchestrator.class);

    /** Guardrail REFER threshold on the summed risk weight (encodes appetite; review before prod). */
    static final int REFER_THRESHOLD = 6;

    // Learned thresholds (doc 5 §6).
    private static final double LEARNED_DECLINE_CLAIM_PROB = 0.70;
    private static final double LEARNED_DECLINE_LOSS_RATIO = 1.5;
    private static final double LEARNED_REFER_CLAIM_PROB = 0.55;
    private static final double LEARNED_REFER_LOSS_RATIO = 1.0;

    private static final Set<String> REFER_CODES = Set.of(
            "MISSING_FIELD", "DATA_CONTRADICTION", "LOCATION_UNRESOLVED", "MISSING_LIABILITY_LIMIT");

    private final List<UnderwritingAgent> agents;
    private final LlmReasoner primaryReasoner;
    private final TemplateLlmReasoner templateReasoner;
    private final DecisionStore decisionStore;
    private final DecisionMetrics metrics;

    public DecisionOrchestrator(List<UnderwritingAgent> agents,
                                LlmReasoner primaryReasoner,
                                TemplateLlmReasoner templateReasoner,
                                @Nullable DecisionStore decisionStore,
                                @Nullable DecisionMetrics metrics) {
        this.agents = agents.stream()
                .sorted(Comparator.comparingInt(UnderwritingAgent::order))
                .toList();
        this.primaryReasoner = primaryReasoner;
        this.templateReasoner = templateReasoner;
        this.decisionStore = decisionStore;
        this.metrics = metrics;
    }

    public Decision decide(Submission submission) {
        UnderwritingContext ctx = new UnderwritingContext(submission);
        for (UnderwritingAgent agent : agents) {
            agent.handle(ctx);
        }

        List<Finding> findings = ctx.findings();
        int riskScore = ctx.riskScore();
        LearnedAssessment learned = ctx.learnedAssessment();

        DecisionOutcome guardrail = deriveGuardrailOutcome(findings, riskScore);
        DecisionOutcome learnedOutcome = deriveLearnedOutcome(learned);
        DecisionOutcome outcome = DecisionOutcome.mostConservative(guardrail, learnedOutcome);

        List<String> conditions = deriveConditions(findings, outcome);

        String rationale = generateRationale(ctx, submission, outcome, findings, learned);

        ctx.audit("DecisionOrchestrator",
                "Outcome=%s (guardrail=%s, learned=%s) riskScore=%d conditions=%d"
                        .formatted(outcome, guardrail, learnedOutcome, riskScore, conditions.size()));

        Decision decision = new Decision(
                submission.reference(),
                outcome,
                riskScore,
                findings,
                conditions,
                ctx.indicativePremium(),
                rationale,
                learned,
                ctx.auditTrail().entries(),
                Instant.now());

        persist(decision, submission);
        record(outcome, submission);
        return decision;
    }

    private DecisionOutcome deriveGuardrailOutcome(List<Finding> findings, int riskScore) {
        if (findings.stream().anyMatch(Finding::isKnockout)) {
            return DecisionOutcome.DECLINE;
        }
        boolean dataGap = findings.stream().anyMatch(f -> REFER_CODES.contains(f.code()));
        if (dataGap || riskScore >= REFER_THRESHOLD) {
            return DecisionOutcome.REFER;
        }
        return DecisionOutcome.APPROVE;
    }

    private DecisionOutcome deriveLearnedOutcome(LearnedAssessment learned) {
        if (learned == null || learned.coldStart()) {
            return DecisionOutcome.APPROVE; // neutral
        }
        if (learned.claimProbability() >= LEARNED_DECLINE_CLAIM_PROB
                || learned.expectedLossRatio() >= LEARNED_DECLINE_LOSS_RATIO) {
            return DecisionOutcome.DECLINE;
        }
        if (learned.claimProbability() >= LEARNED_REFER_CLAIM_PROB
                || learned.expectedLossRatio() >= LEARNED_REFER_LOSS_RATIO) {
            return DecisionOutcome.REFER;
        }
        return DecisionOutcome.APPROVE;
    }

    private List<String> deriveConditions(List<Finding> findings, DecisionOutcome outcome) {
        Set<String> conditions = new LinkedHashSet<>();
        for (Finding f : findings) {
            switch (f.code()) {
                case "INSPECTION_INTERVAL_BREACH" -> conditions.add(
                        "Attach a 72-hour inspection condition precedent (inspect the vacant property at least every 72h), or decline.");
                case "STR_WITHOUT_ENDORSEMENT" -> conditions.add(
                        "Attach a short-term-rental endorsement, or decline.");
                case "REMOTE_LOCATION" -> conditions.add(
                        "Confirm fire-response arrangements given the remote location; consider a higher deductible.");
                case "WATER_NOT_SHUT_OFF" -> conditions.add(
                        "Require water shut-off and a drained system, or rate for escape-of-water.");
                case "MISSING_FIELD", "MISSING_LIABILITY_LIMIT", "DATA_CONTRADICTION", "LOCATION_UNRESOLVED" ->
                        conditions.add("Obtain/clarify the missing or contradictory information from the broker before binding.");
                case "PRIOR_LOSSES" -> conditions.add("Review the full loss history before binding.");
                case "HIGH_VALUE_ITEMS_UNSCHEDULED" -> conditions.add(
                        "Individually schedule the high-value items, or cap the unscheduled sub-limit.");
                default -> { /* no condition for this finding */ }
            }
        }
        if (conditions.isEmpty() && outcome == DecisionOutcome.REFER) {
            conditions.add("Underwriter review required before binding.");
        }
        return new ArrayList<>(conditions);
    }

    private String generateRationale(UnderwritingContext ctx, Submission submission,
                                     DecisionOutcome outcome, List<Finding> findings,
                                     LearnedAssessment learned) {
        String rationale;
        String reasonerUsed;
        try {
            rationale = primaryReasoner.summarize(submission, outcome, findings, learned, ctx.indicativePremium());
            reasonerUsed = primaryReasoner.name();
        } catch (LlmReasoningException e) {
            log.warn("Primary reasoner failed; falling back to offline template: {}", e.getMessage());
            rationale = templateReasoner.summarize(submission, outcome, findings, learned, ctx.indicativePremium());
            reasonerUsed = templateReasoner.name() + " (fallback)";
        }
        ctx.audit("LlmReasoner", "Rationale generated by " + reasonerUsed);
        return rationale;
    }

    private void persist(Decision decision, Submission submission) {
        if (decisionStore == null) {
            return;
        }
        try {
            double coverage = submission.requestedCoverage() != null
                    ? submission.requestedCoverage().amount() : 0.0;
            decisionStore.save(decision, submission.effectiveLine().name(), coverage);
        } catch (Exception e) {
            // Degrade-to-floor: a persistence failure never fails the decision response.
            // Messages are PII-redacted before logging.
            log.warn("Failed to persist decision {}: {}",
                    decision.reference(), PiiRedactor.redact(e.getMessage()));
        }
    }

    private void record(DecisionOutcome outcome, Submission submission) {
        if (metrics != null) {
            metrics.record(outcome, submission.effectiveLine());
        }
    }
}
