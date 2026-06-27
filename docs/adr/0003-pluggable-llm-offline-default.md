# ADR-0003: Pluggable LLM reasoner, offline by default

**Status:** Accepted
**Date:** 2026-06-19

## Context

We want an optional, richer LLM-written rationale (Anthropic Claude) without making the service
depend on network access, an API key, or a paid call to function. The system must run and be
testable fully offline, and must never fail a decision because of an LLM problem.

## Decision

Define an `LlmReasoner` interface with two implementations:

- `TemplateLlmReasoner` — deterministic, offline, always present (the default).
- `AnthropicLlmReasoner` — `@ConditionalOnProperty(underwriter.llm.anthropic.api-key)` and
  `@Primary`, so it is created and preferred only when an API key is configured.

The `DecisionOrchestrator` injects the `@Primary` reasoner **plus** the `TemplateLlmReasoner`
explicitly as a fallback, and catches `LlmReasoningException` to degrade gracefully.

## Consequences

**Positive**
- Zero-config, fully-offline operation; no key needed to run or test.
- Enabling Claude is a single environment variable; the decision is unchanged.
- LLM failures degrade to the offline rationale; decisions never fail.

**Negative / trade-offs**
- Two rationale styles to maintain.
- When Claude is enabled, finding text is sent to Anthropic — must be cleared against data-
  handling policy (noted in HLD §9).

## Implementation note (bug avoided)

Injecting `List<LlmReasoner>` and taking `get(0)` does **not** honor `@Primary` (list order is
registration order). We therefore inject the primary `LlmReasoner` by type (which honors
`@Primary`) and the concrete `TemplateLlmReasoner` separately for the fallback.
