# ADR-0018: Config-driven rules (rules as data, not code)

**Status:** Accepted
**Date:** 2026-06-20
**Related:** [ADR-0004](0004-rule-autodiscovery.md) (superseded), [ADR-0009](0009-line-of-business-plugin.md), [doc 02 §5](../02-architecture-design.md)

## Context

Underwriting rules change often as appetite evolves. Previously each rule was a hard-coded Java
`@Component` (`CompletenessRule`, `RemotenessRule`, `ConditionPrecedentRule`, `RiskFactorRule`,
`FireProtectionRule`, `CoverageAdequacyRule`). Every threshold tweak, severity change, or new
simple rule meant a code change, recompile and redeploy — and required a developer. We want rule
changes to be **configuration**, editable (and reviewable) without touching code.

Pure-declarative config can't express everything (geographic remoteness via haversine, $/sqft,
and line-specific derived values such as the vacant-home line's months-vacant), so some
computation must stay in code — but as *facts*, not *rules*.

## Decision

Move the rule **logic into YAML rule files, split by line of business** (`rules/shared.yml` for
all lines, plus one `rules/<line>.yml` per line), evaluated by a small, dependency-free
**config-driven engine**:

1. **`FactExtractor`** (code) flattens a `Submission` into named facts, computing the few derived
   values rules can't (geo remoteness, perSqft, line-specific derived values such as the vacant-
   home line's monthsVacant, presence/contradiction flags).
2. **Rule files** (`rules/shared.yml` + per-line `rules/<line>.yml`): each rule is data — `id`,
   optional `line` (defaults to the file's line), `code`,
   `category`, `severity`, an `all:` list of conditions (`{fact, op, value}`), and
   `message`/`rationale` with `{fact}` interpolation. Operators: `eq, ne, gt, gte, lt, lte,
   eqIgnoreCase, isTrue, isFalse, isNull, isNotNull`.
3. **`ConfigurableRulesEngine`** runs the rules that apply to the submission's line and whose
   conditions all hold, emitting `Finding`s. It replaces the old `Rule`/`RulesEngine` classes.
4. **No new dependency** — SnakeYAML (already shipped with Spring Boot) parses the file; a simple
   typed condition DSL is used instead of SpEL/Drools (no expression-injection risk, no Maven
   Central dependency, easy to reason about).
5. **External override** — `underwriter.rules.dir` points at a directory (same file names) to edit
   the rule set without rebuilding; otherwise the classpath `rules/` is used.

Line-of-business scoping moves from the old `Rule.appliesTo(...)` method to the `line:` field in
config (omit for shared/all-lines), so adding a line's rules is also config-only.

## Consequences

**Positive**
- Threshold/severity/message changes and simple new rules are **config edits**, no code change;
  fail-fast validation at startup.
- Underwriting appetite is visible and reviewable in one file; pairs naturally with the
  model-governance change gate ([doc 13](../13-ai-governance-model-risk.md)).
- Decision logic stays deterministic and testable; facts (incl. geo) remain unit-tested in code.

**Negative / trade-offs**
- A new operator or a genuinely new *derived fact* still needs a (small) code change in the engine
  or `FactExtractor` — but that's rare versus threshold/appetite changes.
- The typed DSL has no OR within a rule; an "OR" is expressed as two rules (acceptable, and
  explicit).
- Authy of the YAML must be governed (a bad edit changes decisions) — handled by the change gate.

## Alternatives considered

- **Keep rules in Java (ADR-0004).** Rejected — every appetite change needs a developer + redeploy.
- **SpEL expressions in config.** More expressive, but injection/footgun risk and harder to verify
  without a compiler; the typed DSL covers every current rule.
- **Drools / a full rules engine.** Powerful but heavyweight, another dependency (unresolvable in
  the restricted sandbox), and overkill for these conditions. Revisit only if business users need a
  richer authoring environment.
