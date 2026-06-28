package com.iqspark.underwriter.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Enrichment config ({@code underwriter.enrichment.*}). Default provider is offline; point at MCP
 * tool servers in production. Responses are cached to bound cost/latency (doc 14).
 */
@Component
@ConfigurationProperties("underwriter.enrichment")
public class EnrichmentProperties {

    /** Master switch for the enrichment agent. */
    private boolean enabled = true;
    /** Cache TTL for enrichment lookups, seconds. */
    private long cacheTtlSeconds = 3600;
    /** Score at/above which a peril is a HIGH finding; ~0.6× of it is MEDIUM. */
    private double highThreshold = 0.7;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public void setCacheTtlSeconds(long cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
    public double getHighThreshold() { return highThreshold; }
    public void setHighThreshold(double highThreshold) { this.highThreshold = highThreshold; }
}
