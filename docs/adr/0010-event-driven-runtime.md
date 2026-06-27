# ADR-0010: Event-driven runtime, durable audit & observability

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0002](0002-multi-agent-pipeline.md), [ADR-0008](0008-ai-maximized-architecture.md), [doc 10](../10-runtime-audit-observability.md)

## Context

The reference core is synchronous and in-memory. Production multi-line underwriting with external
enrichment, LLM reasoning, and human-in-the-loop needs an operational backbone the current design
lacks: an async/queue runtime, durable persistent audit, and metrics/dashboards. These are also
prerequisites for safe autonomy/STP and for the data flywheel, so they are foundational rather
than late-stage polish.

## Decision

1. **Hybrid runtime.** Keep a **synchronous fast-path** for instant quotes / STP-eligible simple
   cases; process everything involving enrichment, LLM, or HITL **asynchronously** via an
   event-driven pipeline (accept → publish event → durable workflow → notify).
2. **Durable workflow orchestration (Temporal).** The per-submission lifecycle runs as a durable
   workflow with built-in retries, timeouts, and **wait-for-human-signal** for referrals. Replaces
   hand-rolled sagas; makes HITL and long-running steps first-class.
3. **Event backbone (Kafka).** `SubmissionReceived` / `DecisionMade` / `OutcomeRecorded` as an
   immutable, **replayable** log — also the audit source and flywheel feed.
4. **Reliability patterns.** Outbox (atomic DB write + publish), idempotency keys, retries with
   backoff, and a dead-letter queue.
5. **Durable state machine.** Explicit, persisted submission lifecycle in Postgres.
6. **Durable, tamper-evident audit & lineage.** Append-only Postgres table + `audit` topic, with
   correlation/trace ids, prompt/rule/model/threshold **versioning**, optional hash-chaining,
   retention, and a compliance read API. Operational logs are kept separate (structured JSON →
   Loki/ELK).
7. **Observability & dashboards.** Micrometer→Prometheus metrics, OpenTelemetry traces, Grafana
   for ops, and an embedded **underwriting performance dashboard** (STP rate, decision mix, time-
   to-decision, premium, override rate, predicted-vs-realized loss, LLM cost).
8. **Right-sizing.** Start lean (Postgres + Actuator/Micrometer + state column + `@Async`) and
   graduate to Kafka + Temporal as volume/autonomy grow — seams unchanged.

## Consequences

**Positive**
- Resilient to slow/bursty external + LLM calls; retries and DLQ instead of lost work.
- HITL is durable; referrals pause and resume cleanly.
- Every decision is persisted, auditable, reproducible (versioned), and replayable.
- Real operational + business + model-quality measurement; safe basis for widening autonomy.
- Event log powers the data flywheel and back-testing.

**Negative / trade-offs**
- More infrastructure (broker, workflow engine, datastore, metrics/trace stack) and operational
  surface — mitigated by the lean-start tier and phased adoption.
- Async introduces eventual consistency — mitigated by durable workflow + state machine +
  idempotency + outbox.
- Cost/complexity must be justified by volume — adopt Kafka/Temporal on real need, not upfront.

## Alternatives considered

- **Stay synchronous.** Rejected — can't handle enrichment/LLM latency, spikes, retries, or HITL;
  no durability for audit/flywheel.
- **Hand-rolled saga + DB queue instead of Temporal.** Viable lean-start option (documented), but
  Temporal removes large classes of retry/timer/HITL bugs at production scale.
- **RabbitMQ instead of Kafka.** Simpler work-queue; chosen as the lean alternative. Kafka
  preferred at scale for the replayable log (audit + flywheel + back-testing).
- **Logs-only "audit" (no dedicated store).** Rejected — not queryable, immutable, or tamper-
  evident enough for regulatory needs.
