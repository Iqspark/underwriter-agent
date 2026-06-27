# ADR-0004: Rule auto-discovery via Spring components

**Status:** Accepted
**Date:** 2026-06-19

## Context

Underwriting rules change often as appetite evolves. We want adding or removing a rule to be a
low-risk, localized change that can't break unrelated logic, and that is easy to test in
isolation.

## Decision

Model each rule as a `Rule` implementation annotated `@Component`. `RulesEngine` receives
`List<Rule>` by constructor injection — Spring discovers every rule bean automatically — and
runs them all, concatenating findings. Adding a rule means adding one class and its unit test;
no central registry or wiring edit.

## Consequences

**Positive**
- Open/closed: extend behavior by adding a class, not editing the engine.
- Each rule is independently unit-testable (no Spring context required).
- Findings are uniform (`Finding`), so downstream scoring/ordering is rule-agnostic.

**Negative / trade-offs**
- Rule execution order is not guaranteed; rules must be order-independent (they are — each is a
  pure function of the submission, and findings are aggregated, then ordered by severity).
- No built-in per-rule enable/disable or config-driven thresholds yet (future: externalize
  thresholds to configuration).

## Alternatives considered

- *Central rule registry / explicit list* — rejected: every change touches shared code.
- *External rules DSL (e.g. Drools)* — rejected as over-engineered for v1.0; revisit if business
  users need to author rules without code.
