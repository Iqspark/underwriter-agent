# ADR-0027: Dashboards, business metrics & the data flywheel

**Status:** Accepted (baseline built)
**Date:** 2026-06-27
**Related:** [ADR-0010](0010-event-driven-runtime.md), [ADR-0017](0017-data-integration.md), [ADR-0019](0019-phase1-persistence-metrics.md), [doc 10 §4](../10-runtime-audit-observability.md), [doc 17 §3](../17-data-integration.md)

## Context

Phases 0–4 produce decisions, tiers, learned predictions and (async) cases, but there's no way to
**measure the operation** or to **close the loop** with realized outcomes. Doc 10 §4 calls for
business/quality metrics and an embedded underwriting-performance dashboard; doc 17 §3 calls for the
**flywheel** — `OutcomeRecorded` events feeding predicted-vs-realized and (later) the book/RAG/eval
sets. These are prerequisites for safely widening autonomy.

## Decision

Build the baseline observability + flywheel slice (Grafana/Prometheus stay external; we emit the
data):

1. **Realized-outcome capture** — `RealizedOutcomeEntity` + `OutcomeService` + `POST /outcomes`
   record claim/loss for a decision and emit an `OutcomeRecorded` outbox event (the flywheel input).
2. **KPI columns on decisions** — `DecisionEntity` now stores `tier`, `predictedClaimProbability`
   and `expectedLossRatio` so the dashboard is queryable from the system of record (both the sync
   `/submissions` and async `/cases` paths).
3. **Embedded dashboard** — `DashboardService` + `GET /dashboard` returns curated KPIs: decision mix,
   **STP/auto rate**, average premium, average predicted claim probability, and the calibration
   signal (predicted vs **realized** claim rate, realized loss ratio, outcome coverage).
4. **Business metrics** — `DecisionMetrics` exports decision mix by outcome/line/**tier**, a premium
   distribution, and realized-outcome counters via Micrometer/Prometheus for Grafana.
5. **Security** — dashboard is read access (underwriters/auditors/admin); the outcome feed is a
   service identity or senior underwriter (PAS/claims).

## Consequences

**Positive**
- The operation is measurable (throughput mix, STP rate, premium) and the flywheel is real
  (predicted-vs-realized calibration) — the basis for widening autonomy on evidence.
- Queryable from stored columns; no JSON parsing for the core KPIs.
- `OutcomeRecorded` events are ready for the book/RAG/eval refresh jobs.

**Negative / trade-offs**
- The **book-refresh / retrain** jobs that consume `OutcomeRecorded` are **future work** — this slice
  captures and surfaces outcomes; it doesn't yet re-train the k-NN book or RAG corpus at runtime.
- Dashboard KPIs are a curated set (doc 10 §4); richer cuts (referral-reason distribution, drift over
  time, override rate) come with the warehouse/Grafana layer.
- Realized outcomes mature slowly, so calibration ramps over time (works on current data meanwhile).

## Alternatives considered

- **Grafana/Prometheus only, no embedded dashboard** — rejected: business users need a curated
  in-product view; metrics alone aren't a dashboard.
- **Parse decision JSON for KPIs** — rejected for the core metrics: storing `tier`/predicted columns
  keeps the dashboard cheap and queryable.
- **Refresh the book on every outcome now** — deferred: heavier and needs governance; capture first,
  retrain as a governed batch/job later.
