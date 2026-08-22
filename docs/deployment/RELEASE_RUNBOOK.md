# Lawyer AI Assistant — Release Runbook

## 1. Purpose

This document defines the standard production deployment, verification,
rollback, and troubleshooting procedures for `lawyer-ai-assistant`.

Production stack:

- MCP Agent
- MCP Server
- MySQL
- Redis
- Ollama / bge-m3
- Prometheus
- Grafana

---

## 2. Deployment Architecture

```text
External
   │
   ├── MCP Agent :8080
   │
   └── Grafana :3000
          │
          │
──────────────── Docker Network ────────────────
          │
          ├── MCP Server :8081
          ├── MySQL :3306
          ├── Redis :6379
          ├── Ollama :11434
          └── Prometheus :9090
```

Production principles:

- MCP Server is internal-only.
- MySQL is internal-only.
- Redis is internal-only.
- Ollama is internal-only.
- Prometheus is internal-only.
- Secrets must not be committed to Git.
- Containers must use resource limits.
- Containers must use log rotation.
- Application containers must expose readiness health checks.

---

## 3. Pre-Release Checklist

### 3.1 Verify Git Status

```powershell
git status
```

Expected:

```text
nothing to commit, working tree clean
```

Record current commit:

```powershell
git log -1 --oneline
git rev-parse HEAD
```

---

### 3.2 Run Unit Tests

```powershell
mvn clean test
```

Release must stop if:

```text
BUILD FAILURE
Failures > 0
Errors > 0
```

Expected:

```text
BUILD SUCCESS
```

---

### 3.3 Package Application

```powershell
mvn clean package
```

Expected:

```text
BUILD SUCCESS
```

---

## 4. Environment & Secrets Validation

Required production secrets must be supplied through environment variables
or an ignored `.env` file.

Never commit real secrets into:

- Git
- application YAML
- Dockerfile
- docker-compose.yml
- .env.example

Example required variables:

```text
DEEPSEEK_API_KEY
SECURITY_JWT_SECRET
MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD
GRAFANA_ADMIN_PASSWORD
```

Do not print secret values during validation.

---

## 5. Validate Production Compose

Run:

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  config
```

Verify:

```text
MCP Agent     Host :8080
Grafana       Host :3000

MCP Server    Internal :8081
MySQL         Internal :3306
Redis         Internal :6379
Ollama        Internal :11434
Prometheus    Internal :9090
```

Also verify:

- Resource limits are configured.
- Logging rotation is configured.
- Restart policy is `unless-stopped`.
- Agent and Server health checks use readiness endpoints.

WARNING:

`docker compose config` may expand environment variables and secrets.

Do not publish or store the complete output in public logs.

---

## 6. Build Docker Image

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  build
```

Verify:

```powershell
docker images lawyer-ai-assistant
```

Production releases should use immutable/versioned image tags.

Recommended:

```text
lawyer-ai-assistant:<version>-<git-sha>
```

Example:

```text
lawyer-ai-assistant:1.0.0-a1b2c3d
```

Avoid relying on mutable tags for rollback.

---

## 7. Persistent Data

Before releases involving database/schema changes, create and verify an
appropriate backup.

Important persistent components:

```text
MySQL
Redis
Ollama
Prometheus
Grafana
```

MySQL business data has the highest backup priority.

Do NOT run:

```powershell
docker compose down -v
```

during a normal deployment.

The `-v` option removes named volumes and can destroy persistent data.

---

## 8. Production Deployment

Start/update the production stack:

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  up -d --build
```

Check status:

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  ps -a
```

Expected:

```text
mysql          healthy
redis          healthy
ollama         healthy
ollama-init    Exited (0)
mcp-server     healthy
mcp-agent      healthy
prometheus     Up
grafana        Up
```

`ollama-init` is a one-shot initialization container.

`Exited (0)` is expected.

---

## 9. Readiness Verification

### MCP Agent

```powershell
curl.exe http://localhost:8080/actuator/health/readiness
```

Expected:

```json
{"status":"UP"}
```

### MCP Server

MCP Server is internal-only.

Test through the Docker network:

```powershell
docker run --rm `
  --network lawyer-ai-network `
  curlimages/curl `
  http://mcp-server:8081/actuator/health/readiness
```

Expected:

```json
{"status":"UP"}
```

---

## 10. Infrastructure Verification

### Redis

```powershell
docker run --rm `
  --network lawyer-ai-network `
  redis:7 `
  redis-cli -h redis PING
```

Expected:

```text
PONG
```

### Ollama

```powershell
docker run --rm `
  --network lawyer-ai-network `
  curlimages/curl `
  http://ollama:11434/api/tags
```

Verify:

```text
bge-m3
```

is available.

---

## 11. Monitoring Verification

Prometheus is internal-only.

Query it through the Docker network:

```powershell
docker run --rm `
  --network lawyer-ai-network `
  curlimages/curl `
  "http://prometheus:9090/api/v1/query?query=up"
```

Verify MCP Agent and MCP Server targets are UP.

Grafana:

```text
http://localhost:3000
```

Verify the production dashboard contains current data for:

- Total Agent Executions
- Average Agent Duration
- Total LLM Calls
- Average LLM Duration
- Tool Calls
- MCP Calls
- Cache Hit Ratio
- Evidence Reduction
- Retry / No-progress
- JVM Live Threads

---

## 12. Production Smoke Test

Health checks are not sufficient to declare a release successful.

Execute one complete Agent request through Postman.

Expected flow:

