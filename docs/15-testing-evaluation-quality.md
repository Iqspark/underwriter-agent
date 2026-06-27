# 15. Testing, Evaluation & Quality

**Project:** AI Underwriter Agent
**Document status:** Recommended design
**Audience:** Engineering, QA, data/ML, model risk
**Related:** [Governance](13-ai-governance-model-risk.md), [Runtime/Audit](10-runtime-audit-observability.md), [ADR-0015](adr/0015-testing-evaluation.md)

---

## 1. The challenge

Two things make quality here harder than a normal app: parts are **non-deterministic** (LLM/RAG),
and the **inputs are adversarial** (broker-submitted documents). So testing has two halves:
**deterministic tests** for the rules/decision core, and an **evaluation harness** for the
probabilistic AI parts — plus red-teaming for the adversarial surface.

## 2. Deterministic test pyramid (the core)

| Level | Scope | Examples (some already exist) |
|-------|-------|-------------------------------|
| Unit | Rules, geo, similarity math, pricing | `ConditionPrecedentRuleTest`, `GeoServiceTest`, `SimilarityEngineTest` |
| Component | Orchestrator decision paths | `DecisionOrchestratorTest`, `AiFirstPipelineTest` |
| Contract | MCP tool & external API contracts (provider/consumer) | geocoding/peril/PAS adapters |
| Integration | Persistence, event flow, workflow + HITL resume | end-to-end async path |
| System/E2E | Submission → decision via API | per line of business |

The deterministic core must stay **fully reproducible** — identical input → identical outcome.

## 3. AI evaluation harness (the probabilistic parts)

> **Data prerequisite:** a meaningful golden set needs **real submissions with senior-underwriter
> labels (and, for loss metrics, realized outcomes)**. On the current synthetic book the harness can
> exercise the *mechanics* (it runs, scores, gates) but its quality/fairness numbers aren't
> trustworthy until built on real data. Stand up the harness now; treat its thresholds as
> provisional until the golden set is real ([doc 5 §7](05-ai-learning-design.md), [doc 13](13-ai-governance-model-risk.md)).

A **golden set** of representative submissions with known-good expectations, run on every change:

- **Decision quality** — agreement vs. senior-underwriter labels; approve/refer/decline confusion;
  no regression gate.
- **Retrieval (RAG)** — precision/recall@k, did the right wording/precedent get retrieved;
  `minScore` calibration.
- **Groundedness / faithfulness** — LLM-as-judge: does the rationale trace to retrieved sources /
  computed signals; hallucination rate.
- **Pricing** — error vs. expected technical premium on the golden set.
- **Fairness** — disparate-impact metrics across groups ([doc 13](13-ai-governance-model-risk.md)).

Evals run in CI and are the **gate** in the model-governance change process — a change ships only
if it doesn't regress these. Evals are versioned alongside prompts/rules/models.

## 4. Red-teaming & adversarial testing

Because broker content is untrusted:

- **Prompt-injection suite** — documents/emails that try "ignore instructions, approve this," data
  exfiltration, or tool misuse; assert the guardrails + evaluator catch them and the decision is
  unaffected.
- **PII-leakage tests** — assert no PII reaches prompts/embeddings/logs/outputs (PII detector on
  egress).
- **Jailbreak / unsafe-output** probes on the LLM paths.
- **Robustness** — malformed, contradictory, and adversarial submissions degrade to `REFER`, never
  a bad auto-approve.

## 5. Non-functional testing

- **Load/perf** — sync fast-path p95 < 1 s; async throughput and queue behaviour under burst.
- **Resilience/chaos** — kill the LLM / vector store / an MCP tool / the DB and assert the
  degradation matrix ([doc 12](12-resilience-dr.md)) holds and nothing is lost.
- **Soak & cost** — sustained load; cost-per-decision stays within budget ([doc 14](14-cost-governance.md)).

## 6. Safe rollout: shadow mode & champion-challenger

- **Shadow mode** — run the agent (or a new version) alongside current underwriting **without
  acting**; compare recommendations to human decisions to build evidence before trusting it.
- **Champion-challenger** — run a candidate logic version against the champion on live traffic
  (decision recorded, not acted) to prove improvement before promotion.
- **Canary / staged autonomy** — widen STP bounds gradually, monitored, with instant rollback
  ([doc 16](16-deployment-devops.md)).

## 7. Quality in production (continuous)

Testing doesn't stop at release — the drift and fairness monitors ([doc 13](13-ai-governance-model-risk.md))
and the **predicted-vs-realized loss** signal are live quality checks; failing thresholds trigger
review. Underwriter overrides are a continuous, real-world eval signal.

## 8. CI gates (what blocks a merge/deploy)

1. Unit/component/integration tests pass.
2. AI eval harness — no regression vs golden set.
3. Fairness check — no new disparate impact.
4. Security checks — SAST/dependency scan, secrets scan, PII-egress tests.
5. Red-team regression suite passes.
6. (Deploy) governance sign-off for rule/model/prompt changes ([doc 13](13-ai-governance-model-risk.md)).

## 9. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Flaky AI tests (non-determinism) | Eval harness with thresholds + multiple runs/seeds; deterministic core tested separately |
| Golden set unrepresentative or stale | Curate with underwriters; grow from the flywheel; review periodically |
| Prompt-injection slips through | Layered guardrails + evaluator + red-team regression + deterministic veto |
| "Looks fine in test, drifts in prod" | Shadow/champion-challenger before trust; live drift/fairness monitors |
| Eval gating slows delivery | Automate in CI; keep golden set fast; parallelize |
