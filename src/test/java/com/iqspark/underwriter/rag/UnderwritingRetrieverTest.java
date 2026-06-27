package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.domain.decision.RetrievedSource;
import com.iqspark.underwriter.support.Submissions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnderwritingRetrieverTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final UnderwritingRetriever retriever = new UnderwritingRetriever(vectorStore, new RagProperties());

    @Test
    void mapsHitsToRetrievedSources() {
        Document doc = new Document("Unoccupancy Conditions PR0003 cl.2 — inspect every 72 hours.",
                Map.of("sourceId", "PR0003-cl2", "type", "WORDING"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<RetrievedSource> sources = retriever.retrieve(Submissions.vacantClean());

        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).sourceId()).isEqualTo("PR0003-cl2");
        assertThat(sources.get(0).type()).isEqualTo("WORDING");
        assertThat(sources.get(0).snippet()).isNotBlank();
    }

    @Test
    void buildsAQueryFromSalientFeatures() {
        String q = retriever.buildQuery(Submissions.vacantClean());
        assertThat(q).contains("Toronto").contains("vacant").contains("coverage");
    }

    @Test
    void emptyWhenNoHits() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        assertThat(retriever.retrieve(Submissions.vacantClean())).isEmpty();
    }
}
