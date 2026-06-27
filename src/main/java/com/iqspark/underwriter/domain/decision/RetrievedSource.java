package com.iqspark.underwriter.domain.decision;

/**
 * A grounding source retrieved by the RAG layer and cited in the rationale, carried on the
 * {@link Decision} for transparency. {@code sourceId} is the stable citation handle (e.g.
 * {@code PR0003-cl2}, {@code HP-00231}, {@code NOTE-0042}).
 */
public record RetrievedSource(String sourceId, String type, double score, String snippet) {}
