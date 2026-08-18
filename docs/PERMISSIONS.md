# Permissions

LAI shall be **explicit, fail-closed, and user-controlled** — no silent grants.

## Current

*   **Accessibility:** `AccessibilityAutomationService` (`BIND_ACCESSIBILITY_SERVICE`, `canRetrieveWindowContent`, `canTakeScreenshot`, `flagIncludeNotImportantViews`, `notificationTimeout 100`) — user must enable via `Settings.ACTION_ACCESSIBILITY_SETTINGS`, Xiaomi lock guide (`Lock + No restrictions + Autostart`), weak `AccessibilityGateway` ref, `rootInActiveWindow` recycled.
*   **Shizuku:** `ShizukuController` `READY_UID_2000` (`PERMISSION_REQUIRED` → `requestPermission()`, `argv` allowlist), `PrivilegedUserService` `AIDL` (`IPrivilegedService`), no raw shell.
*   **Storage:** SAF `ACTION_OPEN_DOCUMENT_TREE` (`WorkspaceRepository` persistable `GRANTED/REVOKED/NOT_GRANTED`), never `MANAGE_EXTERNAL_STORAGE`, never `content://`→`File` path. `model`/`models` auto-import from `storage/LAI/models` on `install -r`.
*   **Network:** Only `platform:download` holds `INTERNET` (for signed `models-v1.json` + reviewed GGUF); `validate_repo.sh` + `check_architecture_boundaries` enforce it.

## Target (NpuHub-grade)

*   **Discovery:** `AVAILABLE` (loader/library found) → `SUPPORTED` (workload/model validated) → `ACTIVE` (executing) → `MEASURED` (value). Unmeasured = `N/A`.
*   **Grants:** `OBSERVE < SUGGEST < PREPARE < EXECUTE < ADVANCED` + `READ_ONLY < INTERACTION < SENSITIVE < ELEVATED < HIGH < CRITICAL`. `EXECUTE`/`HIGH`/`CRITICAL` require explicit **one-run preview + approval** (`60s` evidence, `5-min` window, atomic one-use `OcrRunPreparationGrant`-style). Payment/account/credential/message-send/install/destructive file → purpose-built confirmations.
*   **Scopes:** `one-run` (single tool call, as today `ToolAuditLedger`), `task-scoped` (from `AGENT.md` `Task`), `automatic` (only `READ_ONLY`/`OBSERVE`).
*   **UI:** Per-step `Approve/Deny` dialog shows `risk` + `summary` + `rejectionCodes`, never model text as authority. `Emergency stop` is always visible.

## Dangerous Operations

`shell.operation` `argv` allowlist (`settings.get`, `device.info`, `package.*`, etc.) is the only elevated path; `rm -rf` / `su` / `sandbox bypass` / `credential harvesting` are out of scope.

## Testing

`Adv-` loops (self-approval, prompt injection), `REVOKED` → `GRANTED` re-grant, `install -r` persistence, `WorkManager` kill mid-grant, `Shizuku` `UID 2000` vs `PERMISSION_REQUIRED`.
