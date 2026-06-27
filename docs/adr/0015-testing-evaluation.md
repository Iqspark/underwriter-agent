# ADR-0015: Testing & evaluation strategy (incl. shadow mode)

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0008](0008-ai-maximized-architecture.md), [ADR-0013](0013-ai-governance-model-risk.md), [doc 15](../15-testing-evaluation-quality.md)

## Context

Quality assurance must cover a deterministic decision core, non-deterministic AI parts (LLM/RAG),
and an adversarial input surface (broker documents). Standard unit tests alone are insufficient.

## Decision

1. **Two-track testing** — a deterministic test pyramid for the rules/decision core (reproducible)
   plus an **evaluation harness** (golden set) for the AI parts: decision agreement, retrieval
   precision/recall, groundedness (LLM-as-judge), pricing error, and fairness.
2. **Red-teaming as a first-class suite** — prompt-injection, PII-leakage, jailbreak and robustness
   tests, run in regression because broker input is untrusted.
3. **Eval gate in CI/governance** — changes to rules/models/prompts must pass the eval + fairness +
   red-team suites with no regression before deploy ([ADR-0013](0013-ai-governance-model-risk.md)).
4. **Safe rollout** — **shadow mode** and **champion-challenger** to prove a version on live traffic
   without acting, then **canary/staged autonomy** with rollback.
5. **Continuous production quality** — drift/fairness monitors and predicted-vs-realized loss are
   live quality signals; overrides are a real-world eval.

## Consequences

**Positive**
- Confidence in both deterministic and probabilistic behaviour; adversarial surface explicitly tested.
- New versions are proven before they're trusted; regressions blocked at the gate.
- Quality is continuous, not a point-in-time pass.

**Negative / trade-offs**
- Building/curating a representative golden set and red-team suite is real effort (worth it).
- Eval gates add CI time — mitigated by automation/parallelism.

## Alternatives considered

- **Unit tests only.** Rejected — can't measure RAG/LLM quality, fairness, or injection resistance.
- **Manual QA per release.** Rejected — not repeatable or scalable; automate evals.
- **Launch then watch.** Rejected — shadow/champion-challenger first avoids acting on an unproven
  version in a regulated decision.
