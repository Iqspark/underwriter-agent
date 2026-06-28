package com.iqspark.underwriter.history;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Hybrid predictive-model config ({@code underwriter.model.*}). The trained model blends with the
 * k-NN claim-probability signal; the default blend is the most conservative (higher-risk) of the two.
 */
@Component
@ConfigurationProperties("underwriter.model")
public class ModelProperties {

    /** Use the trained model's prediction (blended with k-NN). */
    private boolean enabled = true;
    /** Blend mode: max (conservative) | mean | model | knn. */
    private String blend = "max";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBlend() { return blend; }
    public void setBlend(String blend) { this.blend = blend; }
}
