package com.iqspark.underwriter.drafting;

/** Draft artifacts prepared for one-click underwriter review (doc 7 §2 "Drafting"). */
public record Drafts(
        String quoteSummary,
        String conditionsSummary,
        String brokerEmail,
        String underwriterMemo
) {}
