# ADR-0016: Deployment, DevOps & environments

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0012](0012-resilience-dr.md), [ADR-0013](0013-ai-governance-model-risk.md), [ADR-0015](0015-testing-evaluation.md), [doc 16](../16-deployment-devops.md)

## Context

The system must ship frequently and safely under regulatory constraints: every change to code,
config, rules, models or prompts must pass quality/fairness/security gates, be reversible, and run
within Canadian data-residency boundaries.

## Decision

1. **Containerized + Kubernetes** (managed, Canadian region), multi-AZ, autoscaled, with
   liveness/readiness probes and rolling/blue-green deploys.
2. **Four environments** (dev/test/staging/prod) with per-environment externalized config and
   secrets; synthetic/masked data outside prod.
3. **Gated CI/CD** — build/tests → security scans → AI eval (no regression) → fairness → red-team →
   governance sign-off (for rule/model/prompt changes) → canary/blue-green with auto-rollback.
4. **Config & logic as governed releases** — rules/thresholds/prompts/LOB modules and authZ
   policies are versioned artifacts promoted through the same gates; feature flags for staged
   enablement.
5. **IaC + GitOps** — all infra and policy as code (Terraform/GitOps); reproducible envs and DR.
6. **Pipeline security** — SAST, dependency/container/secret scanning, SBOM, signed images,
   PII-egress tests, least-privilege deploy creds, audited deploys.

## Consequences

**Positive**
- Frequent, safe, reversible delivery; regulated changes provably gated and audited.
- Reproducible environments and fast DR stand-up; residency enforced in code.
- No "config hotfix" bypass — threshold/prompt changes are governed releases.

**Negative / trade-offs**
- Upfront platform investment (k8s, IaC, pipelines) — phase it; start simpler if volume is low.
- Strict gates add release latency — automation keeps it acceptable.

## Alternatives considered

- **Manual/scripted deploys.** Rejected — not reproducible, auditable, or safe for regulated changes.
- **Treat rules/prompts as ungoverned config.** Rejected — they change decisions; govern them like code.
- **Single shared environment.** Rejected — no safe place to test/shadow before prod.
- **Serverless-only.** Viable for parts, but the stateful workflow/broker/datastore favour a
  managed-k8s baseline; revisit per-component.
