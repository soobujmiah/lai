# LAI Project State

Snapshot date: 2026-08-17 (end of session)
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Latest release: **`v0.9.7`** (`5921f1b`) — **green, production-signed** · Releases `v0.9.0`–`v0.9.7` all carry signed APKs
Graph: **16 Gradle modules** · Source footprint ~0.9 MB (limit 128 MB) · **169 unit tests** · WorkManager 2.10.1 · Actions majors current (checkout v7, upload-artifact v7, setup-java v5, setup-android v4, gradle/actions v6)

> Handoff snapshot. Source and CI are authoritative; `docs/ROADMAP.md` is the canonical Phase 0–14 roadmap and accepted ADRs govern architecture.

**Status vocabulary.** **Implemented** = source exists · **Build verified** = CI compiles/tests it · **Device validated** = named behaviour observed on physical hardware · **Scaffold** = compiling boundary with honest unavailable behaviour · **Pending/Planned** = not built.

> ✅ **Milestones this cycle:** first chat replies ever (P0 closed, en+bn) → KV-prefix reuse **device-validated at ~25× faster steady-state TTFT (17 s → ~0.6 s)** → production signing key + 8 signed releases → rolling context window → Bangla quality pass → background downloads → persistent chat history → closed-loop thermal governor.

---

## 1. Architecture Progress

### 1.1 LLM Core & native runtime

| Area | Status | Result | Remaining |
|---|---|---|---|
| Inference contract | Build verified | `InferenceEngine`, opaque `BackendId`/`BackendDescriptor`, streaming events, honest metrics incl. `evaluatedPromptTokens` | — |
| llama.cpp CPU adapter | **Device validated (load + generation)** | arm64 JNI/C++, pinned llama.cpp, mmap GGUF, chat template, cancel, metrics. Load 0.7–1.5 s; decode 12–19 tok/s; prefill 17–31 tok/s | — |
| **KV-prefix reuse** | **Device validated (0.9.5)** | `kv_tokens_` mirrors the cache; longest-common-prefix reuse + `llama_memory_seq_rm`; TTFT flat ~0.6 s, `evaluatedPromptTokens` as low as 1; exceptions invalidate wholesale | — |
| Reviewed model | Device validated | Qwen 2.5 1.5B Instruct Q4_K_M, exact 1,117,320,736 bytes + SHA-256 | Bangla-stronger model decision |
| Bangla quality pass | Build verified (0.9.4) | Tuned bilingual system prompt (short simple Bangla, no literal translation, admit ignorance) + repetition penalty 1.1/64 after top-p | Qualitative device check — several 3-token replies observed |
| Backend scheduler | Device validated (CPU) | Compatibility, memory preflight, battery/thermal admission, evidence-based selection | — |
| Token streaming | Device validated | `trySendBlocking` + `buffer(256)`; 14+ streamed replies observed | — |
| **Thermal governor** | Build verified (0.9.7) | Closed loop: `PowerManager` callback flow → `ThermalGovernorPolicy` (hysteresis, 9 tests) → JNI atomic → threads changed only between `llama_decode` calls; plain-language notices; `LAI-llama` trace | Device validation under sustained warm load |
| Native stall tracing | Device proven useful | µs logs: mutex wait, template, tokenize, per-chunk prefill, first token, thermal thread changes | — |
| Rolling context window | Build verified (0.9.3) | `ContextWindowPolicy` applies `keepLastTurns` before token counting; `windowedConversationTurns` in diagnostics, separate from overflow trims | Exercise on device (slider low → windowed > 0) |
| Vulkan backend | Planned | `llama-vulkan` descriptor reserved | Compile ggml Vulkan, Adreno qualification |
| Qualcomm QNN/HTP (NPU) | Planned — no code | Boundary documented only | Licensed QAIRT CI + model conversion |

### 1.2 Accessibility Service & Android control

