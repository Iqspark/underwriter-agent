# AI Underwriter Agent

An **AI-first**, multi-line **property & casualty (P&C) underwriting** decision-support agent
built on Spring Boot (Java 21). It risk-assesses insurance submissions across lines of business
and recommends `APPROVE` / `REFER` / `DECLINE` with an indicative price and a cited rationale for
a human underwriter. It mirrors the architecture insurtechs are shipping today — a pipeline of
specialist agents coordinated by a decision orchestrator — with one central idea: **the agent
learns from the book of business.** **Vacant home (Canadian vacant-property) is the first line
built and the worked reference example** — the agent is line-agnostic by design (see the
Line-of-Business plug-in model).

For each submission it retrieves the most similar past policies (case-based / k-NN), reads how
they actually performed (claims, loss ratios, perils), and folds in area-level theft/claim
signals — and that learned evidence drives the risk view and the price. Deterministic rules are
kept only as **guardrails** (completeness, geographic eligibility / remoteness screens, and
condition-precedent **knockouts** — in the vacant-home line, the 100 km screen and the decisive
72-hour inspection warranty); the orchestrator takes the **most conservative** of the learned and
guardrail outcomes. A pluggable **LLM reasoner** writes a rationale grounded in the comparable
cases but never makes the decision. The service *recommends*; a human underwriter stays in the loop.

See [`docs/05-ai-learning-design.md`](docs/05-ai-learning-design.md) for the learning design.

## Documentation

Full design docs live in [`docs/`](docs/README.md): Requirements/BRD, Architecture & Design
(HLD) with Mermaid diagrams, API specification, ADRs, and an Operations runbook.

## Architecture

```
                                DecisionOrchestrator
                                   │  (runs agents in order; blends learned + guardrail; assembles decision)
   ┌───────────┬──────────────────┼───────────────────┬────────────────┬──────────────┐
   ▼           ▼                  ▼                   ▼                ▼              ▼
IntakeAgent  RiskProfiling   PatternLearning      ComplianceAgent  PricingAgent  (LlmReasoner)
            │ RulesEngine    │ AI-first core       checks knockouts  rate from      narrates over
            │ (guardrails)   │ learns from book    (72h warranty)    comparables    comparables
            ▼                ▼
   CompletenessRule,    SimilarityEngine (k-NN, Gower) + AreaRiskService
   RemotenessRule,      over HistoricalPolicyRepository (the book)
   ConditionPrecedent,  → LearnedAssessment: claim prob, loss ratio,
   RiskFactorRule          fair rate, dominant peril, area theft, comparables
```

Every agent appends to a shared **audit trail**, so each decision ships with an ordered,
regulator-ready trace plus the comparable cases that justify it.

### Decision policy (most conservative of two layers)

| Layer | `DECLINE` | `REFER` |
|-------|-----------|---------|
| **Guardrails** (rules) | any knockout (e.g. inspection > 72h) | missing/contradictory data, unresolved location, or rule risk weight ≥ 6 |
| **Learned** (data) | claim prob ≥ 0.70 or loss ratio ≥ 1.5 | claim prob ≥ 0.55 or loss ratio ≥ 1.0 |

Otherwise `APPROVE`. Knockouts always carry a *curing condition* so an underwriter can convert a
decline to a conditional bind. On a **cold-start** book the learned layer is neutral and
guardrails decide.

## Running it

```bash
mvn spring-boot:run
# or
mvn package && java -jar target/underwriter-agent-1.0-SNAPSHOT.jar
```

The service runs **fully offline by default** using the deterministic template reasoner.
To use Claude for the written rationale, set an API key (the rest of the logic is unchanged):

```bash
# Windows PowerShell
$env:UNDERWRITER_LLM_ANTHROPIC_API_KEY="sk-ant-..."
# bash
export UNDERWRITER_LLM_ANTHROPIC_API_KEY="sk-ant-..."
```

## API

| Method | Path                              | Body                | Returns    |
|--------|-----------------------------------|---------------------|------------|
| POST   | `/api/underwriting/submissions`   | `Submission` JSON   | `Decision` (incl. `learnedAssessment`) |
| POST   | `/api/underwriting/documents`     | raw `text/plain`    | `Decision` |
| GET    | `/api/underwriting/health`        | —                   | status     |
| GET    | `/api/underwriting/history/stats` | —                   | book size + claim rate |
| GET    | `/api/underwriting/history/areas/{city}` | —            | `AreaRiskStat` for an area |
| POST   | `/api/underwriting/history/comparables` | `Submission` JSON | `LearnedAssessment` (comparables preview) |

### Examples

