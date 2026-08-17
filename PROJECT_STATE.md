# LAI Project State

Snapshot date: 2026-08-17
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime`
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Latest release: **`v0.9.1`** (`6206199`) — **green, production-signed, device-validated** (chat replies + IME close animation)
Graph: **15 Gradle modules** · Source footprint **823 KB** (limit 128 MB) · **153** unit tests · WorkManager 2.10.1

> Handoff snapshot. Source and CI are authoritative; `docs/ROADMAP.md` is the canonical Phase 0–14 roadmap and accepted ADRs govern architecture.

**Status vocabulary.** **Implemented** = source exists · **Build verified** = CI compiles/tests it · **Device validated** = named behaviour observed on physical hardware · **Scaffold** = compiling boundary with honest unavailable behaviour · **Pending/Planned** = not built.

> ✅ **P0 closed 2026-08-17.** Build `0.9.0` (signed) produced **6 completed generations on the device** in English and Bangla — the first replies in project history. Root cause confirmed: prefill runs at ~25–31 tok/s, so the old ~407-token prompt needed ~14 s and the former 4 s watchdog killed every healthy generation. Evidence in §4.

---

## 1. Architecture Progress

### 1.1 LLM Core & native runtime

| Area | Status | Result | Remaining |
|---|---|---|---|
| Inference contract | Build verified | `InferenceEngine`, opaque `BackendId`/`BackendDescriptor`, streaming events, metrics | — |
| llama.cpp CPU adapter | **Device validated (load + generation)** | arm64 JNI/C++, pinned llama.cpp, mmap GGUF, model chat template, cancel, metrics. Load **721 ms**; decode **15–19 tok/s**; prefill **25–31 tok/s** (0.9.0, six generations) | KV-prefix reuse: TTFT grows linearly with history (6.2 s → 17 s by turn 6) because every generate re-prefills the whole conversation |
| Reviewed model | Device validated (load) | Qwen 2.5 1.5B Instruct Q4_K_M, exact 1,117,320,736 bytes + SHA-256 | Bangla quality pack |
| Backend scheduler | Device validated (CPU) | Compatibility, memory preflight (1.93 GB est. peak), battery/thermal admission, evidence-based selection | Closed-loop throttling |
| Token streaming | **Device validated** | `trySendBlocking` + `buffer(256)` — six streamed replies observed (0.9.0) | — |
| Thermal admission | Build verified | Refuses new generation at `SEVERE`+ | **Reactive only**; no closed-loop governor |
| CPU thread policy | Build verified | Half the cores (2–4) for decode *and* batch | Device heat retest |
| Vulkan backend | Planned | `llama-vulkan` descriptor reserved | Compile ggml Vulkan, Adreno qualification |
| Qualcomm QNN/HTP (NPU) | **Planned — no code** | Boundary documented only | Licensed QAIRT CI, model conversion, dedicated `runtime:qnn` |

### 1.2 Accessibility Service & Android control

| Area | Status | Result | Remaining |
|---|---|---|---|
| Service lifecycle | Device validated (connects) | Weak service ref, connection state, `AccessibilityGateway` | Service-death/rebind tests |
| Screen snapshot | Device validated | Flattened tree, ≤400 nodes / depth 24, password text omitted | Foreground-screen binding |
| Actions | Build verified | Click/type/scroll/global/launch; selectors by viewId/text/contentDescription/path | Physical per-action harness |
| Screenshot | Capability observed | Android 11+ accessibility screenshot → in-memory ARGB | OCR + redaction |
| Shizuku | Device validated (UID 2000) | Binder state, dedicated UserService, argv allowlist, time/output bounds | Recipe orchestration |
| One-shot tool proposals | Build verified | Bounded JSON parser, canonical schemas, trusted Compose review, second-dispatch validation; **`ToolInstructionGate` relevance gate + compressed instruction (this session)** | ❗ Physical retest blocked by §1.1 |
| Persistent tool audit | Build verified | App-private no-backup JSONL hash chain, approval-before-authority fsync, replay guard | Restart/replay device test |

### 1.3 Bangla OCR

| Area | Status | Result | Remaining |
|---|---|---|---|
| OCR contracts | Build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult` (blocks, language, confidence, polygon, handwriting flag) | Preserve schema |
| Screenshot→OCR pipeline | Implemented scaffold | Capture, background dispatch, bitmap recycle, typed unavailable error | Integrate real model |
| Printed Bangla OCR | **Pending — placeholder only** | `PlaceholderBanglaOcrEngine` honestly fails with `OcrModelRequiredException` | ❗ **Blocked on your dataset/licence decision** |
| Handwritten Bangla OCR | Planned | Schema seam only | Dataset/model/licence |
| QNN OCR acceleration | Planned | — | After CPU baseline |