| Area | Status | Result | Remaining |
|---|---|---|---|
| Service lifecycle | Device validated (connects) | Weak service ref, connection state, `AccessibilityGateway` | Service-death/rebind tests |
| Screen snapshot | Device validated | Flattened tree ≤400 nodes/depth 24, password text omitted | Foreground-screen binding |
| Actions (click/type/scroll/global/launch) | Build verified | Selectors by viewId/text/contentDescription/path | Physical per-action harness |
| Screenshot | Capability observed | Android 11+ accessibility screenshot → in-memory ARGB | OCR + redaction |
| Shizuku | Device validated (UID 2000, READY) | Binder state, dedicated UserService, argv allowlist, bounds | Recipe orchestration |
| One-shot tool proposals | Device validated (parse path) | Bounded JSON parser; 6 responses examined on device, all correctly `NOT_TOOL_CALL`; `ToolInstructionGate` relevance gate + compressed instruction | Physical action-dispatch test |
| Persistent tool audit | Build verified | App-private no-backup JSONL hash chain, approval-before-authority, replay guard | Restart/replay device test |

### 1.3 Bangla OCR

| Area | Status | Result | Remaining |
|---|---|---|---|
| OCR contracts | Build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult` (blocks, language, confidence, polygon, handwriting flag) — structured JSON for the LLM runtime | Preserve schema |
| Screenshot→OCR pipeline | Implemented scaffold | Capture, background dispatch, bitmap recycle, typed unavailable error | Integrate real model |
| Printed Bangla OCR | **Pending — placeholder only** | `PlaceholderBanglaOcrEngine` honestly fails with `OcrModelRequiredException` | ❗ **Blocked on owner's dataset/licence decision** |
| Handwritten Bangla OCR | Planned | Schema seam only | Dataset/model/licence |

### 1.4 GitHub Actions, delivery & supply chain

| Area | Status | Result |
|---|---|---|
| Source policy | Ready | 128 MB cap; no binaries/models/SDKs/keystores; history scanned clean |
| Architecture policy | Ready | Network only in `platform:download`; audit bytes only in `platform:audit`; module direction enforced |
| Android build | Ready | JDK 17, API 35, NDK 27, CMake, Gradle 8.13, pinned llama.cpp; tests + lint + APK; action majors current |
| Tests / coverage | Ready | 169 tests incl. `platform:history`; JaCoCo ratchets (contracts .15 / policy .55 / scheduler .70 / model .50 / plugins .50) |
| **Releases** | **Production-signed** | Tag-triggered; `v0.9.0`–`v0.9.7` signed with the permanent `lai-release` RSA-4096 key (V1–V4), cert SHA-256 `80:03:8D:3E…7E:8E`, verified against published assets |
| Signing key custody | Done | PKCS12 in Actions secrets (`ANDROID_KEYSTORE_*`) + owner's offline copy — **never in the repo** |
| Dependabot | Curated | 5 action bumps merged & combination-verified; **#4/#9/#10/#11 open and failing** (androidx, okhttp 5, AGP 9.3.1, Kotlin group) — need one coordinated Gradle 9 + AGP 9 + Kotlin upgrade session |
| Production supply chain | Partial | Still pending: SBOM, provenance, reproducible builds |

### 1.5 Product surfaces (UI)

| Area | Status | Result | Remaining |
|---|---|---|---|
| Compose three-mode shell | Device validated | Chat, Screen Reader, Automator; Developer Mode hidden | — |
| Chat | **Device validated** | Streaming, Stop watchdog, metrics; en+bn replies observed | Bangla reply-quality judgement |
| Keyboard/IME | **Device validated (0.9.1)** | Composer above keyboard; mode bar keyed on `imeAnimationTarget` — close animation confirmed smooth | — |
| **Chat history** | Build verified (0.9.6) | History sheet: restore/continue/delete; auto-save every reply; New chat archives | Device walkthrough |
| **Background downloads** | Build verified (0.9.5) | WorkManager; survives app exit; Pause/Cancel; reattach on relaunch | Device walkthrough (close app mid-download) |
| Model management | Build verified (0.9.7) | Catalog one-tap install, Load/Unload, **Delete** (active-guarded), storage summary, Keep copy export | — |
| Quick settings ⚙ | Device validated (renders) | Creativity/focus/reply-length/memory; Apply once · Save default · Reset | Exercise memory slider on device |
| Tools Dashboard | Planned | Spec only | — |

---

## 2. Current Directory Tree

```text
lai/
├── PROJECT_STATE.md                     # this file
├── README · CHANGELOG · CONTRIBUTING · SECURITY · LICENSE
├── THIRD_PARTY_NOTICES · THIRD_PARTY_LICENSES · MODEL_LICENSES
├── build.gradle.kts                     # root plugins + JaCoCo coverage ratchets
├── settings.gradle.kts                  # 16-module graph
├── gradle.properties · gradle/libs.versions.toml
├── .github/
│   ├── dependabot.yml · pull_request_template.md
│   └── workflows/{android_build.yml, catalog_publish.yml}
├── catalog/{catalog-public-key.pem, models-v1.json}      # signed catalog revision 3
├── app/                                                   # composition root + Compose shell
│   ├── src/main/java/dev/lai/runtime/
│   │   ├── LaiApplication.kt · MainActivity.kt            # high-refresh-rate opt-in
│   │   ├── core/AppContainer.kt                           # single composition root
│   │   └── ui/{LaiApp.kt, MainViewModel.kt, QuickSettingsSheet.kt,
│   │           WorkspaceSettingsCoordinator.kt, theme/Theme.kt}
│   ├── src/main/res/{drawable, values, values-bn}         # en/bn string parity (68 each)
│   └── src/test/…/WorkspaceSettingsCoordinatorTest.kt
├── core/                                                  # pure JVM: no Android/network/JNI/vendor
│   ├── contracts/ → agent · audit · automation · core/JsonConfig · diagnostics
│   │                history/ChatHistoryModels · inference (incl. BackgroundDownloadStatus,
│   │                GenerationMetrics.evaluatedPromptTokens) · ocr · settings · shell · workspace
│   ├── model/    → ReviewedModelCatalog
│   ├── policy/   → agent{AgentPolicy,BuiltInToolCatalog,ToolAuditLedger,ToolInstructionGate,…}
│   │               privacy · settings/{SettingsPolicy,SettingsSession,ContextWindowPolicy}
│   │               shell · workspace/WorkspacePolicy
│   └── scheduler/→ InferenceScheduler · ModelMemoryEstimator · ThermalGovernorPolicy
├── platform/                                              # Android authority boundaries
│   ├── accessibility/→ AccessibilityAutomationService · AccessibilityGateway · NodeSnapshotter
│   ├── audit/        → ToolAuditRepository                # only module writing audit bytes
│   ├── device/       → AndroidRuntimeEnvironmentProvider  # + thermalStates() callback flow
│   ├── download/     → ModelRepository · ModelDownloadWorker · ModelDownloadCoordinator
│   │                   RemoteModelCatalogRepository       # ONLY network owner; WorkManager here
│   ├── history/      → ChatHistoryRepository              # ONLY content-bearing store; no-backup
│   ├── shizuku/      → ElevatedShell · PrivilegedUserService · ShizukuController (+AIDL)
│   └── workspace/    → WorkspaceRepository · WorkspaceSettingsStore · WorkspaceDiscovery
│                       WorkspaceSaf · ModelFormatDetector
├── runtime/                                               # replaceable adapters
│   ├── llama/    → NativeInferenceEngine.kt (setDecodeThreadLimit)
│   │               cpp/{native_inference, llama_cpu_backend (KV reuse, tracing, thermal atomic),
│   │                    backend_registry, include/lai/backend.h}
│   ├── ocr/      → BanglaOcrService (placeholder engine)
│   └── orchestrator/ → AgentRuntime
├── plugins/api/  → LaiPlugin
├── docs/         → 45+ files: ARCHITECTURE · MODULES · STATUS · ROADMAP · BANGLA_OCR
│                   VENDOR_BACKEND_STRATEGY · PRIVACY_INVARIANTS · adr/ · architecture/
│                   implementation/ · product/ · legal/
│                   device-results/ (6 reports incl. first-replies and kv-reuse-validated)
└── scripts/      → validate_repo.sh · check_architecture_boundaries.py
                    validate_documentation.py · validate_model_catalog.py · ci/fetch_llama_cpp.sh
