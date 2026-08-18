# Current implementation state

Audit date: 2026-08-18 (M1 alignment)  
Source baseline: working tree aligned to 16 Gradle modules (`platform:history` present)  
Audit method: inspected source, tests, Gradle files, native code, manifests, scripts, CI, catalog, existing documentation, and recorded device evidence. The repository source gate was executed and passed. A full Android build was not rerun in this audit environment.

Status vocabulary is restricted to **IMPLEMENTED**, **PARTIAL**, **EXPERIMENTAL**, **PLANNED**, **MISSING**, **DEPRECATED**, and **UNKNOWN**. Status means source state, not device or production validation; evidence is stated separately.

## Repository and build

- **IMPLEMENTED:** 16-module Gradle graph (`settings.gradle.kts` includes `platform:history`), Kotlin 2.4.10, AGP 9.3.1, Compose BOM 2025.05.01, JDK 17 target, compile/target SDK 36, arm64 native runtime.
- **IMPLEMENTED:** source-only 128 MiB policy; APK/AAB/AAR/SO/model/keystore/wrapper-JAR and token-pattern rejection.
- **IMPLEMENTED:** CI installs Gradle 9.5.0, Android SDK/Build Tools 36, NDK 27, CMake 3.22.1, and pinned llama.cpp commit; runs unit/coverage/lint/native/APK jobs.
- **PARTIAL:** release signing supports secrets but falls back to debug signing; SBOM, attestations, dependency verification, immutable action pins, production key operations, and reproducible comparison are missing.
- **PARTIAL:** local reproducibility is limited by the intentionally omitted Gradle wrapper JAR and external toolchain bootstrap.

## Modules and boundaries

- **IMPLEMENTED:** `core:contracts`, `core:policy`, `core:scheduler`, `core:model`, `plugins:api`.
- **IMPLEMENTED:** `platform:download`, `platform:audit`, `platform:device`, `platform:accessibility`, `platform:workspace`, `platform:shizuku`, `platform:history`.
- **IMPLEMENTED:** `runtime:llama`, `runtime:ocr`, `runtime:orchestrator`, and `app` composition.
- **IMPLEMENTED:** architecture script checks network ownership, Android authority, JNI/native ownership, analytics patterns, vendor terms, and module direction.
- **PARTIAL:** `MainViewModel` remains a large orchestration hotspot; feature UI modules are not split. An app-module coordinator test exists.

## Source-verified class and interface inventory

| Module | Principal declarations found in source |
|---|---|
| `app` | `LaiApplication`, `MainActivity`, `AppContainer`, `AppRuntimeEvent`, `MainViewModel`, `MainUiState`, `UiMode`, `RuntimeOperation`, `QuickSettingsSheet`, `ToolsDashboard`, `WorkspaceSettingsCoordinator` |
| `core:contracts` | `InferenceEngine`; inference/backend/model events and records; `ToolCall`, `ToolResult`, `ToolDefinition`; audit, automation, OCR, settings, shell, diagnostics, and workspace contracts |
| `core:policy` | `AgentPolicy`, `BuiltInToolCatalog`, `ToolCallParser`, `ToolAuditLedger`, `LocalFirstPolicy`, `SettingsPolicy`, `ContextWindowPolicy`, `ShellCommandPolicy`, `WorkspacePolicy`, `WorkspaceSettingsCodec`, `AtomicNamedDocumentReplace` |
| `core:scheduler` | `InferenceScheduler`, `ModelMemoryEstimator`, `ThermalGovernorPolicy`, `DeviceProfile`, `BackendCapability`, `RuntimeEnvironment`, scheduling decisions/evaluations |
| `core:model` | `ReviewedModelCatalog`, `ReviewedModel`, `ReviewedModelCatalogDocument`, artifact review state |
| `platform:download` | `ModelRepository`, `RemoteModelCatalogRepository`, `ModelDownloadWorker`, bounded stream-copy guard |
| `platform:history` | `ChatHistoryRepository` (app-private no-backup sessions) |
| `platform:audit` | `ToolAuditRepository` |
| `platform:device` | `AndroidRuntimeEnvironmentProvider` |
| `platform:accessibility` | `AccessibilityAutomationService`, `AccessibilityGateway`, node snapshot support |
| `platform:workspace` | `WorkspaceRepository`, `WorkspaceSaf`, `WorkspaceSettingsStore`, `WorkspaceDiscovery`, `ModelFormatDetector` |
| `platform:shizuku` | `ShizukuController`, `ElevatedShell`, `PrivilegedUserService`, AIDL client/service boundary |
| `runtime:llama` | `NativeInferenceEngine`, `NativeBindings`, `NativeTokenCallback`; C++ `Backend`, `BackendSession`, CPU implementation and JNI functions |
| `runtime:ocr` | `OcrEngine`, `BanglaOcrService`, `PlaceholderBanglaOcrEngine`, `OcrModelRequiredException` |
| `runtime:orchestrator` | `AgentRuntime` |
| `plugins:api` | `PluginManifest`, `PluginExecutionContext`, `PluginResult`, `LaiPlugin`, capability/risk/data-policy enums |

