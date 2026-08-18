# Pipeline / Workflow Engine

LAI shall provide a **future pipeline abstraction** for sequential/parallel steps, dependencies, conditional branches, retries, timeouts, cancellation, verification, and failure handling — validated as a DAG.

## Current

**Missing** — no `core:pipeline`. One-shot `AgentRuntime` is the only execution path.

## Target

*   **Graph:** `Node { id, tool, inputs, outputs, dependsOn[], condition, retry, timeout, verification }` + `Edge`.
*   **Validation:** `core:pipeline` pure JVM validates: no cycles (topological order), no missing/duplicate ids, no dangling dependencies, bounded size, deterministic.
*   **Execution:** `AgentRuntime` or `PipelineRunner` expands `Plan` → `DAG` → ordered `Execution` (parallel where `dependsOn` allows, sequential otherwise), bounded concurrency (e.g. `max 2` on SM8735 to stay < `little 7 idle → big 0-3 burst`), per-step `cancellation` (`isCancelled`), `emergency stop`, `retry` (bounded, backoff), `timeout` (per-step + global), `observation` (typed `ToolResult`), `verification` (independent, not `success`), `failure` (canonical codes, no stack leak).
*   **Artifacts:** `Task` (from `AGENT.md`) owns the `Pipeline` instance; `Task` state is `PLANNED/RUNNING/PAUSED/CANCELLED/FAILED/SUCCEEDED` + `Step` states.
*   **Audit:** Each step writes `ToolAuditLedger` + `Verification` record; `Pipeline` itself is versioned and exportable as `benchmark` JSON (no prompts).

## API (sketch)

```kotlin
PipelineValidator.validate(nodes): Result<Pipeline> // cycle + bounds
PipelineRunner.execute(pipeline, grants, isCancelled): Flow<PipelineEvent> // StepStarted/Completed/Failed/Verification
```

## Testing

Cycle detection, duplicate id, oversized graph, parallel limit under thermal, cancellation between `llama_decode` chunks, `install -r` grant survival, `WorkManager` kill mid-pipeline.

## Rollback

A `Pipeline` that fails validation never runs. A running `Pipeline` that is cancelled clears `kv_tokens_` and returns to `IDLE` (no partial KV overstate).
