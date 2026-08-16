# LAI Project State

Snapshot date: 2026-08-16  
Repository: `soobujmiah/lai`  
Application ID: `dev.lai.runtime`  
Source baseline before this state-file commit: `6d02c14`  
Latest device-test release: [`v0.8.2`](https://github.com/soobujmiah/lai/releases/tag/v0.8.2) (`cbf6ff9`)  
Release APK: `https://github.com/soobujmiah/lai/releases/download/v0.8.2/app-release.apk`  
Last documentation/build verification: GitHub Actions run `31959866932`  
Current physical baseline: Xiaomi/Redmi Turbo 4 Pro, Android API 36, QTI SM8735 / Snapdragon 8s Gen 4

This file is a handoff snapshot. `docs/STATUS.md`, `docs/ROADMAP.md`, implementation source, and Git history remain authoritative if a later change conflicts with this snapshot.

## 1. Architecture Progress

Status vocabulary:

- **Implemented:** source and contracts exist.
- **Build verified:** GitHub Actions compiles/tests/packages it.
- **Device validated:** named physical behavior has been observed.
- **Scaffold:** real boundary exists but the production model/adapter is intentionally absent.
- **Planned:** documented only; do not advertise as working.

### 1.1 LLM Core and native runtime

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| Generic inference contract | Implemented / build verified | `InferenceEngine`, opaque `BackendId`, `BackendDescriptor`, conversations, streaming events, metrics | Keep all future runtime providers behind these contracts |
| llama.cpp CPU adapter | Device validated basic | arm64 JNI/C++ runtime, pinned llama.cpp commit, mmap GGUF load, model-native template, token count, bounded generation, UTF-safe streaming, cancellation callback, metrics | Physical Stop/recovery, forced context trimming, memory-pressure recovery, 10-minute thermals |
| Current reviewed model | Device validated | Qwen 2.5 1.5B Instruct Q4_K_M, exact 1,117,320,736-byte artifact and SHA-256 | Broader Bangla quality pack and signed-web-catalog refresh test |
| Multi-turn context | Device validated basic | Full user/assistant history reaches native formatting; prompt growth observed; oldest completed turns can be omitted | Rolling Context Window with explicit summary checkpoints and physical trim/reset tests |
| Backend scheduler | Device validated on CPU | Model/backend/ABI compatibility, memory preflight, battery/thermal admission, evidence-based selection | Closed-loop throttling, Vulkan and QNN evidence |
| Vulkan backend | Planned | Namespaced `llama-vulkan` descriptor path reserved | Compile real ggml Vulkan, runtime self-test, Adreno qualification, fallback evidence |
| Qualcomm QNN/HTP | Planned, isolated boundary documented | No QNN code or claims leak into generic core/llama module | Licensed CI acquisition, conversion/calibration, dedicated runtime adapter, physical Snapdragon validation |
| Native C++ TaskGraph | Planned | Memory-admitted 3B–5B LLM + Embedding/Whisper chaining is documented | Implement only after current 1.5B baseline and memory/thermal gates remain stable |

Physical v0.8.0 averages from four CPU generations: 514 ms model load, 9,174 ms TTFT, 47.36 prefill tok/s, 20.70 decode tok/s, 10,421 ms total. These prompts included the tool instruction and are not directly comparable to earlier shorter prompts.

### 1.2 Model acquisition, storage, and lifecycle

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| Signed supported-model catalog | Build verified; catalog bytes/signature independently verified | ECDSA P-256 detached signature, embedded fallback, verified cache, downgrade prevention, revision 3 artifact/backend/ABI metadata | Physical signed-web refresh/cache/restart-offline test |
| Download/import | Device validated baseline | Explicit reviewed download, Range resume implementation, mandatory size/SHA/GGUF validation, SAF local import | Physical interruption/resume and hash-mismatch tests |
| Private runtime model | Implemented | mmap-friendly no-backup private copy; never auto-loaded at startup | Preserve for every future provider-backed workspace design |
| Retained model | Device validated | **Keep copy** survived uninstall; offline **Import file** restored exact reviewed model and loaded | Keep regression test active for future releases |
| `/sdcard/LAI/` workspace | Planned | SAF/scoped-storage, directory layout, settings restore, and discovery behavior documented | Implement explicit tree grant and bounded workspace module; no broad-storage permission |
| Model Center | Planned | Categories, foreground download, pause/resume/cancel, local-unreviewed import states specified | Implement after typed configuration/workspace foundation |

### 1.3 Accessibility Service and Android control

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| Accessibility service lifecycle | Implemented / device connection validated | Weak service reference, connection state, fresh active-root access | Service-death/rebind instrumentation and long-run tests |
| Screen snapshot | Implemented / device validated | Flattened immutable tree, maximum 400 nodes/depth 24, password text/description omitted | Foreground-screen binding for model proposals |
| Actions | Implemented / build verified | Click, set text, scroll, Back/Home/Recents/Notifications, app launch | Dedicated harmless physical harness for every selector/action/failure path |
| Screenshot | Implemented / device capability observed | Android 11+ accessibility screenshot copied to in-memory ARGB bitmap | OCR/redaction and lifecycle stress tests |
| Shizuku | Implemented / connection validated | Binder state, UID 2000 observation, dedicated UserService, argv-only allowlist, time/output bounds | Recipe orchestration, foreground binding, service-death recovery, step/loop/time limits |
| Model tool proposals | Build verified; model compliance not yet passed | Exact bounded JSON parser, canonical tool schemas, trusted one-time Compose review, second dispatch validation | v0.8.2 physical retest using privacy-safe `ACCEPTED`/`REJECTED_*`/`NOT_TOOL_CALL` counters |
| Persistent tool audit | Build verified | Private no-backup JSONL hash chain, approval fsync before authority, exact-call replay guard, corruption/reopen tests | Physical process-restart/replay/diagnostics validation; keyed integrity is future work |

The v0.8.0 physical attempt enabled proposal mode but produced no recognized valid proposal, so dialog approval/denial execution is not yet device validated.

### 1.4 Bangla OCR

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| OCR contracts | Implemented / build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult`, blocks, language, confidence, polygons, handwriting flag | Preserve schema compatibility when a model is added |
| Screenshot-to-OCR pipeline | Implemented scaffold | In-memory bitmap capture, background dispatch, bitmap recycle, typed unavailable error | Integrate a licensed printed Bangla model and measure it |
| Printed Bangla OCR | Pending | No model bundled; placeholder honestly reports model required | Dataset/license/runtime selection, quality pack, device latency/memory tests |
| Handwritten Bangla OCR | Planned | Schema seam only | Dataset/model/license selection and later Snapdragon acceleration |
| QNN OCR acceleration | Planned | Vendor boundary documented | Implement only behind generic OCR adapter after CPU correctness baseline |

### 1.5 GitHub Actions, catalog, and delivery

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| Source policy | Ready | 128 MB limit; heavy binaries/models/SDKs/keystores forbidden; secret-pattern check | Keep as first CI gate |
| Architecture policy | Ready | Network, Accessibility, Shizuku, JNI, vendor terms, analytics, and dependency direction checked | Extend checks when real workspace/task modules are added |
| Android build | Ready | GitHub installs JDK 17, API 35, Build Tools, NDK 27, CMake, Gradle; fetches pinned llama.cpp; tests/lints/builds APK | Keep current 1.5B job unchanged while adding isolated future jobs |
| Tests/coverage | Ready | Pure-JVM coverage ratchets, app tests, private-audit Android unit tests, lint | Add workspace/config/parser tests with next implementation phase |
| Catalog publish | Ready | Validates, signs, verifies, and publishes exact `catalog-v1` assets | Physical in-app refresh remains pending |
| Releases | Ready for device testing | Tag-triggered release and APK publication | Temporary/debug signing is not a production update path |
| Production supply chain | Pending | No permanent production key, SBOM, provenance/attestation, or reproducible comparison | Add before production designation |

No Android SDK/NDK, QNN SDK, model weights, APK, or heavy ML dependency is installed in the source-generation workspace.

### 1.6 UI and product surfaces

| Area | Status | Current result | Remaining work |
|---|---|---|---|
| Current Compose shell | Implemented / build verified | Chat, Screen Reader, Automator, Settings, Bangla resources, Developer Mode | Extract feature modules when implementation warrants it |
| Chat | Device validated basic | Local streaming, Stop button implementation, New chat implementation, metrics, model load controls | Physical Stop/New chat/trim tests; rolling context |
| Standalone Tools Dashboard | Planned | Product/architecture spec exists | Implement shared tool cards for OCR, Voice, Image Generation, Vector Search |
| Chat **+ Attach Tools** | Planned | Product/architecture spec exists | Implement attachment/tool chips over the same contracts as dashboard |
| Contextual ⚙ quick settings | Planned | Typed ranges/precedence/bottom-sheet behavior specified | Implement after settings contracts/store |
| Image Generation, Voice, Vector Search | Planned | Parameter and plugin seams documented | No production engines/models yet; unavailable UI must remain honest |

## 2. Current Directory Tree

Generated/build/cache directories and `.git` internals are intentionally omitted. Every tracked source/config/document group is represented below.

```text
lai/
├── PROJECT_STATE.md                         # this handoff snapshot
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
├── .editorconfig
├── .gitignore
├── build.gradle.kts                         # root plugins + JVM coverage ratchets
├── settings.gradle.kts                      # fourteen-module graph
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
├── .github/
│   ├── dependabot.yml
│   ├── pull_request_template.md
│   └── workflows/
│       ├── android_build.yml                # authoritative Android/NDK/test/release pipeline
│       └── catalog_publish.yml              # signed catalog publication
├── catalog/
│   ├── catalog-public-key.pem
│   └── models-v1.json                       # signed-source catalog revision 3
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/dev/lai/runtime/
│       │   ├── LaiApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── core/AppContainer.kt         # composition root
│       │   └── ui/
│       │       ├── LaiApp.kt                # Compose product shell
│       │       ├── MainViewModel.kt         # current product state/orchestration
│       │       └── theme/Theme.kt
│       └── res/
│           ├── drawable/ic_lai.xml
│           ├── values/strings.xml
│           ├── values/styles.xml
│           └── values-bn/strings.xml
├── core/
│   ├── contracts/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/lai/runtime/
│   │       │   ├── agent/ToolModels.kt
│   │       │   ├── audit/ToolAuditModels.kt
│   │       │   ├── automation/AutomationModels.kt
│   │       │   ├── core/JsonConfig.kt
│   │       │   ├── diagnostics/DiagnosticsModels.kt
│   │       │   ├── inference/InferenceEngine.kt
│   │       │   ├── inference/InferenceModels.kt
│   │       │   ├── inference/ModelModels.kt
│   │       │   ├── ocr/OcrModels.kt
│   │       │   └── shell/ShellModels.kt
│   │       └── test/kotlin/dev/lai/runtime/
│   │           ├── agent/ToolModelsTest.kt
│   │           ├── automation/AutomationContractsTest.kt
│   │           ├── automation/NodeSelectorTest.kt
│   │           ├── diagnostics/DiagnosticsModelsTest.kt
│   │           ├── inference/InferenceContractsTest.kt
│   │           ├── inference/ModelContractsTest.kt
│   │           ├── ocr/OcrContractsTest.kt
│   │           └── shell/ShellContractsTest.kt
│   ├── model/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/lai/runtime/model/ReviewedModelCatalog.kt
│   │       └── test/kotlin/dev/lai/runtime/model/ReviewedModelCatalogTest.kt
│   ├── policy/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/lai/runtime/
│   │       │   ├── agent/AgentPolicy.kt
│   │       │   ├── agent/BuiltInToolCatalog.kt
│   │       │   ├── agent/ToolAuditLedger.kt
│   │       │   ├── agent/ToolProposalTelemetry.kt
│   │       │   ├── privacy/LocalFirstPolicy.kt
│   │       │   └── shell/ShellCommandPolicy.kt
│   │       └── test/kotlin/dev/lai/runtime/
│   │           ├── agent/AgentPolicyTest.kt
│   │           ├── agent/ToolAuditLedgerTest.kt
│   │           ├── agent/ToolCallParserTest.kt
│   │           ├── privacy/LocalFirstPolicyTest.kt
│   │           └── shell/ShellCommandPolicyTest.kt
│   └── scheduler/
│       ├── README.md
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/dev/lai/runtime/scheduler/
│           │   ├── InferenceScheduler.kt
│           │   └── ModelMemoryEstimator.kt
│           └── test/kotlin/dev/lai/runtime/scheduler/
│               ├── InferenceSchedulerTest.kt
│               └── ModelMemoryEstimatorTest.kt
├── platform/
│   ├── accessibility/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── kotlin/dev/lai/runtime/automation/
│   │       │   ├── AccessibilityAutomationService.kt
│   │       │   ├── AccessibilityGateway.kt
│   │       │   └── NodeSnapshotter.kt
│   │       └── res/
│   │           ├── values/strings.xml
│   │           ├── values-bn/strings.xml
│   │           └── xml/accessibility_service_config.xml
│   ├── audit/
│   │   ├── README.md
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/AndroidManifest.xml
│   │       ├── main/kotlin/dev/lai/runtime/audit/ToolAuditRepository.kt
│   │       └── test/kotlin/dev/lai/runtime/audit/ToolAuditRepositoryTest.kt
│   ├── device/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── kotlin/dev/lai/runtime/device/AndroidRuntimeEnvironmentProvider.kt
│   ├── download/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml            # sole network permission owner
│   │       └── kotlin/dev/lai/runtime/
│   │           ├── inference/ModelRepository.kt
│   │           └── model/RemoteModelCatalogRepository.kt
│   └── shizuku/
│       ├── build.gradle.kts
│       ├── consumer-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── aidl/dev/lai/runtime/shell/IPrivilegedService.aidl
│           └── kotlin/dev/lai/runtime/shell/
│               ├── ElevatedShell.kt
│               ├── PrivilegedUserService.kt
│               ├── ShizukuController.kt
│               └── ShizukuUserServiceClient.kt
├── runtime/
│   ├── llama/
│   │   ├── build.gradle.kts
│   │   ├── consumer-rules.pro
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── kotlin/dev/lai/runtime/inference/NativeInferenceEngine.kt
│   │       └── cpp/
│   │           ├── CMakeLists.txt
│   │           ├── backend_registry.cpp
│   │           ├── llama_cpu_backend.cpp
│   │           ├── native_inference.cpp
│   │           └── include/lai/backend.h
│   ├── ocr/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── kotlin/dev/lai/runtime/ocr/BanglaOcrService.kt
│   └── orchestrator/
│       ├── build.gradle.kts
│       └── src/main/
│           ├── AndroidManifest.xml
│           └── kotlin/dev/lai/runtime/agent/AgentRuntime.kt
├── plugins/
│   └── api/
│       ├── README.md
│       ├── build.gradle.kts
│       └── src/
│           ├── main/kotlin/dev/lai/runtime/plugins/LaiPlugin.kt
│           └── test/kotlin/dev/lai/runtime/plugins/PluginManifestTest.kt
├── docs/
│   ├── ARCHITECTURE.md
│   ├── ARCHITECTURE_COMPARISON_NPUHUB.md
│   ├── AUTOMATION_TOOLS.md
│   ├── BANGLA_OCR.md
│   ├── BUILD_AND_RELEASE.md
│   ├── DEVICE_TESTING.md
│   ├── DIAGNOSTICS_EXPORT.md
│   ├── MODELS_AND_BACKENDS.md               # canonical model/config/Model Center product spec
│   ├── MODULES.md
│   ├── PRIVACY_INVARIANTS.md
│   ├── ROADMAP.md
│   ├── SECURITY_AND_SAFETY.md
│   ├── STATUS.md
│   ├── VENDOR_BACKEND_STRATEGY.md
│   ├── adr/
│   │   ├── 0002-modular-local-first-backbone.md
│   │   ├── 0003-dual-connectivity-editions.md
│   │   ├── 0004-single-local-first-app.md
│   │   ├── 0005-snapdragon-first-vendor-neutral-backends.md
│   │   ├── 0006-one-shot-model-tool-proposals.md
│   │   └── 0007-persistent-tool-audit-and-replay-guard.md
│   └── device-results/2026-08-16-redmi-turbo-4-pro.md
└── scripts/
    ├── check_architecture_boundaries.py
    ├── validate_model_catalog.py
    ├── validate_repo.sh
    └── ci/fetch_llama_cpp.sh
```

The repository intentionally contains no APK/AAB/AAR/SO, GGUF/ONNX/TFLite/QNN model, SDK, keystore, generated build output, Gradle wrapper JAR, or user diagnostics attachment.

## 3. API and Module Specifications

### 3.1 Module dependency contract

```text
app (composition + Compose)
├── core:contracts
├── core:policy
├── core:scheduler
├── core:model
├── plugins:api
├── platform:download
├── platform:audit
├── platform:device
├── platform:accessibility
├── platform:shizuku
├── runtime:llama
├── runtime:ocr
└── runtime:orchestrator
```

Rules:

- Core imports no Android, platform, runtime, app, vendor SDK, or native entry point.
- Platform owns Android authority/persistence and never depends upward on app/runtime.
- Runtime implements generic contracts and owns no product UI.
- `app` is the only composition root.
- Network transport/permission exists only in `platform:download`.
- Persistent tool-audit bytes exist only in `platform:audit`.
- Qualcomm/QNN/Hexagon terms and APIs cannot enter generic inference/scheduler core.

### 3.2 LLM/inference API

Kotlin contract:

```kotlin
interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    val contextSize: Int

    suspend fun load(modelPath: String, backend: BackendId? = null): Result<Unit>
    fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig = GenerationConfig(),
    ): Flow<InferenceEvent>
    suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int>
    override fun close()
}
```

Supporting values:

- `BackendId`: validated opaque adapter-owned ID (`llama-cpu`, future `llama-vulkan`, future dedicated vendor IDs).
- `BackendDescriptor`: ID, generic compute class, supported formats/quantizations, adapter preference.
- `GenerationConfig`: `maxNewTokens`, `temperature`, `topP`, `seed`.
- `ConversationMessage`: `SYSTEM | USER | ASSISTANT` plus text.
- `InferenceEvent`: `Token(text)`, `Completed(tokensGenerated, metrics)`, `Failed(message)`.
- `GenerationMetrics`: prompt/output counts, prompt evaluation, TTFT, decode, total, calculated rates.
- `RuntimeCapabilities`: native load state, compiled backend descriptors, truthful detail.

Current native JNI boundary:

```text
runtimeInfo()
createSession(modelPath, backend, contextSize)
countTokens(session, roles[], contents[])
generate(session, roles[], contents[], sampling, callback)
destroySession(session)
lastError()
```

C++ backend boundary:

```cpp
class BackendSession {
    virtual int count_tokens(const std::vector<ChatMessage>&) = 0;
    virtual GenerationResult generate(
        const std::vector<ChatMessage>&,
        const GenerationOptions&,
        const TokenCallback&,
        const CancelCallback&
    ) = 0;
};

