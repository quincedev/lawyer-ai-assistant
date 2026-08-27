# Agent Metrics Contract

## Purpose

This document defines the stable Micrometer and Prometheus contract for
Agent observability.

Metric names are consumed by:

- Prometheus queries;
- Grafana dashboards;
- release regression scripts;
- operational alerts;
- architecture and observability regression tests.

Changing an existing metric name, type, tag contract, or semantic meaning
is a breaking observability change.

## Stable Metrics

| Micrometer name | Prometheus name | Type | Semantic |
|---|---|---|---|
| `agent.executions` | `agent_executions_total` | Counter | Completed Agent executions recorded by the execution observer |
| `agent.llm.calls` | `agent_llm_calls_total` | Counter | Total LLM calls across Agent executions |
| `agent.tool.calls` | `agent_tool_calls_total` | Counter | Total Tool calls across Agent executions |
| `agent.mcp.calls` | `agent_mcp_calls_total` | Counter | Total MCP calls across Agent executions |
| `agent.cache.hits` | `agent_cache_hits_total` | Counter | Agent Tool cache hits |
| `agent.cache.misses` | `agent_cache_misses_total` | Counter | Agent Tool cache misses |
| `agent.retries` | `agent_retries_total` | Counter | Agent retry events |
| `agent.no_progress.suppressions` | `agent_no_progress_suppressions_total` | Counter | No-progress retry suppressions |
| `agent.execution.duration` | `agent_execution_duration_seconds` | Timer | Total duration per Agent execution |
| `agent.llm.duration` | `agent_llm_duration_seconds` | Timer | Aggregated LLM duration per Agent execution |
| `agent.tool.duration` | `agent_tool_duration_seconds` | Timer | Aggregated Tool duration per Agent execution |
| `agent.mcp.duration` | `agent_mcp_duration_seconds` | Timer | Aggregated MCP duration per Agent execution |
| `agent.evidence.original.chars` | `agent_evidence_original_chars` | Distribution Summary | Evidence size before compaction |
| `agent.evidence.compacted.chars` | `agent_evidence_compacted_chars` | Distribution Summary | Evidence size after compaction |
| `agent.performance.guardrail.events` | `agent_performance_guardrail_events_total` | Counter | Performance guardrail events |
| `agent.evaluations` | `agent_evaluations_total` | Counter | Total deterministic Agent quality evaluations |
| `agent.evaluation.score` | `agent_evaluation_score_count/sum/max` | Distribution Summary | Deterministic Agent quality evaluation scores |

The first fourteen meters are explicitly registered when
`DefaultAgentMicrometerMetrics` is created.

`agent.performance.guardrail.events` is registered dynamically when a
performance guardrail event is recorded.

## Counter Semantics

Counters are cumulative for the lifetime of the application process.

For example:

```text
agent.executions
```

is incremented once when an Agent execution is completed and recorded by
the execution observer.

The following counters represent individual downstream calls or events:

- `agent.llm.calls`;
- `agent.tool.calls`;
- `agent.mcp.calls`;
- `agent.cache.hits`;
- `agent.cache.misses`;
- `agent.retries`;
- `agent.no_progress.suppressions`.

Prometheus counters reset when the application process restarts.
PromQL queries that measure change over time should therefore use
functions such as:

```promql
rate(agent_executions_total[5m])
```

or:

```promql
increase(agent_executions_total[1h])
```

## Timer Semantics

The following timers are recorded once per Agent execution:

- `agent.execution.duration`;
- `agent.llm.duration`;
- `agent.tool.duration`;
- `agent.mcp.duration`.

`agent.execution.duration` records the total wall-clock duration of one
Agent execution.

The LLM, Tool, and MCP timers record aggregated duration values for one
Agent execution.

Their timer counts therefore represent the number of Agent executions
recorded, not the number of individual downstream calls.

For example:

```text
agent_llm_duration_seconds_count
```

does not need to equal:

```text
agent_llm_calls_total
```

One Agent execution may perform multiple LLM calls, while the aggregated
LLM duration is recorded once for that execution.

Individual call counts are exposed through:

- `agent_llm_calls_total`;
- `agent_tool_calls_total`;
- `agent_mcp_calls_total`.

Average duration may be calculated using:

```promql
agent_execution_duration_seconds_sum
/
agent_execution_duration_seconds_count
```

The same rule applies to LLM, Tool, and MCP duration metrics.

## Execution Duration SLOs

`agent.execution.duration` currently publishes service-level objective
histogram boundaries for:

- 30 seconds;
- 60 seconds;
- 120 seconds;
- 180 seconds.

The Prometheus histogram series can be used for latency distribution and
service-level analysis.

Example P95 query:

```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(agent_execution_duration_seconds_bucket[5m])
  )
)
```

## Derived Micrometer Meters

Micrometer may automatically register internal derived meters for
timers with configured service-level objectives.

Current example:

