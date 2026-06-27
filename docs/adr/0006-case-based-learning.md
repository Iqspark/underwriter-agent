# ADR-0006: AI-first, case-based learning from the historical book

**Status:** Accepted
**Date:** 2026-06-19
**Supersedes the emphasis of:** ADR-0001 (rules now act as *guardrails*, not the primary driver)

## Context

The product goal is an **AI-first, multi-line property & casualty (P&C) underwriting agent that
learns from the book of business** (vacant home is the first line built and the worked reference
example): given a new submission, it should look at how similar past policies actually performed
(claims, loss ratios, perils), factor in area-level signals (e.g. theft, a vacant-home-relevant
example), and let that evidence drive the risk view and price — rather than relying primarily on
hand-coded risk weights. We still require explainability, auditability, and hard compliance
guarantees.

## Decision

Adopt **case-based reasoning (k-nearest-neighbours)** over the historical book as the primary
risk signal, with the deterministic rules retained as **guardrails**:

- A `HistoricalPolicyRepository` holds the book (a deterministic synthetic book today; a real
  data source in production).
- A `SimilarityEngine` finds the `k` most similar past policies using a weighted Gower
  distance over normalized numeric + matched categorical features, and aggregates their actual
  outcomes — similarity-weighted — into a predicted claim probability, expected loss ratio,
  fair rate, and dominant peril.
- An `AreaRiskService` derives per-area claim/theft statistics from the same book and a pricing
  load.
- A `PatternLearningAgent` turns this into findings; the `PricingAgent` prices from the
  comparable fair rate × area load; the `DecisionOrchestrator` blends the learned outcome band
  with the guardrails and takes the **most conservative**.
- The rules engine still owns the **condition-precedent knockouts** (e.g. the vacant-home line's
  72-hour inspection condition) and completeness guarantees, which no learned signal can override.

## Consequences

**Positive**
- The agent genuinely learns from history: outcomes and price track what comparable risks did.
- Fully explainable: every assessment ships the comparable cases and area stats that justify it.
- Compliance is still guaranteed by deterministic guardrails (knockouts, completeness).
- Swapping the synthetic generator for a real data source upgrades the whole agent with no
  downstream changes.

**Negative / trade-offs**
- Quality depends on book size/representativeness; thin areas trigger a **cold-start** fallback
  to rules + base rate.
- k-NN recomputes distances per request (fine at this book size; index/caching needed at scale).
- Feature weights and learned thresholds encode appetite and need periodic review/back-testing.

## Alternatives considered

- *Trained ML model (gradient boosting) for claim probability/price* — higher ceiling but less
  transparent and needs more data/MLOps; deferred. The interfaces leave room to add it as an
  alternative signal later (see HLD §12).
- *Replace rules entirely* — rejected: loses hard compliance guarantees (the condition-precedent
  knockouts, e.g. the vacant-home line's 72-hour inspection condition).
