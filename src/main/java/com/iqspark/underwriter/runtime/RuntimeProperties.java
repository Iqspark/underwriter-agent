package com.iqspark.underwriter.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Event-driven runtime config ({@code underwriter.runtime.*}). Lean tier: in-process async events +
 * a durable state machine + outbox. {@code async=false} processes inline (used by tests for
 * determinism); set Kafka/Temporal later without changing the seams.
 */
@Component
@ConfigurationProperties("underwriter.runtime")
public class RuntimeProperties {

    /** Process cases asynchronously (true in prod) or inline within the request (false for tests). */
    private boolean async = true;
    /** Max processing attempts before a case is dead-lettered (FAILED). */
    private int maxAttempts = 3;

    public boolean isAsync() { return async; }
    public void setAsync(boolean async) { this.async = async; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
