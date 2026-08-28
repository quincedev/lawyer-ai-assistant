# Frontend API Contract

This document freezes the HTTP API contract used by the separately maintained
Vue frontend. Frontend source code does not belong in this repository.

## Base URL and Authentication

- Local Docker base URL: `http://localhost:8080`
- Except for `POST /api/auth/login` and public Operations endpoints, requests
  use `Authorization: Bearer <accessToken>`.
- The frontend treats the JWT as opaque. Identity and tenant information come
  from `GET /api/auth/me`.
- On `401`, clear the authentication state. A `403` means that the authenticated
  identity lacks permission.

## Authentication

### `POST /api/auth/login`

Request:

```json
{
  "username": "e2e-lawyer",
  "password": "password123"
}
```

Successful response (`200 OK`):

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

`expiresIn` is expressed in seconds. Invalid credentials return `401`.

### `GET /api/auth/me`

Successful response (`200 OK`):

```json
{
  "userId": "user-id",
  "username": "e2e-lawyer",
  "tenantId": "tenant-id",
  "roles": ["LAWYER"]
}
```

The order of values in `roles` is not contractual.

## Agent

### `POST /api/playground/agent`

Request:

```json
{
  "goal": "Analyze the legal liability for an unlawful employment termination."
}
```

Successful response (`200 OK`):

```json
{
  "goal": "Analyze the legal liability for an unlawful employment termination.",
  "reasonSummary": "...",
  "plan": [],
  "observations": [],
  "finalAnswer": "...",
  "status": "FINISHED",
  "executionLogs": []
}
```

The frontend uses `status` to determine execution state. `plan`, `observations`,
and `executionLogs` may be empty. `conversationId` is currently reserved for
development retrieval diagnostics and is not part of this Agent request.

## Evaluation

Live Evaluation must be enabled on the backend. Both endpoints require a
`DIRECTOR` or `ADMIN` role and may take significant time and incur model cost.

### `POST /api/evaluation/live/run/{caseId}`

Runs one case and has no request body. Successful response (`200 OK`):

```json
{
  "runId": "run-id",
  "startedAt": "2026-08-28T08:00:00Z",
  "completedAt": "2026-08-28T08:00:08Z",
  "totalCases": 1,
  "passedCases": 1,
  "failedCases": 0,
  "passRate": 1.0,
  "averageScore": 86.5,
  "totalPromptTokens": 1200,
  "totalCompletionTokens": 600,
  "totalTokens": 1800,
  "totalCostUsd": 0.0012,
  "cases": [
    {
      "caseId": "labor-illegal-termination-001",
      "agentStatus": "FINISHED",
      "score": 86.5,
      "passed": true,
      "answerPreview": "..."
    }
  ]
}
```

Model output varies. Clients and tests must validate `score` as a number and
must not require it to equal `100`. Under the frozen API contract, an unknown
`caseId` returns `404`.

### `POST /api/evaluation/live/run`

Runs all cases within the configured limit and returns the same report shape.
It has no request body. Invoke it manually; do not call it on page load or retry
it automatically after a client timeout.

## Operations

| Method | Path | Successful response |
| --- | --- | --- |
| `GET` | `/actuator/health` | `200`, JSON `status` is `UP` |
| `GET` | `/actuator/health/readiness` | `200`, JSON `status` is `UP` |
| `GET` | `/actuator/prometheus` | `200`, Prometheus text exposition |

These are operational endpoints, not frontend business APIs. Production
ingress policy may restrict them further.

## Standard Error Response

```json
{
  "code": "VALIDATION_ERROR",
  "message": "goal must not be blank",
  "status": 400,
  "path": "/api/playground/agent",
  "timestamp": "2026-08-28T08:00:00Z"
}
```

Security filters may reject a request before controller error handling. The
frontend must therefore handle `401` and `403` by HTTP status without requiring
this JSON body.

| Status | Meaning |
| --- | --- |
| `200` | Request succeeded |
| `400` | JSON, argument, or validation error |
| `401` | Credentials are missing, expired, or invalid |
| `403` | The authenticated identity lacks permission |
| `404` | Route or target resource not found |
| `500` | Unexpected server error |

## Timeout Guidance

- Login and identity: 10 seconds
- Agent: 120 seconds
- One Evaluation case: 5 minutes
- Manually initiated full Evaluation: at least 15 minutes
- Health: 5 seconds

These values are frontend policy, not server completion guarantees.

## CORS Requirements

The backend allows the configured exact frontend origin. Local development
uses `http://localhost:5173`. Production must set `FRONTEND_ORIGIN` to the exact
deployed origin.

Allowed methods are `GET`, `POST`, and `OPTIONS`. Allowed request headers are
`Authorization` and `Content-Type`. Bearer authentication does not require
cookie credentials. A wildcard origin is not supported.

## Credential Boundary

The frontend must never hold DeepSeek or other provider API keys, Ollama
administration credentials, MySQL or Redis credentials, JWT signing secrets,
or observability credentials. These remain in the backend deployment.

The Postman Environment contains only disposable local E2E values. Its
`accessToken` initial value must remain empty. Never commit a real JWT,
production password, or API key.

## Development-only Retrieval Diagnostic

`GET /api/playground/retrieval?question=...&conversationId=...` appears only in
the Postman Development Debugging folder. It may be unavailable in production
and is excluded from the supported frontend API contract.
