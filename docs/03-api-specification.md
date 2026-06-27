# 3. API Specification

**Service:** underwriter-agent
**Base URL (local):** `http://localhost:8080`
**Base path:** `/api/underwriting`
**Content types:** `application/json` (submissions), `text/plain` (documents)
**Auth:** none in v1.0 (add a gateway / API key in front for non-local use)

> **Live, interactive spec:** the running app serves auto-generated OpenAPI via **Swagger UI at
> `/swagger-ui.html`** and the raw spec at **`/v3/api-docs`** (springdoc). That is the runtime source
> of truth; this document is the narrative companion (field dictionary, finding codes, error model).

---

## 3.1 Endpoints

| Method | Path | Request body | Success | Description |
|--------|------|--------------|---------|-------------|
| POST | `/api/underwriting/submissions` | `Submission` (JSON) | `200` `Decision` | Underwrite a structured submission. |
| POST | `/api/underwriting/documents` | raw text (`text/plain`) | `200` `Decision` | Extract a submission from quote-summary text, then underwrite it. |
| GET | `/api/underwriting/health` | — | `200` status | Liveness check. |
| GET | `/api/underwriting/decisions/{reference}` | — | `200` stored decision + audit lineage, or `404` | The persisted decision (latest for that reference) with its durable, hash-chained audit trail. |
| GET | `/api/underwriting/history/stats` | — | `200` book stats | Size + claim rate of the historical book. |
| GET | `/api/underwriting/history/areas/{city}` | — | `200` `AreaRiskStat` | Learned claim/theft stats for an area. |
| POST | `/api/underwriting/history/comparables` | `Submission` (JSON) | `200` `LearnedAssessment` | Preview the comparable cases + prediction without a full decision. |

---

## 3.2 POST /api/underwriting/submissions

Underwrite a fully structured submission.

### Request — `Submission`

```jsonc
{
  "reference": "TRM-VH-2026-0142",          // string, recommended
  "applicant": {
    "name": "1234567 Ontario Inc.",          // string, required
    "brokerName": "Northbridge Brokers",     // string
    "priorLossesDeclared": false,            // boolean
    "priorLossCount": 0                      // integer
  },
  "location": {
    "addressLine": "55 Maple Lane",          // string
    "city": "Sudbury",                       // string, required
    "province": "ON",                        // string, required (2-letter)
    "postalCode": "P3A 1A1",                 // string
    "latitude": 46.4917,                     // number, optional (falls back to province)
    "longitude": -80.9930                    // number, optional
  },
  "building": {
    "construction": "Frame",                 // string
    "occupancyType": "Detached Home",        // string
    "units": 1,                              // integer
    "squareFeet": 2200,                      // integer
    "yearBuilt": 1968,                       // integer
    "roofAgeYears": 24,                      // integer
    "renovationPlanned": false,              // boolean
    "demolitionPlanned": false               // boolean
  },
  "vacancy": {
    "vacantSince": "2025-09-01",             // ISO date (yyyy-MM-dd), required
    "inspectionIntervalHours": 48,           // integer, required (tested vs 72h)
    "utilitiesOn": true,                     // boolean
    "waterShutOff": true,                    // boolean
    "securitySystem": false                  // boolean
  },
  "protection": {
    "monitoredAlarm": false,                 // boolean
    "sprinklered": false,                    // boolean
    "distanceToHydrantMeters": 120,          // integer
    "distanceToFireHallKm": 9                // integer
  },
  "requestedCoverage": {
    "amount": 450000,                        // number
    "currency": "CAD"                        // string
  }
}
```

### Response — `Decision`

```jsonc
{
  "reference": "TRM-VH-2026-0142",
  "outcome": "REFER",                        // APPROVE | REFER | DECLINE
  "riskScore": 16,                           // sum of finding weights
  "findings": [
    {
      "code": "REMOTE_LOCATION",
      "severity": "HIGH",                    // INFO|LOW|MEDIUM|HIGH|KNOCKOUT
      "category": "LOCATION",
      "message": "Remote: 315 km from nearest major city (Toronto)",
      "rationale": "More than 100 km from every major city — slower emergency response ...",
      "source": "RemotenessRule"
    }
  ],
  "conditions": [
    "Confirm fire-response arrangements given the remote location; consider higher deductible."
  ],
  "indicativePremium": { "amount": 3477.60, "currency": "CAD" },
  "rationale": "Recommended action: REFER. Learning from the book: the 25 most similar past policies had a 48% claim rate ...",
  "learnedAssessment": {
    "comparableCount": 25,
    "meanSimilarity": 0.83,
    "claimProbability": 0.48,
    "expectedLossRatio": 0.71,
    "suggestedRatePerThousand": 6.95,
    "dominantPeril": "THEFT",
    "confidence": "HIGH",
    "coldStart": false,
    "topComparables": [
      { "policyId": "HP-00231", "similarity": 0.94, "city": "Sudbury",
        "hadClaim": true, "dominantPeril": "THEFT", "lossRatio": 0.62, "ratePerThousand": 7.10 }
    ],
    "areaRisk": {
      "city": "Sudbury", "sampleSize": 142, "overallClaimRate": 0.41,
      "theftClaimRate": 0.28, "avgLossRatio": 0.55
    }
  },
  "auditTrail": [
    "2026-06-19T12:00:00Z | IntakeAgent           | Ingested submission TRM-VH-2026-0142 at Sudbury, ON",
    "2026-06-19T12:00:00Z | RiskProfilingAgent    | Evaluated 4 rules, raised 5 findings (risk weight=16)",
    "2026-06-19T12:00:00Z | ComplianceAgent       | Compliance clearance: no condition-precedent breaches detected.",
    "2026-06-19T12:00:00Z | PricingAgent          | Indicative premium CAD 3477.60 (base 2025.00, risk load on 16 pts)",
    "2026-06-19T12:00:00Z | LlmReasoner           | Rationale generated by template-offline",
    "2026-06-19T12:00:00Z | DecisionOrchestrator  | Outcome=REFER riskScore=16 conditions=2"
  ],
  "decidedAt": "2026-06-19T12:00:00Z"
}
```

