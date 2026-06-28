package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.intake.SemanticExtractionProperties;
import com.iqspark.underwriter.intake.SemanticFeatureExtractor;
import com.iqspark.underwriter.intake.SemanticFeatures;
import org.springframework.stereotype.Component;

/**
 * Extracts bounded semantic risk features from the submission's unstructured text (inspection report
 * / broker email) and turns them into advisory findings (ADR-0021, order 12 — early, after intake).
 * Features are inputs, never decisions: they raise capped-severity findings (and are carried on the
 * context for later model integration), but never a knockout. Degrades safely when there's no text.
 */
@Component
public class UnstructuredDataAgent implements UnderwritingAgent {

    private final SemanticFeatureExtractor extractor;
    private final SemanticExtractionProperties properties;

    public UnstructuredDataAgent(SemanticFeatureExtractor extractor,
                                 SemanticExtractionProperties properties) {
        this.extractor = extractor;
        this.properties = properties;
    }

    @Override
    public int order() {
        return 12;
    }

    @Override
    public void handle(UnderwritingContext context) {
        if (!properties.isSemanticFeaturesEnabled()) {
            return;
        }
        SemanticFeatures f = extractor.extract(context.submission().notes());
        context.setSemanticFeatures(f);
        if (!f.present()) {
            return; // no unstructured text — nothing to do
        }

        if (f.deferredMaintenancePresent()) {
            context.addFinding(new Finding("SEMANTIC_DEFERRED_MAINTENANCE", Severity.MEDIUM, "SEMANTIC",
                    "Unstructured text indicates deferred maintenance",
                    "Extracted from the inspection/broker notes; verify and rate accordingly.",
                    "UnstructuredDataAgent"));
        }
        if (!f.hazards().isEmpty()) {
            context.addFinding(new Finding("SEMANTIC_HAZARDS", Severity.MEDIUM, "SEMANTIC",
                    "Hazards mentioned in notes: " + String.join(", ", f.hazards()),
                    "Extracted hazards may require survey, exclusion, or rating.",
                    "UnstructuredDataAgent"));
        }
        if ("HIGH".equals(f.priorLossNarrativeSeverity())) {
            context.addFinding(new Finding("SEMANTIC_PRIOR_LOSS_NARRATIVE", Severity.HIGH, "SEMANTIC",
                    "Notes describe a severe prior loss",
                    "Narrative suggests a significant past loss; review loss history before binding.",
                    "UnstructuredDataAgent"));
        } else if ("MEDIUM".equals(f.priorLossNarrativeSeverity())) {
            context.addFinding(new Finding("SEMANTIC_PRIOR_LOSS_NARRATIVE", Severity.MEDIUM, "SEMANTIC",
                    "Notes mention a prior loss/claim",
                    "Narrative mentions a past loss; confirm against declared loss history.",
                    "UnstructuredDataAgent"));
        }
        if (f.recentRenovation()) {
            context.addFinding(new Finding("SEMANTIC_RENOVATION", Severity.LOW, "SEMANTIC",
                    "Notes mention recent/ongoing renovation",
                    "Active renovation introduces trade/liability exposure; confirm scope and contractor cover.",
                    "UnstructuredDataAgent"));
        }
        if ("NEGATIVE".equals(f.inspectorSentiment())) {
            context.addFinding(new Finding("SEMANTIC_NEGATIVE_SENTIMENT", Severity.LOW, "SEMANTIC",
                    "Inspector sentiment is negative",
                    "Overall tone of the notes is negative; a closer review may be warranted.",
                    "UnstructuredDataAgent"));
        }

        context.audit("UnstructuredDataAgent",
                "Extracted semantic features (%s, conf %.2f): deferredMaintenance=%s renovation=%s hazards=%d sentiment=%s priorLoss=%s"
                        .formatted(f.source(), f.confidence(), f.deferredMaintenancePresent(),
                                f.recentRenovation(), f.hazards().size(), f.inspectorSentiment(),
                                f.priorLossNarrativeSeverity()));
    }
}
