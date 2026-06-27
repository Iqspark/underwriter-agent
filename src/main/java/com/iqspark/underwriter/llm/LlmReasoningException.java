package com.iqspark.underwriter.llm;

/** Thrown when an LLM reasoner fails; the orchestrator catches it and falls back to the template. */
public class LlmReasoningException extends RuntimeException {

    public LlmReasoningException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmReasoningException(String message) {
        super(message);
    }
}
