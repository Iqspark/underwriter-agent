# 18. Traceability Matrix, Assumptions & Open Questions

**Project:** AI Underwriter Agent
**Document status:** Living register
**Audience:** Architects, product, compliance, delivery

Single place to (a) trace requirements → design → decision → status, (b) record the assumptions the
design rests on, and (c) track the open questions/decisions the org still owns. Update as the build
progresses. Status: **Built** = implemented (Phase 0); **Designed** = specified, not built.

---

## 1. Requirements traceability matrix

### Functional ([BRD §1.6](01-requirements-brd.md))

| Req | Summary | Design | ADR | Status |
|-----|---------|--------|-----|--------|
| FR-1 | Structured `Submission` → `Decision` via REST | [doc 2](02-architecture-design.md), [doc 3](03-api-specification.md) | 0002 | Built |
| FR-1a | Learn from history (k-NN comparables → claim prob/loss/rate/peril) | [doc 5](05-ai-learning-design.md) | 0006; 0020 (hybrid GBM), 0023 (ANN scale) | Built (k-NN); hybrid GBM + ANN Designed |
| FR-1b | Surface comparable cases + confidence | [doc 5](05-ai-learning-design.md) | 0006 | Built |
| FR-1c | Area-level risk (theft/claim) → pricing & findings | [doc 5](05-ai-learning-design.md) | 0006 | Built |
| FR-1d | Cold-start fallback when book is thin | [doc 5](05-ai-learning-design.md) | 0006 | Built |
| FR-1e | Blend learned + guardrail (most conservative) | [doc 5 §6](05-ai-learning-design.md) | 0006, 0008 | Built |
| FR-2 | Raw document text → extract → underwrite | [doc 3](03-api-specification.md) | 0004; 0021 (semantic feature extraction) | Built (basic extractor; LLM/OCR + semantic features designed) |
| FR-3 | Flag missing fields & contradictions | [doc 2 §5](02-architecture-design.md) | 0004 | Built |
| FR-4 | Geographic eligibility / remoteness screen (vacant-home module instance: the 100 km rule) | [doc 2 §5.2](02-architecture-design.md) | 0004 | Built |
| FR-5 | Condition-precedent knockout (vacant-home module instance: the 72-hour inspection) | [doc 2 §5](02-architecture-design.md) | 0001 | Built |
| FR-6 | Physical + moral-hazard risk factors | [doc 2 §5](02-architecture-design.md) | 0004 | Built |
| FR-7 | Derive APPROVE/REFER/DECLINE | [doc 2 §6](02-architecture-design.md) | 0001, 0008 | Built |
| FR-8 | Always attach knockout curing condition | [doc 2 §6](02-architecture-design.md) | 0001 | Built |
| FR-9 | Indicative premium | [doc 5 §6](05-ai-learning-design.md) | 0006 | Built |
| FR-10 | Written rationale (leads with knockout) | [doc 6](06-rag-design.md) | 0003, 0007; 0022 (reviewer contradiction-check) | Built (template/Claude; RAG-cited baseline built — flag-gated; reviewer agent designed) |
| FR-11 | Ordered audit trail | [doc 10](10-runtime-audit-observability.md) | 0010 | Partial — in-memory built; durable/tamper-evident designed |
| FR-12 | Offline by default; Claude optional w/ fallback | [doc 6](06-rag-design.md) | 0003 | Built |
| FR-13 | New rule = single `@Component` | [doc 2 §5](02-architecture-design.md) | 0004 | Built |

### Non-functional ([BRD §1.7](01-requirements-brd.md))

