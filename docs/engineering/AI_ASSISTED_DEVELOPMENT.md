# AI-assisted Development Workflow

## Purpose

This project uses AI as an engineering assistant for code review, test design,
refactoring analysis, documentation, and troubleshooting.

AI-generated suggestions are not treated as production-ready changes. Every
material change remains subject to human review, automated tests, architecture
checks, and Docker end-to-end verification.

## Engineering Ownership

The developer remains responsible for:

- architecture decisions;
- security boundaries;
- authentication and tenant isolation;
- dependency selection;
- production configuration;
- test acceptance criteria;
- code review;
- deployment approval.

AI assistance does not replace engineering ownership.

## Development Workflow

Each material change follows this workflow:

1. inspect the existing implementation;
2. define the expected behavior and affected boundary;
3. add or update tests;
4. implement the smallest coherent change;
5. run focused unit tests;
6. run the full Maven test suite;
7. run architecture regression tests;
8. rebuild the production Docker image when runtime code changes;
9. complete Postman or Docker end-to-end verification;
10. review the final diff before commit.

## Typical AI-assisted Tasks

AI was used to assist with:

- identifying dependency-direction violations;
- designing port and adapter boundaries;
- generating initial unit-test structures;
- analyzing Spring conditional-wiring failures;
- diagnosing Docker and Maven configuration problems;
- reviewing Redis serialization boundaries;
- designing RAG metadata and incremental-loading tests;
- constructing negative-path security test cases;
- reviewing metric-cardinality risks;
- drafting documentation and release checklists.

## Human Review Requirements

AI-proposed code must be rejected or revised when it:

- invents classes, methods, endpoints, or configuration properties;
- changes an existing public contract without migration planning;
- weakens authentication or authorization;
- introduces sensitive or high-cardinality metric tags;
- couples domain contracts to infrastructure implementations;
- adds unconditional Spring beans that break unrelated profiles;
- suppresses test failures instead of correcting their cause;
- places credentials or access tokens in source control;
- relies on unverified library or framework behavior.

## Verification Layers

### Unit Tests

Unit tests verify isolated domain and service behavior, including:

- Agent runtime policies;
- cache semantics;
- cost calculation;
- deterministic evaluation;
- model-judge parsing;
- RAG metadata;
- knowledge synchronization.

### Spring Context Tests

Context tests verify:

- profile responsibility;
- conditional bean wiring;
- Redis and Caffeine backend selection;
- MyBatis repository selection;
- Flyway ownership;
- Agent and MCP Server role isolation.

### Integration Tests

Integration tests verify:

- authentication;
- MySQL persistence;
- Redis serialization;
- cache failure behavior;
- framework integration.

### Architecture Tests

ArchUnit verifies:

- domain framework independence;
- port and adapter dependency direction;
- Agent, MCP, and RAG role boundaries;
- configuration and adapter naming conventions.

### Docker E2E

Production Docker verification covers:

- authentication and JWT;
- tenant identity;
- Agent execution;
- MCP tool calls;
- RAG retrieval;
- Redis cold and warm cache;
- MySQL and Flyway;
- Redis restart and fail-open behavior;
- Ollama Embedding connectivity;
- Prometheus metrics;
- live deterministic evaluation;
- optional LLM-as-a-Judge evaluation.

## Example: Redis Cache Adapter

The Redis implementation was introduced behind the existing cache ports:

- `ToolResultCache`
- `RetrievalCache`

The development sequence was:

1. preserve existing Caffeine semantics;
2. define the Redis serialization boundary;
3. implement Tool result serialization;
4. implement retrieval result serialization;
5. add backend-conditional wiring;
6. verify cache-key and tenant isolation;
7. verify restart persistence;
8. verify fail-open behavior;
9. run the full regression suite.

A Spring context regression exposed an unconditional Jackson serializer bean.
The solution was to make Redis-specific serialization components conditional
on the Redis backend and align their JSON dependency with the active Spring
Boot stack.

## Example: RAG Hardening

RAG hardening introduced:

- stable document metadata;
- source checksum;
- knowledge version semantics;
- multi-document loading;
- duplicate prevention;
- incremental synchronization;
- stale-document replacement;
- retrieval-cache version invalidation.

Tests were used to distinguish source identity, content identity, document
version, and cache version instead of treating them as the same concern.

## Example: Observability and Evaluation

The observability work added:

- token usage metrics;
- model pricing;
- execution-level cost attribution;
- cost budget guardrails;
- deterministic evaluation;
- optional model-judge evaluation.

Metrics were reviewed for sensitive and high-cardinality tags. Evaluation
reports contain case-level details, while Prometheus exposes only bounded tags.

## Commit and Review Policy

Before committing a change:

```bash
mvn clean verify
git diff --check
git diff
```

For production runtime changes:

```bash
docker compose \
    -f docker-compose.yml \
    -f docker-compose.prod.yml \
    build

docker compose \
    -f docker-compose.yml \
    -f docker-compose.prod.yml \
    up -d
```

The final diff is reviewed for:

- accidental generated files;
- credentials;
- profile regressions;
- renamed contracts;
- weakened tests;
- missing documentation;
- unrelated changes.

## Limitations

This repository does not claim that every line was manually typed.

It claims that every accepted production change was reviewed against defined
behavior, automated tests, architecture rules, and runtime regression gates.

AI is treated as an accelerator. Engineering responsibility remains human.
