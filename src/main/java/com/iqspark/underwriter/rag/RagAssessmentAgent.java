package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.agent.UnderwritingAgent;
import com.iqspark.underwriter.agent.UnderwritingContext;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.decision.Severity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG advisory agent (order 26 — after pattern-learning, before compliance). Retrieves grounding
 * sources, optionally asks the LLM for an advisory risk lean grounded in them, and adds an
 * <b>advisory</b> finding (category {@code RAG}, severity capped — it can push to REFER but never a
 * KNOCKOUT) plus the retrieved sources to the context. Degrades safely: no sources or no LLM means
 * no advisory finding (or an informational one), and the deterministic pipeline is unchanged.
 */
@Component
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class RagAssessmentAgent implements UnderwritingAgent {

    private final UnderwritingRetriever retriever;
    private final ChatClient chatClient; // null when no LLM configured
    private final RagProperties properties;

    public RagAssessmentAgent(UnderwritingRetriever retriever,
                              @Nullable ChatModel chatModel,
                              RagProperties properties) {
        this.retriever = retriever;
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
        this.properties = properties;
    }

    @Override
    public int order() {
        return 26;
    }

    @Override
    public void handle(UnderwritingContext context) {
        List<RetrievedSource> sources = retriever.retrieve(context.submission());
        if (sources.isEmpty()) {
            context.audit("RagAssessmentAgent", "No grounding sources retrieved above minScore.");
            return;
        }
        context.setRetrievedSources(sources);
        String ids = sources.stream().map(RetrievedSource::sourceId).distinct()
                .collect(Collectors.joining(", "));

        if (chatClient == null) {
            context.addFinding(new Finding(
                    "RAG_GROUNDING", Severity.INFO, "RAG",
                    "Grounded %d relevant source(s): %s".formatted(sources.size(), ids),
                    "Retrieved policy wordings/guidelines/precedent relevant to this submission.",
                    "RagAssessmentAgent"));
            context.audit("RagAssessmentAgent", "Grounded " + sources.size() + " sources (offline, no LLM).");
            return;
        }

        try {
            String opinion = chatClient.prompt().user(buildPrompt(sources, context)).call().content();
            Severity severity = elevated(opinion) ? cappedSeverity() : Severity.INFO;
            context.addFinding(new Finding(
                    "RAG_ADVISORY", severity, "RAG",
                    truncate(opinion),
                    "Advisory opinion grounded in retrieved sources: " + ids,
                    "RagAssessmentAgent"));
            context.audit("RagAssessmentAgent",
                    "Advisory opinion from %d sources (severity %s).".formatted(sources.size(), severity));
        } catch (Exception e) {
            // Advisory only — never fail the decision; fall back to an informational grounding finding.
            context.addFinding(new Finding(
                    "RAG_GROUNDING", Severity.INFO, "RAG",
                    "Grounded %d relevant source(s): %s".formatted(sources.size(), ids),
                    "Retrieved sources relevant to this submission (LLM opinion unavailable).",
                    "RagAssessmentAgent"));
            context.audit("RagAssessmentAgent", "LLM advisory unavailable; attached grounding only.");
        }
    }

    private String buildPrompt(List<RetrievedSource> sources, UnderwritingContext context) {
        String context_ = sources.stream()
                .map(s -> "[" + s.sourceId() + "] " + s.snippet())
                .collect(Collectors.joining("\n"));
        return """
                You are an underwriting assistant. Using ONLY the retrieved context below, give a one-line \
                risk lean (lower / elevated / higher) and the specific conditions or wordings that apply, \
                citing source ids in [brackets]. Do not invent facts; if the context is insufficient, say so.

                Retrieved context:
                %s

                Submission line of business: %s
                """.formatted(context_, context.submission().effectiveLine());
    }

    private boolean elevated(String opinion) {
        if (opinion == null) {
            return false;
        }
        String o = opinion.toLowerCase();
        return o.contains("elevated") || o.contains("higher") || o.contains("refer");
    }

    /** The configured cap, never KNOCKOUT (RAG is advisory). */
    private Severity cappedSeverity() {
        try {
            Severity s = Severity.valueOf(properties.getMaxSeverity().trim().toUpperCase());
            return s == Severity.KNOCKOUT ? Severity.HIGH : s;
        } catch (IllegalArgumentException e) {
            return Severity.MEDIUM;
        }
    }

    private static String truncate(String text) {
        if (text == null) {
            return "RAG advisory unavailable.";
        }
        String t = text.strip();
        return t.length() <= 400 ? t : t.substring(0, 400) + "…";
    }
}