| Req | Summary | Design | ADR | Status |
|-----|---------|--------|-----|--------|
| NFR-1 | Performance (offline core p95; hybrid target) | [doc 12](12-resilience-dr.md) | — | Built (core) / Designed (hybrid) |
| NFR-2 | Determinism of the rules engine | [doc 2](02-architecture-design.md) | 0001 | Built |
| NFR-3 | Explainability (every finding) | [doc 2](02-architecture-design.md) | 0001 | Built |
| NFR-4 | Auditability | [doc 10](10-runtime-audit-observability.md) | 0010 | Partial (see FR-11) |
| NFR-5 | LLM failure never fails a decision | [doc 6](06-rag-design.md), [doc 12](12-resilience-dr.md) | 0003, 0012 | Built |
| NFR-6 | Testability | [doc 15](15-testing-evaluation-quality.md) | 0015 | Built (core) / Designed (eval harness) |
| NFR-7 | Extensibility behind seams | [doc 2](02-architecture-design.md), [doc 9](09-multi-line-architecture.md) | 0004, 0009 | Built (rules/agents) / Designed (LOB) |
| NFR-8 | Portability (Java 21 + Spring Boot) | [doc 4](04-operations-runbook.md) | 0005 | Built |
| NFR-9 / 9a–9d | Security, authZ, PII, AI-security, compliance | [doc 11](11-security-privacy.md) | 0011, 0024 | Built (baseline: dual-mode authN, RBAC, authority limits/four-eyes, PII redaction); AI-specific + full hardening Designed |
| NFR-10 | Observability | [doc 10](10-runtime-audit-observability.md) | 0010 | Designed (basic logging Built) |
| NFR-11 | Resilience / DR | [doc 12](12-resilience-dr.md) | 0012 | Designed |
| NFR-12 | Model governance | [doc 13](13-ai-governance-model-risk.md) | 0013 | Designed |
| NFR-13 | Cost governance | [doc 14](14-cost-governance.md) | 0014 | Designed |

> Roadmap that sequences the "Designed" items: [doc 8 §5](08-recommended-solution.md#5-the-committed-delivery-order).

## 2. Assumptions register

| # | Assumption | If wrong… | Owner |
|---|------------|-----------|-------|
| A1 | Submissions arrive normalized to the documented schema (or as text the extractor can parse) | Need richer intake/extraction earlier | Eng |
| A2 | The **synthetic book stands in for real data** until real policy/claims data is integrated | k-NN/area/fairness/loss metrics are illustrative only | Data |
| A3 | Jurisdiction is a **Canadian MGA**; PIPEDA + applicable OSFI guidance + provincial rules apply | Compliance mapping (docs 11/13) must change | Compliance |
| A4 | Decision is **advisory**; binding/issuance happens in the PAS by a human | Autonomy/STP design must be revisited | UW |
| A5 | Thresholds/feature-weights/SLOs are **illustrative, not calibrated** | Must calibrate before production (docs 5/8/13) | UW + Actuarial |
| A6 | Offline ONNX embedding model is fetched once then cached; first run has network | Pre-bake the model into the image | Eng/SRE |
| A7 | Spring AI version pinned at build; API verified by local compile | RAG wiring may need adjustment | Eng |
| A8 | Cloud LLM/embeddings can be configured for **Canadian residency, no-train** | Keep offline floor as the production path | Eng + Legal |
| A9 | PAS/CRM/broker/doc systems expose integratable APIs/events | Integration adapters/timelines change | Eng |
| A10 | Initial volume is modest → lean backbone (Postgres + Actuator) before Kafka/Temporal | Adopt heavy infra sooner | Eng/SRE |

## 3. Open questions / decisions the org owns

| # | Question | Default in the design | Needed by |
|---|----------|-----------------------|-----------|
| Q1 | How wide are the **STP auto-approve bounds**? | Narrow ([doc 8 §4](08-recommended-solution.md)); widen on eval evidence | Phase 6 (autonomy) |
| Q2 | **Cloud vs offline posture** & data residency for LLM/embeddings/enrichment | Offline floor as the baseline; cloud where policy allows | Phase 2 (RAG)/4 (enrichment) |
| Q3 | **Build vs buy** enrichment data (peril/property/geo) | Pluggable via MCP either way | Phase 4 |
| Q4 | When to adopt **Kafka / Temporal** (vs lean Postgres+`@Async`) | Graduate on volume | Phase 3 |
| Q5 | **Fairness group definitions** and data source for disparate-impact testing | Framework defined ([doc 13](13-ai-governance-model-risk.md)); needs real data + agreed groups | Before production |
| Q6 | **Retention periods** & legal-hold specifics | Schedule + crypto-shredding ([doc 11](11-security-privacy.md)); periods TBD | Phase 1 |
| Q7 | **LOB rollout priority** | vacant → rental → contents → farm ([doc 9](09-multi-line-architecture.md)) | Per business |
| Q8 | **QA sampling rate** for auto-approvals | 100% initially → ~10–20% | Phase 6 |
| Q9 | Target **availability/RTO/RPO** and cost-per-decision ceilings | Starting targets in docs 12/14 | Phase 1/7 |

## 4. How to keep this current

Update the matrix status as phases land; add assumptions/questions as they surface; close a
question by recording the decision (ideally as a new ADR). This doc plus the [ADR log](adr/) is the
fastest way to see "where are we and what's undecided."
