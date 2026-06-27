# ADR-0019: Phase 1 — persistence, durable audit & baseline metrics

**Status:** Accepted
**Date:** 2026-06-20
**Related:** [ADR-0010](0010-event-driven-runtime.md), [doc 10](../10-runtime-audit-observability.md), [doc 16](../16-deployment-devops.md)

## Context

Phase 0 kept everything in memory: decisions weren't stored and the audit trail was only returned
in the response. Phase 1 makes decisions durable, the audit trail tamper-evident, and adds baseline
metrics — the foundation for autonomy and the data flywheel. It must not require external
infrastructure for local dev/CI.

## Decision

1. **JPA persistence, H2 by default, Postgres in prod.** `spring-boot-starter-data-jpa` with an
   embedded **H2** datasource by default (so the app and tests run with no external DB) and
   **PostgreSQL** under the `prod` profile. Decisions are persisted **synchronously** on each
   decision (the event-driven/Kafka runtime of [ADR-0010](0010-event-driven-runtime.md) remains a
   later phase). `ddl-auto=update` in dev; `validate` in prod (schema via migrations, [doc 16](../16-deployment-devops.md)).
2. **Schema.** A `decision` table (structured columns + JSON-text for the variable-shape findings/
   conditions/learned-assessment) and an append-only `audit_event` table.
3. **Tamper-evident audit.** Audit entries are **hash-chained per decision**
   (`hash = sha256(prevHash + sequence + detail)`); `DecisionStore.verifyAuditChain` recomputes and
   detects any later edit.
4. **Resilience.** A persistence failure is caught and logged — it never fails the decision response
   (degrade-to-floor, [doc 12](../12-resilience-dr.md)).
5. **Baseline metrics.** Spring Boot Actuator + Micrometer/Prometheus; a decisions counter (by
   outcome + line) and a per-line latency timer; `health/info/metrics/prometheus` exposed.
6. **History API.** `GET /api/underwriting/decisions/{reference}` returns the stored decision + its
   audit lineage.

## Consequences

**Positive**
- Every decision is durable, queryable, and auditable with tamper-evidence; metrics are scrapeable.
- Zero external infra for dev/CI (H2); one profile switch to Postgres for prod.
- Decisions survive store outages (logged, not failed).

**Negative / trade-offs**
- Synchronous persistence adds a little latency and couples the request to the DB write (mitigated
  by the catch-and-log); the async event-sourced path is deferred to the runtime phase.
- JSON-in-columns trades queryability of findings for schema stability — acceptable now; can be
  normalized or moved to `jsonb`/search later.
- `ddl-auto` is a dev convenience; production must adopt migrations (Flyway) before go-live.

## Alternatives considered

- **Postgres-required for tests** — rejected; would block local/CI runs. H2 default + Postgres prod
  is the standard Spring approach.
- **Event-sourced audit via Kafka now** — deferred ([ADR-0010](0010-event-driven-runtime.md)); the hash-chained
  table gives durability + tamper-evidence without the broker yet.
- **Child tables for findings/conditions** — more normalized but heavier; JSON columns are simpler
  and the findings are read as a unit.
