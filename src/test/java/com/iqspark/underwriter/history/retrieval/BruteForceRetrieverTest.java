package com.iqspark.underwriter.history.retrieval;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BruteForceRetrieverTest {

    @Test
    void returnsTheWholeSameLineBook() {
        HistoricalPolicyRepository repo = new HistoricalPolicyRepository(500, 42);
        List<HistoricalPolicy> vacantBook = repo.all().stream()
                .filter(p -> p.line() == LineOfBusiness.VACANT_HOME)
                .toList();
        PolicyFeatures query = PolicyFeatures.fromSubmission(Submissions.vacantClean());

        List<HistoricalPolicy> candidates =
                new BruteForceRetriever().candidates(query, vacantBook, repo.featureRanges(), 25);

        assertThat(candidates).isSameAs(vacantBook);
    }
}
