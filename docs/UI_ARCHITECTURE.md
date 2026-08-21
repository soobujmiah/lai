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

## 2026-08-21 Phase 2 functional Material 3 integration

This phase continues after PR #15 and keeps `MainUiState`/`MainViewModel` as the authority for UI state and actions. The Material 3 screens added in the foundation phase are now bound to real state through presentation adapters in `app/src/main/java/dev/lai/runtime/ui/screens/LaiUiStateAdapters.kt`.

### Navigation

`LaiApp` now uses an adaptive Material 3 shell:

- compact widths use `NavigationBar`;
- expanded widths use `NavigationRail`;
- Chat, Screen Reader, and Automator still call `MainViewModel.setMode(UiMode)` and preserve the existing runtime modes;
- Models, Workspace, Providers, and Settings are presentation destinations layered over the existing state rather than replacement architecture.

Settings continues to use `MainUiState.settingsVisible` and the existing quick-settings/session flow. Chat history, quick settings, pending tool approval, streaming cancellation, Accessibility, Shizuku, diagnostics, and the existing chat semantics remain in the original ViewModel paths.

### Models surface

`LaiModelsScreen` consumes `MainUiState.toModelsUi()` and calls only existing ViewModel actions:

- `refreshSupportedModels()`;
- `installRecommendedModel()` / `installSupportedModel(modelId)`;
- `loadModel(modelId)`;
- `unloadModel()`;
- `deleteModel(modelId)`.

The screen shows installed versus reviewed catalog models, active model state, recommended status, architecture, quantization, artifact format, size, reviewed evidence, compatible/preferred/fallback backend labels, download progress, and runtime/scheduler detail. It does not perform repository, storage, or inference work directly.

### Workspace surface

`LaiWorkspaceScreen` uses the existing `ACTION_OPEN_DOCUMENT_TREE` launcher and calls the current workspace actions:

- `grantWorkspace(treeUri)`;
- `revokeWorkspace()`;
- `refreshWorkspace()`;
- `scanWorkspaceModels()`.

The screen intentionally exposes only coarse status: grant state, settings source/status, reviewed model count, local-unreviewed count, and scan status. Raw file paths, document IDs, filenames, and hashes remain behind the SAF workspace boundary.

### Provider surface

`LaiProvidersScreen` reflects the current provider/runtime state without faking adapters. It identifies local runtime, CPU fallback, GPU acceleration qualification status, and planned cloud-provider routes. Cloud providers are shown as not active until a real provider adapter and policy-backed configuration exist behind the AI Gateway/provider abstraction. No Compose code imports provider SDKs.

### Runtime and acceleration boundary

The UI does not mark GPU/Vulkan stable from presence alone. Qualcomm/Adreno acceleration remains gated by compile/runtime probing and device-validation evidence. CPU fallback remains visible and functional as the default safe route.

### Validation notes

Local source/documentation policy was run after this change. Full Android build requires the repository CI toolchain or a local Android SDK/JDK 17 environment. The sandbox used for this change did not include the repository's requested `./gradlew` wrapper or Android SDK; a downloaded Gradle 9.5.0 plus JDK 17 failed in the constrained sandbox when the daemon was killed during dependency instrumentation. CI remains the authoritative Android build validation path.
