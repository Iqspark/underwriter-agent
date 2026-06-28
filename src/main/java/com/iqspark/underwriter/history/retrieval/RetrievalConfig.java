package com.iqspark.underwriter.history.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the candidate-retrieval strategy via {@code underwriter.similarity.index}
 * (bruteforce | ann). Default is bruteforce (exact). In production, pgvector serves the ANN seam.
 */
@Configuration
public class RetrievalConfig {

    private static final Logger log = LoggerFactory.getLogger(RetrievalConfig.class);

    @Bean
    public CandidateRetriever candidateRetriever(
            @Value("${underwriter.similarity.index:bruteforce}") String index) {
        CandidateRetriever retriever = "ann".equalsIgnoreCase(index)
                ? new AnnRetriever() : new BruteForceRetriever();
        log.info("Similarity candidate retriever: {}", retriever.name());
        return retriever;
    }
}
