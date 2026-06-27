# ADR-0002: Multi-agent pipeline with a decision orchestrator

**Status:** Accepted
**Date:** 2026-06-19

## Context

Underwriting a submission involves distinct concerns — intake, risk profiling, compliance,
pricing — that the 2025–2026 insurtech pattern models as specialist agents coordinated by an
orchestrator. We want that separation of concerns without distributed-systems overhead at
this stage.

## Decision

Implement an in-process pipeline: each concern is an `UnderwritingAgent` with an `order()`,
operating on a shared `UnderwritingContext`. A `DecisionOrchestrator` runs the agents in order,
then derives the outcome, conditions and rationale and assembles the `Decision`. Agents
communicate only through the context and the audit trail.

## Consequences

**Positive**
- Clear single responsibility per agent; easy to test and reason about.
- New stages slot in by adding an agent and an `order()` — no orchestrator edits.
- The shared audit trail yields an ordered, regulator-ready trace for free.

**Negative / trade-offs**
- In-process and sequential: no parallelism or independent scaling of agents in v1.0.
- The shared mutable context must be used carefully (single submission per pipeline instance,
  not thread-shared).

## Alternatives considered

- *Distributed agents (queue/service per agent)* — rejected as premature; revisit if stages
  need independent scaling or long-running external calls.
- *Single monolithic service method* — rejected: poor separation, harder to test/extend.
