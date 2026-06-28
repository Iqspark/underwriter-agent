# ADR-0025: Autonomy tiers / straight-through-processing routing

**Status:** Accepted (built)
**Date:** 2026-06-27
**Related:** [ADR-0022](0022-reviewer-agent.md), [ADR-0024](0024-phase1-baseline-security.md), [ADR-0006](0006-case-based-learning.md), [doc 7 §4.2](../07-target-architecture.md), [doc 8 §4](../08-recommended-solution.md)

## Context

The decision pipeline now produces, for every submission, a deterministic outcome, a learned
assessment (claim probability / loss ratio / confidence), RAG advisory flags, and Reviewer flags —
but every file still lands the same way. Doc 7/8 call for **autonomy tiers**: route clean, low-risk,
high-confidence files to straight-through auto-approval within tight bounds, send most files to an
underwriter, and escalate knockouts / low-confidence / large or edge risks to a senior. This unlocks
automation on the easy majority while guaranteeing humans see anything ambiguous or high-stakes.

## Decision

Add an **`AutonomyRouter`** (gated by `underwriter.autonomy.enabled`, default on) that the
`DecisionOrchestrator` runs after the Reviewer, producing an **`AutonomyAssessment`**
(`tier`, `qaSampled`, `reasons`) carried on the `Decision`. It is **advisory routing metadata** — it
never changes the outcome or clears a knockout (ADR-0001).

Tiers (doc 8 §4):

- **SPECIALIST** — any condition-precedent knockout or a recommended `DECLINE`; otherwise low
  learning confidence, data contradiction, an edge risk (prior losses, demolition/renovation, remote,
  unresolved location), a high-severity Reviewer flag, or coverage above the specialist threshold.
- **AUTO** (straight-through) — only when **all** hold: outcome `APPROVE`; no contradiction/missing
  data/edge factor; no high Reviewer flag; learned claim probability `< claimProbabilityMax` (0.15)
  and expected loss ratio `< lossRatioMax` (0.5); learning confidence `HIGH` (when required);
  coverage `≤ maxCoverage` (CAD 750,000).
- **ASSISTED** — everything else (AI-prepared; underwriter decides).

**QA sampling.** Auto-approvals are sampled at `qaSampleRate` (default **1.0 = 100%** initially,
taper as the eval record builds). Bounds are configuration owned by UW leadership.

The bounds are **illustrative defaults on synthetic data**; they must be calibrated against real loss
experience and pass the model-governance gate ([doc 13](../13-ai-governance-model-risk.md)) before
any real straight-through binding is enabled. Pairing with authority limits / four-eyes
([ADR-0024](0024-phase1-baseline-security.md)) governs the actual bind.

## Consequences

**Positive**
- Concentrates human effort where judgment matters; makes "safe straight-through" measurable and audited.
- Composes the existing signals (rules, learned confidence, RAG, Reviewer, coverage) into one routing call.
- Fully deterministic and offline-testable; advisory-only keeps decision authority unchanged.

**Negative / trade-offs**
- Thresholds encode appetite and are uncalibrated on synthetic data — must be governed before real STP.
- Routing is advisory metadata today; actually executing an auto-bind needs the binding/workflow path
  (authority limits exist; durable workflow arrives with the event-driven runtime phase).
- QA sampling is a simple rate; richer risk-based sampling is future work.

## Alternatives considered

- **No tiers (treat all files the same)** — rejected: forgoes the biggest efficiency gain and the
  ability to safely automate the easy majority.
- **Let the model auto-bind directly** — rejected: binding stays behind deterministic authority +
  four-eyes; the router only *recommends* a tier.
- **Fold routing into the rules engine** — rejected: routing depends on learned confidence and
  Reviewer flags (post-assembly signals), so it belongs after the decision is assembled.