class Backend {
    virtual std::string name() const = 0;
    virtual bool available() const = 0;
    virtual std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) = 0;
};
```

Current implementation is `llama-cpu`; unavailable adapters never appear in runtime capabilities.

### 3.3 Model API

- `ModelSpec`: ID, display name, HTTPS URL, SHA-256, expected bytes.
- `ModelImportSpec`: reviewed local-import ID/name/digest/size.
- `InstalledModel`: private registry metadata; never contains weights.
- `ReviewedModel`: signed/fallback artifact metadata including format, quantization, backend IDs, context, memory, ABI, license, review evidence.
- `ModelRepository`: list/resolve/download/import/export and exact validation. Private copy is the native load source.
- `RemoteModelCatalogRepository`: embedded/cache/web catalog selection, ECDSA verification, monotonic revision, local cache.

### 3.4 Tool and policy API

Wire envelope:

```kotlin
data class ToolCall(val id: String, val name: String, val arguments: JsonObject)
data class ToolResult(
    val callId: String,
    val success: Boolean,
    val output: JsonObject,
    val error: ToolError?,
)
data class ToolDefinition(
    val name: String,
    val description: String,
    val risk: ToolRisk,
    val requiresConfirmation: Boolean,
)
enum class ToolRisk { READ_ONLY, INTERACTION, SENSITIVE, ELEVATED }
```

Current built-in tools:

1. `screen.snapshot`
2. `screen.click`
3. `screen.type`
4. `screen.scroll`
5. `system.global_action`
6. `app.launch`
7. `ocr.current_screen`
8. `shell.operation`

Execution boundary:

```kotlin
class AgentRuntime {
    val tools: List<ToolDefinition>
    val modelToolInstruction: String
    fun parseToolProposal(modelOutput: String): ToolCallParseResult
    suspend fun execute(call: ToolCall, userConfirmed: Boolean = false): ToolResult
}
```

`ToolCallParser` accepts only a bounded complete JSON response with exact per-tool shapes. `AgentRuntime` validates again before policy/authority dispatch. Model output cannot set trusted confirmation. The Compose dialog supplies one-time confirmation separately. `ToolAuditRepository` fsyncs a content-free approval before model-proposed authority and appends completion afterward; exact approved call fingerprints cannot replay.

Shell requests are `PrivilegedCommand(operation, arguments)` values. `ShellCommandPolicy` emits argv arrays only; no raw command or `sh -c` escape exists.

### 3.5 Accessibility API

```kotlin
object AccessibilityGateway {
    val connected: StateFlow<Boolean>
    val foregroundPackage: StateFlow<String?>
    suspend fun execute(command: AutomationCommand): AutomationResult
    suspend fun captureScreen(): Result<Bitmap>
}
```

`AutomationCommand` variants are `Snapshot`, `Click`, `SetText`, `Scroll`, `GlobalAction`, and `LaunchApp`. Selectors support reviewed view ID, hierarchy path, visible text, and content description. Every operation obtains fresh nodes; password text is omitted and model typing always uses `allowSensitiveInput=false`.

### 3.6 OCR API

```kotlin
interface OcrEngine {
    val id: String
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult>
}

