# ADR-0023: k-NN scalability — approximate-nearest-neighbour index over pgvector

**Status:** Proposed
**Date:** 2026-06-26
**Related:** [ADR-0006](0006-case-based-learning.md), [ADR-0007](0007-rag-spring-ai.md), [ADR-0019](0019-phase1-persistence-metrics.md), [doc 2 §12](../02-architecture-design.md), [doc 5 §7](../05-ai-learning-design.md)

## Context

The `SimilarityEngine` computes a weighted Gower distance from each new submission to **every**
policy in the book, then keeps the top `k` ([ADR-0006](0006-case-based-learning.md)). This brute-force
scan is **O(n) per request**. It is fine at the current synthetic book size (~1,500) but does not
scale: a real book of hundreds of thousands to millions of policies makes every decision slow and
CPU-bound, and it does not parallelize or cache well across requests. [Doc 2 §12](../02-architecture-design.md)
already flags "indexing at scale" as a near-term step; this ADR commits the approach.

## Decision

Replace the per-request linear scan with an **approximate-nearest-neighbour (ANN) index**, hosted in
the **pgvector** store already being stood up in Phase 1 / used by RAG ([ADR-0019](0019-phase1-persistence-metrics.md),
[ADR-0007](0007-rag-spring-ai.md)) — no new infrastructure.

1. **Feature vector per policy.** Each `HistoricalPolicy` is encoded once into a fixed-length numeric
   vector: normalized numeric features plus an encoding of the weighted categoricals. Weights from
   the Gower model are **baked into the vector** (scale each component by √weight) so ordinary
   distance over the vector approximates the current weighted distance.
2. **ANN index in pgvector.** Store vectors in pgvector with an **HNSW** index; retrieve the top-`k`
   candidates with an approximate search in ~O(log n).
3. **Exact re-rank.** Re-rank the small ANN candidate set with the **exact** weighted-Gower distance,
   so the comparables shown and the aggregation are computed on the true metric — accuracy preserved,
   only the *candidate generation* is approximate.
4. **Incremental updates.** New/updated policies upsert their vector into the index (data flywheel),
   instead of forcing a full rescan.
5. **Pluggable + fallback.** Retrieval sits behind the existing `SimilarityEngine` seam:
   `BruteForceRetriever` (today, exact — default for small books / dev / CI) and `AnnRetriever`
   (pgvector, for scale), selected by `underwriter.similarity.index = bruteforce | ann`. The
   brute-force path remains the offline/cold-start floor and the correctness oracle in tests.
6. **Cold-start unchanged.** Thin-book cold-start behaviour is independent of the index ([doc 5 §5](../05-ai-learning-design.md)).

## Consequences

**Positive**
- Decision latency becomes ~logarithmic in book size → real million-policy books stay fast.
- Reuses the Phase 1 Postgres/pgvector — no new datastore, shared ops/backup/DR.
- Exact re-rank keeps the comparables and learned numbers on the true metric; explainability intact.
- Incremental upserts fit the data flywheel; no nightly full rebuilds required.

**Negative / trade-offs**
- ANN is **approximate**: a true nearest neighbour can occasionally be missed at the candidate stage.
  Mitigated by a generous candidate multiple (retrieve `m·k`, re-rank to `k`) and tuned HNSW params;
  recall is monitored against the brute-force oracle.
- Adds an encoding/index-maintenance step and couples retrieval to pgvector when in `ann` mode.
- Vector encoding must be **kept in sync** with the Gower feature/weight definitions — a versioning
  concern (governed alongside the model, [doc 13](../13-ai-governance-model-risk.md)).

## Alternatives considered

- **Keep brute-force, scale hardware** — rejected: O(n) per request does not scale economically to
  millions of policies regardless of hardware.
- **In-process ANN library (FAISS/HNSWlib)** — viable but adds a separate index to persist, warm,
  and replicate; pgvector reuses infrastructure already present and shares durability/DR.
- **Precomputed/cached neighbours** — rejected as primary: the query is the new submission, not a
  known policy, so neighbours cannot be fully precomputed; caching helps only repeat queries.
