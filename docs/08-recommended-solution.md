# 8. Recommended Solution (the committed decision)

**Project:** AI Underwriter Agent
**Document status:** Recommended — this is what we should build
**Audience:** Everyone

This is the single, opinionated recommendation that consolidates docs 1–7 and the ADRs. Where a
choice was open, it is now **made**, with sensible starting defaults that are tunable later.

---

## 1. The recommendation in one line

Build an **AI-maximized, human-augmenting, multi-line property & casualty (P&C) underwriter on
Spring Boot + Spring AI** (vacant home is the first line built and the worked reference example):
AI does intake, enrichment, retrieval, analysis and drafting; a **deterministic core retains the
binding-relevant authority**; clean low-risk files flow straight through within tight bounds;
everything else reaches an underwriter fully prepared. Deliver it in the nine phases (0–8) of §5,
**starting with the foundational backbone — persistence, durable audit, baseline security and
metrics** — not with a feature. (§5 is the single source of truth for the roadmap; other docs
reference it.)

## 1a. Scope: multi-line from the start

The agent is **designed to be multi-line**, not vacant-home-only. We use a **Line-of-Business
plug-in model** — a generic core with pluggable modules for vacant home, **rental/landlord,
contents/personal belongings, and farm** (and future lines). Adding a line is adding a module, not
a rewrite. See [doc 9](09-multi-line-architecture.md) and [ADR-0009](adr/0009-line-of-business-plugin.md).
Recommended line rollout: vacant home (the only line **built** today) → rental → contents → farm;
the LOB abstraction itself is the first refactor, not yet implemented.

## 2. The committed stack

