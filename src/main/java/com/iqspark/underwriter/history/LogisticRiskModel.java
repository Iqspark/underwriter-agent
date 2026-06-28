package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.HistoricalPolicy;
import com.iqspark.underwriter.history.model.PolicyFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Offline, dependency-free trained model (ADR-0020): a logistic regression over the normalized
 * numeric features, fit by gradient descent on the historical book at startup. It learns feature
 * importance from data (unlike the fixed Gower weights) and predicts claim probability. A real
 * gradient-boosting model plugs in behind {@link RiskModel}; this keeps the system runnable offline.
 */
@Component
public class LogisticRiskModel implements RiskModel {

    private static final Logger log = LoggerFactory.getLogger(LogisticRiskModel.class);
    private static final int MIN_BOOK = 20;
    private static final int ITERATIONS = 200;
    private static final double LEARNING_RATE = 0.1;

    private final List<String> keys = PolicyFeatures.NUMERIC_KEYS;
    private final FeatureRanges ranges;
    private final double[] weights;   // [0..n-1] feature weights, [n] bias
    private final boolean ready;

    public LogisticRiskModel(HistoricalPolicyRepository repository) {
        List<HistoricalPolicy> book = repository.all();
        this.ranges = repository.featureRanges();
        int n = keys.size();
        this.weights = new double[n + 1];
        if (book.size() < MIN_BOOK) {
            this.ready = false;
            return;
        }
        train(book, n);
        this.ready = true;
        log.info("LogisticRiskModel trained on {} policies ({} features)", book.size(), n);
    }

    private void train(List<HistoricalPolicy> book, int n) {
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (HistoricalPolicy p : book) {
                double[] x = vector(p.features());
                double pred = sigmoid(score(x));
                double error = pred - (p.hadClaim() ? 1.0 : 0.0);
                for (int j = 0; j < n; j++) {
                    weights[j] -= LEARNING_RATE * error * x[j];
                }
                weights[n] -= LEARNING_RATE * error; // bias
            }
        }
    }

    @Override
    public String name() {
        return "logistic";
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public double predictClaimProbability(PolicyFeatures features) {
        if (!ready) {
            return 0.0;
        }
        double p = sigmoid(score(vector(features)));
        return Math.max(0.0, Math.min(1.0, p));
    }

    private double[] vector(PolicyFeatures f) {
        double[] x = new double[keys.size()];
        for (int j = 0; j < keys.size(); j++) {
            x[j] = ranges.normalize(keys.get(j), f.num(keys.get(j)));
        }
        return x;
    }

    private double score(double[] x) {
        double z = weights[keys.size()]; // bias
        for (int j = 0; j < x.length; j++) {
            z += weights[j] * x[j];
        }
        return z;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
