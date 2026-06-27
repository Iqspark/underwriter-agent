package com.iqspark.underwriter.rag.corpus;

import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.history.model.HistoricalPolicy;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializes historical policies into narrative "cards" for semantic retrieval (so RAG can surface
 * comparable precedent in natural language, complementing the numeric k-NN). Capped to a sample for
 * ingestion speed in this slice.
 */
@Component
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class PolicyCardWriter {

    private static final int MAX_CARDS = 300;

    private final HistoricalPolicyRepository repository;

    public PolicyCardWriter(HistoricalPolicyRepository repository) {
        this.repository = repository;
    }

    public List<Document> documents() {
        List<HistoricalPolicy> all = repository.all();
        List<Document> cards = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_CARDS, all.size()); i++) {
            HistoricalPolicy p = all.get(i);
            cards.add(toCard(p));
        }
        return cards;
    }

    private Document toCard(HistoricalPolicy p) {
        String text = ("%s policy %s in %s, %s: %s construction, %d sq ft, built %d, roof %d years, "
                + "vacant %d months, inspection every %d h, %s, fire hall %d km. Outcome: %s%s.")
                .formatted(
                        p.line(), p.id(), p.city(), p.province(), p.construction(),
                        p.squareFeet(), p.yearBuilt(), p.roofAgeYears(), p.vacancyMonths(),
                        p.inspectionIntervalHours(),
                        p.securitySystem() ? "security system present" : "no security system",
                        p.distanceToFireHallKm(),
                        p.hadClaim() ? "claim" : "no claim",
                        p.hadClaim() ? " (" + p.dominantPeril() + ", loss ratio "
                                + String.format("%.2f", p.lossRatio()) + ")" : "");
        Map<String, Object> meta = Map.of(
                "type", "POLICY_CARD",
                "sourceId", p.id(),
                "peril", p.dominantPeril().name(),
                "city", p.city() == null ? "" : p.city(),
                "lob", p.line().name());
        return new Document(text, meta);
    }
}
