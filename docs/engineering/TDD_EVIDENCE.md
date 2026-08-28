# TDD and Regression Evidence

## Purpose

This document records representative examples of test-driven and
regression-driven development used in this project.

It is not a generated test-count report. It describes how tests protected
behavior while the architecture evolved.

## Test Strategy

The project uses five complementary verification layers:

| Layer | Purpose |
| --- | --- |
| Unit tests | Verify isolated business and policy behavior |
| Context tests | Verify Spring profiles and conditional wiring |
| Integration tests | Verify real framework and infrastructure integration |
| Architecture tests | Protect dependency direction and naming rules |
| Docker E2E | Verify the production runtime topology |

## Representative Development Cycles

### 1. Redis Serialization Boundary

Red:

- Authentication context failed because a Redis serializer was created
  unconditionally.
- The serializer expected a different Jackson `ObjectMapper` generation.

Green:

- Redis serialization components were restricted to the Redis backend.
- Serializer dependencies were aligned with the active framework stack.

Regression protection:

- Caffeine context starts without Redis serializer beans.
- Redis context creates the required serializer beans.
- Authentication tests are independent of the selected cache backend.

### 2. Cache Fail-open Behavior

Red:

- Redis unavailability could propagate infrastructure exceptions into the
  Agent and retrieval workflows.

Green:

- Cache reads treat Redis failures as misses when fail-open is enabled.
- Cache writes, invalidation, and clear operations do not interrupt the primary
  business flow.

Regression protection:

- unavailable Redis;
- successful cold-cache execution;
- successful warm-cache execution;
- cache persistence after Redis restart.

### 3. Knowledge Synchronization

Red:

- repeated knowledge-base startup could reload duplicate documents;
- changed sources could leave stale vector documents.

Green:

- source checksums and manifest state determine whether a source is added,
  unchanged, replaced, or removed.

Regression protection:

- multiple source loading;
- duplicate prevention;
- unchanged-source skip;
- changed-source replacement;
- removed-source cleanup;
- knowledge-version cache invalidation.

### 4. Profile and Role Isolation

Red:

- local legal tools were created while their RAG dependency was disabled;
- MCP Server beans could require unavailable retrieval components.

Green:

- related components use consistent conditional wiring.
- role-specific beans follow Agent and MCP Server profile ownership.

Regression protection:

- base context;
- MCP Agent context;
- MCP Server context;
- production profile composition;
- RAG enabled and disabled combinations.

### 5. Architecture Naming Rules

Red:

- `DocumentConfig` and `RagConfig` violated the configuration naming rule.

Green:

- classes were renamed to `DocumentConfiguration` and `RagConfiguration`.

Regression protection:

- all `@Configuration` classes must use the `Configuration` suffix;
- adapters and repositories follow the defined naming contract.

### 6. Token and Cost Attribution

Red:

- token metrics existed without execution-level cost attribution;
- calls with missing pricing could not be distinguished safely.

Green:

- token usage is aggregated per execution;
- model pricing produces cost in a stable currency;
- missing usage and missing pricing are recorded explicitly.

Regression protection:

- prompt, completion, and total token aggregation;
- model-price lookup;
- decimal cost calculation;
- pricing-missing counters;
- execution cost snapshots;
- budget guardrails.

### 7. Evaluation Metrics

Red:

- a `DistributionSummary` used an invalid zero minimum expected value;
- `BigDecimal.equals()` caused a scale-sensitive test failure.

Green:

- the invalid Micrometer boundary was removed or made strictly positive;
- decimal assertions compare numerical value rather than representation scale.

Regression protection:

- deterministic score;
- evaluation pass and failure counters;
- bounded evaluation tags;
- cost-budget evaluation;
- optional model-judge results.

## Release Commands

Focused tests are run during implementation:

```bash
mvn -Dtest=RelevantTest test
```

Full backend verification:

```bash
mvn clean verify
```

Production runtime verification:

```bash
docker compose \
    -f docker-compose.yml \
    -f docker-compose.prod.yml \
    up -d --build
```

## Acceptance Rule

A change is not accepted merely because the new focused test passes.

It must also preserve:

- the complete Maven verification suite;
- Spring profile startup;
- architecture constraints;
- production Docker startup;
- security and tenant isolation;
- existing public API behavior.