```

**Verified clean:** no APK/AAB/AAR/SO, GGUF/ONNX/TFLite/QNN model, SDK, keystore, or Gradle wrapper JAR is tracked — checked across full history.

---

## 3. API & Module Specifications

### 3.1 Module dependency contract

```text
app (composition + Compose)
├── core:contracts · core:policy · core:scheduler · core:model · plugins:api
├── platform:download · platform:audit · platform:device · platform:history
├── platform:accessibility · platform:workspace · platform:shizuku
└── runtime:llama · runtime:ocr · runtime:orchestrator
```

**Enforced by `scripts/check_architecture_boundaries.py`:** core imports no Android/platform/runtime/vendor; platform never depends upward; runtime owns no UI; `app` is the only composition root; **network only in `platform:download`** (WorkManager also lives there; the app never imports androidx.work); **audit bytes only in `platform:audit`**; chat content only in `platform:history`.

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
// NativeInferenceEngine additionally: fun setDecodeThreadLimit(decodeThreads: Int)  // thermal hook

data class GenerationMetrics(promptTokens, generatedTokens, promptEvaluationMs,
    timeToFirstTokenMs, decodeMs, totalMs,
    evaluatedPromptTokens /* = promptTokens when no KV prefix was reused */)
// promptTokensPerSecond divides by evaluatedPromptTokens — never the inflated total.
```

