# LAI Project State

Snapshot date: 2026-08-17 (pause — initialization + hotfix complete)
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Latest release: **`v0.9.7`** (`5921f1b`) — **green, production-signed** · Debug head: `f7a0db0` (hotfix prefill stall) + `7fce77a` (Tools Dashboard v1 + SBOM) — CI #127 building
Graph: **16 Gradle modules** · Source footprint ~0.93 MB (limit 128 MB) · **169 unit tests** · WorkManager 2.10.1 · Actions majors current (checkout v7, upload-artifact v7, setup-java v5, setup-android v4, gradle/actions v6) · **Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10 ✅ verified (run 32018545745 green)**

> Handoff snapshot. Source and CI are authoritative; `docs/ROADMAP.md` is the canonical Phase 0–14 roadmap and accepted ADRs govern architecture.

**Status vocabulary.** **Implemented** = source exists · **Build verified** = CI compiles/tests it · **Device validated** = named behaviour observed on physical hardware · **Scaffold** = compiling boundary with honest unavailable behaviour · **Pending/Planned** = not built.

> ✅ **Milestones this cycle:** first chat replies ever (P0 closed, en+bn) → KV-prefix reuse **device-validated at ~25× faster TTFT (17 s → 0.6 s)** → production signing key + 8 signed releases → rolling context window → Bangla quality pass → background downloads → persistent chat history → closed-loop thermal governor → **coordinated AGP 9.3 upgrade green + Tools Dashboard v1 + hotfix for SM8735 prefill stall (179 sec → fix).**

---

## 1. Architecture Progress

### 1.1 LLM Core & native runtime
| Area | Status | Result | Remaining |
|---|---|---|---|
| Inference contract | Build verified | `InferenceEngine`, opaque `BackendId`/`BackendDescriptor`, streaming events, honest metrics incl. `evaluatedPromptTokens` | — |
| llama.cpp CPU adapter | **Device validated (load + generation)** | arm64 JNI/C++, pinned llama.cpp, mmap GGUF, chat template, cancel, metrics. Load 0.7–1.5 s; decode 12–19 tok/s; prefill 17–31 tok/s | Hotfix batch 128→64 for SM8735 |
| **KV-prefix reuse** | **Device validated (0.9.5)** | `kv_tokens_` mirrors cache; longest-common-prefix reuse + `llama_memory_seq_rm`; TTFT flat ~0.6 s, `evaluatedPromptTokens` as low as 1; exceptions invalidate wholesale | — |
| Reviewed model | Device validated | Qwen 2.5 1.5B Instruct Q4_K_M, exact 1,117,320,736 bytes + SHA-256 | Bangla-stronger model decision |
| Bangla quality pass | Build verified (0.9.4) | Tuned bilingual system prompt (short simple Bangla, no literal translation, admit ignorance) + repetition penalty 1.1/64 after top-p | Qualitative device check — several 3-token replies observed |
| Backend scheduler | Device validated (CPU) | Compatibility, memory preflight, battery/thermal admission, evidence-based selection | — |
| Token streaming | Device validated | `trySendBlocking` + `buffer(256)`; 14+ streamed replies observed | — |
| **Thermal governor** | Build verified (0.9.7 + hotfix f7a0db0) | Closed loop: `PowerManager` callback → `ThermalGovernorPolicy` (hysteresis, 9 tests) → JNI atomic → threads changed only between `llama_decode`; minimum 2 threads (was 1 at CRITICAL) | Device validation under sustained warm load (run 28671 stalled at 0.71 tok/s, fix in-flight) |
| Native stall tracing | Device proven useful | µs logs: mutex wait, template, tokenize, per-chunk prefill, first token, thermal thread changes — used to diagnose 179 sec stall | — |
| Rolling context window | Build verified (0.9.3) | `ContextWindowPolicy` applies `keepLastTurns` before token counting; `windowedConversationTurns` in diagnostics | Exercise on device (slider low → windowed > 0) |
| Vulkan backend | Planned | `llama-vulkan` descriptor reserved | Compile ggml Vulkan, Adreno qualification |
| Qualcomm QNN/HTP (NPU) | Planned — no code | Boundary documented only | Licensed QAIRT CI + model conversion |

### 1.2 Accessibility Service & Android control
| Area | Status | Result | Remaining |
|---|---|---|---|
| Service lifecycle | Device validated (connects) | Weak service ref, connection state, `AccessibilityGateway` | Service-death/rebind tests |
| Screen snapshot | Device validated | Flattened tree ≤400 nodes/depth 24, password text omitted | Foreground-screen binding |
| Actions (click/type/scroll/global/launch) | Build verified | Selectors by viewId/text/contentDescription/path | Physical per-action harness |
| Screenshot | Capability observed | Android 11+ accessibility screenshot → in-memory ARGB | OCR + redaction |
| Shizuku | Device validated (UID 2000, READY) | Binder state, dedicated UserService, argv allowlist, bounds — **run 126 + 28671: READY_UID_2000** | Recipe orchestration |
| One-shot tool proposals | Device validated (parse path) | Bounded JSON parser; diagnostics show `toolProposalsEnabled:true`, all correctly `NOT_TOOL_CALL` before chat | Physical action-dispatch test |
| Persistent tool audit | Build verified | App-private no-backup JSONL hash chain, approval-before-authority, replay guard, `auditIntegrityValid:true` | Restart/replay device test |

