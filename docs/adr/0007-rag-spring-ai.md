# ADR-0007: RAG via Spring AI (advisory, offline-first)

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0001](0001-rules-decide-llm-explains.md), [ADR-0003](0003-pluggable-llm-offline-default.md), [ADR-0006](0006-case-based-learning.md), [doc 6](../06-rag-design.md)

## Context

The agent learns from structured history via numeric k-NN (ADR-0006), but it cannot reason over
**unstructured knowledge** — policy wordings, underwriting guidelines, precedent notes — nor
retrieve **semantic** precedent. We want a Retrieval-Augmented Generation (RAG) layer that
grounds the agent's reasoning in those sources, while preserving the determinism, auditability
and human-in-the-loop guarantees established earlier.

Two libraries were considered: **Spring AI** and **LangChain4j**. The app is Spring Boot, and we
value offline-by-default operation.

## Decision

Adopt **Spring AI** for RAG, in an **advisory** configuration:

1. **Spring AI** — native auto-configuration and abstractions (`ChatClient`, `EmbeddingModel`,
   `VectorStore`, RAG advisors, ETL splitters) fit the existing Spring Boot app with the least
   boilerplate.
2. **Advisory, not deciding** — RAG produces a capped-severity advisory finding and a
   source-cited rationale. It can move an outcome to `REFER` but never to a `KNOCKOUT`; the
   deterministic rules guardrails and the numeric k-NN retain decision authority. This keeps
   ADR-0001 intact (LLM does not decide) while letting RAG genuinely contribute.
3. **Offline embeddings + pgvector store** — in-process **ONNX (all-MiniLM)** embeddings (no API
   key, private) with a **`pgvector`** (Postgres) vector store for persistence and scale — one
   Postgres serves both vectors and relational data. This couples RAG to a running Postgres (shared
   with the Phase 1 persistence work); `SimpleVectorStore` stays as a dev/test fallback profile.
   Claude (via the Anthropic starter) is used for generation only when a key is set.
4. **Complementary to k-NN** — RAG handles unstructured/semantic content; the structured
   `SimilarityEngine` keeps the numeric claim-probability/pricing signal. We do not replace one
   with the other.

The layer is gated by `underwriter.rag.enabled`; when off, the system behaves exactly as the
pre-RAG pipeline.

## Consequences

**Positive**
- Grounds reasoning in the actual wordings/guidelines and semantic precedent; citeable.
- Reuses two existing seams (the `LlmReasoner` interface and the agent pipeline) — small blast radius.
- Key-free embeddings (offline ONNX); persistent, scalable vectors via pgvector; degrades safely if RAG/LLM is unavailable.
- Compliance guarantees unchanged (guardrails veto; RAG can't knockout).

**Negative / trade-offs**
- Adds Spring AI dependencies and an ONNX model fetched on first run (network once, then cached).
- Spring AI's API surface shifted across its 1.0/2.0 line — wiring must be verified by compiling
  locally (the current sandbox can't run Maven).
- **pgvector couples RAG to a running Postgres** (no longer "no external services"); mitigated by
  sharing the Phase 1 Postgres and keeping a `SimpleVectorStore` dev/test fallback profile.
- Retrieval quality on a synthetic corpus is only indicative until real documents are ingested.

## Alternatives considered

- **LangChain4j** — more feature-rich for agentic/tool-calling RAG and also supports in-process
  embeddings; rejected as the default only because Spring AI is more idiomatic here. Revisit if
  we move to a tool-calling agent.
- **Cloud embeddings** — higher-quality vectors but require a key and send text out; rejected for
  the default to preserve offline operation. Easy to switch via config.
- **In-memory `SimpleVectorStore` as the primary store** — zero-setup and fully offline, but loses
  its index on restart and won't scale; rejected as the production choice (kept only as a dev/test
  fallback). **pgvector is the chosen store** — it persists, scales, and reuses the Phase 1 Postgres.
- **Full RAG agent (LLM decides)** — rejected: weakens determinism/auditability and conflicts
  with ADR-0001.
