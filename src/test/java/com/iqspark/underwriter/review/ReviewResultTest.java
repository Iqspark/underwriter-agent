package com.iqspark.underwriter.review;

import com.iqspark.underwriter.domain.decision.ReviewFlag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResultTest {

    @Test
    void humanReviewRecommendedOnlyForHighSeverityFlag() {
        ReviewResult none = new ReviewResult(List.of(), false);
        ReviewResult medium = new ReviewResult(
                List.of(new ReviewFlag("X", "MEDIUM", "m", "r")), false);
        ReviewResult high = new ReviewResult(
                List.of(new ReviewFlag("Y", "HIGH", "m", "r")), true);

        assertThat(none.humanReviewRecommended()).isFalse();
        assertThat(medium.humanReviewRecommended()).isFalse();
        assertThat(high.humanReviewRecommended()).isTrue();
    }
}