### 1.4 GitHub Actions, catalog & delivery

| Area | Status | Result |
|---|---|---|
| Source policy | Ready | 128 MB cap; no binaries/models/SDKs/keystores; token scan — **verified clean** |
| Architecture policy | Ready | Network, Accessibility, Shizuku, JNI, vendor-term and dependency-direction checks |
| Android build | Ready | JDK 17, API 35, NDK 27, CMake, Gradle 8.13, pinned llama.cpp; tests + lint + APK |
| Tests / coverage | Ready | 142 tests; JaCoCo ratchets (contracts .15 / policy .55 / scheduler .70 / model .50 / plugins .50) |
| Catalog publish | Ready | Validates/signs/verifies `catalog-v1` (revision 3) |
| Releases | **Production-signed (v0.9.0)** | Tag-triggered APK; `v0.9.0` (run 88) signed with the permanent `lai-release` RSA-4096 key (V1–V4), cert SHA-256 `80:03:8D:3E…7E:8E`, verified against the published asset |
| Production supply chain | **Partial** | ✅ Permanent key (PKCS12, secrets `ANDROID_KEYSTORE_*`; owner holds offline copy — never commit it) · still pending: SBOM, provenance, reproducible builds |

### 1.5 Tool configuration & SAF workspace (Phase 2A — complete)

| Area | Status | Result |
|---|---|---|
| Typed settings contracts | Build verified | `SettingsDocumentV1` (Llm/ImageGen/Voice/Search), bounded ranges, Qwen defaults |
| Settings validation/migration | Build verified | `SettingsPolicy` validate/sanitize/migrate; unknown-field warnings |
| Session semantics | Build verified | `SettingsSession` — saved defaults vs one-request override; validate-before-transition |
| Workspace decision layer | Build verified | `WorkspacePolicy.classify` + `WorkspaceSettingsCodec` |
| Pure ports | Build verified | `WorkspaceGrantPort` / `SettingsStorePort` / `ModelDiscoveryPort` — no Android type crosses |
| SAF adapter | Build verified; **device pending** | Grant, persistable permission, temp-write-then-replace, bounded SHA-256 discovery |
| Composition | Build verified | `WorkspaceSettingsCoordinator` over pure ports; `MainViewModel` binds |
| Chat ⚙ quick settings | **Device validated (renders)** | Bottom sheet: creativity/focus/reply-length/memory; Apply once · Save default · Reset |

### 1.6 UI & product surfaces

| Area | Status | Result | Remaining |
|---|---|---|---|
| Compose three-mode shell | Device validated | Chat, Screen Reader, Automator; Developer Mode hidden | — |
| Chat | **Device validated (0.9.0)** | Six replies, en + bn, streaming and metrics captured | Bangla output quality is weak (base-model limitation) — Bangla quality pack |
| Keyboard/IME insets | **Device validated (0.9.1)** | Composer above keyboard; mode bar keyed on `imeAnimationTarget` — close animation confirmed smooth on device, no layout displacement | — |
| High refresh rate | Build verified; **device pending** | Highest display mode requested at current resolution (90/120 Hz) | Confirm |
| List performance | Build verified | Stable `ChatMessage.id` keys; only changed bubble recomposes; auto-scroll | Confirm |
| Settings back navigation | Device validated | Back arrow + `BackHandler` | — |
| Tools Dashboard / Attach Tools | Planned | Spec only | — |

---

## 2. Current Directory Tree

