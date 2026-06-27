# 1. Requirements & Business Requirements Document (BRD)

**Project:** AI Underwriter Agent
**Scope:** Multi-line property & casualty (P&C) underwriting (MGA pre-bind lane)
**First line built:** Vacant / unoccupied property (Canadian) — the worked reference example
**Document status:** Baseline v1.0
**Owner:** Underwriting Engineering

---

## 1.1 Purpose

Define the business need, scope, and requirements for an **AI-first**, multi-line **property &
casualty (P&C) underwriting** decision-support agent that risk-assesses insurance submissions
across lines of business by **learning from the historical book of business** and recommends
`APPROVE` / `REFER` / `DECLINE` with an indicative price and a cited rationale for a human
underwriter. The system is **decision support**: it accelerates and standardizes the
underwriter's work; it does not replace the underwriter or auto-bind risk.

**Vacant home (Canadian vacant-property) is the first line built and the worked reference
example** — the agent is line-agnostic by design (see the Line-of-Business plug-in model).

"AI-first" means the primary risk signal comes from data — the agent retrieves the most similar
past policies, reads how they actually performed (claims, loss ratios, perils), and folds in
area-level theft/claim signals. Deterministic rules are retained only as guardrails (see §1.4).

## 1.2 Background & problem statement

Today a submission is triaged and assessed manually. Each file takes a underwriter 45–60
minutes to read, cross-check for completeness and contradictions, screen for geographic
eligibility, and check condition-precedent compliance (for the vacant-home line, notably the
72-hour inspection warranty). This is slow, inconsistent between underwriters, and the
reasoning behind a decision is rarely captured in a structured, reviewable form.

Industry direction (2025–2026) is toward **agentic underwriting**: orchestrated specialist
agents that ingest, profile, check compliance and price a risk, surfacing a recommendation
with full traceability while keeping a human in the loop for the bind decision.

## 1.3 Goals & objectives

| ID | Objective | Measure of success |
|----|-----------|--------------------|
| G1 | Cut time-to-triage per submission | Decision returned in seconds, not 45–60 min |
| G2 | Standardize underwriting judgment | Same submission always yields the same deterministic outcome & findings |
| G3 | Make every decision explainable & auditable | Each decision ships with ordered findings, a rationale, and an audit trail |
| G4 | Never let automation create silent risk | Condition-precedent knockouts (e.g. the vacant-home 72h breach) always surfaced; missing/contradictory data forces referral |
| G5 | Keep a human in the loop | System recommends only; no auto-bind / auto-decline |
| G6 | Price & assess risk from real experience | Risk/price track how comparable past policies actually performed; improves as the book grows |

## 1.4 Scope

### In scope (v1.0)

- Ingest a **structured submission** (JSON) or **raw quote-summary text** (extracted to a submission).
- **Learn from the historical book**: retrieve the most similar past policies (case-based / k-NN),
  aggregate their actual outcomes into a predicted claim probability, expected loss ratio, fair
  rate and dominant peril, and surface the comparable cases as evidence.
- **Area risk** learned from the book (theft/claim rates per city) feeding the price and findings.
- Completeness & data-integrity checks; contradiction detection (guardrail).
- Geographic eligibility / remoteness screen — the vacant-home module instantiates this as the
  100 km rule against major Canadian cities (guardrail).
- Condition-precedent knockouts — the vacant-home module instantiates this as the 72-hour
  inspection warranty (guardrail).
- Indicative premium derived from comparable history × area load (cold-start: reference rater).
- Recommended outcome (blend of learned + guardrail, most conservative) + conditions + rationale + audit trail.
- A deterministic **synthetic historical book** generated at startup so the agent runs end-to-end now.
- REST API (incl. history stats / area / comparables previews); runs fully offline by default,
  optional Anthropic Claude for the rationale.

### Out of scope (v1.0)

- Binding, issuing, or declining policies (the human underwriter does this in the PAS).
- Production-grade OCR/document understanding (a seam exists; a simple extractor ships).
- A trained ML model for claim probability/price (case-based learning ships first; model is future, §1.9).
- Integration to a real policy/claims data source (synthetic book ships; the loader seam is documented).
- Actuarial pricing / rating tables (pricing learns from the book but is illustrative, not filed rates).
- Persistence, authn/authz, multi-tenant, UI workbench (see §1.9 future).
- Lines of business beyond the ones built in v1.0 — the agent is **multi-line by design** via a
  Line-of-Business plug-in model (vacant home, rental/landlord, contents/personal belongings, farm,
  …), with vacant home as the first line and worked reference example. See
  [doc 9 — Multi-Line Architecture](09-multi-line-architecture.md); the engine is line-agnostic,
  with each line added as a module rather than a rewrite.

