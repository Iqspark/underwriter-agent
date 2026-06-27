package com.iqspark.underwriter.rag.corpus;

import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Synthetic precedent underwriter notes — short natural-language rationales that RAG can retrieve as
 * "how we handled a similar file". Real decision notes replace these in production.
 */
@Component
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class DecisionNoteLibrary {

    public List<Document> documents() {
        return List.of(
                note("NOTE-0001", "THEFT", "Winnipeg",
                        "Vacant frame home, no security, 14 months vacant — referred for a security "
                                + "warranty and shorter inspection interval before binding; theft-prone area."),
                note("NOTE-0002", "WATER", "Sudbury",
                        "Utilities on, water not shut off, roof 24 years — required water shut-off and "
                                + "drained system; escape-of-water risk on an older roof."),
                note("NOTE-0003", "NONE", "Toronto",
                        "Well-maintained vacant home, 24-hour inspection, security and monitored alarm — "
                                + "approved on standard terms."),
                note("NOTE-0004", "FIRE", "Flin Flon",
                        "Remote property 25 km from fire hall, multiple prior losses — declined pending "
                                + "fire-response confirmation and loss-history review."),
                note("NOTE-0005", "VANDALISM", "Saskatoon",
                        "Long vacancy with demolition planned — referred; insurable interest and intent "
                                + "require special handling."));
    }

    private static Document note(String sourceId, String peril, String city, String text) {
        return new Document(text, Map.of(
                "type", "DECISION_NOTE", "sourceId", sourceId, "peril", peril, "city", city,
                "lob", "VACANT_HOME"));
    }
}
