package com.iqspark.underwriter.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG configuration ({@code underwriter.rag.*}). {@code enabled} is the master switch — when false
 * the pipeline behaves exactly like the pre-RAG design.
 */
@Component
@ConfigurationProperties("underwriter.rag")
public class RagProperties {

    /** Master switch. */
    private boolean enabled = false;
    /** Top-k chunks retrieved per source type. */
    private int topK = 4;
    /** Drop retrievals below this similarity score. */
    private double minScore = 0.6;
    /** Cap on the advisory RAG finding's severity (INFO|LOW|MEDIUM|HIGH — never KNOCKOUT). */
    private String maxSeverity = "MEDIUM";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getMinScore() { return minScore; }
    public void setMinScore(double minScore) { this.minScore = minScore; }
    public String getMaxSeverity() { return maxSeverity; }
    public void setMaxSeverity(String maxSeverity) { this.maxSeverity = maxSeverity; }
}
