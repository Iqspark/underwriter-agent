# 14. Cost Governance (AI FinOps)

**Project:** AI Underwriter Agent
**Document status:** Recommended design
**Audience:** Engineering, finance/FinOps, product, underwriting leadership
**Related:** [Target Architecture](07-target-architecture.md), [Runtime/Audit](10-runtime-audit-observability.md), [Recommended Solution](08-recommended-solution.md), [ADR-0014](adr/0014-cost-governance.md)

---

## 1. Why

AI and external data turn a fixed-cost app into a **variable-cost-per-decision** system. Left
ungoverned, LLM tokens and per-call enrichment APIs can quietly make some submissions unprofitable
to underwrite. The goal: **spend AI/data dollars only where they add decision value, measure cost
per decision, and cap the downside.**

## 2. Cost drivers

| Driver | Notes |
|--------|-------|
| **LLM generation/reasoning** | Tokens for rationale, drafting, evaluator; the biggest swing cost |
| **Embeddings** | Cheap if offline ONNX; per-call if hosted |
| **External enrichment APIs** | Geocoding, peril/property data — often **priced per call**, can dominate |
| **Infra** | Broker, workflow engine, datastore, vector store, compute |

## 3. Controls (design already supports most)

- **Model routing** — cheap/small model for triage & extraction, frontier only for nuanced
  reasoning/drafting, **offline** for simple rationale; never use a frontier model where rules +
  k-NN suffice ([doc 7](07-target-architecture.md)).
- **Sync fast-path** — STP-eligible simple files decide on rules + k-NN with **no LLM/enrichment
  spend** at all.
- **Call only when valuable** — don't run every enrichment tool on every submission; fetch a
  peril/property signal only when it can change the outcome (e.g., not already a knockout).
- **Caching & dedupe** — cache stable enrichment (geocoding, area/peril rarely change), embeddings,
  and idempotent results; dedupe re-submissions.
- **Token discipline** — bounded `max_tokens`, concise prompts, retrieve only top-k chunks, trim
  context.
- **Budgets, quotas & circuit-breakers** — per-tenant/LOB/day budgets; on breach, **degrade to the
  offline/rules path** instead of overspending (reuses the resilience fallback, [doc 12](12-resilience-dr.md)).
- **Batching** where providers price it favourably.
- **Vendor terms** — committed-use/volume discounts; cheaper tiers for non-critical calls.

## 4. Measure: cost attribution & unit economics

Surfaced on the dashboard ([doc 10](10-runtime-audit-observability.md)):

- **Cost per decision** — overall and by **line of business** and **autonomy tier** (STP vs
  assisted vs specialist).
- Breakdown by driver (LLM vs embeddings vs enrichment vs infra).
- **Unit economics** — cost per decision vs the value it creates: premium written, underwriter
  minutes saved, and loss-ratio improvement. Target a cost-per-decision ceiling per LOB.
- Trend + **anomaly/spike alerts**; cost shown alongside quality so we optimise the *ratio*, not
  just the bill.

## 5. Governance

- A named **cost owner**; monthly FinOps review of cost-per-decision and unit economics by LOB/tier.
- Budgets set with finance; alerts to the owner; circuit-breakers enforce the cap automatically.
- Cost is a release consideration — a change that improves quality but blows unit economics is a
  governance decision, not an automatic ship (ties to the [model-governance gate](13-ai-governance-model-risk.md)).

## 6. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Token/enrichment costs make some files unprofitable | Cost-per-decision ceilings, routing, call-only-when-valuable, sync fast-path |
| Runaway spend from a bug or spike | Budgets + quotas + circuit-breaker to offline path + anomaly alerts |
| Optimising cost at the expense of quality | Track cost **and** quality together; decisions go through the governance gate |
| Hidden enrichment API costs | Per-call attribution + caching + only-when-needed fetching |
| Cloud LLM price changes / lock-in | Provider-swappable `LlmReasoner` seam; offline floor caps exposure |
