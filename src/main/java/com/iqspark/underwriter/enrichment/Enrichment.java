package com.iqspark.underwriter.enrichment;

/**
 * External enrichment for a risk location, normalized into a small, line-agnostic shape (the
 * anti-corruption layer output). Scores are 0..1. {@code available=false} means enrichment couldn't
 * be resolved or the provider was down — the pipeline degrades to book-only signals.
 */
public record Enrichment(
        boolean available,
        String source,
        double crimeScore,
        double floodScore,
        double wildfireScore,
        double windScore
) {
    public static Enrichment unavailable() {
        return new Enrichment(false, "none", 0, 0, 0, 0);
    }
}
