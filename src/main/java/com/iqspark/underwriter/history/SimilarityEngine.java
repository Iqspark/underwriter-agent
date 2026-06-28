package com.iqspark.underwriter.history;

import com.iqspark.underwriter.domain.model.LineOfBusiness;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.ComparableCase;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.history.model.Peril;
import com.iqspark.underwriter.history.model.PolicyFeatures;
import com.iqspark.underwriter.history.retrieval.BruteForceRetriever;
import com.iqspark.underwriter.history.retrieval.CandidateRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The AI-first core: case-based (k-NN) retrieval over the book using a weighted Gower distance,
 * then a similarity-weighted aggregation of the comparables' actual outcomes into a
 * {@link LearnedAssessment}. Comparables are restricted to the submission's line of business; a
 * thin line falls back to a cold-start assessment so guardrails decide.
 *
 * <p>Feature weights and the cold-start floor encode appetite and must be reviewed/back-tested by
 * underwriting/actuarial before production use (doc 5, doc 13).
 */
@Service
public class SimilarityEngine {

    private static final int MIN_BOOK_FOR_LEARNING = 5;
    private static final int TOP_COMPARABLES_SHOWN = 8;

    private static final Map<String, Double> NUMERIC_WEIGHTS = Map.ofEntries(
            Map.entry("roofAgeYears", 1.5),
            Map.entry("vacancyMonths", 1.5),
            Map.entry("priorLossCount", 1.0),
            Map.entry("coverageAmount", 0.8),
            Map.entry("monitoredAlarm", 0.5),
            Map.entry("inspectionIntervalHours", 1.0),
            Map.entry("distanceToFireHallKm", 1.0),
            Map.entry("securitySystem", 1.0),
            Map.entry("units", 0.5),
            Map.entry("squareFeet", 0.5),
            Map.entry("yearBuilt", 0.5));

    private static final Map<String, Double> CATEGORICAL_WEIGHTS = Map.of(
            "city", 2.0,
            "province", 1.0,
            "construction", 0.5,
            "occupancyType", 0.3);

    private static final double TOTAL_WEIGHT =
            NUMERIC_WEIGHTS.values().stream().mapToDouble(Double::doubleValue).sum()
                    + CATEGORICAL_WEIGHTS.values().stream().mapToDouble(Double::doubleValue).sum();

    private final HistoricalPolicyRepository repository;
    private final AreaRiskService areaRiskService;
    private final int k;
    private final CandidateRetriever retriever;

    /** Spring constructor — the retriever is selected by {@code underwriter.similarity.index}. */
    @Autowired
    public SimilarityEngine(HistoricalPolicyRepository repository,
                            AreaRiskService areaRiskService,
                            @Value("${underwriter.similarity.k:25}") int k,
                            CandidateRetriever retriever) {
        this.repository = repository;
        this.areaRiskService = areaRiskService;
        this.k = k;
        this.retriever = retriever;
    }

    /** Convenience constructor (tests) — exact brute-force retrieval. */
    public SimilarityEngine(HistoricalPolicyRepository repository,
                            AreaRiskService areaRiskService,
                            int k) {
        this(repository, areaRiskService, k, new BruteForceRetriever());
    }

    public LearnedAssessment assess(Submission submission) {
        String city = submission.location() != null ? submission.location().city() : null;
        var areaRisk = areaRiskService.forCity(city);

        LineOfBusiness line = submission.effectiveLine();
        List<HistoricalPolicy> sameLine = repository.all().stream()
                .filter(p -> p.line() == line)
                .toList();

        if (sameLine.size() < MIN_BOOK_FOR_LEARNING) {
            return LearnedAssessment.coldStart(areaRisk);
        }

        PolicyFeatures qf = PolicyFeatures.fromSubmission(submission);
        FeatureRanges ranges = repository.featureRanges();

        // Candidate generation (brute force by default; ANN at scale). Exact re-rank follows.
        List<HistoricalPolicy> candidates = retriever.candidates(qf, sameLine, ranges, k);

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (HistoricalPolicy p : candidates) {
            scored.add(new Scored(p, similarity(qf, p.features(), ranges)));
        }
        scored.sort(Comparator.comparingDouble(Scored::similarity).reversed());

        int take = Math.min(k, scored.size());
        List<Scored> top = scored.subList(0, take);

        double simSum = 0, claimW = 0, lossW = 0, rateW = 0, meanSimAcc = 0;
        Map<Peril, Double> perilWeight = new EnumMap<>(Peril.class);
        for (Scored s : top) {
            double sim = Math.max(s.similarity(), 0.0001);
            simSum += sim;
            meanSimAcc += s.similarity();
            HistoricalPolicy p = s.policy();
            if (p.hadClaim()) {
                claimW += sim;
                perilWeight.merge(p.dominantPeril(), sim, Double::sum);
            }
            lossW += sim * p.lossRatio();
            rateW += sim * p.ratePerThousand();
        }

        double claimProbability = round4(claimW / simSum);
        double expectedLossRatio = round4(lossW / simSum);
        double suggestedRate = round2(rateW / simSum);
        double meanSimilarity = round4(meanSimAcc / take);
        Peril dominantPeril = perilWeight.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Peril.NONE);
        String confidence = confidence(take, meanSimilarity);

        List<ComparableCase> comparables = new ArrayList<>();
        for (Scored s : top.subList(0, Math.min(TOP_COMPARABLES_SHOWN, top.size()))) {
            HistoricalPolicy p = s.policy();
            comparables.add(new ComparableCase(p.id(), round4(s.similarity()), p.city(),
                    p.hadClaim(), p.dominantPeril(), p.lossRatio(), p.ratePerThousand()));
        }

        return new LearnedAssessment(take, meanSimilarity, claimProbability, expectedLossRatio,
                suggestedRate, dominantPeril, confidence, false, comparables, areaRisk);
    }

    /** Weighted Gower similarity in [0,1]; {@code 1 - distance}. */
    double similarity(PolicyFeatures q, PolicyFeatures p, FeatureRanges ranges) {
        double distance = 0.0;
        for (Map.Entry<String, Double> e : NUMERIC_WEIGHTS.entrySet()) {
            String key = e.getKey();
            double qn = ranges.normalize(key, q.num(key));
            double pn = ranges.normalize(key, p.num(key));
            distance += e.getValue() * Math.abs(qn - pn);
        }
        for (Map.Entry<String, Double> e : CATEGORICAL_WEIGHTS.entrySet()) {
            String key = e.getKey();
            boolean equal = q.cat(key).equalsIgnoreCase(p.cat(key));
            distance += e.getValue() * (equal ? 0.0 : 1.0);
        }
        return 1.0 - (distance / TOTAL_WEIGHT);
    }

    private static String confidence(int count, double meanSimilarity) {
        if (count >= 15 && meanSimilarity >= 0.80) {
            return "HIGH";
        }
        if (count >= 8 && meanSimilarity >= 0.60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private record Scored(HistoricalPolicy policy, double similarity) {}

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
