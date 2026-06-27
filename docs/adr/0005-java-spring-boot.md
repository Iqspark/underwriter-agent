# ADR-0005: Java 21 + Spring Boot, JDK HttpClient for the LLM call

**Status:** Accepted
**Date:** 2026-06-19

## Context

The repository was scaffolded as a Maven project targeting Java 21. We need a web service with
JSON binding, dependency injection (for rule/agent discovery), and testing support, plus an
optional outbound HTTPS call to the Anthropic API.

## Decision

- Build on **Spring Boot 3.3.x** (`web`, `validation`, `test`) — DI, REST, Jackson and test
  tooling out of the box, and a natural fit for component auto-discovery (ADR-0004).
- Target **Java 21**; use **records** for immutable domain/decision types.
- Use the **JDK `java.net.http.HttpClient`** for the Anthropic call rather than adding an HTTP
  client dependency — the call is small and well-defined.
- Use **JUnit 5 + AssertJ** (bundled with the test starter) for tests.

## Consequences

**Positive**
- Minimal dependency surface; no extra HTTP/JSON libraries.
- Records give concise, immutable, Jackson-friendly models (with `-parameters`, enabled by the
  Spring Boot compiler defaults, Jackson binds records directly).
- Aligns with the existing project's Java level and build tool.

**Negative / trade-offs**
- Spring Boot adds startup weight vs. a bare-bones framework — acceptable for the productivity
  and ecosystem benefits.
- Hand-rolled Anthropic client means we own request/response shaping and version headers (kept
  small and centralized in `AnthropicLlmReasoner`).

## Notes

- The Anthropic API version header (`anthropic-version`) and model id are centralized; update
  them in one place as the API evolves.
- Build/run details are in the [Operations & Runbook](../04-operations-runbook.md).