class BanglaOcrService {
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest = OcrRequest()): Result<OcrResult>
}
```

`OcrRequest` defaults to `bn` + `en`; `OcrResult` schema v1 contains full text, blocks, language, confidence, polygon, handwritten flag, timing, engine, and warning. The default engine is intentionally a typed placeholder.

### 3.7 Plugin API

```kotlin
interface LaiPlugin {
    val manifest: PluginManifest
    fun validateInput(input: JsonObject): List<String>
    suspend fun execute(input: JsonObject, context: PluginExecutionContext): PluginResult
}

interface PluginExecutionContext {
    suspend fun invokeApprovedTool(call: ToolCall): ToolResult
    fun reportProgress(fraction: Float, message: String)
}
```

Plugins are currently contract-only, API version 1, and `LOCAL_ONLY`. Capabilities are Chat Tool, OCR Post-Processor, RAG Source, Speech Input, and Speech Output. Dynamic third-party loading is not implemented.

### 3.8 UI specification

Current UI state:

- `UiMode`: `CHAT`, `SCREEN_READER`, `AUTOMATOR`.
- `RuntimeOperation`: no model, idle, download/import/export/load, ready, generate/cancel, screen read, automate, error.
- `MainUiState`: immutable aggregate for messages, model/catalog state, runtime metrics, device state, proposal counters, pending proposal, persistent audit, and settings/developer visibility.
- `MainViewModel.state`: `StateFlow<MainUiState>`.
- `LaiApp(viewModel)`: top-level Compose shell.

Current user actions exposed by `MainViewModel` include send/cancel/clear chat; load/unload/download/import/export models; refresh catalog; inspect/read screen; Accessibility/Shizuku entry; proposal approve/deny; diagnostics export; and Developer Mode.

Target UI (not implemented): Standalone Tools Dashboard, Chat **+ Attach Tools**, and contextual Chat **⚙** bottom sheet. These must use shared contracts rather than adding tool logic directly to composables.

### 3.9 Composition root

`AppContainer` currently instantiates Shizuku, elevated shell, model repository, signed catalog repository, persistent audit, native inference, scheduler, memory estimator, Android environment provider, OCR service, and `AgentRuntime`. Critical Android memory callbacks close the native model session and emit a UI event.

## 4. Next Logical Implementation Phase

### Phase 2A — Typed Tool Configuration and SAF Workspace Foundation

This phase is the dependency for quick settings, Model Center, Dashboard/Chat tool parity, and auto-discovery. Do **not** begin Image Generation, Whisper, SQLCipher, 3B–5B task chaining, or a universal backend manager first.

#### 4.1 Code to add

1. **Typed settings contracts in pure core**
   - Add `core/contracts/src/main/kotlin/dev/lai/runtime/settings/ToolSettings.kt`.
   - Define `SettingsDocumentV1`, `LlmSettings`, `ImageGenerationSettings`, `VoiceSettings`, `SearchSettings`, model/tool-scoped defaults, and schema version.
   - Preserve current Qwen test defaults: temperature `0.7`, Top-P `0.9`, maximum new tokens `256` in product state.
   - Do not use an untyped `Map<String, Any>`.

2. **Validation and migration policy**
   - Add `core/policy/src/main/kotlin/dev/lai/runtime/settings/SettingsPolicy.kt`.
   - Validate every documented range, finite number, enum/preset, context-dependent max-token limit, and unknown field policy.
   - Add deterministic `v1 -> future` migration seam and safe embedded defaults.
   - Add JVM tests for minimum/maximum, NaN/infinity, unknown fields, malformed schema, Bangla-safe round trip, and reset/precedence.

3. **Concrete `platform:workspace` Android module**
   - Add it to `settings.gradle.kts`, app dependencies, `docs/MODULES.md`, and architecture checks.
   - Implement `WorkspaceRepository` around a user-selected `ACTION_OPEN_DOCUMENT_TREE` URI and persistable permission.
   - Never translate arbitrary `content://` URIs into raw paths and never request `MANAGE_EXTERNAL_STORAGE`.
   - Implement canonical child resolution for `models/`, `tools/`, `config/settings.json`, and `cache/` through provider document IDs.

