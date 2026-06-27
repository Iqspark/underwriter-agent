# ADR-0013: AI governance & model risk management

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0001](0001-rules-decide-llm-explains.md), [ADR-0008](0008-ai-maximized-architecture.md), [ADR-0010](0010-event-driven-runtime.md), [doc 13](../13-ai-governance-model-risk.md)

> Not legal advice — align with model-risk policy and applicable regulators.

## Context

All decision logic (rules, thresholds, k-NN config, embedding model, LLM, prompts, raters) is a
"model" in regulatory terms. For automated insurance decisions this creates two exposures: unfair
outcomes (notably proxy discrimination via location/area features we deliberately use) and
un-auditable changes. We need governed model risk management without losing delivery speed.

## Decision

1. **Model inventory + model cards** for every governed asset (purpose, data, method, performance,
   limitations, fairness notes, owner, versions).
2. **Mandatory change-management gate** — no rule/threshold/model/prompt/rater change ships unless
   it passes: (a) eval harness vs golden set (no regression), (b) fairness/bias test, (c)
   groundedness/safety for AI text, and (d) **segregated sign-off** (author ≠ approver; committee
   for material changes). Changes are versioned, audited, and rollback-ready; the version id is
   recorded on every decision.
3. **Fairness by design** — proxy review of every feature (esp. location/area), pre/post-deploy
   disparate-impact testing, factor justification, and continuous fairness monitoring. Prohibited
   characteristics are never inputs.
4. **Independent validation + named owners + governance committee** with periodic review.
5. **Drift-triggered review** — performance/decision/override/data/fairness drift and falling
   groundedness trigger review or rollback, using production signals from [doc 10](../10-runtime-audit-observability.md).
6. **Transparency** — model cards, validation/fairness reports, and per-decision lineage retained;
   decisions are AI-assisted, human-decided, with explainable rationale.

## Consequences

**Positive**
- Defensible model risk posture (OSFI/responsible-AI aligned); fairness actively tested, not assumed.
- No silent logic changes; every decision reproducible against its exact logic version.
- Drift caught early; clear accountability and rollback.

**Negative / trade-offs**
- The gate adds release overhead — mitigated by automating eval + fairness checks in CI.
- Fairness testing needs representative data and agreed metrics — invest in the golden set and
  group definitions with compliance.

## Alternatives considered

- **Ship changes freely, monitor only.** Rejected — unfair/regressive logic could reach production
  before detection; unacceptable for regulated decisions.
- **Fairness as a one-time pre-launch check.** Rejected — drift makes fairness a continuous
  obligation.
- **Treat rules/prompts as "config", exempt from governance.** Rejected — they change outcomes and
  must be governed like models.
