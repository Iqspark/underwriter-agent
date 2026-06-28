package com.iqspark.underwriter.review;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.ReviewFlag;
import com.iqspark.underwriter.domain.decision.Severity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Reviewer agent — an LLM "skeptical underwriter" (ADR-0022) that checks the assembled decision
 * for internal coherence and raises advisory {@link ReviewFlag}s. It <b>never</b> changes the
 * outcome; the deterministic guardrails and authority remain authoritative (ADR-0001).
 *
 * <p>A lightweight deterministic check runs even offline (e.g. the rationale must reflect every
 * knockout finding). When an LLM is configured, an additional critic pass checks for groundedness
 * and contradictions. Gated by {@code underwriter.reviewer.enabled} (default on).
 */
@Component
@ConditionalOnProperty(name = "underwriter.reviewer.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewerAgent {

    private final ChatClient chatClient; // null when no LLM configured

    public ReviewerAgent(@Nullable ChatModel chatModel) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
    }

    public ReviewResult review(DecisionOutcome outcome, List<Finding> findings, String rationale) {
        List<ReviewFlag> flags = new ArrayList<>();
        boolean rationaleDefect = false;
        String r = rationale == null ? "" : rationale.toLowerCase();

        // 1) Rationale must reflect every knockout finding (the primary check).
        boolean mentionsKnockout = r.contains("knockout")
                || r.contains("condition precedent") || r.contains("condition-precedent");
        for (Finding ko : findings) {
            if (ko.isKnockout() && !mentionsKnockout) {
                flags.add(new ReviewFlag("RATIONALE_OMITS_KNOCKOUT", "HIGH",
                        "Rationale does not reflect knockout finding " + ko.code(), ko.code()));
                rationaleDefect = true;
            }
        }

        // 2) Outcome/rationale coherence.
        if (outcome == DecisionOutcome.DECLINE && !r.contains("declin")) {
            flags.add(new ReviewFlag("OUTCOME_RATIONALE_MISMATCH", "MEDIUM",
                    "Outcome is DECLINE but the rationale does not state a decline", "outcome"));
            rationaleDefect = true;
        }
        if (outcome == DecisionOutcome.APPROVE
                && findings.stream().anyMatch(f -> f.severity() == Severity.HIGH || f.isKnockout())) {
            flags.add(new ReviewFlag("OUTCOME_COHERENCE", "MEDIUM",
                    "Outcome APPROVE despite HIGH/knockout findings", "outcome"));
        }

        // 3) LLM critic pass (groundedness + subtle contradictions), advisory only.
        if (chatClient != null) {
            flags.addAll(llmCritic(outcome, findings, rationale));
        }

        return new ReviewResult(List.copyOf(flags), rationaleDefect);
    }

    private List<ReviewFlag> llmCritic(DecisionOutcome outcome, List<Finding> findings, String rationale) {
        List<ReviewFlag> flags = new ArrayList<>();
        try {
            String prompt = """
                    You are a senior underwriter reviewing a recommended decision for internal consistency. \
                    Reply with exactly 'OK' if the rationale fully and accurately reflects the findings and \
                    outcome (especially any condition-precedent knockout). Otherwise list each problem on its \
                    own line starting with 'ISSUE:'. Do not restate the decision.

                    Outcome: %s
                    Findings:
                    %s
                    Rationale:
                    %s
                    """.formatted(outcome, findingLines(findings), rationale);
            String response = chatClient.prompt().user(prompt).call().content();
            if (response != null && !response.strip().equalsIgnoreCase("OK")) {
                for (String line : response.split("\\r?\\n")) {
                    String l = line.strip();
                    if (l.regionMatches(true, 0, "ISSUE:", 0, 6)) {
                        flags.add(new ReviewFlag("REVIEW_ISSUE", "MEDIUM", l.substring(6).strip(), "rationale"));
                    }
                }
            }
        } catch (Exception e) {
            // Advisory only — a reviewer failure never affects the decision.
        }
        return flags;
    }

    private static String findingLines(List<Finding> findings) {
        return findings.stream()
                .map(f -> "- [" + f.severity() + "] " + f.code() + ": " + f.message())
                .collect(Collectors.joining("\n"));
    }
}
