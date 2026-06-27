# ADR-0012: Resilience & disaster recovery

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0010](0010-event-driven-runtime.md), [doc 12](../12-resilience-dr.md)

## Context

The system depends on external LLMs, enrichment APIs, a broker, a workflow engine and a datastore,
all of which fail eventually. Underwriting must keep working (or fail safe), lose no work, and meet
recovery objectives — within Canadian data-residency constraints.

## Decision

1. **Degrade to a deterministic floor.** The rules guardrails + numeric k-NN have no external
   dependency and are always available; losing LLM/RAG/enrichment reduces capability, not
   availability. A documented **degradation matrix** defines each fallback; a decision-critical
   missing input degrades to **REFER**, never a guess.
2. **SLOs/RTO/RPO.** Target 99.9% on the core path; RTO ≤ 1h (region) / ≤ 5m (AZ); RPO ≤ 5m
   (≈0 for committed audit via the event log).
3. **HA topology.** Stateless app across ≥2 AZs; Postgres primary+standby with failover + PITR;
   replicated broker/workflow; vector store replicated **and** rebuildable from source.
4. **Backups, tested.** PITR + cross-region (Canadian) copies; audit via append-only + replicated
   event log; restore drills on a schedule.
5. **In-app resilience.** Timeouts, circuit breakers, bulkheads, retries+backoff, DLQ, fallbacks
   (Resilience4j); idempotency + durable workflow guarantee resume-without-duplication.
6. **DR.** Multi-AZ active + cross-region warm standby (Canadian) with runbooks and scheduled DR /
   chaos game-days.

## Consequences

**Positive**
- Underwriting survives third-party AI and infra outages; no lost or duplicated decisions.
- Recovery objectives are explicit and testable; residency preserved in DR.
- The AI layering doubles as the resilience story (offline floor = built-in redundancy).

**Negative / trade-offs**
- Cross-region standby and drills add cost and operational effort — right-size to volume/criticality.
- Degraded modes give lower-quality (but safe) outcomes; must be surfaced via alerts/dashboard.

## Alternatives considered

- **Single-AZ, best-effort.** Rejected — unacceptable availability/RPO for regulated decisions.
- **Hard dependency on the cloud LLM.** Rejected — an external outage would halt underwriting; the
  offline floor removes that single point of failure.
- **Active-active multi-region.** Strong but costly/complex and residency-constrained; warm standby
  chosen as the right balance, revisit if volume demands.