4. **Bounded settings store**
   - Implement `WorkspaceSettingsStore` with strict maximum bytes, strict JSON, temp/new-document verification, safe replacement strategy, and local migration warning.
   - Reject secrets/user-content fields by schema; settings must not become a prompt/document log.
   - Fall back to embedded defaults when no root is granted, the grant is revoked, or the file is malformed.

5. **Bounded discovery service**
   - Implement `WorkspaceDiscovery` with count/depth/time/size limits.
   - Enumerate only granted `models/` and `tools/`; never scan shared storage globally.
   - For `.gguf`: stream size/magic/SHA-256, deduplicate by digest, match catalog when known, otherwise mark `LOCAL_UNREVIEWED`.
   - Registration must not allocate weights or auto-load inference.
   - Loading continues to copy/verify into app-private runtime storage.

6. **Minimal UI vertical slice**
   - Add workspace selection/revocation/status to Settings.
   - Add the Chat top-right **⚙** button and a typed LLM-only quick-settings bottom sheet first.
   - Support Apply once / Save default / Reset.
   - Do not show active Image/Voice/Search controls until a real compatible adapter capability exists.
   - Keep the current three-mode navigation and Qwen 1.5B chat path functioning while this foundation lands.

7. **Composition and diagnostics**
   - Wire settings/workspace into `AppContainer` and `MainViewModel` through interfaces, not direct `DocumentFile` calls in Compose.
   - Export only schema version, workspace-enabled boolean, coarse discovery counts/status, and active non-sensitive numeric settings.
   - Never export tree URI, filenames, prompts, document text, credentials, or model/tool contents.