```text
agent.execution.duration.histogram
```

This meter is automatically derived from the SLO configuration of:

```text
agent.execution.duration
```

It is an internal Micrometer implementation detail and is not treated as
a stable business meter name.

The stable public contract remains:

- Micrometer Timer:

  ```text
  agent.execution.duration
  ```

- Prometheus series:

  ```text
  agent_execution_duration_seconds_count
  agent_execution_duration_seconds_sum
  agent_execution_duration_seconds_bucket
  ```

Derived meters:

- are excluded from the explicit business-meter name baseline;
- are excluded from the non-blank description requirement;
- are not consumed directly by application code;
- are not used directly in Grafana queries;
- remain subject to sensitive and high-cardinality tag restrictions.

Micrometer versions may change the internal representation or naming of
derived meters without changing the stable business metric contract.

The observability baseline test therefore distinguishes between:

```text
explicit business meters
```

and:

```text
Micrometer-derived meters
```

## Distribution Summary Semantics

The following Distribution Summaries are recorded once per Agent
execution:

- `agent.evidence.original.chars`;
- `agent.evidence.compacted.chars`.

`agent.evidence.original.chars` records the size of legal evidence before
compaction.

`agent.evidence.compacted.chars` records the size after compaction.

Evidence reduction may be calculated using:

```promql
1 -
(
  agent_evidence_compacted_chars_sum
  /
  agent_evidence_original_chars_sum
)
```

If the original evidence size is zero, the ratio is undefined and a
Grafana panel may display `N/A`.

This is expected and must not be treated as an application failure.

## Guardrail Metric Semantics

Performance guardrail events use:

```text
agent.performance.guardrail.events
```

This metric is registered dynamically when at least one performance
guardrail result is recorded.

Its current tags are:

- `level`;
- `metric`.

Example:

```text
agent_performance_guardrail_events_total{
  level="WARN",
  metric="llmCalls"
}
```

Allowed `level` values are bounded by the application enum, for example:

- `OK`;
- `WARN`;
- `CRITICAL`.

The `metric` tag must only contain a bounded, application-controlled
performance metric name.

It must not contain arbitrary user input.

## Allowed Tags

Current dynamic guardrail metrics may use:

- `level`;
- `metric`.

The common low-cardinality application tag may use:

- `application`.

Future metrics may use bounded values such as:

- `model`;
- `currency`;
- `result`;
- `evaluation_type`.

A new tag is allowed only when:

1. its possible values are bounded;
2. it does not contain user-controlled text;
3. it does not identify an individual user or tenant;
4. it does not expose credentials or business content;
5. its expected Prometheus cardinality has been reviewed.

## Forbidden Tags

The following data must not be used as metric tags:

- tenant ID;
- user ID;
- username;
- question or goal;
- conversation ID;
- request ID;
- Agent execution ID;
- JWT or access token;
- API key;
- password;
- document ID when unbounded;
- document content;
- Tool result content;
- exception message containing user-controlled data.

These values are sensitive, high-cardinality, or both.

Adding them to metric tags can cause:

- security exposure;
- personal or tenant data leakage;
- excessive Prometheus time-series growth;
- increased storage and query cost;
- degraded Prometheus and Grafana performance.

Sensitive or high-cardinality correlation values belong in controlled
structured logs or traces, not in metric labels.

## Metric Descriptions

Every explicitly registered business meter must have a non-blank
description.

The description requirement applies to:

- explicitly registered Counters;
- explicitly registered Timers;
- explicitly registered Distribution Summaries;
- dynamically registered application-owned guardrail Counters.

The description requirement does not apply to internal meters
automatically derived by Micrometer.

This distinction prevents framework implementation details from becoming
part of the application-owned observability contract.

## Prometheus Naming

Application code uses Micrometer dot notation:

```text
agent.llm.calls
agent.execution.duration
```

Prometheus exports normalized snake-case names:

```text
agent_llm_calls_total
agent_execution_duration_seconds
```

Counter metrics receive the `_total` suffix.

Timer metrics may export:

```text
_seconds_count
_seconds_sum
_seconds_max
_seconds_bucket
```

depending on the MeterRegistry and histogram configuration.

Internal Micrometer meter names must not be copied directly into PromQL
without checking the actual Prometheus exposition format.

## Baseline Test Contract

`AgentObservabilityBaselineTest` must verify:

1. stable explicit business-meter names;
2. stable Meter types;
3. cumulative Counter semantics;
4. per-execution Timer semantics;
5. Evidence Distribution Summary semantics;
6. non-blank descriptions for explicit business meters;
7. absence of sensitive and high-cardinality tags;
8. bounded guardrail tag keys;
9. existence of the configured execution-duration SLO histogram.

The baseline test must not require internal Micrometer-derived meters to:

- be part of the stable business-meter name set;
- have an application-owned description;
- remain unchanged across Micrometer versions.

