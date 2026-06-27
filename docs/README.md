# AI Underwriter Agent — Documentation

This folder contains the full design and reference documentation for the **AI Underwriter
Agent**, an **AI-first, multi-line property & casualty (P&C)** decision-support service that
risk-assesses insurance submissions across lines of business and recommends `APPROVE` / `REFER` /
`DECLINE` for a human underwriter. **Vacant home (Canadian vacant-property) is the first line
built and the worked reference example** — the agent is line-agnostic by design (see
[doc 9](09-multi-line-architecture.md)).

## Document index

| # | Document | Audience | What it covers |
|---|----------|----------|----------------|
| 0 | [**Architecture Overview**](00-architecture-overview.md) | Everyone | **Start here** — the whole picture, system context, principles, reading guide |
| 1 | [Requirements / BRD](01-requirements-brd.md) | Product, business, UW leadership | Why we're building it, scope, functional & non-functional requirements, user stories, acceptance criteria |
| 2 | [Architecture & Design (HLD)](02-architecture-design.md) | Engineers, architects | System architecture, multi-agent pipeline, components, data flow, decision policy, extensibility, security |
| 3 | [API Specification](03-api-specification.md) | API consumers, integrators | Endpoints, request/response schemas, field dictionary, errors, examples |
| 4 | [Operations & Runbook](04-operations-runbook.md) | Developers, operators | Build, run, configure, deploy, observe, troubleshoot |
| 5 | [AI-First Learning Design](05-ai-learning-design.md) | Engineers, data, underwriting | Case-based learning: similarity model, prediction, area risk, the synthetic book, how learning drives decision & price |
| 6 | [RAG Pipeline Design (Spring AI)](06-rag-design.md) | Engineers, data, underwriting | *Proposed.* Retrieval-augmented grounding: corpus, ingestion, retrieval, advisory agent, cited rationale, offline ONNX embeddings + **pgvector** store |
| 7 | [Target-State Architecture](07-target-architecture.md) | Leadership, architects, UW/ops | *Vision.* AI-maximized lifecycle: MCP enrichment, orchestrator+evaluator, autonomy tiers/STP, AI-ops platform, data flywheel, roadmap |
| 8 | [**Recommended Solution**](08-recommended-solution.md) | Everyone | **The committed decision:** stack, decision model, autonomy defaults, delivery order, immediate next step. Start here. |
| 9 | [Multi-Line Architecture](09-multi-line-architecture.md) | Engineers, architects, product | Line-of-Business plug-in model: generic core + pluggable modules (vacant home, farm, rental, contents) |
| 10 | [Runtime, Audit & Observability](10-runtime-audit-observability.md) | Engineering, SRE, compliance | The operational backbone: event-driven/queue runtime, durable workflow + HITL, persistent tamper-evident audit, metrics & dashboards |
| 11 | [Security, AuthZ & PII / Privacy](11-security-privacy.md) | Engineering, security, compliance | AuthN/authZ (RBAC+ABAC, authority limits), PII handling, AI-specific PII & prompt-injection defenses, secrets, threat model, compliance |
| 12 | [Resilience & Disaster Recovery](12-resilience-dr.md) | Engineering, SRE, BC | SLOs/RTO/RPO, graceful-degradation matrix, HA/multi-AZ + cross-region DR, backups, resilience patterns |
| 13 | [AI Governance & Model Risk](13-ai-governance-model-risk.md) | Model risk, compliance, UW | Model inventory/cards, change-management eval+fairness gate, bias/proxy testing, drift, oversight |
| 14 | [Cost Governance (AI FinOps)](14-cost-governance.md) | Eng, finance, product | Cost drivers, routing/caching/budgets, cost-per-decision & unit economics, alerting |
| 15 | [Testing, Evaluation & Quality](15-testing-evaluation-quality.md) | Eng, QA, data/ML | Test pyramid + AI eval harness, red-teaming, contract/load tests, shadow & champion-challenger, CI gates |
| 16 | [Deployment, DevOps & Environments](16-deployment-devops.md) | Eng, SRE, security | Containers/k8s, gated CI/CD, IaC/GitOps, environments, blue-green/canary, rollback, residency |
| 17 | [Data & Integration Architecture](17-data-integration.md) | Eng, data, integration | Data stores, lineage/quality, the flywheel pipeline, external integrations & contracts |
| 18 | [Traceability, Assumptions & Open Questions](18-traceability-assumptions.md) | Architects, product, compliance | Requirements→design→ADR→status matrix; assumptions register; open decisions the org owns |
| — | [Architecture Decision Records](adr/) | Engineers, architects | The key decisions and their rationale |

