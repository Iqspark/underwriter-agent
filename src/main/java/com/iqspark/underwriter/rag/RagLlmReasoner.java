package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import com.iqspark.underwriter.llm.LlmReasoner;
import com.iqspark.underwriter.llm.LlmReasoningException;
import com.iqspark.underwriter.llm.TemplateLlmReasoner;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a rationale grounded in the retrieved sources, with {@code [sourceId]} citations. Primary
 * reasoner when RAG is enabled. With no LLM configured it delegates to the offline
 * {@link TemplateLlmReasoner} (which still appends the grounded source ids), so rationales are cited
 * even offline. On an LLM failure it throws {@link LlmReasoningException} so the orchestrator falls
 * back to the template reasoner. The LLM only explains — it never decides (ADR-0001).
 */
@Component
@Primary
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class RagLlmReasoner implements LlmReasoner {

    private final ChatClient chatClient; // null when no LLM configured
    private final TemplateLlmReasoner template;

    public RagLlmReasoner(@Nullable ChatModel chatModel, TemplateLlmReasoner template) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
        this.template = template;
    }

    @Override
    public String name() {
        return "rag-grounded";
    }

    @Override
    public String summarize(Submission submission, DecisionOutcome outcome, List<Finding> findings,
                            LearnedAssessment learned, Money premium,
                            List<RetrievedSource> retrievedSources) {
        if (chatClient == null || retrievedSources == null || retrievedSources.isEmpty()) {
            // Offline / no grounding: the template reasoner already appends the grounded source ids.
            return template.summarize(submission, outcome, findings, learned, premium, retrievedSources);
        }
        try {
            return chatClient.prompt().user(buildPrompt(outcome, findings, retrievedSources)).call().content();
        } catch (Exception e) {
            throw new LlmReasoningException("RAG rationale generation failed", e);
        }
    }

    private String buildPrompt(DecisionOutcome outcome, List<Finding> findings,
                               List<RetrievedSource> sources) {
        String ctx = sources.stream()
                .map(s -> "[" + s.sourceId() + "] " + s.snippet())
                .collect(Collectors.joining("\n"));
        String findingText = findings.stream()
                .map(f -> "- [" + f.severity() + "] " + f.code() + ": " + f.message())
                .collect(Collectors.joining("\n"));
        return """
                Write a concise underwriting rationale (3-6 sentences) for the recommended decision below. \
                Ground every claim in the retrieved context and cite source ids in [brackets]. Lead with any \
                condition-precedent knockout. Do NOT change the decision; only explain it.

                Recommended outcome: %s

                Findings:
                %s

                Retrieved context:
                %s
                """.formatted(outcome, findingText, ctx);
    }
}
