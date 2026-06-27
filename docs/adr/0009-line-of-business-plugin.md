# ADR-0009: Line-of-Business plug-in architecture (multi-line)

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0002](0002-multi-agent-pipeline.md), [ADR-0004](0004-rule-autodiscovery.md), [ADR-0006](0006-case-based-learning.md), [doc 9](../09-multi-line-architecture.md)

## Context

The agent is a generic, multi-line property & casualty (P&C) underwriter by design — vacant home
is simply the first line built and the worked reference example. It must cover many lines — vacant
home, farm, rental/landlord, contents/personal belongings, and future lines. Each line has
different risk fields, rules, knockouts, knowledge (wordings) and rating, but they share
location/peril enrichment, sanctions, the learning machinery, RAG, the orchestrator, guardrails
and audit. We need multi-line scope as the default — not a vacant-home product later generalized —
without a per-line rewrite and without weakening the existing guarantees.

## Decision

Adopt a **generic core + pluggable Line-of-Business (LOB) modules** ("product factory"):

1. **Generic, LOB-agnostic core** — `RulesEngine`, `SimilarityEngine`, RAG, pricing orchestration,
   `DecisionOrchestrator`, evaluator, audit and MCP enrichment stay shared and unchanged.
2. **`LineOfBusiness` SPI** — each line is a Spring `@Component` supplying its `RiskDetails` type,
   required fields, `FeatureExtractor`, LOB-specific `Rule`s (incl. knockouts), knowledge sources,
   and a `Rater`. The core injects `List<LineOfBusiness>` and dispatches by `id()` — the same
   auto-discovery pattern already used for `Rule` and `UnderwritingAgent`.
3. **Sealed `RiskDetails`** — a `sealed interface` with one record per line on the `Submission`
   envelope (common fields stay on the envelope). Type-safe and explicit.
4. **LOB-partitioned data** — the historical book and RAG corpus carry a `lineOfBusiness` tag;
   comparable retrieval and document retrieval are filtered to the same line.
5. **Shared cross-line services** — geocoding, peril/crime/flood/wildfire scoring, property data,
   sanctions/AML are written once in the core (via MCP) and serve every line.

Vacant home (Canadian vacant-property) is built as the first module and the reference example;
rental, contents and farm follow — the core stays line-agnostic throughout.

## Consequences

**Positive**
- New lines are added as modules, not core changes — the core is touched once to introduce the SPI.
- Shared enrichment/learning/RAG/decisioning are reused across all lines.
- Sealed `RiskDetails` keeps each line explicit and type-safe; pattern matching is clean.
- Per-line rules/knockouts/knowledge/rating are isolated and independently testable.

**Negative / trade-offs**
- One upfront refactor to extract the abstraction and move generic rules out of the vacant-home set.
- Adding a line still requires a new `RiskDetails` record + extractor + rules + knowledge + rater
  (intended — it's the line's real content), unlike a fully dynamic map-based model.
- Per-line cold-start until each line has enough history (mitigated by the existing fallback).

## Alternatives considered

- **Map-based attributes (`Map<String,Object>`) instead of sealed `RiskDetails`** — fully dynamic,
  no code per line, but loses type safety, validation and clarity; rejected for a regulated domain.
- **One model/rule set for all lines** — simplest, but conflates very different risks and knockouts;
  rejected as inaccurate and non-compliant.
- **Separate service per line** — clean isolation but massive duplication of the shared core and
  ops overhead; rejected in favour of one core with modules.
- **Config/DSL-driven products (e.g. rules in external config)** — powerful for business-authored
  products; deferred — can layer on top of the SPI later if non-engineers need to author lines.
