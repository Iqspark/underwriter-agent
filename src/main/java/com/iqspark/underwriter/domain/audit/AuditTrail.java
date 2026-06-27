package com.iqspark.underwriter.domain.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * An append-only, ordered log of agent actions threaded through the {@code UnderwritingContext}.
 * Each entry is rendered as {@code "<iso-instant> | <agent> | <detail>"}. Emitted on every
 * {@code Decision} as the audit trail.
 */
public class
AuditTrail {

    private final List<String> entries = new ArrayList<>();

    /** Record an action by an agent. The agent label is padded for readable, columnar logs. */
    public void record(String agent, String detail) {
        String ts = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        entries.add("%s | %-21s | %s".formatted(ts, agent, detail));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }
}
