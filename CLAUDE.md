# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Phase 0 decision core is implemented in `src/main/java/com/iqspark/underwriter` (Java 21, Spring
Boot 3.3): config-driven rules engine (rules as YAML under `src/main/resources/rules/`), case-based
k-NN learning over a deterministic synthetic book, geo remoteness screen, indicative pricing,
template + Anthropic reasoners, REST API, JPA/H2 persistence with a hash-chained audit trail,
Micrometer metrics, and JUnit 5 tests. Phase 1 baseline security is in place (Spring Security:
dual-mode authN — HTTP Basic offline / OIDC-JWT when `issuer-uri` is set — RBAC, underwriting
authority limits + four-eyes binding, and PII redaction). Phase 2 RAG grounding is built behind
`underwriter.rag.enabled` (default off): Spring AI corpus ingest + retrieval + advisory agent
(order 26, capped severity) + cited rationale (`RagLlmReasoner`), in-process ONNX embeddings, and an
in-memory vector store by default (pgvector via the `pgvector` Maven profile). Three lines are wired:
vacant home (reference), rental, contents. **Note the package is `com.iqspark.underwriter`** (the design docs' illustrative listings
say `org.example` — the code is the source of truth). The environment used to scaffold this code
could not run Maven/JDK 21, so compile and run the tests locally before relying on the build.

## Commands

Maven is the build tool (no wrapper `mvnw` present — use a locally installed `mvn`).

```powershell
mvn compile          # compile
mvn test             # run all tests
mvn package          # build artifact
mvn -Dtest=ClassName test                 # run a single test class
mvn -Dtest=ClassName#methodName test      # run a single test method
```

Dependencies (Spring Boot web/validation/test, JUnit 5 + AssertJ) are configured in `pom.xml`, and
tests exist under `src/test/java`. The service runs fully offline by default; set
`UNDERWRITER_LLM_ANTHROPIC_API_KEY` to enable Claude for the written rationale.
