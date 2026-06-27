# ADR-0021: Semantic feature extraction from unstructured documents

**Status:** Proposed
**Date:** 2026-06-26
**Related:** [ADR-0020](0020-hybrid-predictive-model.md), [ADR-0006](0006-case-based-learning.md), [doc 7 §2](../07-target-architecture.md), [doc 8 §5](../08-recommended-solution.md), [doc 11](../11-security-privacy.md)

## Context

Submissions arrive with rich **unstructured** material — inspection reports, broker emails,
property photos, prior-loss narratives — that the system currently ignores. Today's
`DocumentExtractor` only does key-value parsing to *normalize a submission* into the documented
schema; it does not read the prose. As a result, decision-relevant signals ("roof patched but
underlying deck rotted", "neighbouring unit recently had a break-in", "owner mid-renovation") never
reach the predictive model. The LLM in the pipeline currently only narrates a decision already made
— a supercomputer used as a calculator.

## Decision

Add an **`UnstructuredDataAgent`** early in the pipeline (after intake, before pattern-learning) that
uses an LLM to extract a **bounded, schema-constrained set of semantic risk features** from
unstructured inputs, which then **feed the predictive model** (k-NN + GBM, [ADR-0020](0020-hybrid-predictive-model.md)).

1. **Bounded feature schema.** The agent extracts to a fixed, versioned schema of typed features —
   e.g. `deferredMaintenancePresent: bool`, `recentRenovation: bool`, `hazardsMentioned: string[]`,
   `inspectorSentiment: enum`, `priorLossNarrativeSeverity: enum` — never free-form text injected
   into the decision. New features are added deliberately and governed, not improvised per call.
2. **Extracted features are inputs, not decisions.** They become additional `PolicyFeatures` fed to
   the similarity/trained model. They **cannot** clear a knockout or set an outcome directly; the
   deterministic guardrails remain the authority ([ADR-0001](0001-rules-decide-llm-explains.md)).
3. **Provenance + confidence.** Each extracted feature carries a source span and an extraction
   confidence; low-confidence or contradicted extractions are surfaced as findings (and can trigger
   **refer**), not silently trusted.
4. **Untrusted input handling.** Broker/inspection documents are untrusted: extraction runs behind
   the layered guardrails with prompt-injection defenses and PII redaction before any external LLM
   call ([doc 11](../11-security-privacy.md)). The extractor cannot invoke tools or change control flow.
5. **Offline floor.** With no LLM configured, the agent is a no-op and the pipeline runs on
   structured features exactly as today — additive, never a hard dependency ([ADR-0012](0012-resilience-dr.md)).
6. **Governance + flywheel.** The extraction prompt is a versioned, governed artifact ([doc 13](../13-ai-governance-model-risk.md));
   extracted features and realized outcomes feed the data flywheel so their predictive value can be
   measured and pruned.

## Consequences

**Positive**
- Unlocks a large body of currently-ignored evidence → more accurate, more nuanced risk assessment.
- Uses the LLM for what it is uniquely good at (reading messy prose/images) while keeping the
  decision deterministic and the predictive numbers in the trained/k-NN models.
- Slots behind the existing `DocumentExtractor`/agent seams; no core rewrite.

**Negative / trade-offs**
- New LLM cost/latency on intake (mitigated by model routing — a cheap/fast model for extraction,
  [doc 14](../14-cost-governance.md)) and a fresh prompt-injection / hallucinated-feature surface
  (mitigated by the bounded schema, confidence gating, and guardrails).
- Extracted features must be **validated for predictive lift and fairness** before they influence
  decisions — a proxy-bias risk (e.g. sentiment correlating with a protected attribute) the
  governance gate must test ([doc 13](../13-ai-governance-model-risk.md)).
- Requires labelled examples to evaluate extraction quality (precision/recall per feature).

## Alternatives considered

- **Dump raw document text into the rationale LLM** — rejected: unbounded, unauditable, injection-
  prone, and it does not improve the *predictive* model, only the narrative.
- **Hand-written regex/NLP extractors** — rejected as the primary approach: brittle across document
  formats and unable to capture nuance; may still complement the LLM for high-precision fields.
- **Defer entirely** — rejected for priority: this is the single highest-value way to improve
  accuracy beyond the structured-feature ceiling.