### cURL

```bash
curl -s -X POST http://localhost:8080/api/underwriting/submissions \
  -H "Content-Type: application/json" \
  --data @samples/submission-refer.json
```

---

## 3.3 POST /api/underwriting/documents

Submit raw quote-summary text. The `KeyValueDocumentExtractor` parses `key: value` lines into
a `Submission`, then the same pipeline runs. Unrecognised/absent keys are left null on purpose
so completeness rules can flag them — the extractor never invents data.

### Request (`text/plain`)

```
reference: TRM-VH-2026-0142
applicant: 1234567 Ontario Inc.
city: Sudbury
province: ON
inspection_interval_hours: 96
utilities_on: yes
water_shut_off: no
...
```

Recognised keys (snake_case): `reference, applicant, broker, prior_losses_declared,
prior_loss_count, address, city, province, postal_code, latitude, longitude, construction,
occupancy, units, square_feet, year_built, roof_age, renovation_planned, demolition_planned,
vacant_since, inspection_interval_hours, utilities_on, water_shut_off, security_system,
monitored_alarm, sprinklered, hydrant_distance_m, fire_hall_distance_km, requested_coverage,
line_of_business`.

Booleans accept `true/false/yes/no/y`. Dates are ISO `yyyy-MM-dd`.

### cURL

```bash
curl -s -X POST http://localhost:8080/api/underwriting/documents \
  -H "Content-Type: text/plain" \
  --data-binary @samples/quote-summary.txt
```

Response is the same `Decision` schema as §3.2.

---

## 3.4 GET /api/underwriting/health

```bash
curl -s http://localhost:8080/api/underwriting/health
# {"status":"UP","service":"underwriter-agent"}
```

---

## 3.5 Enumerations

**`outcome` (DecisionOutcome):** `APPROVE`, `REFER`, `DECLINE`.

**`lineOfBusiness` (optional on `Submission`):** `VACANT_HOME` (default), `RENTAL`, `CONTENTS`, `FARM`. Omitting it defaults to `VACANT_HOME` (the first line built and worked reference example; the agent is multi-line by design). **Vacant home, rental, and contents are underwritten today; farm is a placeholder.** A `RENTAL` submission carries a `rental` section instead of `vacancy`:

```jsonc
"lineOfBusiness": "RENTAL",
"rental": {
  "tenancyType": "LONG_TERM",        // or "SHORT_TERM" (STR / Airbnb)
  "shortTermRentalEndorsed": false,   // STR endorsement in place?
  "liabilityLimit": 2000000,          // CAD; null/absent -> flagged
  "tenantScreening": true
}
```

A `CONTENTS` submission carries a `contents` section and may omit `building` (personal belongings):

```jsonc
"lineOfBusiness": "CONTENTS",
"contents": {
  "contentsValue": 90000,            // total sum insured (CAD); null/absent -> flagged
  "highValueItemsValue": 40000,      // jewellery/art/electronics
  "highValueItemsScheduled": false,  // individually scheduled?
  "replacementCost": true,           // replacement cost vs ACV
  "securityDevice": true
}
```

**`severity` (Severity):** `INFO` (0), `LOW` (1), `MEDIUM` (3), `HIGH` (6), `KNOCKOUT` (100) — number is the risk weight.

**Finding `category`:** `COMPLETENESS`, `LOCATION`, `COMPLIANCE`, `RISK`, `MORAL_HAZARD`, `LEARNED`.

**`dominantPeril` / area `Peril`:** `NONE`, `THEFT`, `FIRE`, `WATER`, `VANDALISM`, `OTHER`.

**`confidence` (LearnedAssessment):** `LOW`, `MEDIUM`, `HIGH` — derived from comparable count and mean similarity.

## 3.6 Finding codes (reference)

