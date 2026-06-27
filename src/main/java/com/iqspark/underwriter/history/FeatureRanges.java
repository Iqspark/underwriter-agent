package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-numeric-feature min/max over the book, used to normalize numeric features to [0,1] for the
 * Gower distance so features with large raw ranges (e.g. coverageAmount) don't dominate.
 */
public final class FeatureRanges {

    private final Map<String, double[]> ranges; // key -> {min, max}

    private FeatureRanges(Map<String, double[]> ranges) {
        this.ranges = ranges;
    }

    public static FeatureRanges computeFrom(List<HistoricalPolicy> book) {
        Map<String, double[]> r = new HashMap<>();
        for (String key : PolicyFeatures.NUMERIC_KEYS) {
            r.put(key, new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
        }
        for (HistoricalPolicy p : book) {
            PolicyFeatures f = p.features();
            for (String key : PolicyFeatures.NUMERIC_KEYS) {
                double val = f.num(key);
                double[] mm = r.get(key);
                if (val < mm[0]) mm[0] = val;
                if (val > mm[1]) mm[1] = val;
            }
        }
        // Guard empty book: leave a degenerate range that normalizes to 0.
        for (String key : PolicyFeatures.NUMERIC_KEYS) {
            double[] mm = r.get(key);
            if (mm[0] == Double.POSITIVE_INFINITY) {
                mm[0] = 0;
                mm[1] = 0;
            }
        }
        return new FeatureRanges(r);
    }

    /** Normalize a raw feature value into [0,1]; a degenerate (min==max) range yields 0. */
    public double normalize(String feature, double value) {
        double[] mm = ranges.get(feature);
        if (mm == null) {
            return 0.0;
        }
        double span = mm[1] - mm[0];
        if (span <= 0) {
            return 0.0;
        }
        double norm = (value - mm[0]) / span;
        if (norm < 0) return 0.0;
        if (norm > 1) return 1.0;
        return norm;
    }
}