8. **Tests and CI**
   - Add pure settings-policy coverage and Android/JVM fake-provider or repository tests for revoked grants, partial writes, malformed JSON, duplicate hashes, oversized files, cancellation, and empty roots.
   - Add new module tests to the existing GitHub Actions command without changing the pinned llama.cpp/Qwen 1.5B build path.
   - Update architecture, privacy, security, device-testing, status, roadmap, changelog, and a new ADR only if a consequential storage decision is made.

#### 4.2 Acceptance criteria

Phase 2A is complete only when:

- existing Qwen 1.5B download/import/load/chat and retained-copy behavior still pass;
- app starts without a workspace grant and uses safe defaults;
- user can grant/revoke the LAI tree without broad storage permission;
- valid `settings.json` restores typed LLM values; malformed/oversized settings fall back without crash;
- quick-sheet one-request override does not silently mutate saved defaults;
- save/reset persists and reopens through SAF;
- discovery registers reviewed and local-unreviewed GGUF metadata but never auto-loads;
- unknown or changed bytes cannot inherit a reviewed digest/status;
- all work compiles in GitHub Actions and the source repository stays below 128 MB.

#### 4.3 Immediate evidence gate before/alongside coding

Install v0.8.2 and run one explicit harmless Android action request. Export diagnostics immediately and report `lastProposalOutcome` (`ACCEPTED`, `NOT_TOOL_CALL`, or `REJECTED_*`). Also test Stop/recovery, New chat, forced trimming, signed-catalog refresh, persistent-audit restart/replay, and sustained thermals when practical. A failure in those current release gates takes priority over new workspace/UI feature expansion.

#### 4.4 Subsequent sequence after Phase 2A

1. Categorized Model Center + WorkManager foreground downloader/pause/resume.
2. Dual Dashboard/Chat **+ Attach Tools** shell over shared contracts.
3. Rolling Context Window and explicit summaries.
4. Real printed Bangla OCR CPU baseline.
5. Vulkan qualification and closed-loop thermal/battery control.
6. Native C++ micro-model TaskGraph, then streaming STT/TTS and Barge-In.
7. SQLCipher vector database and RAG.
8. QNN/HTP adapter only after licensed tooling and converted artifacts are available.

## Pause State

- Repository source is clean before adding this file.
- Existing Qwen 1.5B catalog/build/release pipeline must remain active.
- Latest release is temporary/debug signed; uninstall may be required between APKs.
- The retained user-owned GGUF has already passed uninstall/offline-restore validation.
- No production OCR, Vulkan, QNN, image-generation, STT/TTS, vector database, SAF workspace, Model Center background worker, dual UX, or native TaskGraph implementation should be claimed yet.
