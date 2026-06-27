# ADR-0014: Cost governance (AI FinOps)

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0003](0003-pluggable-llm-offline-default.md), [ADR-0008](0008-ai-maximized-architecture.md), [ADR-0012](0012-resilience-dr.md), [doc 14](../14-cost-governance.md)

## Context

LLM tokens and per-call enrichment APIs make this a variable-cost-per-decision system. Without
governance, AI/data spend can quietly exceed the value of underwriting some files and can spike
uncontrollably.

## Decision

1. **Spend only where it adds value.** Model routing (cheap→frontier→offline), a sync fast-path
   that decides simple files on rules + k-NN with no AI spend, and fetch enrichment only when it
   can change the outcome.
2. **Cap the downside.** Per-tenant/LOB/day budgets and quotas; on breach, **circuit-break to the
   offline/rules path** rather than overspend (reuses the resilience fallback).
3. **Reduce unit cost.** Cache stable enrichment/embeddings, dedupe, bound `max_tokens`, trim
   context to top-k.
4. **Measure.** Track **cost per decision** by LOB and autonomy tier, broken down by driver, with
   unit economics (cost vs premium / time saved / loss-ratio impact) and spike alerts on the
   dashboard.
5. **Govern.** Named cost owner, monthly FinOps review, budgets agreed with finance; cost is a
   release consideration within the model-governance gate.

## Consequences

**Positive**
- AI is used where it pays off; simple files stay cheap; spend has a hard ceiling.
- Cost-per-decision is visible next to quality, enabling ratio optimisation and clear unit economics.
- Provider-swappable seam + offline floor cap vendor/price exposure.

**Negative / trade-offs**
- Budget circuit-breakers can reduce capability under load (safe, but lower-quality output) — must
  be surfaced via alerts.
- Caching adds invalidation logic; stale enrichment risk managed with TTLs.

## Alternatives considered

- **No budgets, optimise later.** Rejected — risks unprofitable decisions and bill shock.
- **Frontier model for everything (simplicity).** Rejected — needless cost where rules/k-NN/cheap
  models suffice; routing is worth the complexity.
- **Cut AI to save money.** Rejected — the point is value per dollar; track cost **and** quality and
  let governance decide trade-offs.
