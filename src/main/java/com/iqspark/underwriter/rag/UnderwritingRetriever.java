package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.domain.model.Submission;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a natural-language query from a {@link Submission} and runs a similarity search over the
 * vector store, returning ranked {@link RetrievedSource}s (with score + sourceId) above the
 * configured minimum score.
 */
@Service
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class UnderwritingRetriever {

    private final VectorStore vectorStore;
    private final RagProperties properties;

    public UnderwritingRetriever(VectorStore vectorStore, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<RetrievedSource> retrieve(Submission submission) {
        SearchRequest request = SearchRequest.builder()
                .query(buildQuery(submission))
                .topK(Math.max(properties.getTopK() * 3, 6))
                .similarityThreshold(properties.getMinScore())
                .build();

        List<Document> hits = vectorStore.similaritySearch(request);
        if (hits == null) {
            return List.of();
        }
        List<RetrievedSource> sources = new ArrayList<>();
        for (Document d : hits) {
            Object sourceId = d.getMetadata().get("sourceId");
            Object type = d.getMetadata().get("type");
            double score = d.getScore() != null ? d.getScore() : 0.0;
            sources.add(new RetrievedSource(
                    sourceId == null ? "?" : sourceId.toString(),
                    type == null ? "?" : type.toString(),
                    score,
                    snippet(d.getText())));
        }
        return sources;
    }

    /** Compose the retrieval query from the submission's salient risk features. */
    String buildQuery(Submission s) {
        StringBuilder q = new StringBuilder();
        q.append(s.effectiveLine()).append(" submission");
        if (s.location() != null) {
            q.append(" in ").append(nz(s.location().city())).append(", ").append(nz(s.location().province()));
        }
        if (s.building() != null) {
            q.append("; ").append(nz(s.building().construction())).append(" ")
                    .append(nz(s.building().occupancyType()));
            if (s.building().roofAgeYears() != null) {
                q.append(", roof age ").append(s.building().roofAgeYears());
            }
        }
        if (s.vacancy() != null) {
            q.append("; vacant ").append(monthsVacant(s)).append(" months");
            if (s.vacancy().inspectionIntervalHours() != null) {
                q.append(", inspection every ").append(s.vacancy().inspectionIntervalHours()).append(" hours");
            }
            if (Boolean.FALSE.equals(s.vacancy().securitySystem())) {
                q.append(", no security system");
            }
        }
        if (s.applicant() != null && s.applicant().priorLossCount() != null
                && s.applicant().priorLossCount() > 0) {
            q.append("; ").append(s.applicant().priorLossCount()).append(" prior losses");
        }
        if (s.requestedCoverage() != null) {
            q.append("; coverage ").append((long) s.requestedCoverage().amount());
        }
        q.append(". Relevant unoccupancy conditions, theft/water precedent, and comparable outcomes.");
        return q.toString();
    }

    private static long monthsVacant(Submission s) {
        if (s.vacancy() == null || s.vacancy().vacantSince() == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.MONTHS.between(s.vacancy().vacantSince(), LocalDate.now()));
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 160 ? text : text.substring(0, 160) + "…";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