**JNI boundary:** `runtimeInfo / createSession / countTokens / generate / setThreadLimit / destroySession / lastError` (metrics array = 7 slots).
**C++ boundary:** `BackendSession { count_tokens, generate, set_thread_limit }` — thread budget is an atomic applied only **between** `llama_decode` calls.
**KV reuse:** `kv_tokens_` mirrors the cache token-for-token after every successful decode; longest common prefix kept via `llama_memory_seq_rm(0, reused, -1)`; ≥1 prompt token always re-decoded for fresh logits; any exception clears cache + bookkeeping.
**Sampling:** top-p → repetition penalty (1.1/64) → temperature → dist; greedy path untouched.
**Streaming:** `callbackFlow` + `trySendBlocking` + `.buffer(256)` — never `trySend`.

### 3.3 Tools & policy

15 built-in tools: `screen.snapshot` `screen.click` `screen.type` `screen.scroll` `system.global_action` `app.launch` `ocr.current_screen` `shell.operation` `device.info` `settings.get` `settings.put` `package.list_user` `package.install_existing` `package.force_stop` `input.keyevent`.

```kotlin
AgentRuntime.parseToolProposal(modelOutput): ToolCallParseResult   // Accepted | Rejected | NotToolCall
AgentRuntime.execute(call, userConfirmed=false): ToolResult
ToolInstructionGate.shouldIncludeInstruction(latestUserText): Boolean
    // EN word-boundary regex + inflection-tolerant Bangla stems; recall-biased;
    // "hi" carries ZERO tool tokens even with proposals enabled.
```

Invariants: one proposal per response; validated twice; approval recorded **before** authority; replay blocked; sensitive text entry forbidden; model text can never self-approve.

### 3.4 Settings, history & downloads

```kotlin
// core:policy
SettingsSessionPolicy { applyOnce / prepareSave / resolveForRequest / maxNewTokensCeiling }
ContextWindowPolicy.applyTurnWindow(history, keepLastTurns): WindowedConversation
    // keeps last N completed turns + in-flight request; drops from the front; droppedTurns reported
ThermalGovernorPolicy.decide(state, previous, baselineThreads): Decision
    // Decision(decodeThreads, admitNewGeneration, reason); threads fall instantly,
    // rise only at fully NOMINAL (hysteresis)

// core:contracts (history) — platform:history is the storage authority
StoredChatSession(schemaVersion=1, id, title, createdAt, updatedAt, messages)
ChatHistoryRepository { list / load / save / delete }   // ≤100 sessions, ≤512 msgs, atomic writes

// core:contracts (downloads) — platform:download is the authority
BackgroundDownloadStatus(modelId, state ∈ {ENQUEUED,RUNNING,SUCCEEDED,FAILED,CANCELLED}, progress, failureReason)
ModelDownloadCoordinator { enqueue(spec) / stop(id) / observe(id) / observeAll() }
    // pause = stop (keeps .part, HTTP-Range resume); cancel = stop + discardPartial(id)
```

