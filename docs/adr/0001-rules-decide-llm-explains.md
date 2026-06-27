# ADR-0001: Rules decide, the LLM only explains

**Status:** Accepted
**Date:** 2026-06-19

## Context

The system must reach underwriting decisions that are reproducible, testable, auditable, and
defensible to a regulator. LLMs are excellent at producing fluent, file-specific narrative but
are non-deterministic, can hallucinate, and are hard to unit-test or audit at the decision
level. Many early "AI underwriting" systems put the LLM in charge of the decision and inherit
all of those problems.

## Decision

Split responsibilities cleanly:

- A **deterministic rules engine** owns all binding logic — completeness, contradictions,
  remoteness, condition-precedent knockouts, risk factors, and the final outcome derivation.
- The **LLM is used only to narrate** the decision the rules already reached. It receives the
  findings and the outcome and writes a rationale; it cannot change the outcome.

## Consequences

**Positive**
- Outcomes are pure functions of the input → fully unit-testable and reproducible.
- The decision is explainable from structured findings independent of any model.
- LLM outages/changes cannot alter decisions.
- Underwriting appetite lives in reviewable code (thresholds, severities), not in a prompt.

**Negative / trade-offs**
- The LLM cannot contribute nuanced judgment to the *decision* (by design); nuanced cases are
  routed to a human via `REFER`.
- New risk judgments require a code change (a new/updated rule) rather than a prompt tweak —
  this is intentional, for auditability.

## Alternatives considered

- *LLM-as-decider with guardrails* — rejected: non-deterministic, weak auditability.
- *LLM proposes, rules veto* — rejected for v1.0: more complex, still couples the decision to
  model behavior. May revisit for ranking/assist features.