### 1.3 Bangla OCR
| Area | Status | Result | Remaining |
|---|---|---|---|
| OCR contracts | Build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult` (blocks, language, confidence, polygon, handwriting flag) — structured JSON for LLM | Preserve schema |
| Screenshot→OCR pipeline | Implemented scaffold | Capture, background dispatch, bitmap recycle, typed unavailable error | Integrate real model |
| Printed Bangla OCR | **Pending — placeholder only** | `PlaceholderBanglaOcrEngine` honestly fails with `OcrModelRequiredException` | ❗ **Blocked on owner's dataset/licence decision** |
| Handwritten Bangla OCR | Planned | Schema seam only | Dataset/model/licence |

### 1.4 GitHub Actions, delivery & supply chain
| Area | Status | Result |
|---|---|---|
| Source policy | Ready | 128 MB cap; no binaries/models/SDKs/keystores; history scanned clean — **928 KB** |
| Architecture policy | Ready | Network only in `platform:download`; audit bytes only in `platform:audit`; module direction enforced |
| Android build | **Verified (124) + in-flight 127** | JDK 17, **API 36**, NDK 27, CMake, **Gradle 9.5.0 + AGP 9.3.1**, pinned llama.cpp; tests + lint + APK; action majors current · 32018545745 green (kotlin.android + kotlinOptions removal); 32019489063 + f7a0db0 building |
| Tests / coverage | Ready | 169 tests incl. `platform:history` + scheduler thermal tests (updated for min 2); JaCoCo ratchets (contracts .15 / policy .55 / scheduler .70 / model .50 / plugins .50) |
| **Releases** | **Production-signed** | Tag-triggered; `v0.9.0`–`v0.9.7` signed with permanent `lai-release` RSA-4096 key (V1–V4), cert SHA-256 `80:03:8D:3E…7E:8E` |
| Signing key custody | Done | PKCS12 in Actions secrets (`ANDROID_KEYSTORE_*`) + owner's offline copy — **never in repo** |
| Dependabot | **Partially merged** | 5 action bumps merged; **#9 (okhttp 5.4.0), #10 (AGP 9.3.1), #11 (Kotlin 2.4.10 + coroutines 1.11.0 + serialization 1.11.0) merged ✅**; **#4 deferred** (androidx 2026.08.00/1.19.0 requires API 37 not on runners) |
| Production supply chain | **Started** | Lightweight SBOM (`app/build/sbom/sbom-*.txt`) artifact added in `7fce77a`; full CycloneDX/provenance pending |

### 1.5 Product surfaces (UI)
| Area | Status | Result | Remaining |
|---|---|---|---|
| Compose three-mode shell | Device validated | Chat, Screen Reader, Automator; Developer Mode hidden | — |
| Chat | **Device validated** | Streaming, Stop watchdog, metrics; en+bn replies observed — **run 28671: stalled at 45s watchdog (179 sec prefill at 0.71 tok/s, hotfix in-flight)** | Re-validate after hotfix on charger |
| Keyboard/IME | **Device validated (0.9.1)** | Composer above keyboard; mode bar keyed on `imeAnimationTarget` — close animation smooth | — |
| **Chat history** | Build verified (0.9.6) | History sheet: restore/continue/delete; auto-save every reply; New chat archives | Device walkthrough |
| **Background downloads** | Build verified (0.9.5) | WorkManager; survives app exit; Pause/Cancel; reattach on relaunch | Device walkthrough (close app mid-download) |
| Model management | Build verified (0.9.7) | Catalog one-tap install, Load/Unload, **Delete** (active-guarded), storage summary, Keep copy export | — |
| Quick settings ⚙ | Device validated (renders) | Creativity/focus/reply-length/memory; Apply once · Save default · Reset | Exercise memory slider |
| **Tools Dashboard** | **Build verified (7fce77a)** | `ToolsDashboard.kt` — categorized Vision/Interaction/Elevated cards with risk badges + confirmation hint; AutomatorScreen now embeds it | Physical click/type dispatch harness |

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
│   │   └── ui/{LaiApp.kt, MainViewModel.kt, ToolsDashboard.kt, QuickSettingsSheet.kt,
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
│   │               cpp/{native_inference, llama_cpu_backend (KV reuse 64-batch, tracing, thermal atomic),
│   │                    backend_registry, include/lai/backend.h}
│   ├── ocr/      → BanglaOcrService (placeholder engine)
│   └── orchestrator/ → AgentRuntime
├── plugins/api/  → LaiPlugin
├── docs/         → 45+ files: ARCHITECTURE · MODULES · STATUS · ROADMAP · BANGLA_OCR
│                   VENDOR_BACKEND_STRATEGY · PRIVACY_INVARIANTS · adr/ · architecture/
│                   implementation/ · product/ · legal/
│                   device-results/ (7 reports incl. run126 stall + kv-reuse-validated)
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
**KV reuse:** `kv_tokens_` mirrors the cache token-for-token after every successful decode; longest common prefix kept via `llama_memory_seq_rm(0, reused, -1)`; ≥1 prompt token always re-decoded for fresh logits; any exception clears cache + bookkeeping. **Hotfix 2026-08-17: batch 128→64** for SM8735 stall.
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
    // rise only at fully NOMINAL (hysteresis); CRITICAL minimum now 2 (was 1) for SM8735

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

`UiMode { CHAT | SCREEN_READER | AUTOMATOR }` · `RuntimeOperation { NO_MODEL … ERROR }` · `MainUiState` immutable `StateFlow` aggregate — now also `chatSessions`, `chatHistoryVisible`, `downloadingModelId`, `windowedConversationTurns`, `thermalGovernorDetail`. `ChatMessage.id` keys `LazyColumn`. Mode-bar visibility keys on `WindowInsets.imeAnimationTarget` (never the current inset). Diagnostics v1 additionally reports `evaluatedPromptTokens` and `windowedConversationTurns`. **New:** `ToolsDashboard.kt` — categorized Vision/Interaction/Elevated cards with risk badges.

---

## 4. Next Logical Implementation Phase

### Priority 1 — Validate hotfix on device (needs the phone, ~5 minutes) — **IMMEDIATE**

The 179 sec prefill stall (run 28671: 128/334 tokens at 0.71 tok/s, `Stalled at AWAITING_FIRST_TOKEN`) was diagnosed via `logcat -s LAI-llama` + `dumpsys meminfo`. Hotfix `f7a0db0` (batch 64 + min 2 threads) is building as run 127.
1. Plug in charger + Performance mode, force-stop → Load model
2. Chat `hi` → `logcat -s LAI-llama | grep prefill` should show <5 sec, not 179 sec
3. Export diagnostics → expect `performance[0].timeToFirstTokenMs` present, `emptyGenerationCount` 0
4. Re-test warm long generation → `thermal: decode threads 4 -> 2` and `Reduced CPU threads…` notice
→ On green, update `docs/device-results/2026-08-17-redmi-turbo-4-pro-run127.md` and close Priority 1.

### Priority 2 — Printed Bangla OCR CPU baseline ⚠️ blocked on the owner

The single biggest unbuilt feature. Requires the owner's dataset/licence decision (see `docs/BANGLA_OCR.md`). Once decided: integrate the model behind the existing `OcrEngine` contract → structured `OcrResult` JSON feeds the LLM runtime → wire `ocr.current_screen` end-to-end with redaction.

### Priority 3 — Physical tool-dispatch harness (next pure-code session)

The proposal parse path is device-validated; actual `screen.click/type/scroll/global/launch` dispatch via `AccessibilityGateway` is not. Build a foreground-bound, per-step-confirmed harness over the existing `argv` allowlist with loop/time limits and global stop — no auto-run.

### Then, in order

1. **Vulkan qualification** (no licence needed; GGUF works directly) — evaluate before QNN.
2. **Production supply chain** — promote lightweight SBOM to CycloneDX + SLSA provenance, reproducible builds.
3. **QNN/HTP NPU** — licensed QAIRT CI + model conversion; furthest out.

### Process notes for the next session

- **CI is the only compile gate.** `scripts/validate_repo.sh` checks size/architecture/docs but does not compile Kotlin. Push, watch "Compile Kotlin, C++ and APK", then claim. Current head `f7a0db0` is the gate.
- **Auth:** git remote + identity are wiped between sessions — re-add `origin` and `git config user.*`. Tokens must never be pasted into chat, committed, or stored in this file. Revoke any token that has been exposed.
- **Release ritual:** merge to main → CI green → annotated tag `v0.9.x` → tag run builds + signs + publishes the APK automatically. Signing needs no per-release action.
- **Regressions to avoid:** never `trySend` for streamed tokens; never `imePadding()` twice; never gate the bottom bar on the current IME inset; the Stop watchdog must not unload the model; reply budget < full context; `kv_tokens_` must only be appended after a *successful* decode; thread changes only between decodes; **CRITICAL threads never below 2 on SM8735**.

