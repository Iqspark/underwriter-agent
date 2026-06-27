package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.Peril;
import org.springframework.stereotype.Component;

/**
 * The AI-first core agent: learns from comparable history (claim probability, loss ratio, dominant
 * peril, area theft) and turns the {@link LearnedAssessment} into findings on the context.
 */
@Component
public class PatternLearningAgent implements UnderwritingAgent {

    private static final double AREA_THEFT_ELEVATED_THRESHOLD = 0.25;

    private final SimilarityEngine similarityEngine;

    public PatternLearningAgent(SimilarityEngine similarityEngine) {
        this.similarityEngine = similarityEngine;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public void handle(UnderwritingContext context) {
        LearnedAssessment learned = similarityEngine.assess(context.submission());
        context.setLearnedAssessment(learned);

        if (learned.coldStart()) {
            context.audit("PatternLearningAgent",
                    "Cold start: too few comparables to learn from; guardrails decide.");
            return;
        }

        Severity sev = (learned.claimProbability() >= 0.55 || learned.expectedLossRatio() >= 1.0)
                ? Severity.HIGH
                : (learned.claimProbability() >= 0.35 || learned.expectedLossRatio() >= 0.7)
                ? Severity.MEDIUM
                : Severity.INFO;

        context.addFinding(new Finding(
                "LEARNED_CLAIM_PROBABILITY", sev, "LEARNED",
                "Comparable history predicts a %s claim probability (loss ratio %.2f)"
                        .formatted(pct(learned.claimProbability()), learned.expectedLossRatio()),
                "Based on %d similar past policies (confidence %s)."
                        .formatted(learned.comparableCount(), learned.confidence()),
                "PatternLearningAgent"));

        if (learned.dominantPeril() == Peril.THEFT) {
            context.addFinding(new Finding(
                    "LEARNED_DOMINANT_PERIL_THEFT", Severity.LOW, "LEARNED",
                    "Theft is the dominant loss peril among comparable policies",
                    "Comparable claims are predominantly theft; consider security requirements.",
                    "PatternLearningAgent"));
        }

        var area = learned.areaRisk();
        if (area != null && area.sampleSize() > 0 && area.theftClaimRate() >= AREA_THEFT_ELEVATED_THRESHOLD) {
            context.addFinding(new Finding(
                    "AREA_THEFT_ELEVATED", Severity.MEDIUM, "LEARNED",
                    "Area %s shows an elevated theft claim rate (%s)"
                            .formatted(area.city(), pct(area.theftClaimRate())),
                    "The book shows above-average theft losses for this area; loads the price.",
                    "PatternLearningAgent"));
        }

        context.audit("PatternLearningAgent",
                "Learned from %d comparables: claimProb=%.2f lossRatio=%.2f peril=%s confidence=%s"
                        .formatted(learned.comparableCount(), learned.claimProbability(),
                                learned.expectedLossRatio(), learned.dominantPeril(), learned.confidence()));
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }
}
