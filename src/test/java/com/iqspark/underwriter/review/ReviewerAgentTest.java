package com.iqspark.underwriter.review;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewerAgentTest {

    private final ReviewerAgent reviewer = new ReviewerAgent(null); // offline, deterministic checks only

    private Finding knockout() {
        return new Finding("INSPECTION_INTERVAL_BREACH", Severity.KNOCKOUT, "COMPLIANCE",
                "Inspection interval 168h exceeds the 72h condition precedent", "…", "rule");
    }

    private Finding medium() {
        return new Finding("OLD_ROOF", Severity.MEDIUM, "RISK", "Roof age 24 years", "…", "rule");
    }

    @Test
    void coherentRationaleHasNoFlags() {
        ReviewResult r = reviewer.review(DecisionOutcome.REFER, List.of(medium()),
                "Recommended action: REFER. Key factors: roof age 24 years. Refer to an underwriter.");
        assertThat(r.flags()).isEmpty();
        assertThat(r.rationaleDefect()).isFalse();
    }

    @Test
    void flagsRationaleThatOmitsAKnockout() {
        ReviewResult r = reviewer.review(DecisionOutcome.DECLINE, List.of(knockout()),
                "Recommended action: DECLINE. The file looks acceptable overall.");
        assertThat(r.flags()).anyMatch(f -> f.code().equals("RATIONALE_OMITS_KNOCKOUT"));
        assertThat(r.rationaleDefect()).isTrue();
        assertThat(r.humanReviewRecommended()).isTrue();
    }

    @Test
    void noFlagWhenKnockoutIsStated() {
        ReviewResult r = reviewer.review(DecisionOutcome.DECLINE, List.of(knockout()),
                "Recommended action: DECLINE. Condition-precedent knockout: inspection interval 168h "
                        + "exceeds the 72h condition precedent. Decline unless cured.");
        assertThat(r.flags()).isEmpty();
        assertThat(r.rationaleDefect()).isFalse();
    }

    @Test
    void flagsOutcomeRationaleMismatch() {
        ReviewResult r = reviewer.review(DecisionOutcome.DECLINE, List.of(),
                "Recommended action: REFER. Refer to an underwriter.");
        assertThat(r.flags()).anyMatch(f -> f.code().equals("OUTCOME_RATIONALE_MISMATCH"));
        assertThat(r.rationaleDefect()).isTrue();
    }
}