```text
lai/
├── PROJECT_STATE.md                     # this file
├── README · CHANGELOG · CONTRIBUTING · SECURITY · LICENSE
├── THIRD_PARTY_NOTICES · THIRD_PARTY_LICENSES · MODEL_LICENSES
├── build.gradle.kts                     # root plugins + JaCoCo coverage ratchets
├── settings.gradle.kts                  # 15-module graph
├── gradle.properties · gradle/libs.versions.toml
├── .github/
│   ├── dependabot.yml · pull_request_template.md
│   └── workflows/{android_build.yml, catalog_publish.yml}
├── catalog/{catalog-public-key.pem, models-v1.json}      # signed catalog revision 3
├── app/                                                   # composition root + Compose shell
│   ├── src/main/java/dev/lai/runtime/
│   │   ├── LaiApplication.kt · MainActivity.kt            # MainActivity: high-refresh-rate opt-in
│   │   ├── core/AppContainer.kt                           # single composition root
│   │   └── ui/{LaiApp.kt, MainViewModel.kt, QuickSettingsSheet.kt,
│   │           WorkspaceSettingsCoordinator.kt, theme/Theme.kt}
│   ├── src/main/res/{drawable, values, values-bn}         # full en/bn string parity (62 each)
│   └── src/test/java/dev/lai/runtime/ui/WorkspaceSettingsCoordinatorTest.kt
├── core/                                                  # pure JVM: no Android/network/JNI/vendor
│   ├── contracts/ → agent · audit · automation · core/JsonConfig · diagnostics
│   │                inference{InferenceEngine,InferenceModels,ModelModels}
│   │                ocr · settings/ToolSettings · shell
│   │                workspace/{WorkspaceContracts, WorkspacePorts}
│   ├── model/    → ReviewedModelCatalog
│   ├── policy/   → agent{AgentPolicy,BuiltInToolCatalog,ToolAuditLedger,ToolProposalTelemetry}
│   │               privacy · settings/{SettingsPolicy,SettingsSession}
│   │               shell · workspace/WorkspacePolicy
│   └── scheduler/→ InferenceScheduler · ModelMemoryEstimator
├── platform/                                              # Android authority boundaries
│   ├── accessibility/→ AccessibilityAutomationService · AccessibilityGateway · NodeSnapshotter
│   ├── audit/        → ToolAuditRepository                # only module writing audit bytes
│   ├── device/       → AndroidRuntimeEnvironmentProvider
│   ├── download/     → ModelRepository · RemoteModelCatalogRepository   # ONLY network owner
│   ├── shizuku/      → ElevatedShell · PrivilegedUserService · ShizukuController (+AIDL)
│   └── workspace/    → WorkspaceRepository · WorkspaceSettingsStore · WorkspaceDiscovery
│                       WorkspaceSaf · ModelFormatDetector
├── runtime/                                               # replaceable adapters
│   ├── llama/    → NativeInferenceEngine.kt
│   │               cpp/{native_inference, llama_cpu_backend, backend_registry, include/lai/backend.h}
│   ├── ocr/      → BanglaOcrService (placeholder engine)
│   └── orchestrator/ → AgentRuntime
├── plugins/api/  → LaiPlugin
├── docs/         → 42 files: ARCHITECTURE · MODULES · STATUS · ROADMAP · BANGLA_OCR
│                   VENDOR_BACKEND_STRATEGY · PRIVACY_INVARIANTS · adr/0002-0007
│                   architecture/ · implementation/ · product/ · device-results/ · legal/
└── scripts/      → validate_repo.sh · check_architecture_boundaries.py
                    validate_documentation.py · validate_model_catalog.py · ci/fetch_llama_cpp.sh
```

**Verified clean:** no APK/AAB/AAR/SO, GGUF/ONNX/TFLite/QNN model, SDK, keystore, build output, or Gradle wrapper JAR is tracked.

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

**Enforced by `scripts/check_architecture_boundaries.py`:** core imports no Android/platform/runtime/vendor; platform never depends upward; runtime owns no UI; `app` is the only composition root; **network only in `platform:download`**; **audit bytes only in `platform:audit`**; Qualcomm/QNN terms never in generic inference/scheduler.

### 3.2 LLM / inference

```kotlin
interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    val contextSize: Int                                    // 4096
    suspend fun load(modelPath: String, backend: BackendId? = null): Result<Unit>
    fun generate(conversation: List<ConversationMessage>,
                 config: GenerationConfig = GenerationConfig()): Flow<InferenceEvent>
    suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int>
    override fun close()
}
```

`BackendId` (validated, namespaced — only `llama-cpu` is real) · `GenerationConfig(maxNewTokens, temperature, topP, seed)` · `ConversationMessage(SYSTEM|USER|ASSISTANT, content)` · `InferenceEvent { Token | Completed | Failed }` · `GenerationMetrics(promptTokens, generatedTokens, promptEvaluationMs, timeToFirstTokenMs, decodeMs, totalMs)`.

**JNI boundary:** `runtimeInfo / createSession / countTokens / generate / destroySession / lastError`.
**C++ boundary:** `Backend { name, available, open() }` + `BackendSession { count_tokens, generate() }`.
**Streaming:** `callbackFlow` + `trySendBlocking` + `.buffer(256)` — never `trySend` (drops on a rendezvous channel).
**Native concurrency:** `count_tokens` and `generate` share `generation_mutex_`; cancellation polled every 8 tokens and latched.

### 3.3 Tools & policy

