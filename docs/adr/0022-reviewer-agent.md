# ADR-0022: Reviewer agent — the LLM "skeptical underwriter" (evaluator/critic, formalized)

**Status:** Accepted (built)
**Date:** 2026-06-26

> **Build note (2026-06-26):** implemented as `com.iqspark.underwriter.review.ReviewerAgent`
> (gated by `underwriter.reviewer.enabled`, default on), invoked by the `DecisionOrchestrator` after
> the decision is assembled. A deterministic coherence check always runs (rationale must reflect
> every knockout; outcome/rationale consistency); an LLM critic pass runs when a chat model is
> configured. Flags are advisory — attached to the `Decision` as `reviewFlags` and audited; a
> rationale defect triggers at most one LLM re-draft. The outcome is never changed (ADR-0001).
**Related:** [ADR-0001](0001-rules-decide-llm-explains.md), [ADR-0008](0008-ai-maximized-architecture.md), [doc 7 §4.4](../07-target-architecture.md), [doc 8 §5](../08-recommended-solution.md), [doc 15](../15-testing-evaluation-quality.md)

## Context

The target architecture already names an **evaluator/critic agent** for groundedness and compliance
([doc 7 §4.4](../07-target-architecture.md), [doc 8](../08-recommended-solution.md) Phase 6), but it
is not yet pinned down as a concrete pipeline step with a defined contract. A specific failure mode
motivates making it concrete now: the generated **rationale can drift from the findings** — e.g.
downplay or omit a decisive condition-precedent knockout, or read as APPROVE-leaning while the
outcome is REFER. The decision can be *technically correct* yet *incoherent* to a human expert, which
erodes trust and creates compliance exposure. Nothing today checks the assembled decision for
*internal contradiction*.

## Decision

Formalize the evaluator as a **`ReviewerAgent`** — an LLM "skeptical senior underwriter" that runs
**last**, after the `DecisionOrchestrator` has assembled the full `Decision`, and performs a
qualitative consistency check. It **flags; it never decides**.

1. **Input.** The complete assembled `Decision`: outcome, findings (with severities), conditions,
   indicative premium, the `LearnedAssessment` (comparables, signals), and the drafted rationale.
2. **Checks.** The agent is prompted to act as a senior underwriter reviewing for:
   - **Rationale-vs-findings contradiction** — does the narrative reflect every HIGH/knockout
     finding, and not downplay or omit any? (the primary check)
   - **Outcome coherence** — does the stated outcome follow from the findings + learned signals?
   - **Groundedness** — does every claim in the rationale trace to a finding, a comparable, or a
     retrieved source (no invented facts)?
   - **Ambiguity / missing caveats** — anything a human reviewer would find unclear or unsupported.
3. **Output = advisory only.** The agent returns structured `reviewFlags` (severity + message +
   the finding/claim referenced). Flags are attached to the decision and the audit trail.
   - A flag **never** changes the outcome on its own. The deterministic outcome and guardrails are
     authoritative ([ADR-0001](0001-rules-decide-llm-explains.md)).
   - A flag of severity ≥ threshold **routes the file to a human** (downgrades any straight-through
     tier to *assisted/specialist*) and, for rationale defects, can trigger **one rationale
     re-draft** pass (evaluator-optimizer loop) before surfacing.
4. **Bounded loop.** At most one revision pass to avoid latency/cost runaway; if still weak, the
   file goes to a human with the flags.
5. **Offline floor.** With no LLM configured, the ReviewerAgent is skipped and the decision surfaces
   as today — additive, never a hard dependency ([ADR-0012](0012-resilience-dr.md)). A lightweight
   deterministic coherence check (e.g. "rationale must name every knockout finding") runs even
   offline as a cheap backstop.
6. **Governance + eval.** The reviewer prompt is a versioned, governed artifact; reviewer
   precision/recall on a labelled set of "incoherent decision" cases is tracked in the eval harness
   ([doc 15](../15-testing-evaluation-quality.md)).

## Consequences

**Positive**
- Adds a self-correction layer that catches subtle incoherence (especially a rationale that
  downplays a knockout) before it reaches an underwriter — coherent output, not just correct output.
- Strengthens the hallucination/groundedness control already envisioned, with a concrete contract.
- Captures reviewer flags in the audit trail → richer lineage and a labelled stream for the flywheel.

**Negative / trade-offs**
- Extra LLM call (latency + cost) on every decision — mitigated by model routing and the
  single-pass bound ([doc 14](../14-cost-governance.md)).
- Risk of **false flags** creating review noise; needs eval-tuned thresholds and the deterministic
  backstop so it adds signal, not friction.
- Must be strictly advisory — a reviewer that could flip outcomes would re-introduce a probabilistic
  decider, violating [ADR-0001](0001-rules-decide-llm-explains.md).

## Alternatives considered

- **Let the evaluator change the outcome** — rejected: authority must stay deterministic.
- **Only a deterministic coherence check (no LLM)** — kept as the offline backstop, but insufficient
  alone: it cannot judge nuance, tone, or groundedness of free-text rationale.
- **Fold the check into the drafting prompt** — rejected: a model grading its own output in one pass
  is weaker than an independent critic; separation is the point.
