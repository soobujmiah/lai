# LAI Project State

Snapshot date: 2026-08-17  
Repository: `soobujmiah/lai` (clone URL `https://github.com/soobujmiah/LAI`)  
Application ID: `dev.lai.runtime`  
Target device: Xiaomi/Redmi Turbo 4 Pro, Android API 36, QTI SM8735 / Snapdragon 8s Gen 4  
Latest verified build: GitHub Actions run [`31975456732`](https://github.com/soobujmiah/LAI/actions/runs/31975456732) (commit `475f942`) — **green** (Phase 2A items 6–8 + the `PROGRESS_STEP_BYTES` main-fix)  
Latest device-test release: [`v0.8.2`](https://github.com/soobujmiah/LAI/releases/tag/v0.8.2) (`cbf6ff9`); temporary/debug-signed APK  
Current graph: **15 Gradle modules** (unchanged this session; Phase 2A items 6–8 completed the vertical slice)

> This is a handoff snapshot. Current source/tests are authoritative; `docs/implementation/current-state.md` is the latest audited state, `docs/ROADMAP.md` is the canonical Phase 0–14 roadmap, and accepted ADRs govern architectural decisions. The documentation/master-roadmap and bounded model-stream changes in the working tree remain CI-unverified until an updated workflow run passes.

Status vocabulary: **Implemented** = source/contracts exist · **Build verified** = CI compiles/tests/packages it · **Device validated** = named physical behavior observed · **Scaffold** = compiling boundary with honest unavailable behavior · **Planned** = documented only.

---

## 1. Architecture Progress

### 1.1 LLM Core & native runtime

| Area | Status | Current result | Remaining |
|---|---|---|---|
| Generic inference contract | Implemented / build verified | `InferenceEngine`, opaque `BackendId`/`BackendDescriptor`, conversations, streaming events, metrics | Keep providers behind these contracts |
| llama.cpp CPU adapter | Device validated (basic) | arm64 JNI/C++, pinned llama.cpp commit, mmap GGUF load, model-native template, token count, bounded generation, UTF-safe streaming, cancel, metrics | Stop/recovery, forced context trim, memory-pressure recovery, thermals |
| Reviewed model | Device validated | Qwen 2.5 1.5B Instruct Q4_K_M, exact 1,117,320,736-byte SHA-256 | Broader Bangla quality pack; signed-web-catalog refresh test |
| Backend scheduler | Device validated (CPU) | Model/backend/ABI compatibility, memory preflight, battery/thermal admission, evidence-based selection | Closed-loop throttling; Vulkan & QNN evidence |
| Vulkan backend | Planned | `llama-vulkan` descriptor path reserved | Compile real ggml Vulkan, Adreno qualification |
| Qualcomm QNN/HTP | Planned (boundary isolated) | No QNN code in generic core/llama | Licensed CI acquisition, conversion/calibration, dedicated adapter |
| Native C++ TaskGraph | Planned | Memory-admitted 3B–5B + Embedding/Whisper chaining documented | Only after 1.5B baseline + memory/thermal gates stable |

### 1.2 Accessibility Service & Android control

| Area | Status | Current result | Remaining |
|---|---|---|---|
| Service lifecycle | Implemented / device connection validated | Weak service ref, connection state, fresh active-root access | Service-death/rebind instrumentation |
| Screen snapshot | Implemented / device validated | Flattened immutable tree, ≤400 nodes / depth 24, password text omitted | Foreground-screen binding for proposals |
| Actions (click/type/scroll/global/launch) | Implemented / build verified | `AutomationCommand` variants; selectors by viewId/text/contentDescription/path; confirmation at agent boundary | Harmless physical harness per selector/action/failure |
| Screenshot | Implemented / device capability observed | Android 11+ accessibility screenshot → in-memory ARGB bitmap | OCR/redaction + lifecycle stress |
| Shizuku | Implemented / connection validated | Binder state, UID 2000, dedicated UserService, argv-only allowlist, time/output bounds | Recipe orchestration, foreground binding, recovery |
| One-shot tool proposals | Build verified; model-compliance retest required | Bounded JSON parser, canonical schemas, trusted Compose review, second dispatch validation | v0.8.2 physical retest counters (`ACCEPTED`/`REJECTED_*`/`NOT_TOOL_CALL`) |
| Persistent tool audit | Build verified | Private no-backup JSONL hash chain, approval-before-authority fsync, exact-call replay guard | Physical restart/replay/diagnostics validation |

### 1.3 Bangla OCR

| Area | Status | Current result | Remaining |
|---|---|---|---|
| OCR contracts | Implemented / build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult` (blocks, language, confidence, polygon, handwriting flag) | Preserve schema when a model is added |
| Screenshot→OCR pipeline | Implemented scaffold | In-memory bitmap capture, background dispatch, bitmap recycle, typed unavailable error | Integrate a licensed printed Bangla model |
| Printed Bangla OCR | Pending | `PlaceholderBanglaOcrEngine` honestly reports model required | Dataset/license/runtime selection, quality pack, latency/memory |
| Handwritten Bangla OCR | Planned | Schema seam only | Dataset/model/license selection |
| QNN OCR acceleration | Planned | Vendor boundary documented | Only behind generic OCR adapter after CPU baseline |

### 1.4 GitHub Actions, catalog & delivery

| Area | Status | Current result | Remaining |
|---|---|---|---|
| Source policy | Ready | 128 MB limit; no binaries/models/SDKs/keystores; token scan | Keep as first CI gate |
| Architecture policy | Ready | Network, Accessibility, Shizuku, JNI, vendor terms, dependency direction checked | Extend when workspace/task modules land |
| Android build | Ready | CI installs JDK 17, API 35, Build Tools, NDK 27, CMake, Gradle; fetches pinned llama.cpp; tests/lints/builds APK | Keep Qwen 1.5B job unchanged while adding isolated jobs |
| Tests / coverage | Ready | Pure-JVM coverage ratchets, app + audit + **workspace** unit tests, lint; **29 new Phase 2A tests** (session policy, coordinator vs fake ports, codec edge cases) | Physical SAF grant/discovery instrumentation |
| Catalog publish | Ready | Validates/signs/verifies/publishes `catalog-v1` | Physical in-app refresh |
| Releases | Ready for device testing | Tag-triggered APK publication | Temporary/debug signing is not production |
| Production supply chain | Pending | — | Permanent key, SBOM, provenance, reproducible comparison |

### 1.5 Tool configuration & SAF workspace (Phase 2A — in progress this session)

| Area | Status | Current result | Remaining |
|---|---|---|---|
| Typed settings contracts | Build verified | `SettingsDocumentV1` (Llm/ImageGen/Voice/Search) bounded numeric/boolean ranges, Qwen defaults | UI + persistence wiring |
| Settings validation/migration | Build verified | `SettingsPolicy` validate/sanitize/migrate; range/finite/type/context-dependent checks; unknown-field warnings; deterministic v1 seam | — |
| Workspace decision layer | Build verified | `WorkspacePolicy.classify` (digest dedup, size/format/count, catalog match) + `WorkspaceSettingsCodec` (bounded, exact-schema) | — |
| `platform:workspace` SAF adapter | Build verified; device pending | `WorkspaceRepository` (grant, persistable permission, child resolution, layout), `WorkspaceSettingsStore` (temp-write-then-replace), `WorkspaceDiscovery` (bounded traversal + SHA-256 + GGUF magic) | Physical SAF grant/discovery test |
| Workspace ports | Build verified | `WorkspaceGrantPort` / `SettingsStorePort` / `ModelDiscoveryPort` in `core:contracts`; the SAF adapters implement them so no Android type crosses the seam | — |
| Settings session semantics | Build verified | `SettingsSession` + `SettingsSessionPolicy`: saved defaults vs one-request override, validate-before-transition, context-aware `maxNewTokens` ceiling | — |
| Composition wiring | Build verified; device pending | `WorkspaceSettingsCoordinator` (pure ports only) owns grant state, session, save/reset, coarse counts; `MainViewModel` binds it and Chat consumes the override once per reply | Physical device pass |
| Chat ⚙ quick-settings UI | Build verified; device pending | `ModalBottomSheet` with creativity/focus/reply-length/memory in plain bilingual language; Apply once · Save default · Reset; ranges mirror `SettingsPolicy` | Physical device pass |
| Workspace status surface | Build verified; device pending | Settings card: `ACTION_OPEN_DOCUMENT_TREE` grant/revoke, status line, manual scan showing coarse REVIEWED / LOCAL_UNREVIEWED counts only | Physical SAF grant/discovery test |

### 1.6 UI & product surfaces

| Area | Status | Current result | Remaining |
|---|---|---|---|
| Compose three-mode shell | Build verified | Chat, Screen Reader, Automator; Developer Mode hidden | Extract feature modules later |
| Chat | Device validated (basic); ⚙ sheet build verified, device pending | Local streaming, Stop, New chat, metrics, load controls, **contextual ⚙ quick settings honoured per reply** | Rolling Context Window; physical trim tests |
| Standalone Tools Dashboard | Planned | Spec exists | Implement over shared contracts |
| Chat + Attach Tools | Planned | Spec exists | Same contracts as dashboard |
| Contextual ⚙ quick settings | Build verified; device pending | LLM-only bottom sheet over shared typed contracts; image/voice/search stay unrendered until a real adapter exists | Physical device pass |

---

## 2. Current Directory Tree

Generated/build/cache dirs and `.git` internals are omitted; every tracked source/config/doc group is represented.

```text
lai/
├── PROJECT_STATE.md                         # this file
├── README.md · CHANGELOG.md · CONTRIBUTING.md · SECURITY.md · LICENSE
├── THIRD_PARTY_NOTICES.md · THIRD_PARTY_LICENSES.md · MODEL_LICENSES.md
├── .editorconfig · .gitignore
├── build.gradle.kts                         # root plugins + pure-JVM coverage ratchets
├── settings.gradle.kts                      # 15-module graph
├── gradle.properties · gradle/libs.versions.toml
├── .github/
│   ├── dependabot.yml · pull_request_template.md
│   └── workflows/{android_build.yml, catalog_publish.yml}
├── catalog/{catalog-public-key.pem, models-v1.json}   # signed-source catalog revision 3
├── app/
│   ├── build.gradle.kts · proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/dev/lai/runtime/{LaiApplication.kt, MainActivity.kt}
│       ├── core/AppContainer.kt             # composition root (holds workspace repo/store/discovery)
│       ├── ui/{LaiApp.kt, MainViewModel.kt, QuickSettingsSheet.kt,          # (Phase 2A items 6-7)
│       │        WorkspaceSettingsCoordinator.kt, theme/Theme.kt}
│       └── res/{drawable, values, values-bn}
│   └── src/test/java/dev/lai/runtime/ui/WorkspaceSettingsCoordinatorTest.kt   # (Phase 2A item 8)
├── core/
│   ├── contracts/  (pure JVM)
│   │   └── src/main/kotlin/dev/lai/runtime/
│   │       ├── agent/ToolModels.kt
│   │       ├── audit/ToolAuditModels.kt
│   │       ├── automation/AutomationModels.kt
│   │       ├── core/JsonConfig.kt            # LaiJson
│   │       ├── diagnostics/DiagnosticsModels.kt
│   │       ├── inference/{InferenceEngine.kt, InferenceModels.kt, ModelModels.kt}
│   │       ├── ocr/OcrModels.kt
│   │       ├── settings/ToolSettings.kt      # SettingsDocumentV1 + sections   (Phase 2A)
│   │       ├── shell/ShellModels.kt
│   │       ├── workspace/WorkspaceContracts.kt                              # (Phase 2A)
│   │       └── workspace/WorkspacePorts.kt      # grant/store/discovery ports (Phase 2A)
│   │   (tests: agent, automation, diagnostics, inference, model, ocr, settings, shell, workspace)
│   ├── model/      → ReviewedModelCatalog (immutable fallback catalog)
│   ├── policy/     (pure JVM)
│   │   └── src/main/kotlin/dev/lai/runtime/
│   │       ├── agent/{AgentPolicy.kt, BuiltInToolCatalog.kt, ToolAuditLedger.kt, ToolProposalTelemetry.kt}
│   │       ├── privacy/LocalFirstPolicy.kt
│   │       ├── settings/SettingsPolicy.kt                                   # (Phase 2A)
│   │       ├── settings/SettingsSession.kt  # saved vs one-request override (Phase 2A)
│   │       ├── shell/ShellCommandPolicy.kt
│   │       └── workspace/WorkspacePolicy.kt                                 # classifier + codec (Phase 2A)
│   │   (tests: agent, privacy, settings, shell, workspace)
│   └── scheduler/  → {InferenceScheduler.kt, ModelMemoryEstimator.kt} (+ tests)
├── platform/
│   ├── accessibility/  → AccessibilityAutomationService · AccessibilityGateway · NodeSnapshotter (+ res/xml)
│   ├── audit/          → ToolAuditRepository (no-backup JSONL) (+ test)
│   ├── device/         → AndroidRuntimeEnvironmentProvider
│   ├── download/       → ModelRepository · RemoteModelCatalogRepository (sole network owner)
│   │   └── tests       → BoundedStreamCopyTest (pre-write stream ceiling; CI pending)
│   ├── shizuku/        → ElevatedShell · PrivilegedUserService · ShizukuController · ShizukuUserServiceClient (+ AIDL)
│   └── workspace/      → WorkspaceRepository · WorkspaceSettingsStore · WorkspaceDiscovery · WorkspaceSaf · ModelFormatDetector (+ test)   # (Phase 2A, NEW)
├── runtime/
│   ├── llama/          → NativeInferenceEngine.kt + cpp/{native_inference.cpp, llama_cpu_backend.cpp, backend_registry.cpp, include/lai/backend.h, CMakeLists.txt}
│   ├── ocr/            → BanglaOcrService (placeholder engine)
│   └── orchestrator/   → AgentRuntime (tool dispatch)
├── plugins/api/        → LaiPlugin (+ test)
├── docs/
│   ├── README.md                           # documentation map
│   ├── architecture/                       # audited overview, module, AI, agent, security, plugin maps
│   ├── implementation/                     # current state, plans, tests, directive coverage
│   ├── product/                            # feature matrix + canonical-roadmap compatibility path
│   ├── decisions/README.md                 # ADR policy/index
│   ├── ROADMAP.md                          # canonical Phase 0–14 roadmap + retained detailed backlog
│   ├── ARCHITECTURE · MODULES · STATUS · SECURITY_AND_SAFETY · BANGLA_OCR
│   ├── AUTOMATION_TOOLS · MODELS_AND_BACKENDS · VENDOR_BACKEND_STRATEGY · PRIVACY_INVARIANTS
│   ├── BUILD_AND_RELEASE · DEVICE_TESTING · DIAGNOSTICS_EXPORT
│   ├── adr/0002-0007                       # accepted legacy-path ADRs
│   └── device-results/2026-08-16-redmi-turbo-4-pro.md
└── scripts/            → {validate_repo.sh, validate_documentation.py, check_architecture_boundaries.py,
                           validate_model_catalog.py, ci/fetch_llama_cpp.sh}
```

The repository contains no APK/AAB/AAR/SO, GGUF/ONNX/TFLite/QNN model, SDK, keystore, generated build output, Gradle wrapper JAR, or diagnostics attachment. Source footprint is ~608 KB (limit 128 MB).

---

## 3. API & Module Specifications

### 3.1 Module dependency contract

```text
app (composition + Compose)
├── core:contracts · core:policy · core:scheduler · core:model · plugins:api
├── platform:download · platform:audit · platform:device
├── platform:accessibility · platform:workspace · platform:shizuku
└── runtime:llama · runtime:ocr · runtime:orchestrator
```

Rules: core imports no Android/platform/runtime/vendor; platform never depends upward on app/runtime; runtime implements core contracts and owns no UI; `app` is the only composition root; network lives only in `platform:download`; persistent audit bytes only in `platform:audit`; Qualcomm/QNN terms never enter generic inference/scheduler core.

### 3.2 LLM / inference interface

```kotlin
interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    val contextSize: Int
    suspend fun load(modelPath: String, backend: BackendId? = null): Result<Unit>
    fun generate(conversation: List<ConversationMessage>, config: GenerationConfig = GenerationConfig()): Flow<InferenceEvent>
    suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int>
    override fun close()
}
```

Supporting types: `BackendId` (validated, namespaced, e.g. `llama-cpu`), `BackendDescriptor`, `GenerationConfig(maxNewTokens, temperature, topP, seed)`, `ConversationMessage(SYSTEM|USER|ASSISTANT, text)`, `InferenceEvent { Token | Completed | Failed }`, `GenerationMetrics`, `RuntimeCapabilities`. Native JNI boundary: `runtimeInfo / createSession / countTokens / generate / destroySession / lastError`. C++ backend boundary: `Backend { name, available, open(...) }` + `BackendSession { count_tokens, generate(...) }`. Only `llama-cpu` is implemented; unavailable adapters never appear in capabilities.

### 3.3 Tool & policy interface

```kotlin
data class ToolCall(val id: String, val name: String, val arguments: JsonObject)
data class ToolResult(val callId: String, val success: Boolean, val output: JsonObject, val error: ToolError? = null)
data class ToolDefinition(val name: String, val description: String, val risk: ToolRisk, val requiresConfirmation: Boolean)
enum class ToolRisk { READ_ONLY, INTERACTION, SENSITIVE, ELEVATED }
```

Built-in tools: `screen.snapshot`, `screen.click`, `screen.type`, `screen.scroll`, `system.global_action`, `app.launch`, `ocr.current_screen`, `shell.operation`.

```kotlin
class AgentRuntime {
    val tools: List<ToolDefinition>
    val modelToolInstruction: String
    fun parseToolProposal(modelOutput: String): ToolCallParseResult   // NotToolCall | Accepted | Rejected
    suspend fun execute(call: ToolCall, userConfirmed: Boolean = false): ToolResult
}
```

`ToolCallParser` accepts only a bounded whole-response JSON envelope with exact per-tool schemas; `AgentRuntime` re-validates before dispatch; model output cannot set trusted confirmation; `ToolAuditRepository` fsyncs a content-free approval before authority and rejects exact-call replay. Shell requests are `PrivilegedCommand(operation, arguments)`; `ShellCommandPolicy` emits argv arrays only (no `sh -c`).

### 3.4 Accessibility interface

```kotlin
object AccessibilityGateway {
    val connected: StateFlow<Boolean>
    val foregroundPackage: StateFlow<String?>
    suspend fun execute(command: AutomationCommand): AutomationResult
    suspend fun captureScreen(): Result<Bitmap>
}
```

`AutomationCommand` variants: `Snapshot`, `Click(selector)`, `SetText(selector, text, allowSensitiveInput=false)`, `Scroll(selector?, forward)`, `GlobalAction(BACK|HOME|RECENTS|NOTIFICATIONS)`, `LaunchApp(package)`. Selectors: `viewId | text | contentDescription | path`. Password text omitted; model typing never uses sensitive input.

### 3.5 OCR interface

```kotlin
interface OcrEngine { val id: String; suspend fun recognize(bitmap: Bitmap, request: OcrRequest): Result<OcrResult> }
class BanglaOcrService(private val engine: OcrEngine = PlaceholderBanglaOcrEngine()) {
    suspend fun recognize(bitmap: Bitmap, request: OcrRequest = OcrRequest()): Result<OcrResult>
}
```

`OcrRequest` defaults `["bn","en"]`; `OcrResult` schema v1 = `{schemaVersion, fullText, blocks[], processingTimeMs, engine, warning?}`; `OcrBlock = {text, language?, confidence?, polygon[], handwritten?}`. Default engine is a typed placeholder.

### 3.6 Settings & workspace interface (Phase 2A)

```kotlin
// core:contracts
data class SettingsDocumentV1(
    val schemaVersion: Int = 1,
    val llm: LlmSettings = LlmSettings(),                 // temp 0.7, topP 0.9, maxNewTokens 256, seed -1, context
    val imageGeneration: ImageGenerationSettings = ImageGenerationSettings(),
    val voice: VoiceSettings = VoiceSettings(),
    val search: SearchSettings = SearchSettings(),
)

// core:policy
class SettingsPolicy {
    fun defaults(): SettingsDocumentV1
    fun validate(document: SettingsDocumentV1): SettingsValidation
    fun validate(raw: JsonObject): SettingsValidation
    fun sanitize(raw: JsonObject): SettingsDocumentV1
    fun migrate(raw: JsonObject): SettingsMigration
}
class WorkspacePolicy {
    fun classify(candidates: List<ModelCandidate>, reviewedBySha256: Map<String,String>, limits: DiscoveryLimits = DiscoveryLimits()): List<DiscoveredModel>
}
class WorkspaceSettingsCodec {
    fun encode(document: SettingsDocumentV1): ByteArray
    fun decode(bytes: ByteArray?, maxBytes: Int): DecodeOutcome        // Loaded | Malformed | Oversized | Absent
    fun verifyForStorage(bytes: ByteArray, maxBytes: Int): StorageVerification
}

// platform:workspace
class WorkspaceRepository(context: Context) { val state: WorkspaceGrantState; fun grant(treeUri: Uri): Result<Unit>; fun revoke(); fun ensureLayout(): Result<Unit> }
class WorkspaceSettingsStore(repository, codec = ..., maxBytes = 32KB) { suspend fun load(): SettingsLoadOutcome; suspend fun save(document): Result<Unit> }
class WorkspaceDiscovery(repository, policy = ...) { suspend fun discoverModels(reviewedBySha256, limits = ...): Result<List<DiscoveredModel>> }
```

```kotlin
// core:contracts - pure ports the app depends on (no Android type crosses them)
interface WorkspaceGrantPort { val state: WorkspaceGrantState }
interface SettingsStorePort {
    suspend fun load(): SettingsLoadOutcome                        // never throws; always usable
    suspend fun save(document: SettingsDocumentV1): Result<Unit>   // exact-v1-schema verified
}
interface ModelDiscoveryPort {
    suspend fun discoverModels(
        reviewedBySha256: Map<String, String>,
        limits: DiscoveryLimits = DiscoveryLimits(),
    ): Result<List<DiscoveredModel>>
}

// core:policy - session semantics (saved defaults vs one-request override)
data class SettingsSession(val saved: SettingsDocumentV1, val pendingLlmOverride: LlmSettings?, ...) {
    val effectiveLlm: LlmSettings          // the override when armed, otherwise saved defaults
}
class SettingsSessionPolicy {
    fun fromLoad(document, fromFile, fellBackToDefaults, warnings): SettingsSession
    fun applyOnce(session, llm): SettingsSessionResult             // Applied | Rejected(issues)
    fun prepareSave(session, document): SettingsSessionResult      // validates before any write
    fun reset(session): SettingsSession
    fun resolveForRequest(session): ResolvedRequestSettings        // consumes the override
    fun maxNewTokensCeiling(session, runtimeContextTokens: Int?): Int
}

// app - composition seam over pure ports only, unit-testable with fakes
class WorkspaceSettingsCoordinator(grant, store, discovery) { val state: StateFlow<WorkspaceUiState> }
```

Privacy invariants: the settings schema has **no free-text field** (cannot store prompts/documents/credentials); settings write verification rejects unknown fields; SAF uses `ACTION_OPEN_DOCUMENT_TREE` with persistable permission — no `MANAGE_EXTERNAL_STORAGE`, no raw path translation; discovery registers metadata only and never auto-loads inference.

### 3.7 UI specification

- `UiMode`: `CHAT | SCREEN_READER | AUTOMATOR`.
- `RuntimeOperation`: none/idle/download/import/export/load/ready/generate/cancel/screen-read/automate/error.
- `MainUiState`: immutable aggregate (messages, model/catalog state, metrics, device state, proposal counters, pending proposal, audit integrity, settings visibility).
- `MainViewModel.state: StateFlow<MainUiState>`; `LaiApp(viewModel)` is the Compose shell.
- Target (not implemented): Tools Dashboard, Chat **+ Attach Tools**, contextual **⚙** quick-settings bottom sheet — all over shared contracts, never duplicate tool engines.

---

## 4. Next Logical Implementation Phase

### Phase 2A is code-complete (items 1–8); what remains is verification

All eight Phase 2A items exist in source and are **build verified**: run
[`31975456732`](https://github.com/soobujmiah/LAI/actions/runs/31975456732) (`475f942`) passed the
source/architecture/catalog/documentation policy, the pure-JVM coverage ratchets, the
`:platform:audit` / `:platform:download` / `:platform:workspace` / `:app` unit tests and lint, the
native llama.cpp arm64 build, and debug APK assembly.

**Open device regression (2026-08-17, build `0.6.78-debug`).** A physical chat run on the Redmi
Turbo 4 Pro reported device heating, an empty reply, and a permanently stuck **Stop**. Root causes
for the heat, the stuck cancel, the empty bubble, the IME/status-bar overlap and the missing
Settings back affordance were fixed in `e4ad398` (CI
[`31979369975`](https://github.com/soobujmiah/lai/actions/runs/31979369975) green). **Still
unconfirmed:** why the generation produced zero tokens and why the model was unloaded — memory
pressure and a masked native failure are both consistent with the evidence. See
`docs/device-results/2026-08-17-redmi-turbo-4-pro-chat-regression.md` for the full analysis and the
capture steps for the next run.

One gate remains:

1. **Device gate (Redmi Turbo 4 Pro, SM8735).** Physically confirm the Phase 2A acceptance list in
   §4.1 below, then record the result in `docs/device-results/`.

> Note for the next session: this session also fixed a **pre-existing** main breakage unrelated to
> Phase 2A. Commit `84862f8` was already red (run `31973297070`): `dcc42d2` deleted
> `ModelRepository.PROGRESS_STEP_BYTES` while the retained-copy export still referenced it. Restored
> in `475f942`. Main is green again.

### 4.1 Phase 2A device acceptance checklist

- App starts with **no** workspace grant and uses safe defaults; nothing crashes and no permission
  dialog appears at launch.
- Chat ⚙ opens the sheet; **Apply once** changes only the next reply, and the reply after it is back
  on saved defaults (watch the status line, which states which is in force).
- **Save default** persists: kill the app, reopen, and the sheet shows the saved values.
- **Reset** returns to 0.7 / 0.9 / 256 and, when a workspace is connected, rewrites the file.
- Settings → Workspace folder: grant via the system folder picker, confirm the status flips to
  Connected, and confirm `models/ tools/ config/ cache/` appear in the chosen folder.
- Hand-edit `config/settings.json` into malformed JSON, reopen the app: it must fall back to
  defaults with an explanatory status line, never crash.
- Put a GGUF in `models/`, press **Scan for models**: counts appear, and **no** model is loaded
  (the active model must not change and no memory spike should occur).
- Revoke the folder permission from Android Settings, reopen LAI: state shows revoked, counts reset,
  defaults in use.
- Existing Qwen 1.5B download/import/load/chat + retained-copy paths still pass unchanged.

### 4.2 Then, in order

1. Categorized Model Center + WorkManager foreground downloader (pause/resume/cancel).
2. Dual Dashboard / Chat **+ Attach Tools** shell over the same shared contracts.
3. Rolling Context Window with explicit summary checkpoints (`keepLastTurns` is already a typed,
   validated, user-editable setting — the window implementation is what's missing).
4. Real printed Bangla OCR CPU baseline behind the existing `OcrEngine` contract.
5. Vulkan qualification + closed-loop thermal/battery control.
6. Native C++ micro-model TaskGraph → streaming STT/TTS + Barge-In.
7. SQLCipher vector DB + RAG.
8. QNN/HTP adapter only after licensed tooling + converted artifacts exist.

### Process note for next session

The in-workspace source gate (`scripts/validate_repo.sh`) does **not** compile Kotlin — it only
checks architecture/size/catalog/documentation. Treat GitHub Actions as the real compile/test gate:
push, then confirm "Unit tests and lint" is green. Do not mark a module "build verified" until it is.

A concrete lesson from the previous session: three intermediate commits failed
`:core:policy:compileKotlin` on errors (a missing argument, missing test imports) that no local gate
could catch. When touching Kotlin, re-read the diff for signature/import correctness before pushing.