Derived meters must still pass sensitive-tag validation.

## Grafana Compatibility

The following existing dashboard queries are part of the current
observability contract:

### Total Agent Executions

```promql
agent_executions_total
```

### Average Agent Duration

```promql
agent_execution_duration_seconds_sum
/
agent_execution_duration_seconds_count
```

### Total LLM Calls

```promql
agent_llm_calls_total
```

### Average LLM Duration

```promql
agent_llm_duration_seconds_sum
/
agent_llm_duration_seconds_count
```

### Tool and MCP Calls

```promql
agent_tool_calls_total
```

```promql
agent_mcp_calls_total
```

### Cache Hit Ratio

```promql
agent_cache_hits_total
/
(
  agent_cache_hits_total
  +
  agent_cache_misses_total
)
```

### Evidence Reduction

```promql
1 -
(
  agent_evidence_compacted_chars_sum
  /
  agent_evidence_original_chars_sum
)
```

### Retry and No-progress Events

```promql
agent_retries_total
```

```promql
agent_no_progress_suppressions_total
```

Changing any source metric requires updating the corresponding Grafana
query.

## Model Judge Metrics

Optional LLM-as-a-Judge evaluation uses a separate observability
boundary from normal Agent execution.

| Micrometer name | Prometheus name | Type | Semantic |
|---|---|---|---|
| `agent.evaluation.judge.calls` | `agent_evaluation_judge_calls_total` | Counter | Completed Judge evaluations, tagged with bounded `result=pass\|fail` |
| `agent.evaluation.judge.failures` | `agent_evaluation_judge_failures_total` | Counter | Judge model, parsing, or infrastructure failures |
| `agent.evaluation.judge.score` | `agent_evaluation_judge_score` | Distribution Summary | Judge overall scores from 0 to 100 |
| `agent.evaluation.judge.duration` | `agent_evaluation_judge_duration_seconds` | Timer | Judge evaluation duration |
| `agent.evaluation.judge.prompt.tokens` | `agent_evaluation_judge_prompt_tokens_total` | Counter | Judge prompt tokens |
| `agent.evaluation.judge.completion.tokens` | `agent_evaluation_judge_completion_tokens_total` | Counter | Judge completion tokens |
| `agent.evaluation.judge.total.tokens` | `agent_evaluation_judge_total_tokens_total` | Counter | Judge total tokens |
| `agent.evaluation.judge.usage.missing` | `agent_evaluation_judge_usage_missing_total` | Counter | Judge calls without complete token usage |
| `agent.evaluation.judge.cost.usd` | `agent_evaluation_judge_cost_usd_total` | Counter | Estimated Judge provider cost in USD |
| `agent.evaluation.judge.pricing.missing` | `agent_evaluation_judge_pricing_missing_total` | Counter | Judge calls with usage but without configured model pricing |

Judge model calls must not be recorded as normal Agent execution calls,
tokens, duration, or cost. The only dynamic Judge metric tag is bounded
`result=pass|fail`.

Judge metrics must not use case ID, tenant ID, user ID, question,
candidate answer, execution ID, model, or model response as tags.

## Compatibility Rules

Adding a new metric is normally backward compatible when:

- it does not change an existing metric;
- it uses bounded tags;
- it does not expose sensitive information;
- it has a non-blank description;
- its semantic meaning is documented.

Renaming, removing, or changing the type or semantic meaning of an
existing metric is a breaking observability change.

A breaking metric change requires:

1. updating this contract;
2. updating `AgentObservabilityBaselineTest`;
3. updating related Micrometer tests;
4. updating Prometheus queries;
5. updating Grafana dashboards;
6. updating alerts and release regression scripts;
7. documenting the migration;
8. completing Docker observability regression.

Changes to internal Micrometer-derived meters do not constitute breaking
changes unless the application explicitly promotes them to stable public
metrics.

## Release Verification

Before release, run:

```powershell
mvn `
    "-Dtest=AgentObservabilityBaselineTest,DefaultAgentMicrometerMetricsTest,DefaultAgentExecutionObserverTest" `
    test
```

Then run the full regression suite:

```powershell
mvn clean test
```

Verify the Prometheus endpoint:

```powershell
$metrics = (
    Invoke-WebRequest `
        -Uri "http://localhost:8080/actuator/prometheus"
).Content

$metrics |
    Select-String `
        "agent_executions_total|agent_llm_calls_total|agent_tool_calls_total|agent_mcp_calls_total|agent_cache_hits_total|agent_cache_misses_total"
```

Verify the execution-duration buckets:

```powershell
$metrics |
    Select-String "agent_execution_duration_seconds_bucket"
```

Verify that sensitive labels are absent:

```powershell
$metrics |
    Select-String `
        "tenantId=|tenant_id=|username=|question=|goal=|conversationId=|executionId=|accessToken=|password="
```

The sensitive-label check must produce no output.
````
