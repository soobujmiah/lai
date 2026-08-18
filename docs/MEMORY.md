# Memory

LAI shall provide **bounded, auditable, user-controlled memory** — not an unbounded chat log.

## Current

*   **Conversation:** `ContextWindowPolicy.applyTurnWindow(history, keepLastTurns)` (keeps last `N` turns + in-flight request, drops from front, reports `windowedConversationTurns` + `trimmedConversationTurns`), `ChatHistoryRepository` (`≤100` sessions, `≤512` msgs, atomic `noBackupFilesDir`, `StoredChatSession` `id/title/createdAt/updatedAt/messages`), `LazyColumn` stable `id` keys.
*   **No** task/project/user memory beyond that.

## Target

*   **Types:** `conversation` (current, `ContextWindowPolicy`), `task` (from `AGENT.md` `Task` — plan/steps/observations/verifications), `project` (`storage/LAI/<project>/` — `WorkspaceRepository` grant + `WorkspaceSettingsStore` typed `settings.json`), `user preferences` (`LlmSettings` + `SettingsDocumentV1` ranges/defaults, no free-text).
*   **Operations:** `store/retrieve/delete` (bounded, `maxRegisteredFiles 64` + `4 GiB` file cap per `DiscoveryLimits`), `provenance` (timestamp + `sha256` where appropriate), `retrieval` (via `RAG` lexical → dense, `KNOWLEDGE_GRAPH` relations), `deletion` (explicit user tap, `ToolAuditLedger` entry), `preferences` (typed, validated, `NaN/infinity` safe).
*   **Privacy:** `DiagnosticsPrivacy` excludes `prompts/generated_text/screenshots/ocr_text/...`; `ChatHistoryRepository` is `no-backup`; `ToolAuditLedger` is content-free (no tool args/outputs). Memory never auto-uploads.
*   **Bounds:** `64` files + `4 GiB` per file + `100` sessions + `512` msgs — all enforced by pure `WorkspacePolicy`/`ContextWindowPolicy`/`ChatHistoryRepository`. No unbounded growth.

## Audit & Control

Every `store`/`delete` writes `ToolAuditLedger` (`tool: memory.*`, `risk` appropriately) — user can export `DiagnosticsReportV1` and inspect `ToolAuditHistory`. `Emergency stop` clears in-flight memory writes.

## Testing

`ContextWindowPolicy` `keepLastTurns` 0–64, `trimmedConversationTurns` vs `windowedConversationTurns`, `ChatHistoryRepository` `≤100` eviction, `512` msg cap, atomic write + `install -r` persistence, `WorkManager` kill mid-save.
