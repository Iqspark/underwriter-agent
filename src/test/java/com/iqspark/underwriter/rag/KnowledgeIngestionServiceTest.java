package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.history.HistoricalPolicyRepository;
import com.iqspark.underwriter.rag.corpus.DecisionNoteLibrary;
import com.iqspark.underwriter.rag.corpus.GuidelineLibrary;
import com.iqspark.underwriter.rag.corpus.PolicyCardWriter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KnowledgeIngestionServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void ingestsAllCorporaIntoTheVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        HistoricalPolicyRepository repo = new HistoricalPolicyRepository(500, 42);
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                vectorStore, new GuidelineLibrary(), new PolicyCardWriter(repo), new DecisionNoteLibrary());

        service.ingest();

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
    }
}
