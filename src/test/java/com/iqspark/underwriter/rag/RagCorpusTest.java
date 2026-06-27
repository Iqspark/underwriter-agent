package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.rag.corpus.DecisionNoteLibrary;
import com.iqspark.underwriter.rag.corpus.GuidelineLibrary;
import com.iqspark.underwriter.rag.corpus.PolicyCardWriter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagCorpusTest {

    @Test
    void guidelinesHaveTypeAndSourceId() {
        List<Document> docs = new GuidelineLibrary().documents();
        assertThat(docs).isNotEmpty();
        assertThat(docs).allSatisfy(d -> {
            assertThat(d.getMetadata()).containsKeys("type", "sourceId");
            assertThat(d.getText()).isNotBlank();
        });
    }

    @Test
    void decisionNotesArePresent() {
        assertThat(new DecisionNoteLibrary().documents()).isNotEmpty();
    }

    @Test
    void policyCardsAreCappedAndCarryMetadata() {
        PolicyCardWriter writer = new PolicyCardWriter(new HistoricalPolicyRepository(1000, 42));
        List<Document> cards = writer.documents();
        assertThat(cards).isNotEmpty().hasSizeLessThanOrEqualTo(300);
        assertThat(cards.get(0).getMetadata()).containsKeys("type", "sourceId", "peril");
    }
}
