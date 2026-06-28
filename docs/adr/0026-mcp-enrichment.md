# ADR-0026: MCP enrichment (tool boundary, offline-first, degrade-to-floor)

**Status:** Accepted (offline baseline built)
**Date:** 2026-06-27
**Related:** [ADR-0017](0017-data-integration.md), [ADR-0010](0010-event-driven-runtime.md), [ADR-0011](0011-security-privacy.md), [doc 7 §4.3](../07-target-architecture.md), [doc 17](../17-data-integration.md)

## Context

The agent reasons over what the submission and the book contain, but real risk needs **external
data** — geocoding, flood/wildfire/wind and **crime/theft** peril scores, property attributes. Doc 7
specifies these arrive through **MCP tool servers** behind a scoped, validated, cached boundary (an
anti-corruption layer), and doc 12/17 require that missing or unavailable enrichment **degrades to
book-only signals** rather than failing a submission.

## Decision

Add an enrichment layer behind an **`EnrichmentProvider`** seam (the tool boundary):

1. **Boundary + anti-corruption shape.** `EnrichmentProvider.enrich(Submission)` returns a small,
   line-agnostic `Enrichment` record (crime/flood/wildfire/wind scores in 0..1, plus `available` +
   `source`). External schemas never leak into the core.
2. **Offline-first default.** `OfflineEnrichmentProvider` derives deterministic scores from the risk
   location so the system runs and is testable with no external services. A real
   `McpEnrichmentProvider` (MCP tool servers) plugs in behind the same interface — same pattern as the
   RAG ONNX/pgvector seams.
3. **Cache + degrade-to-floor.** `EnrichmentService` caches by location (bounds cost/latency, doc 14)
   and converts any provider failure into `Enrichment.unavailable()` — the pipeline proceeds on
   book-only signals; enrichment never fails a submission (doc 12).
4. **Advisory findings.** `EnrichmentAgent` (order 15) turns elevated scores into capped-severity
   findings (MEDIUM/HIGH, category `ENRICHMENT`) — never a knockout; deterministic guardrails keep
   authority (ADR-0001). Gated by `underwriter.enrichment.enabled`.

## Consequences

**Positive**
- Brings external peril/crime signals into the decision via a clean, swappable boundary.
- Fully offline-testable; production swaps in MCP tool servers with no core change.
- Resilient: cached and fail-safe — a provider outage degrades, it doesn't break.

**Negative / trade-offs**
- The offline provider's scores are **illustrative**, not real hazard data — replace before production.
- This slice surfaces enrichment as **findings**; feeding enrichment into pricing/learning features is
  a follow-up (it would deepen accuracy but widen blast radius).
- Real MCP tools add auth/quota/latency concerns handled at the provider + cache + (later) the async
  runtime; binding/financial tools stay human-gated (doc 11).

## Alternatives considered

- **Call external APIs directly from agents** — rejected: leaks external schemas, no anti-corruption
  layer, harder to cache/scope/test.
- **Make enrichment mandatory (fail if unavailable)** — rejected: violates degrade-to-floor; missing
  data should refer or proceed on book-only, not error.
- **Feed enrichment straight into pricing/learning now** — deferred: valuable but larger; start with
  advisory findings behind the boundary.
