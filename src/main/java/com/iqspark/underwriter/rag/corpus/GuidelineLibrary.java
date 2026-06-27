package com.iqspark.underwriter.rag.corpus;

import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Synthetic-but-realistic policy wordings and underwriting guidelines for the vacant-home reference
 * line (PR0003 Unoccupancy Conditions, Supervisory Warranty 300130, CGL, endorsements, manual).
 * Real wordings replace these in production. Each {@link Document} carries {@code type}/{@code sourceId}/
 * {@code lob} metadata for filtered retrieval and citation.
 */
@Component
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class GuidelineLibrary {

    public List<Document> documents() {
        return List.of(
                wording("PR0003-cl2", "Unoccupancy Conditions PR0003 cl.2 — a vacant or unoccupied "
                        + "dwelling must be inspected at least every 72 hours by the insured or their "
                        + "representative; failure to comply is a breach of a condition precedent and a "
                        + "loss arising during non-compliance is not payable."),
                wording("SW-300130", "Supervisory Warranty 300130 — heating maintained or the water "
                        + "supply shut off and the system drained during the vacancy; utilities on with "
                        + "water not shut off materially increases escape-of-water exposure."),
                guideline("GL-VACANCY", "Vacancy guideline — risk rises with vacancy duration; beyond "
                        + "12 months vandalism, theft, water and undetected-loss exposure increase sharply. "
                        + "Require security and shorter inspection intervals for long vacancies."),
                guideline("GL-THEFT", "Theft guideline — theft and vandalism are the dominant vacant-"
                        + "property perils, especially where there is no security system or monitored alarm "
                        + "and in areas with elevated theft claim rates. Consider security requirements."),
                guideline("GL-REMOTE", "Remoteness guideline — properties beyond 100 km from a major city "
                        + "have slower emergency response and harder 72-hour inspection compliance; confirm "
                        + "fire-response arrangements and consider higher deductibles."),
                wording("CGL-LIAB", "Liability — tenant-occupied and short-term-rental risks carry "
                        + "liability exposure; a minimum CAD 1,000,000 limit is typical and short-term "
                        + "rental requires the STR endorsement to respond."));
    }

    private static Document wording(String sourceId, String text) {
        return new Document(text, Map.of("type", "WORDING", "sourceId", sourceId, "lob", "VACANT_HOME"));
    }

    private static Document guideline(String sourceId, String text) {
        return new Document(text, Map.of("type", "GUIDELINE", "sourceId", sourceId, "lob", "VACANT_HOME"));
    }
}
