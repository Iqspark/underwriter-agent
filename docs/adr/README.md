# Architecture Decision Records (ADRs)

Each ADR captures one significant decision: its context, the decision, and its consequences.
Format is lightweight (after Michael Nygard). Status values: Proposed, Accepted, Superseded.

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-rules-decide-llm-explains.md) | Rules decide, the LLM only explains | Accepted |
| [0002](0002-multi-agent-pipeline.md) | Multi-agent pipeline with a decision orchestrator | Accepted |
| [0003](0003-pluggable-llm-offline-default.md) | Pluggable LLM reasoner, offline by default | Accepted |
| [0004](0004-rule-autodiscovery.md) | Rule auto-discovery via Spring components | Accepted |
| [0005](0005-java-spring-boot.md) | Java 21 + Spring Boot, JDK HttpClient for LLM | Accepted |
| [0006](0006-case-based-learning.md) | AI-first, case-based learning from the historical book | Accepted |
| [0007](0007-rag-spring-ai.md) | RAG via Spring AI (advisory, offline-first) | Accepted (baseline built) |
| [0008](0008-ai-maximized-architecture.md) | AI-maximized, human-augmenting target architecture | Proposed |
| [0009](0009-line-of-business-plugin.md) | Line-of-Business plug-in architecture (multi-line) | Proposed |
| [0010](0010-event-driven-runtime.md) | Event-driven runtime, durable audit & observability | Accepted (lean tier built) |
| [0011](0011-security-privacy.md) | Security, authorization & PII / privacy | Proposed |
| [0012](0012-resilience-dr.md) | Resilience & disaster recovery | Proposed |
| [0013](0013-ai-governance-model-risk.md) | AI governance & model risk management | Proposed |
| [0014](0014-cost-governance.md) | Cost governance (AI FinOps) | Proposed |
| [0015](0015-testing-evaluation.md) | Testing & evaluation strategy (incl. shadow mode) | Proposed |
| [0016](0016-deployment-devops.md) | Deployment, DevOps & environments | Proposed |
| [0017](0017-data-integration.md) | Data & integration architecture | Proposed |
| [0018](0018-config-driven-rules.md) | Config-driven rules (rules as data, not code) | Accepted |
| [0019](0019-phase1-persistence-metrics.md) | Phase 1 — persistence, durable audit & baseline metrics | Accepted |
| [0020](0020-hybrid-predictive-model.md) | Hybrid predictive model — GBM predicts, k-NN explains | Accepted (offline baseline built) |
| [0021](0021-semantic-feature-extraction.md) | Semantic feature extraction from unstructured documents | Proposed |
| [0022](0022-reviewer-agent.md) | Reviewer agent — the LLM "skeptical underwriter" (evaluator, formalized) | Accepted (built) |
| [0023](0023-knn-scalability-ann.md) | k-NN scalability — ANN index over pgvector | Proposed |
| [0024](0024-phase1-baseline-security.md) | Phase 1 — baseline security (dual-mode auth, RBAC, authority limits, PII redaction) | Accepted |
| [0025](0025-autonomy-tiers-stp.md) | Autonomy tiers / straight-through-processing routing | Accepted (built) |
| [0026](0026-mcp-enrichment.md) | MCP enrichment (tool boundary, offline-first, degrade-to-floor) | Accepted (offline baseline built) |
| [0027](0027-dashboards-flywheel.md) | Dashboards, business metrics & the data flywheel | Accepted (baseline built) |
| [0028](0028-phase5-intake-drafting.md) | Phase 5 — drafting (and multimodal intake scope) | Accepted (drafting built; multimodal deferred) |
