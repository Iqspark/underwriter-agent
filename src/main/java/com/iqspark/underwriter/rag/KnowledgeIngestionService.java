package com.iqspark.underwriter.rag;

import com.iqspark.underwriter.rag.corpus.DecisionNoteLibrary;
import com.iqspark.underwriter.rag.corpus.GuidelineLibrary;
import com.iqspark.underwriter.rag.corpus.PolicyCardWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * Startup ETL: assembles the corpora (guidelines/wordings, policy cards, decision notes), splits
 * them, and embeds them into the {@link VectorStore} (offline ONNX embeddings). Runs only when RAG
 * is enabled. A failure here is logged, not fatal — the pipeline degrades to the pre-RAG behaviour.
 */
@Service
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final VectorStore vectorStore;
    private final GuidelineLibrary guidelines;
    private final PolicyCardWriter policyCards;
    private final DecisionNoteLibrary decisionNotes;

    public KnowledgeIngestionService(VectorStore vectorStore, GuidelineLibrary guidelines,
                                     PolicyCardWriter policyCards, DecisionNoteLibrary decisionNotes) {
        this.vectorStore = vectorStore;
        this.guidelines = guidelines;
        this.policyCards = policyCards;
        this.decisionNotes = decisionNotes;
    }

    @PostConstruct
    public void ingest() {
        try {
            List<Document> docs = new ArrayList<>();
            docs.addAll(guidelines.documents());
            docs.addAll(policyCards.documents());
            docs.addAll(decisionNotes.documents());

            List<Document> chunks = new TokenTextSplitter().apply(docs);
            vectorStore.add(chunks);
            log.info("RAG ingestion complete: {} source documents -> {} chunks indexed",
                    docs.size(), chunks.size());
        } catch (Exception e) {
            log.warn("RAG ingestion failed; retrieval will return no sources: {}", e.getMessage());
        }
    }
}
