package com.iqspark.underwriter.intake;

import java.util.List;

/**
 * A bounded, typed set of risk features extracted from unstructured text (ADR-0021). The schema is
 * fixed and small on purpose — features are <em>inputs</em> (advisory findings today; predictive-model
 * inputs later), never free text injected into the decision. {@code present=false} means no
 * extraction happened (no text or nothing found).
 */
public record SemanticFeatures(
        boolean present,
        boolean deferredMaintenancePresent,
        boolean recentRenovation,
        List<String> hazards,
        String inspectorSentiment,          // NEGATIVE | NEUTRAL | POSITIVE
        String priorLossNarrativeSeverity,  // NONE | LOW | MEDIUM | HIGH
        double confidence,
        String source                       // "llm" | "heuristic"
) {
    public static SemanticFeatures empty() {
        return new SemanticFeatures(false, false, false, List.of(), "NEUTRAL", "NONE", 0.0, "none");
    }
}
