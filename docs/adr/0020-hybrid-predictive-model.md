# ADR-0020: Hybrid predictive model — trained GBM for prediction, k-NN for explanation

**Status:** Accepted (offline baseline built)
**Date:** 2026-06-26 (built 2026-06-27)

> **Build note (2026-06-27):** implemented behind a `RiskModel` seam in
> `com.iqspark.underwriter.history`. The default `LogisticRiskModel` is an offline, dependency-free
> logistic regression trained on the synthetic book at startup (a real GBM/XGBoost plugs in behind
> the seam). `PatternLearningAgent` blends the model's claim probability with the k-NN signal
> (`underwriter.model.blend`, default **max** = most conservative) while k-NN still supplies the
> comparable cases (explanation) — "GBM predicts, k-NN explains." Gated by `underwriter.model.enabled`;
> on a cold-start book the model is skipped. Separate exposure of the model-vs-k-NN probabilities and
> SHAP-style attributions are the next step.
**Related:** [ADR-0006](0006-case-based-learning.md), [doc 5](../05-ai-learning-design.md), [doc 2 §12](../02-architecture-design.md), [doc 13](../13-ai-governance-model-risk.md)

## Context

The learned signal today is **case-based k-NN with a weighted Gower distance** ([ADR-0006](0006-case-based-learning.md)).
It is transparent and needs no training pipeline, but it has real ceilings:

- **Static, expert-tuned weights.** Feature importance is hand-set (e.g. `roofAgeYears` = 1.5). The
  model cannot *learn* that, say, `priorLossCount` has become more predictive than roof age — it
  only applies a predefined formula.
- **Weak on interactions.** A weighted distance treats each feature's contribution roughly
  independently, so it cannot capture non-linear interactions (e.g. "an old roof is fine in city A
  but high-risk in city B, *and* only when there is no monitored alarm").

We want the higher predictive ceiling of a trained model **without** losing the "show me the
comparable files" explainability that makes k-NN defensible to an underwriter and a regulator.

## Decision

Adopt a **hybrid**: keep k-NN, and add a **trained gradient-boosting model (GBM — XGBoost /
LightGBM class)** behind the existing assessment seam. **Separate prediction from explanation.**

1. **Two numeric signals.** The `PatternLearningAgent` obtains two predictions of
   `claimProbability` / `expectedLossRatio`: one from the existing `SimilarityEngine` (k-NN) and one
   from a new `TrainedModelSignal` (GBM), both implementing a common `RiskSignal` seam.
2. **GBM predicts; k-NN explains.** When the trained model is available and in-confidence, its
   prediction drives the `LearnedAssessment`'s claim-probability / loss-ratio / fair-rate numbers.
   The k-NN retrieval is **always** run to populate `topComparables` — the comparable cases shown as
   evidence — regardless of which model produced the number.
3. **Orchestrator blend.** The `DecisionOrchestrator` combines the signals conservatively: by
   default it takes the **higher-risk** of the two for the decision band (so neither model can
   silently downgrade the other's escalation), while reporting both for transparency. The blend
   policy is configuration (`underwriter.model.blend = max | mean | gbm-primary`).
4. **Deterministic authority unchanged.** Guardrail knockouts and completeness still veto after the
   learned layer ([ADR-0001](0001-rules-decide-llm-explains.md)); no trained-model output clears a
   condition-precedent knockout.
5. **Offline floor.** If no trained model artifact is loaded (or it is out of confidence / a feature
   is missing), the agent degrades to k-NN-only — the current behaviour. The GBM is an additive
   signal, never a hard dependency ([ADR-0012](0012-resilience-dr.md)).
6. **Governance.** The GBM is a first-class model in the inventory with a model card, training data
   lineage, and the change-management eval + fairness gate before any promotion ([doc 13](../13-ai-governance-model-risk.md)).

## Consequences

**Positive**
- Higher accuracy: the GBM learns feature importance and non-linear interactions from the book
  instead of relying on fixed weights.
- Explainability preserved: every decision still ships the comparable cases (k-NN), plus
  per-prediction feature attributions (e.g. SHAP) from the GBM.
- Clean seam: prediction and explanation are decoupled, so either can evolve independently.

**Negative / trade-offs**
- Introduces an **MLOps surface**: training pipeline, model registry, versioning, retraining cadence,
  drift monitoring — all gated by governance ([doc 13](../13-ai-governance-model-risk.md)).
- Needs sufficient **real** labelled history; on the synthetic book the GBM is illustrative only and
  must not be calibrated against it for production.
- Two signals can disagree — requires a defined, audited blend policy (above) and eval coverage.

## Alternatives considered

- **Replace k-NN with the GBM** — rejected: loses the comparable-case explainability that is the
  core of the "AI-first / learn from the book" promise ([ADR-0006](0006-case-based-learning.md)).
- **Keep k-NN only and tune weights harder** — rejected: still cannot learn importance or
  interactions; the ceiling is structural.
- **Deep tabular models (e.g. TabNet)** — deferred: GBMs are the industry standard for tabular risk,
  stronger with limited data, and easier to explain; revisit only if evals show a real lift.
