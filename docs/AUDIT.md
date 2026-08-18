# Audit & Evidence

LAI shall provide a **tamper-evident, privacy-safe, content-free audit**.

## Current

*   **ToolAuditLedger:** `APP_PRIVATE_HASH_CHAIN_V1` `tool_audit.jsonl` in `noBackupFilesDir`, `hash = SHA-256(prevHash + canonicalJson)`, `fsync` before return, `ToolAuditRepository.snapshot()` full-chain verify (`integrityValid`), `takeLast 50`, `records` include `toolName/risk/outcome( USER_APPROVED/USER_DENIED/EXECUTION_SUCCEEDED/EXECUTION_FAILED )/timestampEpochMs` — no prompts, no screenshots, no tool args/outputs.
*   **Approval-before-authority:** `recordDecision(approved=true)` → `ToolAuditLedger` → `AgentRuntime.execute(..., userConfirmed=true)` → `recordCompletion(success)` — two validations, replay blocked (`exact call` check), `sensitive text` entry forbidden, model text can never self-approve.
*   **Evidence states:** `AVAILABLE` (loader found, e.g. `libvulkan.so` on Adreno 825) → `SUPPORTED` (model validated) → `ACTIVE` (executing) → `MEASURED` (value exists, e.g. `16 tok/s` on `0.1.139`) → `UNKNOWN` (insufficient). `MEASURED` without value is invalid → `N/A`.
*   **Diagnostics:** `DiagnosticsReportV1` (`schemaVersion 1`, `generatedAtEpochMs`, `app/device/runtime/models/performance/privacy/automation`) — `excludedData` (`prompts/generated_text/screenshots/ocr_text/...`), exported only via SAF `CreateDocument` on explicit user tap, `prettyPrint true`.

## Target

*   **Per-tool/per-step:** `tool/timestamp/authorization (userApproved)/input digest (where appropriate, e.g. `sha256` of `models/LAI/models/*.gguf`)/result/verification/failure` — per `AGENT.md` `Verification` step (tool `success` ≠ task `succeeded`).
*   **Scheduler telemetry:** `SchedulerDecisionTelemetry` (battery %/charging/thermalState, `BackendCapability` `evidence`, `rejectionReasons`, `selected` — no `requestId/input/modelPath/modelBytes/consent/grant/output`), exported via `benchmark` as deterministic `JSON/Markdown/CSV` (NpuHub `benchmark`).
*   **One-run grants:** `OcrRunPreparationGrant`-style `60s` evidence, `5-min` preview, single-use, atomic, `authorizesExecution=false` until resolver — for `core:agent`/`core:rag` as in NpuHub `LOCAL_AI.md`.
*   **Emergency stop:** `pin_to_little_cores()`, `kv_tokens_.clear()`, `generationStage` reset — audited as `CANCELLED`.

## Verification (never invent)

`validate_repo.sh` (hash history clean), `check_architecture_boundaries.py`, `coverageCheck` `169` tests + `JaCoCo` ratchets, `DEVICE TEST REQUIRED` for `MEASURED` claim. Every completed task must update `DEVELOPMENT_STATE.md` and `docs/device-results/`.

## Privacy

`privacy.excludedData` (`14` categories), `localOnlyUntilUserExport true`, `install -r` keeps `tool_audit.jsonl` (no-backup, not `SAF`).
