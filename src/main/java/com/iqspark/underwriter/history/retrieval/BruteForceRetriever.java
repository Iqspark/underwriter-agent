package com.iqspark.underwriter.history.retrieval;

import com.iqspark.underwriter.history.FeatureRanges;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;

import java.util.List;

/**
 * Exact retrieval: returns the whole same-line book for scoring (the current, O(n)-per-request
 * behaviour). The default and the correctness oracle for the ANN strategy.
 */
public class BruteForceRetriever implements CandidateRetriever {

    @Override
    public String name() {
        return "bruteforce";
    }

    @Override
    public List<HistoricalPolicy> candidates(PolicyFeatures query, List<HistoricalPolicy> sameLineBook,
                                             FeatureRanges ranges, int k) {
        return sameLineBook;
    }
}