```text
Login
  ↓
JWT
  ↓
Agent Request
  ↓
Agent Runtime
  ↓
MCP Server
  ↓
Legal Knowledge / RAG
  ↓
Ollama / bge-m3
  ↓
LLM
  ↓
Final Answer
```

Expected final state:

```text
executionStatus=SUCCESS
agentStatus=FINISHED
```

Verify the application log contains:

```text
Agent execution finished
Agent performance summary
```

Check for abnormal increases in:

```text
LLM Calls
Tool Calls
MCP Calls
Retries
No-progress Suppressions
Execution Duration
```

---

## 13. Security Verification

Only the required Actuator endpoints should be exposed.

Allowed:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
```

Sensitive management endpoints must not expose management data:

```text
/actuator/env
/actuator/beans
/actuator/configprops
/actuator/mappings
```

Verify:

```powershell
curl.exe -i http://localhost:8080/actuator/env
curl.exe -i http://localhost:8080/actuator/beans
```

These endpoints must not return sensitive management information.

---

## 14. Resource Verification

Check runtime limits:

```powershell
docker inspect lawyer-ai-mcp-agent `
  --format='Memory={{.HostConfig.Memory}} NanoCPUs={{.HostConfig.NanoCpus}}'
```

```powershell
docker inspect lawyer-ai-mcp-server `
  --format='Memory={{.HostConfig.Memory}} NanoCPUs={{.HostConfig.NanoCpus}}'
```

Check runtime consumption:

```powershell
docker stats --no-stream
```

Investigate containers approaching their configured memory or CPU limits.

---

## 15. Logging Verification

Check log configuration:

```powershell
docker inspect lawyer-ai-mcp-agent `
  --format='{{json .HostConfig.LogConfig}}'
```

Expected example:

```json
{
  "Type": "json-file",
  "Config": {
    "max-file": "5",
    "max-size": "50m"
  }
}
```

Check recent logs:

```powershell
docker logs lawyer-ai-mcp-agent --tail 200
docker logs lawyer-ai-mcp-server --tail 200
```

Investigate:

```text
ERROR
Exception
OutOfMemoryError
Connection refused
Timeout
MCP failure
```

Production logs must not contain:

- API keys
- JWT secrets
- Access tokens
- Authorization headers
- Database passwords

---

## 16. Release Gate

A release is considered successful only when all checks pass.

```text
Source
[ ] Correct Git commit
[ ] Working tree clean

Build
[ ] Unit tests green
[ ] Maven package successful
[ ] Docker image built

Infrastructure
[ ] Containers started
[ ] Health checks green
[ ] Resource limits active
[ ] Port exposure correct
[ ] Log rotation active

Application
[ ] Agent readiness UP
[ ] Server readiness UP
[ ] Full Agent smoke test SUCCESS

Observability
[ ] Prometheus targets UP
[ ] Grafana receiving metrics
[ ] Performance metrics normal
[ ] Production logs clean
```

If every required check passes:

```text
RELEASE = PASS
```

Otherwise:

```text
RELEASE = FAILED
```

Do not declare the release successful solely because the containers are
running.

---

## 17. Rollback Procedure

If the new release fails:

```text
New Release
    ↓
Release Gate FAILED
    ↓
Stop rollout
    ↓
Restore previous application image
    ↓
Start stack
    ↓
Readiness verification
    ↓
Smoke test
    ↓
Monitoring verification
    ↓
Service restored
```

Production image versions should therefore be immutable and traceable to
Git commits.

Example:

```text
lawyer-ai-assistant:1.0.0-a1b2c3d
lawyer-ai-assistant:1.0.1-d4e5f6g
```

Rollback means redeploying the previous known-good image rather than
rebuilding old source code during an incident.

---

## 18. Database Rollback Warning

Application rollback and database rollback are different operations.

A previous application version may not work after an incompatible schema
migration.

Prefer backward-compatible migrations:

```text
Release A
    ↓
Add new schema

Release B
    ↓
Application starts using new schema

Release C
    ↓
Remove obsolete schema
```

Avoid destructive schema changes in the same release that switches
application behavior.

---

## 19. Emergency Troubleshooting

Use the following order during production incidents.

### Step 1 — Container Status

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  ps -a
```

### Step 2 — Resource Usage

```powershell
docker stats --no-stream
```

### Step 3 — Agent Logs

```powershell
docker logs lawyer-ai-mcp-agent --tail 200
```

### Step 4 — MCP Server Logs

```powershell
docker logs lawyer-ai-mcp-server --tail 200
```

### Step 5 — Agent Readiness

```powershell
curl.exe http://localhost:8080/actuator/health/readiness
```

### Step 6 — Server Readiness

Check through the Docker network.

### Step 7 — Dependency Health

Check:

```text
MySQL
Redis
Ollama
MCP Server
```

### Step 8 — Prometheus

Verify target state and relevant metrics.

### Step 9 — Application Investigation

Only after infrastructure and dependency checks should investigation move
into Agent planning, tools, RAG, MCP, or Java code.

---

## 20. Standard Release Flow

```text
Git Commit
    ↓
Unit Tests
    ↓
Maven Package
    ↓
Environment Validation
    ↓
Persistent Data / Backup Check
    ↓
Compose Validation
    ↓
Docker Build
    ↓
Deploy
    ↓
Container Health
    ↓
Readiness
    ↓
Agent Smoke E2E
    ↓
Prometheus
    ↓
Grafana
    ↓
Logs
    ↓
Performance Sanity Check
    ↓
RELEASE PASS
```

If any mandatory release gate fails:

```text
STOP
 ↓
ROLLBACK
 ↓
VERIFY
```