# Tool UX

LAI shall provide **one-shot, per-step-confirmed tool UX** — never silent auto-run.

## Current

`ToolsDashboard` (Vision/Interaction/Elevated cards, `risk` badge `READ_ONLY/INTERACTION/SENSITIVE/ELEVATED` `secondary/primary/error`, `Requires your tap` vs `Runs after screen access`, `Inspect visible screen` / `Run local OCR` one-tap for safe `screen.snapshot`/`ocr.current_screen`, `Use from Chat — ask LAI to …` for others) + `ToolConfirmationDialog` (`AlertDialog`, `Approve once` primary, `Do not run` tertiary) + `StatusCard` (`Result`).

## Target

*   **Tool card:** `Card` `surfaceVariant` (`14 dp`) → `title` (`SemiBold`) + `risk` `labelSmall` `error` for `SENSITIVE/ELEVATED` + `description` `bodySmall` `onSurfaceVariant` + `Requires your tap` `labelSmall` + `OutlinedButton` for safe tools.
*   **Confirmation:** `AlertDialog` (`review_local_action` title, `summary` `SemiBold`, `risk_label`, `tool_review_explanation` `bodySmall` `onSurfaceVariant`) — `Approve once` records `ToolAuditLedger` before `AgentRuntime.execute(..., userConfirmed=true)`, replay blocked.
*   **Result:** `StatusCard` (`Result`, `Approved action completed` / `was not completed: tool failed` / `audit completion failed`), never raw `shell` `stdout`.
*   **Progress:** `AUTOMATING` `CircularProgressIndicator` centered, `READING_SCREEN` same, `DOWNLOADING` `LinearProgressIndicator` `sbom`, `GENERATING` `Thinking locally…` notice.
*   **Audit:** `Settings → Developer → Tool audit` (`records`, `toolName`, `risk`, `userApproved`, `success`, `integrityValid`) — content-free, `install -r` keeps.

## Permissions

`Tool` `EXECUTE`/`HIGH`/`CRITICAL` → `ToolConfirmationDialog`; `READ_ONLY` → no dialog if `accessibilityConnected` (e.g. `screen.snapshot`). `ToolInstructionGate` decides `includeToolInstruction` — `hi` carries zero tool tokens.

## Testing

`ToolInstructionGate` `hi` vs `click the button`, `screen.type` `isPassword` `allowSensitiveInput false`, `shell.operation` `argv` injection (`package.force_stop` `;id`), `Shizuku` `PERMISSION_REQUIRED` → `connect_shizuku`, Xiaomi lock guide.
