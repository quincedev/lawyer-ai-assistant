# Frontend Repository Handoff

## Purpose

The browser frontend is maintained in a separate repository from the
`lawyer-ai-assistant` backend.

This document defines the ownership, security, runtime, and integration
boundaries between the backend and frontend repositories.

## Repository Ownership

### Backend Repository

Repository:

```text
lawyer-ai-assistant
```

The backend owns:

- authentication and JWT validation;
- tenant identity and authorization;
- Agent execution;
- MCP integration;
- RAG retrieval;
- Redis caching;
- MySQL persistence and Flyway migrations;
- live evaluation;
- model-judge evaluation;
- token and cost attribution;
- metrics and operational health endpoints;
- production Docker infrastructure.

### Frontend Repository

Recommended repository:

```text
lawyer-ai-assistant-frontend
```

The frontend owns:

- browser pages and components;
- frontend routing;
- client-side state management;
- HTTP client configuration;
- access-token handling;
- loading and error states;
- Agent answer presentation;
- evaluation-report presentation.

The frontend must communicate with the backend only through public HTTP APIs.

## Prohibited Direct Access

The frontend must not connect directly to:

- MySQL;
- Redis;
- Ollama;
- MCP Server;
- Prometheus;
- Grafana;
- Flyway;
- the Vector Store.

The frontend must not contain infrastructure credentials or provider API keys.

## Local Runtime

### Backend

```text
http://localhost:8080
```

### Frontend

Recommended Vite development address:

```text
http://localhost:5173
```

### Backend Readiness

```http
GET /actuator/health/readiness
```

Expected response:

```json
{
  "status": "UP"
}
```

## Authentication

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "e2e-lawyer",
  "password": "password123"
}
```

Representative response:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Current Identity

```http
GET /api/auth/me
Authorization: Bearer <JWT>
```

Representative response:

```json
{
  "userId": "e2e-user-001",
  "username": "e2e-lawyer",
  "tenantId": "tenant-e2e",
  "roles": [
    "LAWYER"
  ]
}
```

The frontend must not provide a trusted tenant ID. The backend derives the
tenant identity from the validated JWT.

## Agent Execution

```http
POST /api/playground/agent
Authorization: Bearer <JWT>
Content-Type: application/json
```

Request:

```json
{
  "goal": "Analyze the legal liability for an unlawful employment termination."
}
```

Representative response:

```json
{
  "goal": "Analyze the legal liability for an unlawful employment termination.",
  "status": "FINISHED",
  "finalAnswer": "..."
}
```

The frontend must handle Agent-level failure states independently from HTTP
failure responses. The frontend must also tolerate additional response fields.

## Live Evaluation

### Run All Cases

```http
POST /api/evaluation/live/run
Authorization: Bearer <JWT>
```

### Run One Case

```http
POST /api/evaluation/live/run/{caseId}
Authorization: Bearer <JWT>
```

Example:

```http
POST /api/evaluation/live/run/labor-illegal-termination-001
```

Representative report:

```json
{
  "runId": "6bb7e8fb-9c46-46a5-8753-fa39c4b66e7d",
  "totalCases": 1,
  "passedCases": 1,
  "failedCases": 0,
  "passRate": 1.0,
  "averageScore": 100.0,
  "totalPromptTokens": 23774,
  "totalCompletionTokens": 9402,
  "totalTokens": 33176,
  "totalCostUsd": 0.005962,
  "cases": []
}
```

Live evaluation may take several minutes. The frontend must use a dedicated
timeout rather than the standard API timeout.

## Standard Error Response

Representative response:

```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "An internal error occurred. Please try again later.",
  "status": 500,
  "path": "/api/playground/agent",
  "timestamp": "2026-08-27T04:09:25.363604201Z"
}
```

Recommended frontend behavior:

| HTTP status | Frontend behavior |
| --- | --- |
| `400` | Display a validation message |
| `401` | Clear authentication state and redirect to login |
| `403` | Display an insufficient-permission message |
| `404` | Display a resource-not-found message |
| `409` | Display a state-conflict message |
| `429` | Ask the user to retry later |
| `500` | Display the sanitized backend message |
| `503` | Display a dependency-unavailable message |

The frontend must never display:

- stack traces;
- internal exception details;
- SQL statements;
- Redis addresses;
- provider credentials;
- access tokens.

## Token Handling

For the demonstration frontend:

- keep the access token in memory where practical;
- use session storage only when refresh persistence is required;
- add the token through an HTTP request interceptor;
- clear authentication state after an HTTP `401`;
- never log JWT values;
- never commit tokens into Git.

A production refresh-token or secure-cookie design is outside the current
backend scope.

## Frontend Environment

The frontend repository should provide:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

The frontend environment must not contain:

```text
DEEPSEEK_API_KEY
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
REDIS_PASSWORD
JWT_SECRET
GRAFANA_ADMIN_PASSWORD
```

## CORS Contract

The backend must explicitly allow the local frontend origin:

```text
http://localhost:5173
```

Production deployments must replace it with the exact deployed frontend
origin. The backend must not use a wildcard origin together with credentials.

Required request headers:

```text
Authorization
Content-Type
```

Required methods are limited to the methods exposed by the public API.

## Recommended Frontend Structure

```text
src/
├── api/
│   ├── client.ts
│   ├── auth-api.ts
│   ├── agent-api.ts
│   └── evaluation-api.ts
├── components/
├── router/
├── stores/
│   ├── auth-store.ts
│   └── agent-store.ts
├── types/
│   ├── api-error.ts
│   ├── auth.ts
│   ├── agent.ts
│   └── evaluation.ts
└── views/
    ├── LoginView.vue
    ├── AgentPlaygroundView.vue
    └── EvaluationView.vue
```

This structure is a recommendation and does not create a backend dependency
on the frontend implementation.

## Initial Frontend Scope

The first frontend release is limited to:

1. login;
2. current-user information;
3. Agent playground;
4. single-case evaluation;
5. full evaluation;
6. evaluation-report presentation.

Prometheus and Grafana remain operational tools and are not recreated as
frontend pages.

## API Contract Changes

A backend API change requires:

1. updating backend tests;
2. updating the frontend API contract;
3. updating the Postman Collection;
4. notifying the frontend repository;
5. documenting breaking changes;
6. completing frontend integration regression.

Adding an optional response field is generally backward compatible. Renaming
or removing an existing field is a breaking change.
