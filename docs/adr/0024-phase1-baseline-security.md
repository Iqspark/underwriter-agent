# ADR-0024: Phase 1 — baseline security (dual-mode auth, RBAC, authority limits, PII redaction)

**Status:** Accepted
**Date:** 2026-06-26
**Related:** [ADR-0011](0011-security-privacy.md), [ADR-0019](0019-phase1-persistence-metrics.md), [doc 11](../11-security-privacy.md), [doc 8 §5](../08-recommended-solution.md)

## Context

Phase 0/1 exposed the REST API with **no authentication or authorization**. Doc 11 §11 places the
baseline controls — authN/authZ, secrets, PII redaction in logs/audit — in Phase 1 alongside
persistence. The constraint: the service must stay **runnable and testable fully offline** (no
identity provider), which the target OIDC design assumes is present.

## Decision

Add Spring Security with a **dual-mode** authentication strategy and RBAC, plus underwriting
**authority limits / four-eyes** and **PII redaction**.

1. **Dual-mode authentication** (`SecurityConfig`, one filter chain that branches):
   - **Offline/dev (default):** HTTP Basic against in-memory role users (`OfflineUsersConfig`,
     BCrypt; dev password from `underwriter.security.dev-password`). Keeps the offline floor intact.
   - **Production:** when `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set, it runs as
     an **OAuth2 Resource Server** validating OIDC JWTs; `JwtRolesConverter` maps the `roles` claim
     to `ROLE_*` authorities (plus standard `SCOPE_*`). Sessions are stateless; CSRF disabled (token/API).
2. **RBAC** roles: `BROKER`, `UNDERWRITER`, `SENIOR_UNDERWRITER`, `AUDITOR`, `ADMIN`, `SERVICE`.
   Endpoint rules: underwriting POSTs → underwriters/service; reads → underwriters + auditor
   (read-only); `/actuator/**` → admin (health/info public); Swagger → authenticated; health public.
3. **Authority limits + four-eyes** (`AuthorityService`, `BindingService`, `BindingController`,
   doc 11 §3.1): binding/approval enforces "can bind *this risk*" — within-authority bindings need a
   single approval; high-value (≥ threshold) needs **dual control** (two distinct approvers);
   overriding a `DECLINE` needs **senior authority + four-eyes**; coverage beyond authority is denied.
   Segregation of duties: `ADMIN` (who configures rules) cannot approve; the same person can't be the
   second approver. The approver ledger is **in-memory for this slice** (a durable, workflow-backed
   ledger arrives with the event-driven runtime phase).
4. **PII redaction** (`PiiRedactor`): masks emails, postal codes, and long numbers in error
   responses and logs; the audit trail stays PII-light by design (records *that* a field was used and
   *who* acted, not raw names/addresses). Applied in the exception handler and persistence logging.
5. **Secrets** unchanged in posture: API key and credentials via environment/secret manager, never
   in code or committed config.

## Consequences

**Positive**
- The API is authenticated and authorized with least-privilege roles; ops endpoints are locked down.
- Authority limits + four-eyes encode the underwriting control that generic CRUD security misses.
- Still fully offline-testable; production OIDC is a config switch, no code change.
- PII no longer leaks into error bodies/logs.

**Negative / trade-offs**
- In-memory users and approver ledger are **dev/slice-grade** — production uses the IdP and needs a
  durable, audited approval store (next phases).
- Full ABAC via an external policy engine (OPA/Cerbos), mTLS, field-level encryption, crypto-shredding
  and anomaly alerting remain **later hardening** (doc 11 §11) — this is the *baseline*, not the ceiling.
- Authority limit values are illustrative defaults; UW leadership must set real ones.

## Alternatives considered

- **OIDC/JWT only** — rejected for the baseline: breaks the offline-by-default principle and makes
  local/CI runs depend on an IdP.
- **HTTP Basic only** — rejected: not production-aligned; the dual-mode keeps both paths behind one config.
- **Defer authority limits to later** — rejected here because the user prioritized it; doc 11 still
  treats full SoD/crypto-shredding as later hardening.