This inventory records principal public/internal declarations rather than every private helper. Exact source and tests remain authoritative.

## Application and UX

- **IMPLEMENTED:** one application ID/upgrade path, Compose shell, Chat/Screen Reader/Automator modes, theme, Bangla resources, Developer Mode controls.
- **IMPLEMENTED:** explicit model install/import/export/load/unload, local chat streaming, stop request, new chat, model/runtime metrics, diagnostics export.
- **PARTIAL:** chat context trims old turns but lacks the specified rolling summary/checkpoint system and stress evidence.
- **PARTIAL:** typed settings/workspace contracts and adapters exist, but ViewModel wiring, quick-settings sheet, workspace grant/status UI, and model discovery UI are not implemented.
- **PLANNED:** standalone Tools Dashboard and Chat “Attach Tools.”
- **MISSING:** tablet/foldable workstation layout, command palette, universal search, editor, terminal, Git workbench, build center, project browser.

## Model system and networking

- **IMPLEMENTED:** embedded reviewed model catalog; signed web catalog with ECDSA verification, cache/fallback, revision check, backend/ABI/context/memory metadata.
- **IMPLEMENTED:** explicit HTTPS reviewed-host model download, SHA-256, optional exact size, GGUF magic, app-private activation/registry, Range resume, local SAF import, retained-copy export with destination re-verification.
- **IMPLEMENTED:** only `platform:download` declares Internet/network-state permissions; `LocalFirstPolicy` denies outbound and private/generated network data.
- **IMPLEMENTED:** download/import copy loops now enforce a pre-write final-artifact ceiling from reviewed expected size or available storage minus the 256 MiB reserve, with an 8 GiB absolute cap; deceptive/unknown lengths cannot write beyond the bound. Dedicated unit tests cover inclusive, overflow, and resume-base behavior; CI-verified (run #154 and later, `platform:download` tests green).
- **PARTIAL:** process-death recovery, foreground pause/resume/cancel, and fully recoverable registry/cache replacement are still absent.
- **PLANNED:** categorized Model Center and process-resilient foreground pause/resume/cancel downloads.
- **MISSING:** model license registry/UI, automated benchmark center, provenance beyond current signed metadata, storage-pressure manager.

## Inference and device scheduling

- **IMPLEMENTED:** provider-neutral `InferenceEngine`, opaque backend IDs/descriptors, streamed events, token count, metrics, explicit session close.
- **IMPLEMENTED:** Android device/environment profile and scheduler checks ABI, format, quantization, backend evidence, memory, battery, and thermal admission.
- **IMPLEMENTED:** JNI/C++ backend/session boundary and real llama.cpp CPU adapter.
- **PARTIAL:** cancellation, UTF-8 streaming, context growth, memory-pressure handling, and sustained thermal recovery need broader physical validation.
- **PLANNED:** llama Vulkan backend, dedicated QNN/QAIRT adapter, closed-loop throttling, bounded native micro-model task graph.
- **MISSING:** Universal AI Gateway, provider registry/router/context manager/usage tracker, managed localhost server, remote providers, remote server manager.

## Agent, automation, and audit

- **IMPLEMENTED:** strict bounded whole-response tool parser, exact schemas, canonical tool registry, second dispatch validation, risk/consent policy.
- **IMPLEMENTED:** one-shot proposal review, content-free persistent hash-chain audit, fsync before execution, exact-call replay guard, privacy-safe outcome counters.
- **IMPLEMENTED:** Accessibility connection, fresh-root bounded snapshot, click/type/scroll/global/launch, screenshot; password text omission.
- **IMPLEMENTED:** Shizuku binder/permission/UID state, dedicated UserService, named structured operations, validated argv, timeout and output cap; no raw shell API.
- **PARTIAL:** model exact-format proposal compliance, foreground binding, result provenance, service-death recovery, and broad physical selector/action testing remain open.
- **MISSING:** multi-step planner/executor, task center, persistent task history, checkpoints, retries/budgets, diff-first project mutations, verification engine, rollback/compensation.

## Workspace, settings, and storage

- **IMPLEMENTED:** typed settings v1, ranges/defaults, validation/sanitize/migration, bounded codec, no free-text setting fields.
- **IMPLEMENTED:** SAF tree grant/revoke/layout, no broad storage permission, settings store, bounded-depth/count discovery, GGUF magic and SHA-256 classification.
- **PARTIAL:** discovery enforces the configured file-size policy after hashing rather than during reads and has no time deadline; unknown/lying provider lengths are a resource risk.
- **IMPLEMENTED:** settings persist through `AtomicNamedDocumentReplace`: new bytes go to `settings.json.tmp`, the live file is parked as `settings.json.bak`, then the temp is renamed into place. A failed finalize restores the backup. Load recovers a leftover backup if the live file is missing. Last-known-good bytes are not deleted before the new file is finalized.
- **PARTIAL:** physical SAF grant/discovery on a named device remains pending.
- **MISSING:** project-centric storage, project trust, snapshots/checkpoints, encrypted backup, generalized recovery manager, secret references.

## OCR and multimodal

- **IMPLEMENTED:** OCR interface, Bangla/English request, versioned blocks/confidence/polygon/handwriting schema, screenshot-to-engine pipeline.
- **PARTIAL:** default engine is an explicit model-required placeholder; no recognition model is integrated.
- **PLANNED:** printed/handwritten Bangla OCR, STT, TTS/barge-in, embeddings, image generation, multimodal attachments.
- **MISSING:** RAG parser/chunker/index/retriever/context pipeline, audio/video runtime, production multimodal provider architecture.

## Plugins

- **PARTIAL:** plugin manifest/API v1, capabilities, risk, local-only data policy, schemas, validation, and approved-tool execution context exist.
- **MISSING:** package format, discovery, installation, signature verification, manager, sandbox/isolation, lifecycle, UI contributions, compatibility migration, audit, rollback, store.

## Security, privacy, and permissions

Declared Android permissions after manifest merge originate from modules: `INTERNET`, `ACCESS_NETWORK_STATE`, Accessibility service binding, and Shizuku provider permission requirements. The app does not request `MANAGE_EXTERNAL_STORAGE`.

- **IMPLEMENTED:** local-first policy, no cleartext traffic, backup disabled, explicit SAF export/import, private/no-backup model and audit storage, permission-gated Android authority.
- **IMPLEMENTED:** security documentation, privacy invariants, diagnostics privacy schema, token-pattern source scan.
- **PARTIAL:** no screenshot redaction beyond password-node omission; unkeyed audit chain does not resist root rewrite; native fuzzing absent; CI supply-chain hardening incomplete.
- **MISSING:** project trust, secret manager, localhost/LAN security model, plugin permission dashboard, incident-response runbook, SBOM/provenance.

## Tests and evidence

Thirty test source files exist: 24 pure-JVM/core/plugin tests, 4 Android-library tests (`platform:audit`, `platform:download` bounds, `platform:history`, `platform:workspace` format detector), and 1 app coordinator test, plus the new `AtomicNamedDocumentReplaceTest`.

- **IMPLEMENTED:** unit coverage ratchets for pure-JVM modules; source/catalog/architecture gates; Android lint and APK/native build workflow.
- **PARTIAL:** no direct tests for `MainViewModel`, catalog network/cache, device provider, Accessibility, Shizuku, orchestrator, OCR adapter, or native runtime behavior.
- **PARTIAL:** recorded device evidence names Redmi Turbo 4 Pro / Snapdragon 8s Gen 4 and validates the CPU baseline, retained copy, and diagnostics. It does not prove production readiness or other devices/backends.
- **UNKNOWN:** current head `c53ec20` was not independently compiled during this documentation audit; project records cite a green CI baseline.

## Roadmap capability reality check

| Capability | Status | Evidence summary |
|---|---|---|
| Localhost LLM server | MISSING | no server module/API/lifecycle |
| Universal AI Gateway | MISSING | `InferenceEngine` is not a provider gateway |
| Multi-backend inference | PARTIAL | CPU real; adapter contracts exist; Vulkan/QNN absent |
| Full agent runtime | PARTIAL | safe one-shot execution only |
| Agent task center | MISSING | no task model/UI/history |
| Diff and rollback | MISSING | no diff/checkpoint/rollback engine |
| Project-centric system | MISSING | SAF workspace is not a Project domain |
| Code editor | MISSING | no editor/LSP modules |
| Terminal | MISSING | Shizuku named operations are not a terminal |
| Git workbench | MISSING | repository contains no Git product integration |
| Build/development center | MISSING | CI build is not an on-device build center |
| Linux runtime manager | MISSING | no PRoot/QEMU abstraction |
| Local RAG | MISSING | plugin enum alone is not implementation |
| Multimodal AI | PARTIAL | OCR contracts/screenshot path only |
| Project Trust | MISSING | no trust state/policy/domain |
| Backup and recovery | PARTIAL | retained model copy and safe defaults only |
| Storage-pressure management | PARTIAL | load preflight/critical memory unload; no center/policy lifecycle |
| Performance center | PARTIAL | metrics/device data exist; no center/benchmark protocol |
| Creative studios | MISSING | no vector/paint/3D modules |
| Full Plugin SDK/Manager | PARTIAL | contract only; manager missing |
| Remote development/AI management | MISSING | no provider/server/project remote layer |
| Local API/integration platform | MISSING | no localhost API; typed internal tools only |

## Conflicts resolved by this audit

1. Older docs said 14 or 15 modules; source has **16** including `platform:history`. This document follows `settings.gradle.kts`.
2. Existing status vocabulary (`Ready`, `Build verified`, `Scaffold`) differs from the directive vocabulary. New audit documents use the directive vocabulary while preserving evidence prose.
3. Existing docs describe download/discovery as bounded, but source does not enforce every byte bound during streaming. The implementation remains partial until bounds are enforced in-loop.
4. Existing roadmap phase order predates the master directive. `docs/ROADMAP.md` is now the canonical Phase 0–14 roadmap and retains the useful pre-directive implementation backlog as an appendix.

## Canonical evidence links

- [`../../PROJECT_STATE.md`](../../PROJECT_STATE.md)
- [`../STATUS.md`](../STATUS.md)
- [`../device-results/2026-08-16-redmi-turbo-4-pro.md`](../device-results/2026-08-16-redmi-turbo-4-pro.md)
- [`../ARCHITECTURE.md`](../ARCHITECTURE.md)
- [`../MODULES.md`](../MODULES.md)
- [`../SECURITY_AND_SAFETY.md`](../SECURITY_AND_SAFETY.md)