```bash
# Structured submission (this one refers — remote location, old roof, no security)
curl -s -X POST http://localhost:8080/api/underwriting/submissions \
  -H "Content-Type: application/json" \
  --data @samples/submission-refer.json | jq

# Clean file (approves on standard terms)
curl -s -X POST http://localhost:8080/api/underwriting/submissions \
  -H "Content-Type: application/json" \
  --data @samples/submission-approve.json | jq

# 168-hour inspection + multiple prior losses (declines)
curl -s -X POST http://localhost:8080/api/underwriting/submissions \
  -H "Content-Type: application/json" \
  --data @samples/submission-decline.json | jq

# Raw quote-summary text (96h inspection -> knockout/decline after extraction)
curl -s -X POST http://localhost:8080/api/underwriting/documents \
  -H "Content-Type: text/plain" \
  --data-binary @samples/quote-summary.txt | jq
```

## Sample decision (shape)

```json
{
  "reference": "TRM-VH-2026-0142",
  "outcome": "REFER",
  "riskScore": 16,
  "findings": [
    { "code": "REMOTE_LOCATION", "severity": "HIGH", "category": "LOCATION",
      "message": "Remote: 315 km from nearest major city (Toronto)",
      "rationale": "More than 100 km from every major city — slower emergency response ...",
      "source": "RemotenessRule" }
  ],
  "conditions": [ "Confirm fire-response arrangements given the remote location; ..." ],
  "indicativePremium": { "amount": 3477.60, "currency": "CAD" },
  "rationale": "Recommended action: REFER. Learning from the book: the 25 most similar past policies had ...",
  "learnedAssessment": {
    "comparableCount": 25, "claimProbability": 0.48, "expectedLossRatio": 0.71,
    "suggestedRatePerThousand": 6.95, "dominantPeril": "THEFT", "confidence": "HIGH",
    "topComparables": [ { "policyId": "HP-00231", "similarity": 0.94, "hadClaim": true } ],
    "areaRisk": { "city": "Sudbury", "theftClaimRate": 0.28, "overallClaimRate": 0.41 }
  },
  "auditTrail": [ "... | PatternLearningAgent | Learned from 25 comparables ..." ],
  "decidedAt": "2026-06-19T12:00:00Z"
}
```

## Extending it

- **Use your real book** → replace generation in `HistoricalPolicyRepository` with a reader over
  real policy + claims data (CSV/JDBC/warehouse) that yields `HistoricalPolicy`. Everything
  downstream (similarity, area stats, pricing, decisioning) is source-agnostic.
- **Tune learning** → `underwriter.history.size/seed`, `underwriter.similarity.k`, the feature
  weights in `SimilarityEngine`, and the learned thresholds in `DecisionOrchestrator` / `PatternLearningAgent`.
- **New guardrail rule** → add it to the relevant `resources/rules/*.yml` pack (rules are data,
  ADR-0018); only a genuinely new derived fact or operator needs code (in `FactExtractor` /
  `ConfigurableRulesEngine`). Add a unit test.
- **New agent** → implement `UnderwritingAgent` with an `order()`; the orchestrator runs it.
- **Trained model** → add an ML claim-probability/price signal behind the assessment seam, keeping
  comparables for explainability.

## Project layout

```
src/main/java/com/iqspark/underwriter
├─ UnderwriterAgentApplication.java
├─ api/            REST controllers (underwriting + history), OpenApiConfig, GlobalExceptionHandler
├─ agent/          Intake, RiskProfiling, PatternLearning, Compliance, Pricing agents,
│                  DecisionOrchestrator, UnderwritingContext
├─ history/        HistoricalPolicyRepository, SyntheticHistoryGenerator, SimilarityEngine,
│                  AreaRiskService, FeatureRanges, model/* (HistoricalPolicy, LearnedAssessment, ...)
├─ rules/          ConfigurableRulesEngine, FactExtractor, config/* (RuleConfigLoader, RuleDefinition, Condition)
├─ geo/            GeoService (haversine remoteness / 100 km rule)
├─ extraction/     DocumentExtractor + KeyValueDocumentExtractor
├─ llm/            LlmReasoner (template offline + Anthropic Claude) + LlmReasoningException
├─ persistence/    DecisionEntity/AuditEventEntity, repositories, DecisionStore, StoredDecision
├─ metrics/        DecisionMetrics (Micrometer)
└─ domain/         model/* (Submission, ...), decision/* (Decision, Finding, ...), audit/*

src/main/resources/rules/   shared.yml + vacant-home.yml, rental.yml, contents.yml (rules as data)
```

> Note: this is decision-support tooling. It informs a human underwriter's decision and
> never finalizes one. Where data is missing, contradictory, or the location can't be
> confirmed, the correct output is to flag and refer — not to guess.
