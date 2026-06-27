# ADR-0017: Data & integration architecture

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0006](0006-case-based-learning.md), [ADR-0010](0010-event-driven-runtime.md), [ADR-0011](0011-security-privacy.md), [doc 17](../17-data-integration.md)

## Context

The agent consumes submissions/accounts/documents/enrichment and produces decisions/bindings/audit,
and must close the loop by ingesting realized outcomes to improve. It integrates with broker
portals, CRM, a Policy Admin System, a document store, enrichment providers and an IdP — each a
moving external contract — under PII/residency constraints.

## Decision

1. **Purpose-specific stores** — Postgres (system of record), immutable audit/lineage, vector store
   (de-identified, rebuildable), LOB-partitioned historical book, event log (backbone + replay),
   and an analytics warehouse fed from events.
2. **Event-driven flywheel** — `OutcomeRecorded` (claims/loss) events refresh the book, RAG corpus
   and eval set, so the system learns from experience.
3. **Contract-based integration** — versioned REST/event/webhook contracts, consumer-driven
   contract tests, the outbox pattern, idempotent consumers, and an **anti-corruption layer** so
   external schemas don't leak into the core domain.
4. **Data quality at the edge** — intake validation + contradiction checks gate entry; bad data
   refers, never silently flows through.
5. **Governed data** — classification/retention/lineage per [ADR-0011](0011-security-privacy.md)/
   [ADR-0010](0010-event-driven-runtime.md); de-identified analytics; versioned reference/master data
   (regions, perils, LOB, per-province rules).

## Consequences

**Positive**
- Clear separation of concerns across stores; replayable history; a real improvement loop.
- External changes are absorbed by adapters/contracts without destabilizing the core.
- Multi-line and multi-jurisdiction handled via partitioning + reference data, not code forks.

**Negative / trade-offs**
- More stores and pipelines to operate — justified by audit/learning/analytics needs; start lean
  (Postgres + event log) and add the warehouse/flywheel jobs as outcomes accumulate.
- Outcome data matures slowly, so the flywheel ramps over time (works on the current book meanwhile).

## Alternatives considered

- **One database for everything.** Rejected — conflates OLTP, immutable audit, vectors and
  analytics with very different access patterns and guarantees.
- **Point-to-point integrations without contracts/ACL.** Rejected — brittle; external schema churn
  would ripple into the core.
- **Batch-only outcome ingestion.** Acceptable interim, but event-driven keeps the flywheel and
  dashboards current; batch as a fallback.
