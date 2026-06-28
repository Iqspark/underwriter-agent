package com.iqspark.underwriter.enrichment;

import com.iqspark.underwriter.domain.model.Submission;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Deterministic, offline stand-in for the MCP enrichment tools — derives plausible peril scores from
 * the risk location so the system runs and is testable with no external services. Replace with a
 * {@code McpEnrichmentProvider} in production (same interface). Same input → same scores.
 */
@Component
public class OfflineEnrichmentProvider implements EnrichmentProvider {

    // Illustrative city crime bias (mirrors the synthetic book's theft tendencies).
    private static final Map<String, Double> CITY_CRIME = Map.ofEntries(
            Map.entry("flin flon", 0.85), Map.entry("winnipeg", 0.80), Map.entry("saskatoon", 0.72),
            Map.entry("sudbury", 0.65), Map.entry("regina", 0.70), Map.entry("vancouver", 0.55),
            Map.entry("montreal", 0.45), Map.entry("toronto", 0.40), Map.entry("ottawa", 0.32),
            Map.entry("calgary", 0.38), Map.entry("halifax", 0.30));

    // Illustrative province peril bias: {flood, wildfire, wind}.
    private static final Map<String, double[]> PROVINCE_PERIL = Map.ofEntries(
            Map.entry("BC", new double[]{0.45, 0.80, 0.40}),
            Map.entry("AB", new double[]{0.40, 0.70, 0.45}),
            Map.entry("SK", new double[]{0.35, 0.45, 0.55}),
            Map.entry("MB", new double[]{0.55, 0.40, 0.50}),
            Map.entry("ON", new double[]{0.40, 0.25, 0.35}),
            Map.entry("QC", new double[]{0.50, 0.30, 0.35}),
            Map.entry("NB", new double[]{0.55, 0.20, 0.60}),
            Map.entry("NS", new double[]{0.60, 0.15, 0.70}),
            Map.entry("NL", new double[]{0.55, 0.15, 0.75}),
            Map.entry("PE", new double[]{0.60, 0.15, 0.65}));

    @Override
    public String name() {
        return "offline";
    }

    @Override
    public Enrichment enrich(Submission submission) {
        Submission.RiskLocation loc = submission.location();
        if (loc == null || loc.city() == null || loc.city().isBlank()) {
            return Enrichment.unavailable();
        }
        String city = loc.city().trim().toLowerCase();
        String province = loc.province() == null ? "" : loc.province().trim().toUpperCase();

        double jitter = ((Math.abs(city.hashCode()) % 21) - 10) / 100.0; // deterministic -0.10..+0.10
        double crime = clamp(CITY_CRIME.getOrDefault(city, 0.35) + jitter);

        double[] peril = PROVINCE_PERIL.getOrDefault(province, new double[]{0.35, 0.30, 0.35});
        double flood = clamp(peril[0] + jitter);
        double wildfire = clamp(peril[1] + jitter);
        double wind = clamp(peril[2] + jitter);

        return new Enrichment(true, name(), round2(crime), round2(flood), round2(wildfire), round2(wind));
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
