# Interview Demonstration

## Objective

Demonstrate the enterprise architecture, runtime behavior, security boundaries,
observability, and evaluation capabilities of Lawyer AI Assistant in 8 to 12
minutes.

## Preparation

Before the interview:

1. start the production Docker Compose stack;
2. verify that MySQL, Redis, Ollama, MCP Server, and MCP Agent are healthy;
3. verify that Prometheus targets are up;
4. prepare one `LAWYER` account and one `DIRECTOR` account;
5. run one Agent request so Ollama and the remote model are warm;
6. confirm that no credentials are visible in the terminal or browser history.

## 1. Project Introduction — 1 minute

> Lawyer AI Assistant is a multi-tenant legal AI platform built with Java 21,
> Spring Boot, Spring AI, MCP, RAG, Redis, MySQL, Prometheus, Grafana, and
> Vue 3. It focuses on controllable Agent execution, explicit architecture
> boundaries, production identity, cost visibility, and repeatable AI quality
> evaluation.

## 2. Architecture — 1 minute

Open `docs/ARCHITECTURE.md` and explain:

- the Vue frontend calls only the MCP Agent;
- the MCP Agent owns authentication and orchestration;
- the MCP Server owns legal Tools and RAG;
- MySQL persists identities and Flyway has one migration owner;
- Redis and Caffeine implement the same cache contracts;
- Prometheus and Grafana observe both application roles.

## 3. Authentication and Tenant Isolation — 1 minute

Demonstrate:

1. login as a `LAWYER`;
2. show the current tenant and role;
3. refresh the browser to demonstrate identity restoration;
4. explain that tenant identity comes from validated JWT claims;
5. show that the Evaluation Dashboard is unavailable to the `LAWYER` role.

Frontend route guards improve user experience. Spring Security remains the
authoritative authorization boundary.

## 4. Agent Playground — 2 minutes

Submit:

> 用人单位违法解除劳动合同，赔偿金应当如何计算？

Show:

- execution status;
- reasoning summary;
- execution plan;
- Tool and MCP observations;
- final legal answer;
- execution logs.

Explain that the runtime performs bounded planning, action selection, Tool
execution, reflection, and answer synthesis.

## 5. MCP, RAG, and Redis — 2 minutes

Explain:

- the Agent does not directly implement legal retrieval;
- MCP Server exposes the legal Tool;
- RAG documents contain source, version, checksum, and scope metadata;
- incremental synchronization replaces stale documents;
- cache keys include tenant and knowledge version;
- Tool cache stores raw results;
- cache hits never bypass security;
- Redis failures can fail open without interrupting the Agent workflow.

Optionally show Redis keys:

```powershell
docker exec lawyer-ai-redis `
    redis-cli -n 0 --scan `
    --pattern "lawyer-ai:*"
```

## 6. Observability and Cost — 1 minute

Open Grafana or Prometheus and show:

- Agent executions and duration;
- LLM, Tool, and MCP calls;
- cache hit ratio;
- retry and no-progress suppression;
- prompt and completion tokens;
- estimated model cost;
- performance and cost guardrail events.

Token and cost attribution is recorded per execution without using sensitive or
high-cardinality metric tags.

## 7. Evaluation Dashboard — 2 minutes

Login as `DIRECTOR` and run:

```text
labor-illegal-termination-001
```

Show:

- deterministic score;
- concept coverage and missing concepts;
- forbidden-claim detection;
- execution duration, tokens, and cost;
- optional model-Judge score, reasons, and risks;
- answer preview.

Live evaluation is restricted to `DIRECTOR` and `ADMIN`, invokes the real model,
incurs cost, and is not exposed as an ordinary lawyer feature.

## 8. Engineering Quality — 1 minute

Show:

- Maven Verify;
- JUnit, Mockito, and Testcontainers;
- ArchUnit dependency regression;
- frontend lint, type checking, Vitest, and production build;
- GitHub Actions;
- production Docker Compose deployment.

## Closing Statement

> This project demonstrates how I design an AI Agent as a production software
> system: identity is trusted, dependencies are explicit, execution is bounded,
> caches preserve security semantics, token and cost usage is observable, and
> model quality is evaluated through repeatable regression workflows.

## Demonstration Fallbacks

If an external model is temporarily unavailable:

- show a previously captured evaluation report;
- show deterministic unit tests and architecture tests;
- demonstrate authentication, role boundaries, and container health;
- explain the external failure boundary rather than repeatedly retrying a paid
  request.

Never expose API keys, JWTs, database passwords, or complete environment files
during the demonstration.