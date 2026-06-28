package com.iqspark.underwriter.history.retrieval;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.history.AreaRiskService;
import com.iqspark.underwriter.history.FeatureRanges;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.SimilarityEngine;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.PolicyFeatures;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnRetrieverTest {

    private final HistoricalPolicyRepository repo = new HistoricalPolicyRepository(1500, 42);
    private final FeatureRanges ranges = repo.featureRanges();
    private final List<HistoricalPolicy> vacantBook = repo.all().stream()
            .filter(p -> p.line() == LineOfBusiness.VACANT_HOME)
            .toList();
    private final PolicyFeatures query = PolicyFeatures.fromSubmission(Submissions.vacantClean());

    @Test
    void returnsASubsetOfAtLeastKDeterministically() {
        AnnRetriever ann = new AnnRetriever();
        List<HistoricalPolicy> first = ann.candidates(query, vacantBook, ranges, 25);
        List<HistoricalPolicy> second = ann.candidates(query, vacantBook, ranges, 25);

        assertThat(first).hasSizeGreaterThanOrEqualTo(25);   // bucket >= k, or full-scan fallback
        assertThat(vacantBook).containsAll(first);           // subset of the same-line book
        assertThat(first).isEqualTo(second);                 // deterministic
    }

    @Test
    void similarityEngineWorksEndToEndWithAnn() {
        AreaRiskService area = new AreaRiskService(repo);
        SimilarityEngine engine = new SimilarityEngine(repo, area, 25, new AnnRetriever());

        LearnedAssessment a = engine.assess(Submissions.vacantClean());

        assertThat(a.coldStart()).isFalse();
        assertThat(a.comparableCount()).isBetween(1, 25);
        assertThat(a.claimProbability()).isBetween(0.0, 1.0);
        assertThat(a.topComparables()).isNotEmpty();
    }
}
