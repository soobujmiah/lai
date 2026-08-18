# Agent UX

LAI Agent shall provide a **visual, pausable, auditable task experience** — not a hidden loop.

## Current

**One-shot only:** `ToolConfirmationDialog` (`review_local_action` + `risk_label` + `tool_review_explanation`) → `Approve once` / `Do not run` → `ToolAuditLedger` (`hash chain`, `integrityValid`). No `Agent` tab yet.

## Target

```text
TASK (goal, ID) → PLAN (DAG via core:pipeline, dependencies, retries, timeout)
→ STEP 1 (tool, inputs, risk) → STEP 2 → VERIFY (independent, not tool.success)
→ RESULT (citations, provenance)
```

**Per-step controls:** `Approve` / `Deny` (per-tool `risk` + `ToolInstructionGate`), `Pause` / `Cancel` (`isCancelled` + `45s` watchdog), `Stop` (global `emergency stop` → `pin_to_little` + `kv_tokens_.clear()`), `Inspect` (show `ToolResult` JSON + `Audit` record), `Resume` when `thermal NOMINAL` + `auditIntegrityValid`.

**Task list:** `Agent` tab `LazyColumn` (`TASK` card: `title` + `status` `PLANNED/RUNNING/PAUSED/CANCELLED/FAILED/SUCCEEDED` + `progress` + `estimatedPeakBytes` + `thermalGovernorDetail`). `Plan` is collapsible `DAG` (like `PIPELINE.md`), `Step` is `Card` (`toolName` + `risk` badge + `input digest` + `output` + `verification`).

**Verification:** `Independent verification` step (not `tool.success`) → `ToolAuditLedger` + `Verification` record (`tool/timestamp/authorization/digest/result/verification/failure`).

**History:** `Task` persists via `platform:history`-like `noBackupFilesDir` (`≤50` records), survives `install -r`, exported via `benchmark` `JSON/Markdown/CSV` (no prompts).

## Testing

`multi-step` `5` tools (`screen.snapshot → ocr.current_screen → screen.type`), `cancellation` between `llama_decode` chunks, `emergency stop` during `prefill 32/93`, `service-death` (`AccessibilityGateway` `WeakReference`), `install -r` grant persistence.

## Rollback

`one-shot` remains fallback; `Task` that fails validation never runs; `incomplete` → `FAILED` + `audit` + `install -r` survives.
