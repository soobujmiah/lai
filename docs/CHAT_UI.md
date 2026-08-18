# Chat UX

LAI Chat shall provide **streaming, contextual, cancellable, auditable conversation** — compact, not noisy.

## Current

`ChatScreen` (`home_title` `headLineSmall` + `home_subtitle` `onSurfaceVariant`, `chat_history` + `New chat` `TextButton`, `LazyColumn` `id`-keyed `MessageBubble` `0.86f` `18 dp` `primaryContainer/surfaceVariant` `bodyLarge`, `OutlinedTextField` `message_hint` `ImeAction.Send` + `Send`/`Stop` (`CANCELLING` → `Stopping…`), `LaunchedEffect` auto-scroll to `lastIndex` on `messages.size/last.text`).

**Streaming:** `callbackFlow` + `trySendBlocking` + `buffer(256)` — `InferenceEvent.Token` appends to last `ChatMessage` (`fromUser false`), only that row recomposes.

**Tool execution:** `ToolConfirmationDialog` (`review_local_action`, `risk_label`, `tool_review_explanation`) — `Approve once` → `ToolAuditRepository.recordDecision` → `AgentRuntime.execute(..., userConfirmed=true)` → `recordCompletion` → `ChatMessage` `Approved action completed` / `was not completed` (never raw `shell` output).

**Progress:** `Thinking locally… the first words can take a few seconds.` notice on `GENERATING` (covers `COUNTING_TOKENS → AWAITING_FIRST_TOKEN` `45s` watchdog), `LinearProgressIndicator` for downloads (`sbom`), `CircularProgressIndicator` centered for `AUTOMATING`.

**Citations:** Not yet — future `RAG` will add `Citations` strip (like NpuHub `features:rag`).

**Attachments:** `model/README` `storage/LAI/models` auto-import, `Keep copy` export, `Import file` picker (`*/*`).

**Agent tasks:** Future `Agent` tab will show `TASK → PLAN → STEP → VERIFY → RESULT` with per-step `Approve/Deny/Pause/Cancel`.

**Errors:** `Stalled at AWAITING_FIRST_TOKEN for over 45000 ms` (your `0.6.126`), `Model not loaded`, `Thermal SEVERE` pause (`ফোন গরম… / Paused because device is too hot`), `install -r` keeps `ChatHistoryRepository` (`100` sessions, `512` msgs, atomic).

**Verification:** `GenerationMetrics` (`promptTokensPerSecond`, `decodeTokensPerSecond`) in `Settings → Developer → Last generation`, not in chat bubble.

## Long-running

Compact `LinearProgressIndicator` + `Notice` (`Reduced CPU threads…`), never `logcat` in chat. `Stop` → `CANCELLING` → `45s` watchdog → `restoreAfterStoppedGeneration` (drop empty bubble, `kv_tokens_.clear()`, `pin_to_little`).

## Attachments (future)

`files/images/code` via `ACTION_OPEN_DOCUMENT` (`*/*`), `Tool` `input` as `JsonObject`, `RAG` citations, `code` blocks (monospace, `14 dp` padding, copy button).

## Testing

`LaunchedEffect` scroll on `0.1.139` `93→11` reuse, `Stop` `45s` on `334` prefill, `install -r` history `ChatHistoryRepository` atomic, `ToolInstructionGate` `hi` = zero tokens.
