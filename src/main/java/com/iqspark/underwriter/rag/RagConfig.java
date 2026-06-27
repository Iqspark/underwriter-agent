package com.iqspark.underwriter.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the RAG infrastructure, but <b>only when {@code underwriter.rag.enabled=true}</b> — so the
 * default build/test never loads the ONNX model or a vector store and stays offline.
 *
 * <ul>
 *   <li>Embeddings: in-process ONNX (all-MiniLM via {@link TransformersEmbeddingModel}); downloaded
 *       and cached on first use. Backs off if another {@code EmbeddingModel} bean is present.</li>
 *   <li>Vector store: in-memory {@link SimpleVectorStore} by default; the {@code pgvector} Maven
 *       profile supplies a Postgres-backed {@code VectorStore} that wins via {@code @ConditionalOnMissingBean}.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "underwriter.rag.enabled", havingValue = "true")
public class RagConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        // Spring calls afterPropertiesSet() (InitializingBean) to load the ONNX model.
        return new TransformersEmbeddingModel();
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
