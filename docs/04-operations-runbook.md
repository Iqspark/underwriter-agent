# 4. Operations & Runbook

**Service:** underwriter-agent
**Audience:** Developers, operators

---

## 4.1 Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21 (e.g. Temurin 21) |
| Maven | 3.9+ (no wrapper committed; use a local `mvn`) |
| OS | Any (Windows / macOS / Linux) |

Verify:

```bash
java -version    # expect 21.x
mvn -version
```

## 4.2 Build & test

```bash
mvn clean compile      # compile
mvn test               # run all tests (rules, geo, decision paths, context wiring)
mvn package            # build the runnable jar -> target/underwriter-agent-1.0-SNAPSHOT.jar
```

Run a single test:

```bash
mvn -Dtest=DecisionOrchestratorTest test
mvn -Dtest=ConditionPrecedentRuleTest#inspectionOver72hIsAKnockout test
```

### Test inventory

| Test | Verifies |
|------|----------|
| `ConditionPrecedentRuleTest` | 72h knockout boundary; water-shutoff finding. |
| `GeoServiceTest` | Within-range vs remote; unresolved location; haversine accuracy. |
| `DecisionOrchestratorTest` | Guardrail-only APPROVE / REFER / DECLINE logic in isolation. |
| `SyntheticHistoryGeneratorTest` | Book determinism per seed; plausible claim rate. |
| `AreaRiskServiceTest` | Learns that high-risk areas claim more than low-risk areas; theft load bounds. |
| `SimilarityEngineTest` | Returns k comparables with sane aggregates; cold-start; ordering. |
| `AiFirstPipelineTest` | End-to-end AI-first pipeline: learned evidence attached, comparable pricing, guardrail knockout still declines. |
| `RentalLineTest` / `ContentsLineTest` | Line-specific rules + line-isolated learning for rental & contents. |
| `ConfigurableRulesEngineTest` | Config rules load + evaluate; line scoping; interpolation. |
| `DecisionStoreTest` | Decision + hash-chained audit persist to H2 and read back; chain verifies. |
| `LearningRegressionTest` | Realistic loss ratios; clean file below decline thresholds; risk discrimination. |
| `UnderwriterAgentApplicationTests` | Spring context loads and the orchestrator is wired (offline). |

## 4.3 Run

```bash
mvn spring-boot:run
# or
java -jar target/underwriter-agent-1.0-SNAPSHOT.jar
```

Service starts on `http://localhost:8080`. Smoke test:

```bash
curl -s http://localhost:8080/api/underwriting/health
curl -s http://localhost:8080/actuator/health                          # Actuator health
curl -s http://localhost:8080/actuator/prometheus | grep underwriting  # decision metrics
curl -s http://localhost:8080/api/underwriting/history/stats           # book size + claim rate
curl -s -X POST http://localhost:8080/api/underwriting/submissions \
  -H "Content-Type: application/json" --data @samples/submission-approve.json
# then fetch the persisted decision + tamper-evident audit lineage:
curl -s http://localhost:8080/api/underwriting/decisions/TRM-VH-2026-0203
```

On startup the log shows the book that was loaded, e.g.
`Loaded historical book: 1500 policies (… claim rate)`.

Interactive API docs: **Swagger UI at `http://localhost:8080/swagger-ui.html`**, raw OpenAPI at
`/v3/api-docs`.

## 4.4 Configuration

Configuration lives in `src/main/resources/application.yml`. Override via environment variables
(Spring relaxed binding) or `--key=value` JVM args.

| Property | Env var | Default | Purpose |
|----------|---------|---------|---------|
| `server.port` | `SERVER_PORT` | `8080` | HTTP port. |
| `spring.datasource.url` | `DB_URL` | embedded H2 (`jdbc:h2:mem:…`) | DB connection; activate the `prod` profile for PostgreSQL. |
| `spring.datasource.username` / `.password` | `DB_USER` / `DB_PASSWORD` | `sa` / _(empty)_ | DB credentials. |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | _(none)_ | Set to `prod` for PostgreSQL + `ddl-auto=validate`. |
| `underwriter.rules.dir` | `UNDERWRITER_RULES_DIR` | _(unset → classpath `rules/`)_ | External directory of rule files (`shared.yml`, `<line>.yml`), to change rules without rebuilding. |
| `underwriter.history.size` | `UNDERWRITER_HISTORY_SIZE` | `1500` | Synthetic historical book size generated at startup. |
| `underwriter.history.seed` | `UNDERWRITER_HISTORY_SEED` | `42` | RNG seed for a reproducible book. |
| `underwriter.similarity.k` | `UNDERWRITER_SIMILARITY_K` | `25` | Number of nearest comparable policies per assessment. |
| `underwriter.llm.anthropic.api-key` | `UNDERWRITER_LLM_ANTHROPIC_API_KEY` | _(unset)_ | Enables Claude rationale when set; offline otherwise. |
| `underwriter.llm.anthropic.model` | `UNDERWRITER_LLM_ANTHROPIC_MODEL` | `claude-sonnet-4-6` | Model for the rationale. |
| `logging.level.com.iqspark.underwriter` | — | `INFO` | Service log level. |

