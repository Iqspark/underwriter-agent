# ADR-0008: AI-maximized, human-augmenting target architecture

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0001](0001-rules-decide-llm-explains.md), [ADR-0002](0002-multi-agent-pipeline.md), [ADR-0006](0006-case-based-learning.md), [ADR-0007](0007-rag-spring-ai.md), [doc 7](../07-target-architecture.md)

## Context

The goal is to maximize AI across the entire underwriting lifecycle — intake, enrichment,
retrieval, analysis, drafting, and recommendation — to remove routine work and let skilled people
focus on judgment, while preserving the determinism, auditability, and compliance guarantees the
earlier ADRs established. We have the freedom to use online tools, LLMs, and external data.

## Decision

Adopt the target architecture in [doc 7](../07-target-architecture.md), anchored on these
decisions:

1. **Deterministic decision authority; probabilistic everywhere else.** AI does the work and
   recommends; clearing a compliance knockout or auto-approving is owned by deterministic rules
   and explicit autonomy bounds — never a model. (Extends ADR-0001 from "rules decide" to "rules
   retain *authority* while AI maximizes the surrounding work".)
2. **Autonomy tiers / straight-through routing.** A router assigns each submission to STP-auto,
   assisted, or specialist based on completeness, risk, confidence and appetite bounds. Clean
   low-risk files flow through within tight, audited limits; everything ambiguous reaches a human.
3. **MCP as the tool/data boundary.** External data (geocoding, flood/wildfire/wind/crime peril,
   property/imagery, registry/AML) and internal systems (PAS/CRM/doc store) are exposed via MCP
   tools, separated from orchestration; new sources are pluggable.
4. **Orchestrator-workers + evaluator (reflection).** Specialist workers run under an
   orchestrator; a dedicated evaluator/critic gates every AI assessment for groundedness and
   compliance before it surfaces — the primary hallucination control.
5. **Layered guardrails.** Controls at input, tool-call, tool-response, and output; deterministic
   knockouts are the final veto.
6. **Model routing + offline fallback.** Cheap models for triage/extraction, frontier for
   reasoning/drafting, in-process for embeddings; full degrade to the offline rules + k-NN +
   template path when cloud is unavailable.
7. **AI-ops platform.** Evaluation harness (golden set, LLM-as-judge groundedness, retrieval
   precision, decision-agreement), agent observability (trace trees, cost, tool accuracy, drift),
   and governance (versioned prompts/policies, lineage, reviewer logs) are first-class.
8. **Data flywheel.** Decisions + overrides + realized outcomes feed back into the book, RAG
   corpus, eval set, thresholds, and future models.

## Consequences

**Positive**
- Aggressive AI leverage on the work, with the decision still reproducible, explainable and
  defensible.
- Real straight-through processing on the easy majority; humans focused on judgment.
- Pluggable data/models via MCP and existing seams; capability scales with what's connected.
- Trust is engineered in (evals, observability, governance), not assumed.

**Negative / trade-offs**
- Substantially more moving parts (tools, evaluator, router, eval/observability stack) — phased
  delivery is mandatory; each phase must stand alone.
- Cloud LLMs/tools introduce cost, latency and data-residency questions — the offline floor and
  config gating contain them.
- Autonomy tiers carry business risk — start narrow, widen only on eval evidence, QA-sample
  auto-approvals.

## Alternatives considered

- **Let the LLM make the decision (full autonomy).** Rejected — breaks determinism/auditability
  and regulatory defensibility; conflicts with ADR-0001.
- **No straight-through (every file to a human).** Rejected — leaves the largest efficiency gain
  on the table; the easy majority is exactly where safe automation pays off.
- **Direct point-to-point integrations instead of MCP.** Rejected — couples agents to each data
  source; MCP keeps the tool boundary clean and swappable.
- **Ship capabilities without an eval/observability platform.** Rejected — untrustworthy and
  unprovable in a regulated context; evals/observability are prerequisites, not extras.
