package com.iqspark.underwriter.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqspark.underwriter.domain.decision.DecisionOutcome;
import com.iqspark.underwriter.domain.decision.Finding;
import com.iqspark.underwriter.domain.model.Money;
import com.iqspark.underwriter.domain.model.Submission;
import com.iqspark.underwriter.history.model.LearnedAssessment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Generates the rationale with Anthropic Claude via the Messages API, using the JDK HttpClient (no
 * extra HTTP dependency). Active and {@code @Primary} only when {@code underwriter.llm.anthropic.api-key}
 * is set; otherwise the offline {@link TemplateLlmReasoner} is used. The model only writes the
 * narrative — outcome, risk score, findings and conditions are unaffected (ADR-0001, ADR-0003).
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "underwriter.llm.anthropic", name = "api-key")
public class AnthropicLlmReasoner implements LlmReasoner {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AnthropicLlmReasoner(
            @Value("${underwriter.llm.anthropic.api-key}") String apiKey,
            @Value("${underwriter.llm.anthropic.model:claude-sonnet-4-6}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String name() {
        return "anthropic-claude";
    }

    @Override
    public String summarize(Submission submission, DecisionOutcome outcome, List<Finding> findings,
                            LearnedAssessment learned, Money premium) {
        try {
            String prompt = buildPrompt(submission, outcome, findings, learned, premium);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 600,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmReasoningException("Anthropic API returned HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode text = root.path("content").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new LlmReasoningException("Anthropic API returned no text content");
            }
            return text.asText().trim();
        } catch (LlmReasoningException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmReasoningException("Anthropic rationale generation failed", e);
        }
    }

    private String buildPrompt(Submission submission, DecisionOutcome outcome, List<Finding> findings,
                               LearnedAssessment learned, Money premium) {
        StringBuilder ev = new StringBuilder();
        ev.append("You are an underwriting assistant. Write a concise, professional rationale (3-6 sentences) ")
                .append("for the recommended decision below. Lead with any condition-precedent knockout. ")
                .append("Do NOT change the decision; only explain it, grounded strictly in the evidence given.\n\n");
        ev.append("Recommended outcome: ").append(outcome).append("\n");
        ev.append("Line of business: ").append(submission.effectiveLine()).append("\n");
        if (premium != null) {
            ev.append("Indicative premium: ").append(premium.currency()).append(" ")
                    .append(String.format("%.2f", premium.amount())).append("\n");
        }
        ev.append("\nFindings:\n");
        for (Finding f : findings) {
            ev.append("- [").append(f.severity()).append("] ").append(f.code()).append(": ")
                    .append(f.message()).append("\n");
        }
        if (learned != null && !learned.coldStart()) {
            ev.append("\nComparable history: ").append(learned.comparableCount())
                    .append(" similar policies; claim probability ").append(learned.claimProbability())
                    .append(", expected loss ratio ").append(learned.expectedLossRatio())
                    .append(", dominant peril ").append(learned.dominantPeril())
                    .append(", confidence ").append(learned.confidence()).append(".\n");
        } else {
            ev.append("\nComparable history: cold start (insufficient comparables); guardrails decided.\n");
        }
        return ev.toString();
    }
}
