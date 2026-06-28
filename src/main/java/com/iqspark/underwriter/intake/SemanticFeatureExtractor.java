package com.iqspark.underwriter.intake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts a bounded set of {@link SemanticFeatures} from unstructured text (ADR-0021). Uses the LLM
 * when a chat model is configured (strict JSON schema, "don't invent" prompt); otherwise a
 * deterministic keyword heuristic — so it always runs offline and tests are stable. Output is a
 * fixed schema; the model never returns free text into the decision.
 */
@Service
public class SemanticFeatureExtractor {

    private final ChatClient chatClient; // null when no LLM configured
    private final ObjectMapper mapper = new ObjectMapper();

    public SemanticFeatureExtractor(@Nullable ChatModel chatModel) {
        this.chatClient = chatModel != null ? ChatClient.create(chatModel) : null;
    }

    public SemanticFeatures extract(String text) {
        if (text == null || text.isBlank()) {
            return SemanticFeatures.empty();
        }
        if (chatClient != null) {
            try {
                return viaLlm(text);
            } catch (Exception e) {
                // fall through to heuristic — extraction never fails the pipeline
            }
        }
        return viaHeuristic(text);
    }

    private SemanticFeatures viaLlm(String text) throws Exception {
        String prompt = """
                Extract ONLY these fields from the text as strict JSON (no prose). Do not invent facts.
                {"deferredMaintenancePresent":boolean,"recentRenovation":boolean,
                 "hazards":[string],"inspectorSentiment":"NEGATIVE|NEUTRAL|POSITIVE",
                 "priorLossNarrativeSeverity":"NONE|LOW|MEDIUM|HIGH"}

                Text:
                %s
                """.formatted(text);
        String json = chatClient.prompt().user(prompt).call().content();
        JsonNode n = mapper.readTree(json);
        List<String> hazards = new ArrayList<>();
        if (n.has("hazards") && n.get("hazards").isArray()) {
            n.get("hazards").forEach(h -> hazards.add(h.asText()));
        }
        return new SemanticFeatures(
                true,
                n.path("deferredMaintenancePresent").asBoolean(false),
                n.path("recentRenovation").asBoolean(false),
                List.copyOf(hazards),
                upper(n.path("inspectorSentiment").asText("NEUTRAL"), "NEUTRAL"),
                upper(n.path("priorLossNarrativeSeverity").asText("NONE"), "NONE"),
                0.85, "llm");
    }

    private SemanticFeatures viaHeuristic(String text) {
        String t = text.toLowerCase();
        boolean deferred = containsAny(t, "deferred maintenance", "rot", "disrepair", "neglect",
                "poor condition", "not maintained");
        boolean renovation = containsAny(t, "renovation", "renovated", "remodel", "under construction");

        List<String> hazards = new ArrayList<>();
        if (t.contains("mold")) hazards.add("mold");
        if (t.contains("asbestos")) hazards.add("asbestos");
        if (t.contains("oil tank")) hazards.add("oil tank");
        if (t.contains("knob and tube") || t.contains("old wiring")) hazards.add("wiring");

        String sentiment = containsAny(t, "poor", "concern", "deteriorat", "unsafe", "neglect", "bad")
                ? "NEGATIVE"
                : (containsAny(t, "well maintained", "excellent", "good condition") ? "POSITIVE" : "NEUTRAL");

        String lossSeverity;
        if (containsAny(t, "total loss", "fire damage", "flood damage")) {
            lossSeverity = "HIGH";
        } else if (containsAny(t, "claim", "prior loss", "water damage")) {
            lossSeverity = "MEDIUM";
        } else {
            lossSeverity = "NONE";
        }

        return new SemanticFeatures(true, deferred, renovation, List.copyOf(hazards),
                sentiment, lossSeverity, 0.6, "heuristic");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static String upper(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }
}
