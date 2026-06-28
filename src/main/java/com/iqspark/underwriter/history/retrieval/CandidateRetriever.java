package com.iqspark.underwriter.history.retrieval;

import com.iqspark.underwriter.history.FeatureRanges;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;

import java.util.List;

/**
 * Generates the candidate set the {@code SimilarityEngine} then exact-ranks (ADR-0023). Brute force
 * returns the whole same-line book; the ANN strategy prunes to an approximate neighbourhood for
 * speed at scale. The exact weighted-Gower re-rank afterwards keeps results correct either way.
 */
public interface CandidateRetriever {

    /** A short label recorded for lineage (e.g. "bruteforce", "ann"). */
    String name();

    /**
     * @param query          the submission's features
     * @param sameLineBook   policies already filtered to the submission's line of business
     * @param ranges         feature ranges for normalization
     * @param k              desired neighbour count (candidates should comfortably exceed this)
     * @return candidate policies to exact-rank (a superset of the final top-k)
     */
    List<HistoricalPolicy> candidates(PolicyFeatures query, List<HistoricalPolicy> sameLineBook,
                                      FeatureRanges ranges, int k);
}
