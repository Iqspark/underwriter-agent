package com.iqspark.underwriter.agent;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.Severity;
import com.iqspark.underwriter.enrichment.Enrichment;
import com.iqspark.underwriter.enrichment.EnrichmentProperties;
import com.iqspark.underwriter.enrichment.EnrichmentService;
import org.springframework.stereotype.Component;

/**
 * Pulls external enrichment (peril/crime scores) via the enrichment tool boundary and turns elevated
 * scores into findings (order 15 — after intake, before rule profiling, so the signals are present
 * for the rest of the pipeline). Degrades safely: if enrichment is unavailable the pipeline proceeds
 * on book-only signals; enrichment never fails a submission.
 */
@Component
public class EnrichmentAgent implements UnderwritingAgent {

    private final EnrichmentService enrichmentService;
    private final EnrichmentProperties properties;

    public EnrichmentAgent(EnrichmentService enrichmentService, EnrichmentProperties properties) {
        this.enrichmentService = enrichmentService;
        this.properties = properties;
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    public void handle(UnderwritingContext context) {
        if (!properties.isEnabled()) {
            return;
        }
        Enrichment e = enrichmentService.enrich(context.submission());
        context.setEnrichment(e);

        if (!e.available()) {
            context.audit("EnrichmentAgent", "Enrichment unavailable; proceeding on book-only signals.");
            return;
        }

        addPerilFinding(context, "ENRICHED_CRIME_ELEVATED", "crime/theft", e.crimeScore());
        addPerilFinding(context, "ENRICHED_FLOOD_RISK", "flood", e.floodScore());
        addPerilFinding(context, "ENRICHED_WILDFIRE_RISK", "wildfire", e.wildfireScore());
        addPerilFinding(context, "ENRICHED_WIND_RISK", "wind", e.windScore());

        context.audit("EnrichmentAgent",
                "Enriched from %s: crime=%.2f flood=%.2f wildfire=%.2f wind=%.2f"
                        .formatted(e.source(), e.crimeScore(), e.floodScore(),
                                e.wildfireScore(), e.windScore()));
    }

    private void addPerilFinding(UnderwritingContext context, String code, String peril, double score) {
        double high = properties.getHighThreshold();
        Severity severity;
        if (score >= high) {
            severity = Severity.HIGH;
        } else if (score >= high * 0.6) {
            severity = Severity.MEDIUM;
        } else {
            return; // not elevated — no finding
        }
        context.addFinding(new Finding(
                code, severity, "ENRICHMENT",
                "Elevated %s peril score from enrichment (%.2f)".formatted(peril, score),
                "External data indicates above-average %s exposure for this location; rate/condition accordingly."
                        .formatted(peril),
                "EnrichmentAgent"));
    }
}
