# Security

LAI shall be **fail-closed, evidence-based, auditable** — never bypass authority.

## Principles

Offline-first where practical, privacy-first, `FAIL CLOSED` — if authority unavailable, do not silently bypass. Explicit permissions, user-controlled automation, auditable actions, modular.

## Current

*   `validate_repo.sh` (`128 MB` cap, no `*.apk/*.so/*.gguf/*.jks`, token scan), `check_architecture_boundaries.py` (`network` only in `platform:download`, `audit` only in `platform:audit`), `check_docs_links.py`.
*   `ToolAuditLedger` hash-chained `APP_PRIVATE_HASH_CHAIN_V1`, `fsync`, approval-before-authority, replay guard, `integrityValid`, `install -r` persistence.
*   `AgentPolicy` (`OBSERVE/SUGGEST/PREPARE/EXECUTE/ADVANCED` + risk `READ_ONLY/INTERACTION/SENSITIVE/ELEVATED/HIGH/CRITICAL`), `ElevatedShell` `argv` allowlist (no raw `sh -c`), `ACCESSIBILITY_REQUIRED` / `ELEVATED_SERVICE_REQUIRED` / `CONFIRMATION_REQUIRED`.
*   No `MANAGE_EXTERNAL_STORAGE`, SAF `ACTION_OPEN_DOCUMENT_TREE` only, `noBackupFilesDir` for history/audit.
*   Signed `lai-release` RSA-4096 V1–V4 `80:03:8D…7E:8E`, `PRODUCTION_SIGNED` gating.

## Target

*   **Permission discovery:** `AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN` — unmeasured = `N/A`, never claim.
*   **Grants:** Explicit one-run / task-scoped grants (NpuHub `core:agent` 7.2), short-lived (60s evidence, 5-min preview), single-use, atomic claim, bounded `DiscoveryLimits` (`64` files, `4 GiB`), `CANCEL_GRACE_MS 45s` watchdog already.
*   **Operations:** `destructive`/`network`/`sensitive` require purpose-built safeguards, `payment/account/credential` out of scope.
*   **Network:** Only `platform:download` may fetch `huggingface.co`/`raw.githubusercontent.com` allowlist for signed catalog + reviewed GGUF (SHA-256 + size exact), no user content upload.
*   **Storage:** App-private `SQLCipher` + `Keystore` for RAG/knowledge, versioned migrations, explicit `delete/export`.
*   **Emergency:** Global `Stop` / `emergency stop` — `generationStage` (`IDLE→COUNTING_TOKENS→AWAITING_FIRST_TOKEN→STREAMING→COMPLETED`), `cancelWatchdogJob`, `kv_tokens_` cleared on cancel/error, `pin_to_little` on idle.

## Threat Boundaries

`core` cannot import `platform`/`runtime`/`vendor`; `platform` never depends upward; `runtime` owns no UI; `app` is only composition root. Verified on CI + `DEVICE TEST REQUIRED` for thermal/QNN/OCR.

## Audit

See `AUDIT.md` — tamper-evident `tool/timestamp/authorization/digest/result/verification` with `ToolAuditLedger`.
