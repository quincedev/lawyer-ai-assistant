# Final Backend Baseline

## Baseline Date

2026-08-28

## Purpose

This document freezes the verified backend scope of `lawyer-ai-assistant`
before final GitHub documentation, CI, frontend integration, and delivery work.

## Scope

The backend includes:

- multi-step Agent runtime and SSE streaming;
- Skills, planning, reasoning, reflection, and replanning;
- MCP Agent and MCP Server roles;
- legal RAG retrieval and knowledge synchronization;
- Redis and Caffeine cache adapters;
- JWT authentication and tenant-aware authorization;
- MyBatis, MySQL, and Flyway persistence;
- AI security guardrails and runtime resource policies;
- Micrometer, Prometheus, and Grafana observability;
- token usage, pricing, cost attribution, and cost guardrails;
- deterministic evaluation and optional LLM-as-a-Judge;
- unit, integration, Testcontainers, and ArchUnit regression tests.

The frontend is explicitly outside this repository and will be maintained as a
separate project.

## Frozen Runtime Architecture

```text
Browser / Postman
        |
        | JWT
        v
MCP Agent :8080
        |
        | MCP + signed tenant execution identity
        v
MCP Server :8081
        |
        +--> RAG / Vector Store
        +--> Ollama / bge-m3
        +--> Redis

MCP Agent --> MySQL / Flyway
MCP Agent --> Redis
MCP Agent --> DeepSeek

MCP Agent + MCP Server
        --> Micrometer
        --> Prometheus
        --> Grafana
```

## Role Ownership

### MCP Agent

- public backend APIs;
- authentication, JWT, and tenant context;
- Agent orchestration and streaming;
- MCP client integration;
- live evaluation and model Judge;
- Agent token and cost attribution;
- Flyway migration ownership.

### MCP Server

- internal MCP transport and legal tools;
- RAG retrieval;
- knowledge-base initialization and synchronization;
- embedding integration;
- retrieval cache access.

## Technology Baseline

```text
Java                         21
Spring Boot                  4.0.7
Spring AI                    2.0.0
MySQL                        8.0
Redis                        7
Embedding                    Ollama / bge-m3
Persistence                  MyBatis / Flyway
Observability                Micrometer / Prometheus / Grafana
Testing                      JUnit 5 / Mockito / Testcontainers / ArchUnit
Deployment                   Docker Compose
```

## Configuration Baseline

Production MCP Agent:

```text
SPRING_PROFILES_ACTIVE=mcp-agent,prod
SPRING_FLYWAY_ENABLED=true
AI_CACHE_BACKEND=REDIS
AI_COST_ENABLED=true
```

Live evaluation and Judge are optional, cost-incurring capabilities:

```text
AI_LIVE_EVALUATION_ENABLED=true     # evaluation environment only
AI_EVALUATION_JUDGE_ENABLED=true    # evaluation environment only
```

Production MCP Server:

```text
SPRING_PROFILES_ACTIVE=mcp-server,prod
SPRING_FLYWAY_ENABLED=false
AI_CACHE_BACKEND=REDIS
```

The Agent and Server must use the same legal knowledge version.

## Persistence Baseline

- MySQL is the production identity store.
- MyBatis implements the application-user repository adapter.
- Flyway is enabled only on the MCP Agent.
- Development seed identities are excluded from production.
- Redis is the production cache backend.
- Redis cache failures use fail-open semantics.
- Cache keys include scope, tenant identity where applicable, normalized input,
  and knowledge version.

## Security Baseline

- Authentication uses stateless Bearer JWTs.
- Tenant identity is restored from authenticated claims.
- Evaluation endpoints require the `DIRECTOR` or `ADMIN` role.
- MCP tenant-private execution uses signed short-lived identity propagation.
- Tool authorization and risk policies execute before Tool invocation.
- Input/output guardrails cover prompt injection and sensitive data.
- Runtime policies limit steps, retries, replans, Tool calls, time, context, and
  evidence size.
- `.env` and real credentials are excluded from Git.
- Sensitive and high-cardinality data is forbidden in metric tags.

## Architecture Baseline

ArchUnit rules enforce:

- domain-model framework independence;
- port-to-adapter dependency direction;
- Agent, MCP, and RAG role boundaries;
- persistence and cache adapter isolation;
- Spring configuration naming conventions;
- annotation and adapter naming rules.

## Observability Baseline

The stable metrics contract covers:

- Agent executions and duration;
- LLM, Tool, and MCP calls and duration;
- cache hits and misses;
- retries and no-progress suppression;
- evidence compaction;
- prompt, completion, and total tokens;
- token usage and pricing coverage;
- execution-level Agent cost;
- performance and cost guardrails;
- deterministic evaluation;
- model Judge score, duration, tokens, failures, and cost.

Agent and Judge costs are recorded independently.

## Evaluation Baseline

Verified dataset:

1. unlawful termination;
2. economic compensation;
3. probation termination;
4. overtime compensation;
5. labor arbitration limitation.

Final verified execution:

```text
Total cases                 5
Passed cases                5
Failed cases                0
Pass rate                   100%
Average deterministic score 98
Prompt tokens               131105
Completion tokens           67979
Total Agent tokens          199084
Agent cost USD              0.037393
Judge cost USD              approximately 0.003685
Usage-missing calls         0
Pricing-missing calls       0
```

Every case completed with:

```text
agentStatus=FINISHED
deterministicPassed=true
passed=true
withinCostBudget=true
modelJudge.status=COMPLETED
modelJudge.passed=true
errorType=""
```

Because model output is probabilistic, these values are a verified release
baseline rather than a guarantee that every future provider call produces the
same score.

## Release Gates

The frozen baseline requires:

- Maven `verify` succeeds;
- unit and integration tests report zero failures and errors;
- architecture tests succeed;
- the application JAR is generated;
- production containers become healthy;
- authentication and tenant restoration work;
- Agent-to-MCP-to-RAG execution works;
- Redis cold/warm behavior and fail-open behavior work;
- token and pricing coverage are complete;
- Agent and Judge cost attribution remain isolated;
- the five-case live evaluation dataset passes;
- no real production credentials are committed.

## Permitted Changes After Freeze

- release-blocking bug fixes;
- API contract and CORS work required by the separate frontend;
- CI and repository hygiene;
- documentation;
- security hardening;
- Docker and deployment corrections.

## Deferred Changes

- new Agent strategies;
- new MCP tools;
- new RAG sources;
- new cache backends;
- new authentication mechanisms;
- new evaluation dimensions;
- frontend source code in this repository.