15 built-in tools: `screen.snapshot` `screen.click` `screen.type` `screen.scroll` `system.global_action` `app.launch` `ocr.current_screen` `shell.operation` `device.info` `settings.get` `settings.put` `package.list_user` `package.install_existing` `package.force_stop` `input.keyevent`.

```kotlin
ToolCall(id, name, arguments: JsonObject)
ToolCallParseResult { Accepted(call, definition, confirmationSummary) | Rejected(code, message) | NotToolCall }
AgentRuntime.parseToolProposal(modelOutput): ToolCallParseResult
AgentRuntime.execute(call, userConfirmed=false): ToolResult
```

Invariants: exactly one proposal per response; validated twice (parse + dispatch); approval recorded **before** authority is invoked; exact-call replay blocked; sensitive text entry forbidden; model text can never self-approve.

### 3.4 Settings & workspace

```kotlin
// core:contracts — pure ports, no Android type crosses them
interface WorkspaceGrantPort { val state: WorkspaceGrantState }
interface SettingsStorePort {
    suspend fun load(): SettingsLoadOutcome                       // never throws
    suspend fun save(document: SettingsDocumentV1): Result<Unit>  // exact-v1-schema verified
}
interface ModelDiscoveryPort {
    suspend fun discoverModels(reviewedBySha256: Map<String,String>,
                               limits: DiscoveryLimits = DiscoveryLimits()): Result<List<DiscoveredModel>>
}

// core:policy
class SettingsSessionPolicy {
    fun applyOnce(session, llm): SettingsSessionResult            // Applied | Rejected(issues)
    fun prepareSave(session, document): SettingsSessionResult     // validates before any write
    fun resolveForRequest(session): ResolvedRequestSettings       // consumes the override
    fun maxNewTokensCeiling(session, runtimeContextTokens: Int?): Int   // ≤ half the context
}
```

**Privacy invariants:** the settings schema has **no free-text field**, so it can never absorb a prompt, document, or credential; writes reject unknown fields; SAF uses `ACTION_OPEN_DOCUMENT_TREE` with persistable permission — **no `MANAGE_EXTERNAL_STORAGE`, no raw path translation**; discovery registers metadata only and never auto-loads; diagnostics expose coarse counts only — never filenames or digests.

### 3.5 UI

`UiMode { CHAT | SCREEN_READER | AUTOMATOR }` · `RuntimeOperation { NO_MODEL, IDLE, DOWNLOADING, IMPORTING, EXPORTING, LOADING, READY, GENERATING, CANCELLING, READING_SCREEN, AUTOMATING, ERROR }` · `MainUiState` immutable aggregate exposed as `StateFlow` · `ChatMessage(fromUser, text, contextEligible, id)` — **`id` is required for LazyColumn keying**.

**Diagnostics v1** adds `lastGenerationFailure` (LAI-authored reason, never model output) and `emptyGenerationCount`, plus an internal `GenerationStage { IDLE, COUNTING_TOKENS, AWAITING_FIRST_TOKEN, STREAMING, COMPLETED }` reported on stall.

---

## 4. Next Logical Implementation Phase

### ✅ Priority 0 CLOSED — chat replies on device (0.9.0, 2026-08-17)

Six completed generations exported from the Redmi Turbo 4 Pro, English and Bangla, thermal `NOMINAL` throughout:

| Turn | Prompt tokens | TTFT | Prefill tok/s | Decode tok/s |
|---|---|---|---|---|
| 1 ("hi") | 159 | 6.2 s | 25.6 | 17.6 |
| 4 | 268 | 9.6 s | 28.1 | 15.7 |
| 6 | 470 | 17.0 s | 27.6 | 15.7 |

**Root cause, confirmed by measurement:** prefill runs at ~25–31 tok/s, so the pre-fix ~407-token prompt needed ~14 s — the former 4 s cancel watchdog aborted every healthy generation. The 45 s grace + the prefill cut (`ToolInstructionGate` kept "hi" at 159 tokens; all 6 proposal examinations correctly `NOT_TOOL_CALL`) fixed it. Model load also improved to **721 ms**. `productionSigned: true` — validated on the signed release.

### ✅ Priority 0 (TTFT growth) — KV-prefix reuse implemented, device verification pending

`LlamaCpuSession` now keeps `kv_tokens_` — the exact token sequence resident in the KV cache, maintained strictly after each successful `llama_decode`. Each `generate()` computes the longest common prefix between the cached sequence and the new templated prompt, drops only the divergent tail (`llama_memory_seq_rm(mem, 0, reused, -1)`), and prefills only the suffix. At least one prompt token is always re-decoded so the sampler has fresh logits. Any exception clears both the memory and the bookkeeping (full re-prefill is always correct). Expected: turn-N TTFT drops from O(whole conversation) to O(new turn) — roughly constant ~2–4 s instead of 17 s and climbing.

