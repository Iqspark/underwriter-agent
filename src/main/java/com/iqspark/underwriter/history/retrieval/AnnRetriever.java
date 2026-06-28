package com.iqspark.underwriter.history.retrieval;

import com.iqspark.underwriter.history.FeatureRanges;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Approximate-nearest-neighbour candidate generation via random-hyperplane LSH (ADR-0023): policies
 * are hashed into buckets by the sign of their projection onto fixed random hyperplanes, so a query
 * only scores its bucket instead of the whole book. The {@code SimilarityEngine} then exact-ranks the
 * candidates, so accuracy is preserved; if a bucket is too sparse it falls back to the full scan.
 *
 * <p>This is the offline, dependency-free stand-in; in production the same seam is served by pgvector
 * (HNSW) — see ADR-0023. The per-size index is built once and cached.
 */
public class AnnRetriever implements CandidateRetriever {

    private static final int HYPERPLANES = 12;
    private static final long SEED = 42L;
    private static final List<String> KEYS = PolicyFeatures.NUMERIC_KEYS;

    private final Map<Integer, Index> indexBySize = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "ann";
    }

    @Override
    public List<HistoricalPolicy> candidates(PolicyFeatures query, List<HistoricalPolicy> sameLineBook,
                                             FeatureRanges ranges, int k) {
        if (sameLineBook.isEmpty()) {
            return sameLineBook;
        }
        Index index = indexBySize.computeIfAbsent(sameLineBook.size(), s -> build(sameLineBook, ranges));
        String signature = signature(vector(query, ranges), index.hyperplanes());
        List<HistoricalPolicy> bucket = index.buckets().getOrDefault(signature, List.of());
        // Too few neighbours in the bucket — fall back to an exact scan for correctness.
        return bucket.size() < k ? sameLineBook : bucket;
    }

    private Index build(List<HistoricalPolicy> book, FeatureRanges ranges) {
        Random rnd = new Random(SEED);
        int dim = KEYS.size();
        double[][] hyperplanes = new double[HYPERPLANES][dim];
        for (int h = 0; h < HYPERPLANES; h++) {
            for (int j = 0; j < dim; j++) {
                hyperplanes[h][j] = rnd.nextGaussian();
            }
        }
        Map<String, List<HistoricalPolicy>> buckets = new ConcurrentHashMap<>();
        for (HistoricalPolicy p : book) {
            String sig = signature(vector(p.features(), ranges), hyperplanes);
            buckets.computeIfAbsent(sig, key -> new ArrayList<>()).add(p);
        }
        return new Index(hyperplanes, buckets);
    }

    private static double[] vector(PolicyFeatures f, FeatureRanges ranges) {
        double[] x = new double[KEYS.size()];
        for (int j = 0; j < KEYS.size(); j++) {
            x[j] = ranges.normalize(KEYS.get(j), f.num(KEYS.get(j)));
        }
        return x;
    }

    private static String signature(double[] vec, double[][] hyperplanes) {
        StringBuilder sb = new StringBuilder(hyperplanes.length);
        for (double[] hp : hyperplanes) {
            double dot = 0;
            for (int j = 0; j < vec.length; j++) {
                dot += hp[j] * vec[j];
            }
            sb.append(dot >= 0 ? '1' : '0');
        }
        return sb.toString();
    }

    private record Index(double[][] hyperplanes, Map<String, List<HistoricalPolicy>> buckets) {}
}
