# ADR-0028: Phase 5 — drafting (and multimodal intake scope)

**Status:** Accepted (drafting built; multimodal intake deferred)
**Date:** 2026-06-27
**Related:** [ADR-0021](0021-semantic-feature-extraction.md), [ADR-0003](0003-pluggable-llm-offline-default.md), [doc 7 §2](../07-target-architecture.md), [doc 8 §5](../08-recommended-solution.md)

## Context

Phase 5 (doc 8 §5) is "multimodal intake + drafting." Its semantic-feature-extraction part is
ADR-0021. This ADR covers the remaining two parts: **drafting** the underwriter's deliverables, and
the **scope decision** on multimodal intake.

## Decision

1. **Drafting (built).** A `DraftingService` produces, for a stored decision, a quote summary,
   conditions summary, broker email and underwriter memo. Deterministic templates by default; when a
   chat model is configured it polishes the broker email and memo (failure falls back to the
   template). Exposed read-only at `GET /api/underwriting/decisions/{reference}/drafts` (authorized by
   the existing `/decisions/**` GET rule). Drafts are advisory text — they never change the decision
   (ADR-0001/ADR-0003).
2. **Multimodal intake (deferred).** Structuring PDFs/emails/images into a `Submission` plugs in
   behind the existing `DocumentExtractor` seam (an `LlmDocumentExtractor` with Tika for PDF text and
   a vision model for images). It is **deferred** for now: the text `/documents` path plus the new
   free-text `notes` field already bring unstructured content into the pipeline (feeding ADR-0021),
   and image/vision needs a model we don't run offline. No core change is required to add it later.

## Consequences

**Positive**
- Underwriters get one-click draft deliverables; offline-safe via templates, richer with an LLM.
- Built behind existing seams (`ChatModel`, `DecisionStore`, `DocumentExtractor`) — no core rewrite.

**Negative / trade-offs**
- Drafts are generated on demand from the stored decision (not persisted/versioned yet).
- Multimodal/vision intake remains designed-not-built; the `DocumentExtractor` seam keeps it a
  drop-in later.

## Alternatives considered

- **Attach drafts to the `Decision` record** — rejected for now: would change the decision schema and
  the orchestrator; on-demand drafting from the stored decision keeps blast radius small.
- **Build LLM/vision multimodal intake now** — deferred: lowest incremental value this phase and not
  offline-testable without a vision model.
