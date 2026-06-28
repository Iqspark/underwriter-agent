package com.iqspark.underwriter.history;

import com.iqspark.underwriter.history.model.PolicyFeatures;

/**
 * A trained predictive model for claim probability (ADR-0020). It complements the case-based k-NN:
 * the model <em>predicts</em>, k-NN keeps <em>explaining</em> (the comparable cases). The default
 * {@link LogisticRiskModel} is an offline, dependency-free stand-in; a gradient-boosting model
 * (XGBoost/LightGBM) plugs in behind this seam later.
 */
public interface RiskModel {

    /** A short label recorded for lineage (e.g. "logistic"). */
    String name();

    /** Whether the model has trained successfully and can be used. */
    boolean isReady();

    /** Predicted claim probability in [0,1] for the given features. */
    double predictClaimProbability(PolicyFeatures features);
}
