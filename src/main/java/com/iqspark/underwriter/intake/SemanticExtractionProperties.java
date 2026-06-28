package com.iqspark.underwriter.intake;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Semantic feature extraction config ({@code underwriter.intake.*}). */
@Component
@ConfigurationProperties("underwriter.intake")
public class SemanticExtractionProperties {

    /** Master switch for the unstructured-data agent. */
    private boolean semanticFeaturesEnabled = true;

    public boolean isSemanticFeaturesEnabled() { return semanticFeaturesEnabled; }
    public void setSemanticFeaturesEnabled(boolean v) { this.semanticFeaturesEnabled = v; }
}
