package com.iqspark.underwriter.enrichment;

import com.iqspark.underwriter.domain.model.Submission;

/**
 * The enrichment tool boundary (doc 7 §4.3 / doc 17). Implementations wrap external data sources —
 * geocoding, flood/wildfire/wind and crime peril scores, property data — as scoped tools. A real
 * {@code McpEnrichmentProvider} (Model Context Protocol tool servers) plugs in here behind the same
 * interface; the default {@link OfflineEnrichmentProvider} keeps the system runnable offline.
 */
public interface EnrichmentProvider {

    /** A short label recorded for lineage (e.g. "offline", "mcp"). */
    String name();

    /** Enrich a submission's risk location; never throws for a missing location — returns unavailable. */
    Enrichment enrich(Submission submission);
}