Metrics stay honest: `GenerationResult`/`GenerationMetrics`/diagnostics gained `evaluatedPromptTokens` (total minus reused prefix); `promptTokensPerSecond` divides by evaluated tokens, never the inflated total. The `LAI-llama` trace logs `reusing X of Y prompt tokens`.

**Device verify next session:** multi-turn chat — TTFT should stay roughly flat; diagnostics `performance[]` entries should show `evaluatedPromptTokens` ≪ `promptTokens` from turn 2 onward.

### ✅ Priority 1 CLOSED — IME close animation device-validated (0.9.1)

User confirmed on device: keyboard close is smooth, no bar displacement. `imeAnimationTarget` keying is the pattern to keep — never gate bottom-bar visibility on the current IME inset.

### Priority 2 — Closed-loop thermal governor

Current thermal handling only *refuses* at `SEVERE`. Needed: Android thermal callbacks, dynamic thread/batch reduction, cooldown hysteresis, and a visible reason in the UI. (Note: the 0.9.0 run stayed `NOMINAL` at 99% battery while charging — heat may already be acceptable after the thread-policy fix; re-measure during a long chat before building this.)

### Then, in order

1. **Bangla output quality** — cheap levers SHIPPED (tuned bilingual system prompt: short simple sentences, no literal translation, admit ignorance; + repetition penalty 1.1/64 after top-p). **Device-compare Bangla replies against the 0.9.0 screenshot.** If still weak, the remaining lever is a reviewed Bangla-stronger small model in the signed catalog — a model-selection decision for the owner.
2. **Phase 2A device acceptance** — SAF grant/revoke, settings persist across restart, malformed `settings.json` falls back, scan registers without loading.
3. **WorkManager downloader — DONE (device verify pending)**: `ModelDownloadWorker` + `ModelDownloadCoordinator` (`platform:download`; the app module never imports androidx.work). Downloads survive app exit/process death; interruptions resume from the last byte via the existing HTTP-Range `.part` path, so no foreground service is needed. Transient transport errors retry with backoff (max 8); policy/integrity failures are final. UI: Pause (keeps partial; Download resumes), Cancel (discards partial), background hint, reattach-on-launch via `adoptBackgroundDownloads()`. Remaining for a full Model Center: a dedicated screen listing catalog + installed models with per-model actions.
4. **Rolling Context Window** — `keepLastTurns` is typed and user-editable but does nothing yet; pairs naturally with the KV-prefix work.
5. **Printed Bangla OCR CPU baseline** — ⚠️ blocked on the dataset/licence decision.
6. **Vulkan qualification** (no licence needed, GGUF works directly) — evaluate before QNN.
7. **QNN/HTP NPU** — requires licensed QAIRT in CI **and** model conversion. Furthest out.
8. Production supply chain: ✅ permanent signing key (v0.9.0); still pending SBOM, provenance, reproducible builds.

### Dependency audit (2026-08-17)

**Merged 2026-08-17** (each CI-green individually AND combined on main, run #107): #1 `setup-android` 3→4, #2 `checkout` 4→7, #3 `upload-artifact` 4→7, #5 `gradle/actions` 4→6, #6 `setup-java` 4→5. **Still open, failing CI — do not merge as-is**: #4 androidx group, #9 okhttp 4→5 (major API break), #10 AGP 8.11→9.3.1 (needs Gradle 9; workflow pins 8.13), #11 Kotlin group — these four need one coordinated Gradle+AGP+Kotlin+deps upgrade session. History scan is clean: no keystore, password, or binary was ever committed; the repo is public — the signing key lives only in Actions secrets and the owner's offline copy.

### Process notes for the next session

- **CI is the only compile gate.** `scripts/validate_repo.sh` checks size/architecture/docs but **does not compile Kotlin**. Push, then confirm "Unit tests and lint" is green before claiming anything.
- **Auth:** `gh` CLI device-flow login works and credentials persist in `~/.config/gh/hosts.yml`. The `/tmp` gh binary and the git remote are cleared between sessions — re-add `origin` and re-download `gh` if needed.
- **Trust the diagnostics export over intuition.** Each report narrowed the cause materially; `performance: []` plus a loaded model is the signature of a pre-first-token stall.
- **Regressions to avoid** (all were introduced and fixed here): reply budget must never equal the full context; `safeDrawing` already contains the IME — never apply `imePadding()` twice; the Stop watchdog must not unload the model; never use `trySend` for streamed tokens.
