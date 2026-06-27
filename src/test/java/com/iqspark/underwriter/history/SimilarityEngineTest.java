package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarityEngineTest {

    private final HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1500, 42);
    private final AreaRiskService area = new AreaRiskService(repo);
    private final SimilarityEngine engine = new SimilarityEngine(repo, area, 25);

    @Test
    void assessesAVacantHomeFileWithComparables() {
        LearnedAssessment a = engine.assess(Submissions.vacantClean());
        assertThat(a.coldStart()).isFalse();
        assertThat(a.comparableCount()).isBetween(1, 25);
        assertThat(a.claimProbability()).isBetween(0.0, 1.0);
        assertThat(a.expectedLossRatio()).isGreaterThanOrEqualTo(0.0);
        assertThat(a.confidence()).isIn("LOW", "MEDIUM", "HIGH");
        assertThat(a.topComparables()).isNotEmpty().hasSizeLessThanOrEqualTo(8);
        assertThat(a.meanSimilarity()).isBetween(0.0, 1.0);
    }

    @Test
    void assessesOtherLinesToo() {
        assertThat(engine.assess(Submissions.contents())).isNotNull();
        assertThat(engine.assess(Submissions.rentalStrNoEndorsement())).isNotNull();
    }

    @Test
    void coldStartWhenBookIsTiny() {
        HistoricalPolicyRepository tiny = new HistoricalPolicyRepository(3, 42);
        SimilarityEngine tinyEngine = new SimilarityEngine(tiny, new AreaRiskService(tiny), 25);
        assertThat(tinyEngine.assess(Submissions.vacantClean()).coldStart()).isTrue();
    }
}
