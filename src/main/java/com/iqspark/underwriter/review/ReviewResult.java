package com.iqspark.underwriter.review;

import com.iqspark.underwriter.domain.decision.ReviewFlag;

import java.util.List;

/**
 * The result of a reviewer pass: the advisory flags and whether a <em>rationale</em> defect was
 * found (which may trigger a single re-draft when an LLM reasoner is in use).
 */
public record ReviewResult(List<ReviewFlag> flags, boolean rationaleDefect) {

    public boolean humanReviewRecommended() {
        return flags.stream().anyMatch(f -> "HIGH".equalsIgnoreCase(f.severity()));
    }
}
