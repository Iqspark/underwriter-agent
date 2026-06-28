package com.iqspark.underwriter.autonomy;

import com.iqspark.underwriter.domain.decision.AutonomyAssessment;
import com.iqspark.underwriter.domain.decision.AutonomyTier;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.ReviewFlag;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Routes each assembled decision into an autonomy tier (doc 8 §4). {@code AUTO} (straight-through)
 * requires ALL bounds to hold; knockouts / declines and low-confidence / large or edge risks go to a
 * {@code SPECIALIST}; everything else is {@code ASSISTED}. Auto-approvals are QA-sampled. This is
 * advisory routing only — the deterministic outcome and human-in-the-loop guarantees are unchanged.
 */
@Component
@ConditionalOnProperty(name = "underwriter.autonomy.enabled", havingValue = "true", matchIfMissing = true)
public class AutonomyRouter {

    private static final Set<String> CONTRADICTION_CODES = Set.of("DATA_CONTRADICTION");
    private static final Set<String> MISSING_CODES = Set.of("MISSING_FIELD", "MISSING_LIABILITY_LIMIT");
    private static final Set<String> EDGE_CODES = Set.of(
            "DEMOLITION_PLANNED", "RENOVATION_PLANNED", "REMOTE_LOCATION", "LOCATION_UNRESOLVED", "PRIOR_LOSSES");

    private final AutonomyProperties props;

    public AutonomyRouter(AutonomyProperties props) {
        this.props = props;
    }

    public AutonomyAssessment evaluate(DecisionOutcome outcome, List<Finding> findings,
                                       LearnedAssessment learned, double coverage,
                                       List<ReviewFlag> reviewFlags) {
        List<String> reasons = new ArrayList<>();

        boolean knockout = findings.stream().anyMatch(Finding::isKnockout);
        boolean contradiction = anyCode(findings, CONTRADICTION_CODES);
        boolean missing = anyCode(findings, MISSING_CODES);
        boolean edge = anyCode(findings, EDGE_CODES);
        boolean highReviewFlag = reviewFlags != null
                && reviewFlags.stream().anyMatch(f -> "HIGH".equalsIgnoreCase(f.severity()));
        boolean lowConfidence = learned == null || learned.coldStart()
                || !"HIGH".equalsIgnoreCase(learned.confidence());
        double claimProb = learned == null ? 1.0 : learned.claimProbability();
        double lossRatio = learned == null ? 1.0 : learned.expectedLossRatio();

        // Hard route to a senior: a knockout or a decline.
        if (knockout || outcome == DecisionOutcome.DECLINE) {
            reasons.add(knockout ? "condition-precedent knockout" : "recommended DECLINE");
            return new AutonomyAssessment(AutonomyTier.SPECIALIST, false, reasons);
        }

        boolean autoEligible = outcome == DecisionOutcome.APPROVE
                && !contradiction && !missing && !edge && !highReviewFlag
                && claimProb < props.getClaimProbabilityMax()
                && lossRatio < props.getLossRatioMax()
                && (!props.isRequireHighConfidence() || !lowConfidence)
                && coverage <= props.getMaxCoverage();

        if (autoEligible) {
            reasons.add("clean, low-risk, high-confidence within bounds");
            boolean sampled = qaSampled();
            if (sampled) {
                reasons.add("selected for QA sampling");
            }
            return new AutonomyAssessment(AutonomyTier.AUTO, sampled, reasons);
        }

        // Not auto: specialist for low confidence / contradictions / edge / large or high-flag, else assisted.
        boolean specialist = lowConfidence || contradiction || highReviewFlag || edge
                || coverage > props.getSpecialistCoverage();
        if (specialist) {
            if (lowConfidence) reasons.add("low learning confidence");
            if (contradiction) reasons.add("data contradiction");
            if (edge) reasons.add("edge risk factor");
            if (highReviewFlag) reasons.add("reviewer raised a high-severity flag");
            if (coverage > props.getSpecialistCoverage()) reasons.add("large coverage");
            return new AutonomyAssessment(AutonomyTier.SPECIALIST, false, reasons);
        }

        reasons.add("requires underwriter review (outside straight-through bounds)");
        return new AutonomyAssessment(AutonomyTier.ASSISTED, false, reasons);
    }

    private boolean qaSampled() {
        double rate = props.getQaSampleRate();
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    private static boolean anyCode(List<Finding> findings, Set<String> codes) {
        return findings.stream().anyMatch(f -> codes.contains(f.code()));
    }
}
