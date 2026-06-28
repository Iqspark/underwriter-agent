package com.iqspark.underwriter.drafting;

import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.persistence.StoredDecision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Drafts the quote summary, conditions, broker email and underwriter memo for one-click review. Uses
 * the LLM to polish the broker email + memo when a chat model is configured; otherwise produces
 * deterministic templates. Drafts are advisory text — they never change the decision.
 */
@Service
public class DraftingService {

    private final ChatClient chatClient; // null when no LLM configured

    public DraftingService(@Nullable ChatModel chatModel) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
    }

    public Drafts draft(StoredDecision d) {
        String currency = d.indicativePremium() != null ? d.indicativePremium().currency() : "CAD";
        double premium = d.indicativePremium() != null ? d.indicativePremium().amount() : 0.0;

        String quote = "Quote %s (%s): recommended %s, indicative premium %s %.2f."
                .formatted(d.reference(), d.line(), d.outcome(), currency, premium);

        String conditions = d.conditions() == null || d.conditions().isEmpty()
                ? "No special conditions."
                : String.join("; ", d.conditions());

        String brokerEmail = templateBrokerEmail(d, currency, premium, conditions);
        String memo = templateMemo(d);

        if (chatClient != null) {
            brokerEmail = polish("Write a concise, professional broker email for this underwriting "
                    + "decision. Do not change the decision or invent terms.\n\n" + brokerEmail, brokerEmail);
            memo = polish("Write a concise underwriter file memo from these notes. Do not invent "
                    + "facts.\n\n" + memo, memo);
        }
        return new Drafts(quote, conditions, brokerEmail, memo);
    }

    private String polish(String prompt, String fallback) {
        try {
            String out = chatClient.prompt().user(prompt).call().content();
            return out == null || out.isBlank() ? fallback : out.trim();
        } catch (Exception e) {
            return fallback; // drafting never fails the request
        }
    }

    private static String templateBrokerEmail(StoredDecision d, String currency, double premium, String conditions) {
        String action = switch (d.outcome() == null ? "" : d.outcome()) {
            case "APPROVE" -> "we are pleased to offer terms";
            case "REFER" -> "we need a few items resolved before we can confirm terms";
            case "DECLINE" -> "we are unable to offer terms as presented";
            default -> "please see the assessment below";
        };
        return ("""
                Hello,

                Regarding submission %s (%s): %s. Indicative premium: %s %.2f.

                Conditions: %s

                Please reply with any questions.

                Regards,
                Underwriting
                """).formatted(d.reference(), d.line(), action, currency, premium, conditions);
    }

    private static String templateMemo(StoredDecision d) {
        String keyFindings = d.findings() == null ? "" : d.findings().stream()
                .filter(f -> f.severity().weight() >= 3)
                .map(f -> "- [" + f.severity() + "] " + f.code() + ": " + f.message())
                .collect(Collectors.joining("\n"));
        return ("""
                Underwriter memo — %s (%s)
                Outcome: %s | Risk score: %d
                Rationale: %s

                Key findings:
                %s
                """).formatted(d.reference(), d.line(), d.outcome(), d.riskScore(),
                d.rationale() == null ? "" : d.rationale(),
                keyFindings.isBlank() ? "- none" : keyFindings);
    }
}
