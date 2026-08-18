# UI Architecture

LAI shall provide **one system with progressive disclosure** — not 15 top-level tabs.

## Current — Three-mode shell

`LaiApp` `Scaffold` (`imePadding()` once on `Scaffold`, `contentWindowInsets` `safeDrawing` H+Top, `TopAppBar`, `NavigationBar` `Chat/Screen Reader/Automator` keyed on `imeAnimationTarget` (not `isImeVisible`), `BackHandler` for `settingsVisible`).

*   **Chat:** `ChatScreen` (`home_title`, `home_subtitle`, `chat_history` sheet, `clearConversation`, `LazyColumn` `id`-keyed `MessageBubble` `primaryContainer/surfaceVariant` `18 dp`, `OutlinedTextField` `Send` + `Stop` watchdog `45s`, `LaunchedEffect` auto-scroll).
*   **Screen Reader:** `ScreenReaderScreen` (`readCurrentScreen`, `StatusCard` `Screen access ready`, `Screenshots stay on device`).
*   **Automator:** `AutomatorScreen` (`inspectScreen`, `Shizuku` `READY_UID_2000`, `ToolsDashboard` Vision/Interaction/Elevated cards, Xiaomi lock guide `Lock + No restrictions + Autostart`).
*   **Settings:** `SettingsScreen` (`Local-first privacy` card, `local_action_proposals` `Switch` + `developer_mode` `Switch`, `Developer` `Native runtime/Scheduler/Tool proposal parser/Tool audit/Device environment/Model load/Last generation`, `WorkspaceCard` `GRANTED/REVOKED`, `ModelSetup` `Supported/Installed/Keep copy/Load/Unload/Delete/Scan/Import`, `Support diagnostics` `CreateDocument` JSON).
*   **Sheets/Dialogs:** `QuickSettingsSheet` (`ModalBottomSheet`, `LlmSettings` `maxNewTokens` ceiling, `Apply once/Save default/Reset`), `ToolConfirmationDialog` (`AlertDialog` `Approve once/Do not run`), `ChatHistory` `ModalBottomSheet` (`LazyColumn` `id` key, `delete`).

## Future — Progressive, not dumping

**Potential areas:** `Home, Chat, Agent, Tools, Files, Documents, Projects, Developer, Terminal, Knowledge, Models, Automation, Device, Settings`.

**Not every area is a top-level tab.** Use:
*   `NavigationBar` for `Home/Chat/Agent/Tools` only (like NpuHub `features/home` + `features/models` + `features/rag` + `features/runtime` + `features/ocr/textgen`).
*   **Contextual navigation:** `Agent` → `Task` → `Pipeline` → `Verify`; `Files` → `Documents` → `RAG` tab; `Developer` → `Terminal` → `Linux`.
*   **Command palette** (`Ctrl+K` + search, like NpuHub `benchmark` search) → `model.load`, `tool.screen.snapshot`, `file.search`.
*   **Progressive disclosure:** `Developer` hidden behind `developer_mode` switch; `Advanced manual model` (today) → `Model Manager` (tomorrow `CycloneDX`); `Knowledge Graph` behind `RAG`.

## State

`MainUiState` `StateFlow` immutable aggregate (`mode`, `input`, `messages`, `operation`, `accessibilityConnected`, `shizukuState`, `installedModels`, `chatSessions`, `toolProposalCounters`, `workspace`, `thermalGovernorDetail`, …). `ChatMessage.id` keys `LazyColumn`. `WindowInsets.imeAnimationTarget` drives `NavigationBar` visibility.

## Navigation Principles

`BackHandler` for `Settings`, `imeAnimationTarget` for `NavigationBar`, `LazyColumn` stable keys for streaming, `StatusCard` everywhere, `Adaptive` `little 7 idle` not visible, `install -r` keeps `chatSessions` (`noBackupFilesDir`).