| Layer | Decision |
|-------|----------|
| Language / framework | **Java 21 + Spring Boot 3.3** (keep) |
| AI integration | **Spring AI** — `ChatClient`, `VectorStore`, `EmbeddingModel`, RAG advisors |
| Generation model | **Anthropic Claude (Sonnet-class)** for reasoning/drafting; **Haiku-class** for triage/extraction (model routing); **offline template** as the always-available floor |
| Embeddings | **In-process ONNX (all-MiniLM)** — offline by default; revisit hosted embeddings only if evals show a real lift |
| Vector store | **pgvector** (Postgres extension) — persistent & scalable, shares the Phase 1 Postgres; `SimpleVectorStore` only as a dev/test fallback |
| External data & systems | **MCP tool servers** — geocoding, flood/wildfire/wind/**crime** peril scores, property/imagery, registry/AML, PAS/CRM/doc store |
| Persistence | **Postgres** in prod (decisions, audit, outcomes) — in-memory until then |
| AI-ops | **Golden-set eval harness + LLM-as-judge groundedness + trace/observability** |

## 3. The committed decision model (authority never moves to a model)

> Reflects the canonical principles in [overview §4](00-architecture-overview.md#4-the-principles-that-hold-it-together);
> the most-conservative blend and learned thresholds are defined canonically in [doc 5 §6](05-ai-learning-design.md#6-how-learning-drives-the-decision-and-price).

```
guardrail outcome (rules: knockouts, completeness)   ─┐
learned outcome (numeric k-NN: claim prob / loss ratio)├─►  most conservative  ─►  Decision
RAG advisory (cited, capped severity — can REFER)     ─┤        (+ evaluator/critic gate)
                                                       ─┘
```

- **Deterministic guardrails** own the knockouts (e.g. 72-hour inspection) — final veto.
- **Numeric k-NN** owns the claim-probability / loss-ratio / fair-rate signal.
- **RAG** is advisory and cited — it can push to `REFER`, never to a knockout.
- **Evaluator/critic agent** gates AI output for groundedness + compliance before it surfaces.
- The LLM **never binds**. (Per ADR-0001 / ADR-0008.)

## 4. Committed autonomy defaults (start narrow, widen on evidence)

> The specific numbers below are **illustrative starting points on synthetic data — not calibrated
> against real loss experience.** Treat them as a safe-by-default starting posture; calibrate
> against real outcomes and confirm with UW/actuarial (via the [model-governance gate](13-ai-governance-model-risk.md))
> before enabling straight-through processing.

**Auto (straight-through) only when ALL hold:**

- data complete, no contradictions, location resolved;
- **zero** condition-precedent knockouts;
- learned **claim probability < 0.15** and **expected loss ratio < 0.5**;
- learning **confidence HIGH** (k ≥ 15, mean similarity ≥ 0.80);
- **no** prior losses, **no** demolition/renovation;
- coverage **≤ CAD 750,000**, location **within range** (not remote);
- RAG/evaluator raise no compliance flag.

Otherwise → **assisted** (AI-prepared, underwriter decides) or **specialist** (senior UW) for
knockouts / low confidence / large or edge risks. **QA-sample 100% of auto-approvals initially,
taper to ~10–20%** as the eval record builds. Bounds are config owned by UW leadership.

## 5. The committed delivery order

> Re-sequenced after design review: **persistence, durable audit, eventing and observability are
> foundational** (the operational backbone, [doc 10](10-runtime-audit-observability.md)), not
> late-stage polish — they're prerequisites for safe autonomy and the data flywheel.

| Phase | What | Why this order |
|-------|------|----------------|
| **0** | Decision core (guardrails + k-NN + pricing + in-memory audit + API) | ✅ Done |
| **1** | **Persistence + durable audit + basic metrics + baseline security** (Postgres **with the `pgvector` extension**, append-only tamper-evident audit, Actuator/Micrometer, OIDC authN, RBAC/ABAC, encryption, secrets manager, PII redaction in logs/audit) | Foundational: nothing is trustworthy, measurable, or compliant without it — and stands up the pgvector store RAG uses. **✅ Built:** persistence + tamper-evident audit + metrics ([ADR-0019](adr/0019-phase1-persistence-metrics.md)) and baseline security — dual-mode authN, RBAC, authority limits/four-eyes, PII redaction ([ADR-0024](adr/0024-phase1-baseline-security.md)). Pending: pgvector, encryption-at-rest, external ABAC/OPA. |
| **2** | **RAG grounding** (wordings/guidelines/precedent, cited rationale; offline ONNX embeddings into **pgvector**) | Biggest *credibility* win; reuses the Phase 1 Postgres. **✅ Baseline built** (flag-gated `underwriter.rag.enabled`; in-memory store default, pgvector via Maven profile) — [ADR-0007](adr/0007-rag-spring-ai.md). |
| **3** | **Event-driven runtime + durable workflow + async HITL** (broker, Temporal, retries/DLQ/outbox, state machine) | Handles enrichment/LLM latency, spikes, and human-in-the-loop |
| **4** | **MCP enrichment** (geocoding + peril/**crime** + property data) | Biggest *accuracy* win; needs the async runtime |
| **5** | **Multimodal intake + drafting** — incl. **semantic feature extraction** ([ADR-0021](adr/0021-semantic-feature-extraction.md)) | Biggest *time-savings* for people; also lifts accuracy by feeding extracted features to the model |
| **6** | **Evaluator + autonomy tiers** — the **`ReviewerAgent`** critic ([ADR-0022](adr/0022-reviewer-agent.md)), layered guardrails, STP routing | Unlocks safe straight-through — now measurable & audited. **✅ ReviewerAgent built** (advisory coherence/contradiction check); autonomy tiers/STP routing pending. |
| **7** | **Dashboards + AI-ops + data flywheel** (Grafana + UW performance dashboard, evals, drift, outcome feedback) | Required before widening autonomy |
| **8** | **Production hardening** (authn/authz, model routing, scale, HA/DR) | Operational readiness |

Every phase is independently valuable and slots behind seams already in the code
(`UnderwritingAgent`, `LlmReasoner`, the assessment seam, `DocumentExtractor`) — no core rewrite.
Start lean on the backbone (Postgres + Actuator + a state column + `@Async`) and graduate to
Kafka + Temporal as volume and autonomy grow ([doc 10 §5](10-runtime-audit-observability.md)).

**Security & PII are cross-cutting, not a phase** ([doc 11](11-security-privacy.md)): baseline
controls (authN/authZ, encryption, secrets, PII redaction) land in Phase 1; AI-specific controls
(PII redaction before LLM/embeddings, prompt-injection guardrails, Canadian residency) ship with
the RAG/enrichment phases they protect; full hardening (SoD, crypto-shredding, anomaly alerting,
SOC 2 readiness) completes in Phase 8.

**Three more cross-cutting disciplines** apply throughout, not as one-off phases:
- **Resilience & DR** ([doc 12](12-resilience-dr.md)) — graceful degradation to the deterministic
  floor is built into every AI/enrichment integration; HA/backup/DR harden through Phases 1, 3, 8.
- **AI governance & model risk** ([doc 13](13-ai-governance-model-risk.md)) — the change-management
  eval+fairness gate gates every rule/model/prompt change from Phase 1 onward; fairness monitoring
  matures with the dashboards (Phase 7).
- **Cost governance** ([doc 14](14-cost-governance.md)) — routing/caching/budgets are designed in
  with each AI/enrichment phase; cost-per-decision lands on the dashboard (Phase 7).

## 6. Recommended immediate next step

**Build the Phase 1 RAG thin slice** behind `underwriter.rag.enabled`:

1. Add Spring AI (BOM + Anthropic starter + transformers/ONNX embeddings).
2. `GuidelineLibrary` (synthetic PR0003 / Supervisory Warranty 300130 / CGL / manual) → ingest to **pgvector** (the Phase 1 Postgres; a `SimpleVectorStore` profile is available for quick local dev).
3. `UnderwritingRetriever` (query from submission → top-k cited chunks).
4. `RagLlmReasoner implements LlmReasoner` → grounded, `[sourceId]`-cited rationale; falls back to the template reasoner.
5. Tests (offline) + flip the flag.

This makes the rationale immediately **defensible** — it cites the exact policy wording behind
each condition — with no new infrastructure and no API key required.

## 7. What's deliberately deferred (and safe to defer)

Trained ML model — k-NN carries us first, then a **hybrid GBM** signal is added behind the
assessment seam (GBM predicts, k-NN explains; [ADR-0020](adr/0020-hybrid-predictive-model.md)), with
**ANN indexing** ([ADR-0023](adr/0023-knn-scalability-ann.md)) when the book grows large; hosted
embeddings (offline ONNX is enough to start); full tool-calling autonomy (autonomy tiers are bounded
and human-gated). Each has a clean seam to add later. *(Note: persistence + the **pgvector** store are no longer deferred — they're Phase 1
foundations, since the vector store is now pgvector rather than in-memory.)*

## 8. The two business calls (defaults chosen, yours to confirm)

1. **Autonomy width** — default is the narrow bounds in §4; widen only on eval evidence.
2. **Cloud/data posture** — default keeps the offline path as the floor; enable Claude + online
   enrichment where data policy allows. MCP + the `LlmReasoner`/embedding seams keep providers
   swappable.

> Build note: the sandbox here can't run Maven, and Spring AI's API shifted across its 1.0/2.0
> line, so the Spring AI + pgvector wiring (Phase 2) should be compiled locally as it's written.
