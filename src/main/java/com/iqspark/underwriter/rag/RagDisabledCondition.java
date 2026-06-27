package com.iqspark.underwriter.rag;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when RAG is <em>not</em> enabled. Used to keep exactly one {@code @Primary}
 * {@link com.iqspark.underwriter.llm.LlmReasoner}: the {@code AnthropicLlmReasoner} is primary only
 * when RAG is off; when RAG is on, the {@code RagLlmReasoner} (cited rationale) takes over.
 */
public class RagDisabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("underwriter.rag.enabled");
        return !"true".equalsIgnoreCase(enabled);
    }
}
