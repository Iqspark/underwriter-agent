package com.iqspark.underwriter.llm;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The always-available offline reasoner. Builds a deterministic rationale that leads with any
 * condition-precedent knockout, summarizes what the comparable history predicts, and lists the key
 * risk factors. No network, no API key required.
 */
@Component
public class TemplateLlmReasoner implements LlmReasoner {

    @Override
    public String name() {
        return "template-offline";
    }

    @Override
    public String summarize(Submission submission, DecisionOutcome outcome, List<Finding> findings,
                            LearnedAssessment learned, Money premium) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommended action: ").append(outcome).append(". ");

        // Lead with any knockout.
        findings.stream()
                .filter(f -> f.severity() == Severity.KNOCKOUT)
                .findFirst()
                .ifPresent(ko -> sb.append("Condition-precedent knockout: ").append(ko.message())
                        .append(". ").append(ko.rationale()).append(" "));

        // Learned evidence.
        if (learned != null && !learned.coldStart()) {
            sb.append("Learning from the book: the ").append(learned.comparableCount())
                    .append(" most similar past policies had a ").append(pct(learned.claimProbability()))
                    .append(" claim rate and an average loss ratio of ")
                    .append(twoDp(learned.expectedLossRatio()))
                    .append("; the dominant peril was ").append(learned.dominantPeril())
                    .append(" (confidence ").append(learned.confidence()).append("). ");
            if (learned.areaRisk() != null && learned.areaRisk().sampleSize() > 0) {
                sb.append("The area (").append(learned.areaRisk().city()).append(") shows a ")
                        .append(pct(learned.areaRisk().theftClaimRate())).append(" theft claim rate. ");
            }
        } else {
            sb.append("Insufficient comparable history (cold start) — the deterministic guardrails decided. ");
        }

        // Key risk factors (HIGH and MEDIUM), excluding the knockout already stated.
        String key = findings.stream()
                .filter(f -> f.severity() == Severity.HIGH || f.severity() == Severity.MEDIUM)
                .map(Finding::message)
                .distinct()
                .limit(4)
                .collect(Collectors.joining("; "));
        if (!key.isBlank()) {
            sb.append("Key factors: ").append(key).append(". ");
        }

        if (premium != null) {
            sb.append("Indicative premium: ").append(premium.currency()).append(" ")
                    .append(twoDp(premium.amount())).append(". ");
        }

        sb.append(guidance(outcome));
        return sb.toString().trim();
    }

    private static String guidance(DecisionOutcome outcome) {
        return switch (outcome) {
            case APPROVE -> "No blocking issues found; approve on standard terms subject to underwriter review.";
            case REFER -> "Refer to an underwriter: resolve the flagged items or attach the curing conditions before binding.";
            case DECLINE -> "Decline unless the knockout is cured by the stated condition precedent.";
        };
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    private static String twoDp(double v) {
        return String.format("%.2f", v);
    }
}