## Diagrams

Rendered inline in the documents above (Mermaid). The source files also live standalone in
[`diagrams/`](diagrams/) so they can be pasted into any Mermaid tool:

- [`architecture.mermaid`](diagrams/architecture.mermaid) — container/component view
- [`pipeline-sequence.mermaid`](diagrams/pipeline-sequence.mermaid) — request processing sequence
- [`decision-flow.mermaid`](diagrams/decision-flow.mermaid) — the decision policy
- [`learning-flow.mermaid`](diagrams/learning-flow.mermaid) — the case-based learning flow
- [`rag-pipeline.mermaid`](diagrams/rag-pipeline.mermaid) — the RAG pipeline (proposed)
- [`target-architecture.mermaid`](diagrams/target-architecture.mermaid) — the AI-maximized target state (vision)
- [`lob-architecture.mermaid`](diagrams/lob-architecture.mermaid) — the multi-line (LOB) plug-in model
- [`runtime-architecture.mermaid`](diagrams/runtime-architecture.mermaid) — event-driven runtime, audit & observability
- [`security-architecture.mermaid`](diagrams/security-architecture.mermaid) — security trust boundaries, authZ & PII controls
- [`degradation-matrix.mermaid`](diagrams/degradation-matrix.mermaid) — graceful degradation / fail-to-floor
- [`model-governance-lifecycle.mermaid`](diagrams/model-governance-lifecycle.mermaid) — model change-management gate & lifecycle
- [`system-context.mermaid`](diagrams/system-context.mermaid) — C4 system context
- [`cicd-pipeline.mermaid`](diagrams/cicd-pipeline.mermaid) — CI/CD with quality/fairness/security gates
- [`data-integration.mermaid`](diagrams/data-integration.mermaid) — data stores, integrations & the flywheel
- [`domain-model.mermaid`](diagrams/domain-model.mermaid) — domain objects

## One-paragraph summary

The agent is **AI-first**: it learns from the book of business. A submission enters through a
REST API and is processed by a pipeline of specialist agents (intake, risk profiling, **pattern
learning**, compliance, pricing) coordinated by a **decision orchestrator**. The
`PatternLearningAgent` retrieves the most similar past policies, reads how they actually
performed (claims, loss ratios, perils), and folds in area-level theft/claim signals — that
learned evidence drives the risk view and the price. A **deterministic rules engine** remains as
*guardrails* (completeness, geographic eligibility screens, and condition-precedent knockouts —
e.g. the vacant-home line's 72-hour inspection warranty), and the orchestrator
takes the **most conservative** of the learned and guardrail outcomes. A **pluggable LLM
reasoner** writes the human-readable rationale grounded in the comparable cases but never makes
the decision. The service *recommends*; a human underwriter always stays in the loop.

## Status & provenance

- **Status legend:** docs marked **Baseline v1.0** describe what's **built**; **Recommended /
  Proposed / Vision** docs are **designed, not built**. Only **Phase 0** is implemented today
  (vacant-home decision core: rules + k-NN + pricing + in-memory audit + REST API + tests).
  Everything else (RAG, enrichment, event runtime, persistence, security, multi-line, dashboards)
  is designed — see [doc 8 §5](08-recommended-solution.md#5-the-committed-delivery-order).
- Reference implementation; design grounded in 2025–2026 insurtech agentic-underwriting
  patterns and the TRM vacant-property underwriting playbook.
- **AI-first / case-based learning is the primary risk driver — see [ADR-0006](adr/0006-case-based-learning.md)
  and [doc 5](05-ai-learning-design.md).** Rules act as guardrails (ADR-0001).
- The historical book is a deterministic synthetic dataset today; replace with real
  policy/claims data to go production-grade (doc 5 §7).