## 1.5 Stakeholders

| Stakeholder | Interest |
|-------------|----------|
| Underwriters | Faster triage, trustworthy recommendations, clear reasoning to accept/override |
| Underwriting manager | Consistency, auditability, controlled risk appetite |
| Brokers | Faster turnaround on quotes |
| Compliance / audit | Traceable, regulator-ready decision records |
| Engineering | Maintainable, testable, extensible service |

## 1.6 Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Accept a structured `Submission` via REST and return a `Decision`. | Must |
| FR-1a | Learn from historical policies: retrieve the k most similar past policies and aggregate their outcomes into a predicted claim probability, expected loss ratio, fair rate and dominant peril. | Must |
| FR-1b | Surface the comparable cases and a confidence level as evidence in the decision. | Must |
| FR-1c | Derive area-level risk (theft/claim rates) from the book and apply it to pricing and findings. | Must |
| FR-1d | Fall back gracefully (cold start) to guardrails + base rate when the book is too thin. | Must |
| FR-1e | Blend the learned outcome with the guardrail outcome, taking the most conservative. | Must |
| FR-2 | Accept raw document text, extract it to a `Submission`, then underwrite it. | Should |
| FR-3 | Flag every missing required field and every self-contradiction (e.g. detached home with 10 units); never silently fix data. | Must |
| FR-4 | Apply a geographic eligibility / remoteness screen; in the vacant-home line, flag a property only if it is >100 km from **every** major Canadian city. | Must |
| FR-5 | Detect condition-precedent breaches as knockouts; in the vacant-home line, treat inspection interval > 72h as a **knockout**. | Must |
| FR-6 | Evaluate physical and moral-hazard risk factors, each with a one-line rationale. | Must |
| FR-7 | Derive a recommended outcome: `DECLINE` on knockout; `REFER` on blocking gap or risk weight ≥ threshold; otherwise `APPROVE`. | Must |
| FR-8 | Always attach the curing condition for a knockout so a decline can be reconsidered as a conditional bind. | Must |
| FR-9 | Produce an indicative premium from coverage + risk load. | Should |
| FR-10 | Produce a written rationale leading with any condition-precedent breach. | Must |
| FR-11 | Emit a complete, ordered audit trail of every agent action. | Must |
| FR-12 | Run fully offline; optionally use Anthropic Claude for the rationale when an API key is set, with graceful fallback. | Must |
| FR-13 | Make adding a new rule a single-class change (no central wiring). | Should |

## 1.7 Non-functional requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-1 | Performance | A decision (offline reasoner) returns in < 200 ms p95 for a single submission (starting target for the offline core; the target hybrid sync fast-path including external calls is < 1 s p95, see [doc 12](12-resilience-dr.md)). |
| NFR-2 | Determinism | The rules engine is pure: identical input → identical findings, score, and outcome. |
| NFR-3 | Explainability | Every finding carries a code, severity, message, rationale, and source. |
| NFR-4 | Auditability | Every decision includes an ordered audit trail suitable for compliance review. |
| NFR-5 | Resilience | LLM failure must never fail a decision; fall back to the offline reasoner. |
| NFR-6 | Testability | Rules, geo math, and decision paths covered by unit tests. |
| NFR-7 | Extensibility | New rules/agents/extractors/reasoners added behind interfaces with no core changes. |
| NFR-8 | Portability | Java 21 + Spring Boot; no external infra required to run. |
| NFR-9 | Security | No secrets in code; API key supplied via environment; no PII persisted in v1.0. |
| NFR-10 | Observability | Structured logs per agent; decision outcome and provider logged. |
| NFR-9a | Security / authZ | OIDC SSO + MFA, OAuth2 resource server, RBAC+ABAC with underwriting authority limits, four-eyes & segregation of duties (see [doc 11](11-security-privacy.md)). |
| NFR-9b | Privacy / PII | Classify & minimize PII; encrypt in transit + at rest (field-level for sensitive); tokenization; redaction in logs/audit; retention + crypto-shredding for erasure. |
| NFR-9c | AI security | Redact/pseudonymize PII before any external LLM/embedding call; de-identified embeddings; offline routing for PII content; Canadian data residency; prompt-injection guardrails on untrusted broker content. |
| NFR-9d | Compliance | Align with PIPEDA, applicable OSFI guidance, and SOC 2 (confirm with legal/privacy). |
| NFR-11 | Resilience / DR | Starting targets (tune with the business, see [doc 12](12-resilience-dr.md)): 99.9% core availability; RTO ≤1h region / ≤5m AZ; RPO ≤5m; graceful degradation to the deterministic floor; tested backups. |
| NFR-12 | Model governance | All decision logic versioned & governed; change-management gate (eval + fairness + sign-off); bias/proxy testing; drift monitoring (see [doc 13](13-ai-governance-model-risk.md)). |
| NFR-13 | Cost governance | Measure cost per decision by LOB/tier; budgets + circuit-breakers; routing/caching to control spend (see [doc 14](14-cost-governance.md)). |