| Code | Severity | Category | Raised when |
|------|----------|----------|-------------|
| `MISSING_FIELD` | MEDIUM | COMPLETENESS | A required field is absent. |
| `DATA_CONTRADICTION` | HIGH | COMPLETENESS | Fields contradict (e.g. detached home with many units / huge area; implausible year). |
| `REMOTE_LOCATION` | HIGH | LOCATION | >100 km from every major city. |
| `LOCATION_WITHIN_RANGE` | INFO | LOCATION | Within 100 km of a major city. |
| `LOCATION_UNRESOLVED` | MEDIUM | LOCATION | No coordinates or recognised province. |
| `INSPECTION_INTERVAL_BREACH` | KNOCKOUT | COMPLIANCE | Inspection interval > 72 h. |
| `WATER_NOT_SHUT_OFF` | HIGH | COMPLIANCE | Utilities on and water not shut off. |
| `LONG_VACANCY` | HIGH | RISK | Vacant ≥ 12 months. |
| `MODERATE_VACANCY` | MEDIUM | RISK | Vacant 6–12 months. |
| `NO_SECURITY` | MEDIUM | RISK | No security system. |
| `NO_MONITORED_ALARM` | LOW | RISK | No monitored alarm. |
| `FAR_FROM_FIRE_HALL` | MEDIUM | RISK | Fire hall > 13 km away. |
| `OLD_ROOF` | MEDIUM | RISK | Roof age > 20 years. |
| `DEMOLITION_PLANNED` | HIGH | RISK | Demolition planned. |
| `RENOVATION_PLANNED` | LOW | RISK | Renovation planned. |
| `FAR_FROM_HYDRANT` | MEDIUM | RISK | Nearest hydrant > 300 m (limited firefighting water supply). |
| `SPRINKLERED` | INFO | RISK | Property is sprinklered (mitigating factor). |
| `POSSIBLE_UNDERINSURANCE` | MEDIUM | COMPLETENESS | Coverage < ~$100/sqft of floor area. |
| `COVERAGE_HIGH_VS_AREA` | LOW | COMPLETENESS | Coverage > ~$1,500/sqft (re-verify sum insured/area). |
| `PRIOR_LOSSES` | HIGH | MORAL_HAZARD | ≥ 2 prior losses declared. |
| `LEARNED_CLAIM_PROBABILITY` | INFO/MEDIUM/HIGH | LEARNED | Comparable history's predicted claim probability / loss ratio (severity scales with the prediction). |
| `LEARNED_DOMINANT_PERIL_THEFT` | LOW | LEARNED | Theft is the dominant loss peril among comparable policies. |
| `AREA_THEFT_ELEVATED` | MEDIUM | LEARNED | The submission's area shows an elevated theft claim rate in the book. |
| `STR_WITHOUT_ENDORSEMENT` | KNOCKOUT | COMPLIANCE | *(RENTAL)* Short-term rental with no STR endorsement. |
| `MISSING_LIABILITY_LIMIT` | MEDIUM | COMPLETENESS | *(RENTAL)* Required liability limit not provided (forces REFER). |
| `LOW_LIABILITY_LIMIT` | MEDIUM | RISK | *(RENTAL)* Liability limit below the $1M minimum. |
| `SHORT_TERM_RENTAL` | LOW | RISK | *(RENTAL)* Short-term tenancy — higher turnover/occupancy risk. |
| `NO_TENANT_SCREENING` | MEDIUM | MORAL_HAZARD | *(RENTAL)* No tenant screening performed. |
| `HIGH_VALUE_ITEMS_UNSCHEDULED` | HIGH | RISK | *(CONTENTS)* High-value items above the sub-limit not individually scheduled. |
| `CONTENTS_NO_SECURITY` | MEDIUM | RISK | *(CONTENTS)* No security device protecting the contents. |
| `CONTENTS_ACV_BASIS` | LOW | RISK | *(CONTENTS)* Actual-cash-value (not replacement-cost) settlement basis. |

> Codes are stable contract; severities encode current underwriting appetite and may be tuned.

## 3.7 Error handling

| Status | When | Body |
|--------|------|------|
| `200 OK` | Submission processed (including `DECLINE`/`REFER` — these are valid outcomes, not errors). | `Decision` |
| `400 Bad Request` | Malformed JSON / wrong content type. | Spring error JSON (`timestamp`, `status`, `error`, `path`). |
| `415 Unsupported Media Type` | Wrong `Content-Type` on `/documents` (must be `text/plain`). | Spring error JSON. |
| `500 Internal Server Error` | Unexpected server fault. | Spring error JSON. |

Note: a missing or contradictory field does **not** produce a `400` — it is captured as a
`Finding` and drives a `REFER`. The decision pipeline is the validator of business content.

## 3.8 Notes for integrators

- The decision is **advisory**. Do not auto-bind or auto-decline from `outcome`; route it into
  the underwriter's queue/workbench.
- The `auditTrail` and `findings` should be persisted with the file for compliance.
- Timestamps are UTC ISO-8601.
- Adding the Anthropic key changes only the `rationale` text and the `LlmReasoner` audit line;
  `outcome`, `riskScore`, `findings`, and `conditions` are unaffected.
- Operational endpoints are exposed by Spring Boot Actuator: `GET /actuator/health`,
  `/actuator/metrics`, and `/actuator/prometheus` (decision counter by outcome/line + latency timer).
- Every decision is persisted; retrieve it later (with its hash-chained audit lineage) via
  `GET /api/underwriting/decisions/{reference}`.