**Privacy invariants:** settings schema has no free-text field; chat content lives ONLY in `platform:history` (no-backup, never SAF/network/diagnostics); diagnostics exclude prompts, replies, filenames, digests; SAF is grant-based (`ACTION_OPEN_DOCUMENT_TREE`), no `MANAGE_EXTERNAL_STORAGE`.

### 3.5 UI

`UiMode { CHAT | SCREEN_READER | AUTOMATOR }` · `RuntimeOperation { NO_MODEL … ERROR }` · `MainUiState` immutable `StateFlow` aggregate — now also `chatSessions`, `chatHistoryVisible`, `downloadingModelId`, `windowedConversationTurns`, `thermalGovernorDetail`. `ChatMessage.id` keys `LazyColumn`. Mode-bar visibility keys on `WindowInsets.imeAnimationTarget` (never the current inset). Diagnostics v1 additionally reports `evaluatedPromptTokens` and `windowedConversationTurns`.

---

## 4. Next Logical Implementation Phase

### Priority 1 — Device walkthrough of the 0.9.6/0.9.7 features (needs the phone, ~10 minutes)

1. **Chat history:** chat → New chat → History → tap old session → continues in context; force-close app → History intact.
2. **Background download:** start a model download → close the app → reopen → progress reattached; try Pause → Download (resumes) and Cancel.
3. **Thermal governor:** long generation while the device is warm → expect the "Reduced CPU threads…" notice; `adb logcat -s LAI-llama` shows `thermal: decode threads X -> Y`.
4. **Memory slider:** set conversation memory to 1–2, chat 4+ turns, export diagnostics → `windowedConversationTurns > 0`.
5. **Bangla reply quality:** judge whether 0.9.4's short replies read as appropriately concise or over-trimmed (several 3-token replies were observed). If over-trimmed: soften the brevity instruction and/or drop the penalty to 1.05.

### Priority 2 — Printed Bangla OCR CPU baseline ⚠️ blocked on the owner

The single biggest unbuilt feature. Requires the owner's dataset/licence decision (see `docs/BANGLA_OCR.md`). Once decided: integrate the model behind the existing `OcrEngine` contract → structured `OcrResult` JSON feeds the LLM runtime → wire `ocr.current_screen` end-to-end with redaction.

### Priority 3 — Coordinated dependency upgrade session (pure code, one PR)

Dependabot #10 (AGP 9.3.1) requires Gradle 9; #11 (Kotlin group), #4 (androidx), #9 (okhttp 5) fail against the current matrix. Do all together: bump `GRADLE_VERSION` in the workflow + AGP + Kotlin + androidx in one branch, fix API breaks (okhttp 5 changes in `platform:download`), verify on CI, then close the four PRs.

### Then, in order

1. **Tools Dashboard / physical tool-dispatch harness** — the proposal parse path is device-validated; actual click/type dispatch is not.
2. **Vulkan qualification** (no licence needed; GGUF works directly) — evaluate before QNN.
3. **Production supply chain** — SBOM, provenance attestation, reproducible builds.
4. **QNN/HTP NPU** — licensed QAIRT CI + model conversion; furthest out.

### Process notes for the next session

- **CI is the only compile gate.** `scripts/validate_repo.sh` checks size/architecture/docs but does not compile Kotlin. Push, watch "Compile Kotlin, C++ and APK", then claim.
- **Auth:** git remote + identity are wiped between sessions — re-add `origin` and `git config user.*`. Tokens do not persist; ask the owner (device-flow needs no secret in chat; a classic PAT needs `repo, workflow` to touch workflow files). The owner was advised to revoke the tokens used this session.
- **Release ritual:** merge to main → CI green → annotated tag `v0.9.x` → tag run builds + signs + publishes the APK automatically. Signing needs no per-release action.
- **Regressions to avoid:** never `trySend` for streamed tokens; never `imePadding()` twice; never gate the bottom bar on the current IME inset; the Stop watchdog must not unload the model; reply budget < full context; `kv_tokens_` must only be appended after a *successful* decode; thread changes only between decodes.