## 1.8 User stories & acceptance criteria

**US-1 — Triage a clean file**
> As an underwriter, I submit a complete, low-risk file and get an `APPROVE` recommendation
> so I can bind quickly.
- **AC-1.1** Given a complete submission with no knockouts and risk weight < threshold, the outcome is `APPROVE`.
- **AC-1.2** The response includes an indicative premium and an audit trail.

**US-2 — Catch a condition-precedent breach**
> As an underwriter, when a file's inspection interval exceeds 72 hours, I want it flagged as
> decisive so I never bind a deniable risk by accident.
- **AC-2.1** Inspection interval > 72h produces a `KNOCKOUT` finding `INSPECTION_INTERVAL_BREACH`.
- **AC-2.2** The outcome is `DECLINE` and the conditions list includes attaching a 72-hour condition precedent.
- **AC-2.3** Inspection interval = 72h does **not** raise the knockout.

**US-3 — Refer an incomplete or elevated file**
> As an underwriter, when data is missing/contradictory or risk is elevated, I want it
> referred to me rather than auto-decided.
- **AC-3.1** Any missing required field, data contradiction, or unresolved location yields `REFER`.
- **AC-3.2** A no-knockout file with risk weight ≥ 6 yields `REFER`.

**US-4 — Understand the reasoning**
> As an underwriter, I want a short written rationale and a factor list so I can agree or
> override with full sight of the reasoning.
- **AC-4.1** The rationale leads with any condition-precedent breach.
- **AC-4.2** Each finding has a human-readable `message` and `rationale`.

**US-5 — Screen for remoteness**
> As an underwriter, I want remote properties flagged because response times and inspection
> compliance are harder.
- **AC-5.1** A property >100 km from every major city is flagged `REMOTE_LOCATION` (HIGH).
- **AC-5.2** A property within range is recorded as `LOCATION_WITHIN_RANGE` (INFO).
- **AC-5.3** An unresolvable location yields `LOCATION_UNRESOLVED` and forces referral.

**US-6 — Use AI for the narrative, safely**
> As an operator, I want to optionally enable Claude for a richer rationale without changing
> the decision or risking an outage.
- **AC-6.1** With no API key, the service runs and uses the offline reasoner.
- **AC-6.2** With an API key, Claude writes the rationale; the deterministic outcome is unchanged.
- **AC-6.3** If the Claude call fails, the decision still returns using the offline reasoner.

## 1.9 Future scope (post-v1.0)

Persistence + decision-history endpoint; authn/authz; a web underwriting workbench; LLM/OCR
document extraction; real rating tables; configurable risk appetite per program; additional
lines of business; reviewer feedback capture for model/rule improvement.

## 1.10 Assumptions, constraints, risks

- **Assumption:** submissions are normalized to the documented schema (or to text the extractor can parse).
- **Constraint:** decision-support only — the bind decision and policy issuance happen in the PAS.
- **Risk:** rule thresholds encode underwriting appetite and must be reviewed by UW leadership before production use.
- **Risk:** the bundled extractor and city table are coarse; production needs richer extraction and a maintained geo dataset.

## 1.11 Glossary

See [02-architecture-design.md §10](02-architecture-design.md#10-glossary) for the shared glossary
(knockout, condition precedent, PR0003, Supervisory Warranty, remoteness, etc.).