### Enable Claude for the rationale

```bash
# bash / Linux / macOS
export UNDERWRITER_LLM_ANTHROPIC_API_KEY="sk-ant-..."
mvn spring-boot:run

# Windows PowerShell
$env:UNDERWRITER_LLM_ANTHROPIC_API_KEY="sk-ant-..."
mvn spring-boot:run
```

When set, the `AnthropicLlmReasoner` bean activates and becomes `@Primary`; the decision is
unchanged, only the `rationale` text differs. **Never commit the key.**

## 4.5 Deploy

The artifact is a self-contained Spring Boot jar. Example container:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/underwriter-agent-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

```bash
mvn clean package
docker build -t underwriter-agent:1.0 .
docker run -p 8080:8080 \
  -e UNDERWRITER_LLM_ANTHROPIC_API_KEY=sk-ant-... \
  underwriter-agent:1.0
```

Provide the API key via your platform's secret manager (env injection), not in the image.

## 4.6 Observability

- **Logs:** SLF4J/Logback to stdout. Each decision logs agent audit lines, the chosen
  `outcome`, `riskScore`, and the reasoner `provider`. A failed Claude call logs a warning and
  the fallback is used.
- **Audit trail:** returned in every `Decision.auditTrail`; persist it with the file for
  compliance.
- **Health:** `GET /api/underwriting/health`. (For richer probes, add Spring Boot Actuator.)

## 4.7 Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| App won't start: "no qualifying bean / multiple LlmReasoner" | Custom reasoner added without `@Primary`/qualifier | Ensure exactly one `@Primary` reasoner; see ADR-0003. |
| Rationale is generic even with key set | Key not picked up | Confirm `UNDERWRITER_LLM_ANTHROPIC_API_KEY` is exported in the same shell; check startup log for the Anthropic bean. |
| Every Claude call falls back | API/network/auth error | Check the warning log line from `AnthropicLlmReasoner`; verify key, network egress, and `anthropic-version`/model. |
| Outcome is `REFER` unexpectedly | Missing/contradictory field or risk weight ≥ 6 | Inspect `findings`; the blocking finding's `rationale` explains why. |
| Outcome is `DECLINE` | A knockout (e.g. inspection > 72h) | See `findings` where `severity = KNOCKOUT`; the curing condition is in `conditions`. |
| Remote flag seems wrong | Coarse city table / province fallback | Supply `latitude`/`longitude`; for production, expand `GeoService` city data. |
| 415 on `/documents` | Wrong content type | Send `Content-Type: text/plain`. |

## 4.8 Maintenance tasks

- **Change / add a rule (no code):** edit the rule files under `resources/rules/` — `shared.yml`
  for cross-line rules, or `rules/<line>.yml` (e.g. `vacant-home.yml`, `rental.yml`) for a line
  (rules in a line file are auto-scoped to that line). Override the whole set with an external
  directory via `underwriter.rules.dir`. Adjust a `severity`/threshold (`value`), or add an entry
  (`id`, `code`, `category`, `severity`, `all:` conditions, `message`/`rationale`). Restart to
  reload. Review via the model-governance gate ([doc 13](13-ai-governance-model-risk.md)); document
  new finding codes in [API §3.6](03-api-specification.md#36-finding-codes-reference). Only a
  brand-new derived fact or operator needs code (`FactExtractor` / `ConfigurableRulesEngine`).
- **Add a new line of business:** add the `LineOfBusiness` enum value and drop in
  `resources/rules/<line>.yml` — the loader picks it up automatically (plus any line-specific facts
  in `FactExtractor` and a details record on `Submission`).
- **Tune appetite:** edit thresholds/severities in the relevant `rules/*.yml`; `REFER_THRESHOLD`
  lives in `DecisionOrchestrator`. Review with UW leadership; update [HLD §6](02-architecture-design.md#6-decision-policy).
- **Add an agent:** implement `UnderwritingAgent` with an `order()`.
- **Swap extraction:** implement `DocumentExtractor` (OCR/LLM) and mark it `@Primary`.
- **Update Claude model/version:** edit `AnthropicLlmReasoner` (centralized) or the config.

## 4.9 Known limitations (v1.0)

- No persistence, authn/authz, or rate limiting — front with a gateway for non-local use.
- Document extractor and city table are coarse reference implementations.
- Pricing is illustrative, not actuarial.
- In-process, sequential pipeline (no per-agent scaling).
