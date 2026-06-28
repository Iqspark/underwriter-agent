package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.history.ModelProperties;
import com.iqspark.underwriter.history.RiskModel;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.Peril;
import com.iqspark.underwriter.history.model.PolicyFeatures;
import org.springframework.stereotype.Component;

/**
 * The AI-first core agent: learns from comparable history (claim probability, loss ratio, dominant
 * peril, area theft) and turns the {@link LearnedAssessment} into findings on the context. The
 * trained {@link RiskModel} <em>predicts</em> the claim probability (blended with k-NN); k-NN keeps
 * <em>explaining</em> via the comparable cases (ADR-0020).
 */
@Component
public class PatternLearningAgent implements UnderwritingAgent {

    private static final double AREA_THEFT_ELEVATED_THRESHOLD = 0.25;

    private final SimilarityEngine similarityEngine;
    private final RiskModel riskModel;
    private final ModelProperties modelProperties;

    public PatternLearningAgent(SimilarityEngine similarityEngine, RiskModel riskModel,
                                ModelProperties modelProperties) {
        this.similarityEngine = similarityEngine;
        this.riskModel = riskModel;
        this.modelProperties = modelProperties;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public void handle(UnderwritingContext context) {
        LearnedAssessment learned = similarityEngine.assess(context.submission());

        // Hybrid: the trained model predicts; blend it with the k-NN claim probability (most
        // conservative by default). k-NN comparables remain the explanation.
        if (!learned.coldStart() && modelProperties.isEnabled() && riskModel.isReady()) {
            double knnProb = learned.claimProbability();
            double modelProb = riskModel.predictClaimProbability(
                    PolicyFeatures.fromSubmission(context.submission()));
            double blended = blend(knnProb, modelProb);
            learned = withClaimProbability(learned, blended);
            context.audit("PatternLearningAgent",
                    "Hybrid claimProb: knn=%.2f model=%.2f blended=%.2f (blend=%s, model=%s)"
                            .formatted(knnProb, modelProb, blended,
                                    modelProperties.getBlend(), riskModel.name()));
        }

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

    private double blend(double knn, double model) {
        double v = switch (modelProperties.getBlend() == null ? "max" : modelProperties.getBlend().toLowerCase()) {
            case "mean" -> (knn + model) / 2.0;
            case "model" -> model;
            case "knn" -> knn;
            default -> Math.max(knn, model); // conservative
        };
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static LearnedAssessment withClaimProbability(LearnedAssessment l, double claimProbability) {
        return new LearnedAssessment(
                l.comparableCount(), l.meanSimilarity(), claimProbability, l.expectedLossRatio(),
                l.suggestedRatePerThousand(), l.dominantPeril(), l.confidence(), l.coldStart(),
                l.topComparables(), l.areaRisk());
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }
}
