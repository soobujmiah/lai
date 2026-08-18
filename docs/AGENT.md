# Agent

LAI shall provide a **bounded, auditable, user-controlled multi-step agent** — not an autonomous loop.

## Target Workflow

```text
USER INTENT → CONTEXT → PLAN → MEMORY → TOOL SELECTION → PERMISSION/APPROVAL
→ EXECUTION → OBSERVATION → VERIFICATION → NEXT STEP → FINAL RESULT
```

Planning, execution, observation, and verification are distinct. `Tool returned successfully ≠ task succeeded`.

## Current State

**One-shot only:** `AgentRuntime.parseToolProposal` (bounded JSON, per-tool schema, `ToolInstructionGate` recall-biased), `BuiltInToolCatalog` (8 primary tools + `shell.operation` allowlist), `ToolAuditLedger` (hash-chained `APP_PRIVATE_HASH_CHAIN_V1`, fsync, approval-before-authority, replay guard), `ToolsDashboard` (Vision/Interaction/Elevated cards). No planner, no memory, no multi-step.

## Future Architecture (additive)

*   **Intent:** Natural-language intent matching (EN word-boundary + Bangla stems) → typed `Agent intent` (NpuHub `core:agent` 7.1).
*   **Context & Memory:** Bounded `core:agent` memory (auditable, task-scoped) + `ContextWindowPolicy` (`keepLastTurns`) + `ChatHistoryRepository` (already).
*   **Planning:** Deterministic planner → DAG via `core:pipeline` (validate, order, cycle detect) → `Tool selection` under `AgentPolicy` (permission `OBSERVE/SUGGEST/PREPARE/EXECUTE/ADVANCED`, risk `READ_ONLY/INTERACTION/SENSITIVE/ELEVATED`).
*   **Permission/Approval:** Per-step confirmation preview (exact validated `call` + `risk` + `summary`), one-run grants (NpuHub `core:agent` 7.2), emergency stop, global stop. **Fail-closed** — if authority unavailable, do not bypass.
*   **Execution:** `AgentRuntime.execute(call, userConfirmed)` (argv allowlist, bounds, timeout), `ElevatedShell`/`AccessibilityGateway` foreground-bound, service-death recovery.
*   **Observation:** Typed `ToolResult` (structured output, not raw log) → `MEMORY`.
*   **Verification:** Independent `Verification` step (not just `tool.success`) → `Audit` (`ToolAuditLedger` + `Verification` record) → `Next step` or `Final result` with citations.

## Task State & Control

Task has `ID, goal, plan, context, steps, status (PLANNED/RUNNING/PAUSED/CANCELLED/FAILED/SUCCEEDED)`, bounded retries, `maxSteps`/`timeout`, `cancellation` (cooperative via `isCancelled` + watchdog `45s`), `emergency stop` (global, immediate, `kv_tokens_` cleared).

## Audit & Evidence

Every step records `tool, timestamp, authorization (userApproved), input digest (where appropriate), result, verification, failure` — content-free (no prompts, no screenshots). States `AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN` — unmeasured = `N/A`.

## UI

`Agent` tab shows `TASK → PLAN → STEP → VERIFY → RESULT`, with per-step `Approve/Deny/Pause/Cancel/Stop/Inspect/Resume`. Chat shows compact `Proposed local action: …` + `Review` dialog, not raw JSON.

## Testing

Adversarial loops (prompt injection, self-approval), budget/timeout, service-death/restart, `WorkManager` kill, `install -r` grant persistence, replay, `install -r` after one-run grant expiry.

## Rollback

One-shot mode remains fallback; incomplete tasks are safely abandoned/resumed per `Task` policy; no model-authored confirmation is ever trusted.
