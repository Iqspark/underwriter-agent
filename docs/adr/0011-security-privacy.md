# ADR-0011: Security, authorization & PII / privacy

**Status:** Proposed
**Date:** 2026-06-19
**Related:** [ADR-0008](0008-ai-maximized-architecture.md), [ADR-0010](0010-event-driven-runtime.md), [doc 11](../11-security-privacy.md)

> Not legal advice — confirm compliance mappings with legal/privacy & compliance.

## Context

The system holds personal and risk data and makes regulated decisions for a Canadian MGA, so it
needs strong authN/authZ, encryption and audit. Adding LLMs + RAG introduces AI-specific risks —
PII leakage into prompts/embeddings/logs, prompt injection from untrusted broker documents, and
over-broad agent/tool permissions — that must be designed for, not bolted on.

## Decision

1. **AuthN** — OIDC SSO + MFA for staff; OAuth2 client-credentials/signed keys for partners; mTLS
   service-to-service; the app is a **Spring Security OAuth2 Resource Server** validating JWTs.
   Edge enforces TLS 1.2+, WAF, rate limiting, schema validation.
2. **AuthZ** — **RBAC + ABAC** via an externalized policy engine (OPA/Cerbos) so policies are
   auditable and changeable without redeploys. Enforce an **underwriting authority matrix**
   (approve/override/bind within limits), **four-eyes** for high-value/out-of-appetite and for
   overriding declines, and **segregation of duties** (rule authors ≠ approvers; STP auto-approvals
   QA-sampled). Every authZ decision is audited.
3. **PII handling** — classify and minimize; TLS in transit; AES-256 at rest with **field-level
   encryption** for sensitive PII; **tokenization / PII vault** in the working pipeline; **redaction
   of PII in logs and audit**; retention schedules and **crypto-shredding** for right-to-erasure
   without breaking the immutable audit chain.
4. **AI-specific controls** — **redact/pseudonymize PII before any external LLM/embedding call**;
   **de-identified embeddings** (raw PII stays in the encrypted store by reference); **route
   PII-bearing content to an offline/in-house model**; pin cloud AI to **Canadian residency** with
   training/retention disabled. Treat broker content as **untrusted** (data, not instructions);
   apply **layered guardrails** + the evaluator gate; no AI output overrides a deterministic
   knockout.
5. **Secrets** — external secrets manager / KMS, rotation, short-lived creds; no secrets in code,
   config, or logs.
6. **Agent/tool scoping** — least-privilege scoped identities; MCP tools expose only what's needed,
   responses validated; no autonomous binding/financial actions.

Baseline controls are foundational (Phase 1 with persistence/audit); AI-specific controls ship
with the RAG/enrichment phases; full hardening completes in production hardening.

## Consequences

**Positive**
- Defensible posture for regulated, PII-bearing, AI-assisted decisions; supports PIPEDA/OSFI/SOC 2.
- PII can be kept out of third-party models while still using cloud AI on de-identified inputs.
- Prompt injection and over-broad tool access are contained by design; decisions stay auditable.
- Policy-as-data (OPA/Cerbos) keeps complex authority rules maintainable and reviewable.

**Negative / trade-offs**
- More moving parts (IdP, policy engine, secrets manager, PII vault, redaction) and integration work.
- Redaction/tokenization add latency and engineering effort; imperfect redaction is a residual risk
  mitigated by defense-in-depth and offline routing.
- Crypto-shredding requires disciplined per-record key management.

## Alternatives considered

- **App-coded authorization (if/else roles)** — rejected: not auditable or maintainable for
  authority limits/SoD; use a policy engine.
- **Send full PII to cloud LLMs with a DPA only** — rejected as the default: residual leakage/
  residency risk; prefer redaction + offline routing, cloud on de-identified inputs.
- **Encrypt-everything without classification** — rejected: blunt, hurts usability; classify then
  apply field-level controls where they matter.
- **Hard delete for erasure** — rejected: breaks immutable audit; use crypto-shredding instead.
