# LAI Project State

Snapshot date: 2026-08-19 (initialization + CI repair + diagnostics + signing rule complete)
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Head: **`e80bd1e`** · CI: **#172 green** (signed release APK + R8 mapping; debug skipped by rule) · **187 unit tests** · 16 Gradle modules

> Handoff snapshot. Source and CI are authoritative; `docs/ROADMAP.md` is the canonical Phase
> 0–14 roadmap and accepted ADRs govern architecture.
>
> **Status vocabulary:** Implemented = source exists · Build verified = CI compiles/tests it ·
> Device validated = observed on physical hardware · Scaffold = honest unavailable boundary ·
> Pending/Planned = not built.

---

## 1. Architecture Progress

### 1.1 LLM Core & native runtime
| Area | Status | Result | Remaining |
|---|---|---|---|
| Inference contract | Build verified | `InferenceEngine` (load / streaming `Flow<InferenceEvent>` / capabilities / context size) + `GenerationConfig` + `GenerationMetrics` (honest `evaluatedPromptTokens`) | — |
| llama.cpp CPU adapter | **Device validated** | `llama_session.{h,cpp}` shared session: KV-prefix reuse (~0.6 s steady TTFT), chat template, cancellation, thermal thread limit, `LAI-llama` µs stall tracing. Decode 2.5–15 tok/s; prefill 10–20 tok/s | — |
| **Vulkan (GPU) adapter** | Implemented; **device qualification pending** | Real `VulkanBackend::open()` with full layer offload (`LLAMA_LOAD_MODE_NONE` for GPU, mmap kept for CPU); IGPU device probe (Adreno 825 is integrated); `GGML_VK_DISABLE_COOPMAT/_2` + **`GGML_VK_DISABLE_MMVQ`** for the Adreno driver's confirmed failing shader `mul_mat_vec_q4_k_f32_f32`; `std::cerr` → `LAI-llama` logcat + in-app capture of the failing pipeline name; auto **CPU fallback** on accelerator failure/stall (fail-closed, no acceleration claim until device-validated) | **Retest GPU with release-172+ on device**; record evidence in `docs/device-results/`; if another shader fails the log names it |
| Backend scheduler | Device validated (CPU) | `InferenceScheduler`: evidence gates — accelerators need `DEVICE_VALIDATED` (granted per build via `-Plai.validatedAccelerators`, default `llama-vulkan`); memory/battery/thermal admission; model catalog declares compatible backends (rev 4: `llama-cpu` preferred, `llama-vulkan` fallback) | — |
| Rolling context window / Bangla pass / thermal governor | Build verified | `ContextWindowPolicy`, tuned bilingual system prompt + repetition penalty, closed-loop thermal governor (min 2 threads) | Device re-validation |
| Qualcomm QNN/HTP (NPU) | Planned — no code | Boundary documented only | Licensed QAIRT CI + model conversion (Phase 3) |

### 1.2 Accessibility Service & Android control
| Area | Status | Result | Remaining |
|---|---|---|---|
| Service lifecycle / snapshot | Device validated | `AccessibilityAutomationService` + `AccessibilityGateway`, flattened tree ≤400 nodes, password text omitted | Rebind tests |
| Actions (click/type/scroll/global/launch) | Build verified | typed `AutomationCommand`s, selectors by viewId/text/content-desc/path | Physical per-action harness |
| One-shot tool proposals + audit | Device validated (parse path) / build verified | bounded JSON parser, `ToolInstructionGate`, hash-chained no-backup audit ledger, replay guard, user confirmation before authority | Device action-dispatch test |
| Shizuku | Device validated | `READY_UID_2000`, dedicated UserService, argv allowlist, no raw shell | Recipe orchestration |

### 1.3 Bangla OCR
| Area | Status | Result | Remaining |
|---|---|---|---|
| OCR contracts | Build verified | `OcrEngine`, bilingual `OcrRequest`, versioned `OcrResult` (blocks, language, confidence, polygon, handwriting) | Preserve schema |
| Screenshot → OCR pipeline | Scaffold | capture → background dispatch → typed unavailable error | Integrate real model |
| Printed / handwritten Bangla OCR | **Pending — placeholder** | `PlaceholderBanglaOcrEngine` fails `OcrModelRequiredException` | ❗ Blocked on owner's dataset/licence decision |

### 1.4 GitHub Actions, delivery & supply chain
| Area | Status | Result |
|---|---|---|
| Source/architecture policy | Ready | `validate_repo.sh` (128 MB, no binaries, token scan) + boundary/catalog/docs validators |
| Android build | **Verified (#172 green)** | JDK 17, API 36, NDK 27, CMake, Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10, pinned llama.cpp `ad1de39`, Vulkan-Headers `v1.4.311`, SPIRV-Headers `vulkan-sdk-1.4.357.0`, `glslc`, hardened apt step |
| **Working rule: signed-only default** | Ready + enforced | `build_type` default `release`; pushes/tags build **only the signed release APK**; debug only on PR or explicit `build_type=debug/both` |
| **Release signing** | Ready | `v*` tags → production keystore from `ANDROID_KEYSTORE_*` secrets (`PRODUCTION_SIGNED=true`); otherwise **deterministic test keystore** from `scripts/ci/generate_test_keystore.py` (committed seed; same cert every run → release APKs install over previous). Stable cert SHA-256 `D3:A6:6C:E0:6B:2A:4A:57:52:DD:0A:F6:86:0B:57:AB:07:70:7A:80:21:0F:2A:E2:C4:38:F2:58:3C:CD:D6:1C` |
| R8 / release diagnostics | Ready | Minification stays on; `-keepattributes SourceFile,LineNumberTable`; `lai-release-mapping-<run>` artifact (90 days) |
| Tests / coverage | 187 tests | JaCoCo ratchets (contracts .15 / policy .55 / scheduler .70 / model .50 / plugins .50) |
| Dependabot | Partially merged | 5 action bumps + okhttp/AGP/Kotlin merged; androidx deferred (needs API 37) |

### 1.5 Product surfaces (UI) & diagnostics
| Area | Status | Result |
|---|---|---|
| Compose three-mode shell | Device validated | Chat, Screen Reader, Automator; Developer Mode hidden |
| Chat + history + quick settings | Device validated / build verified | streaming + Stop watchdog, history persistence, ⚙ sheet (Apply once / Save default / Reset) |
| Model management | Build verified | catalog one-tap install, Load/Unload/Delete, Keep copy export, background WorkManager downloads, auto-import of `storage/LAI/models/*.gguf` (on startup, grant-active, manual grant, and manual scan — serialized to avoid a 3-way `.part` race) |
| **Diagnostic logging** | Ready | `LaiLog` (logcat + app-private file 512 KiB rotation + in-memory tail in the export) with per-build verbosity: debug `DEBUG`, signed release `INFO`; `LaiLogRedactor` (tokens/api keys/passwords/PEM/AWS redacted, 11 unit tests); uncaught-crash handler; in-app "Export diagnostic log" + `logs[]` in the JSON export; `std::cerr` redirect so ggml errors name the shader |

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
├── catalog/{catalog-public-key.pem, models-v1.json}      # signed catalog revision 4
├── app/                                                   # composition root + Compose shell
│   ├── src/main/java/dev/lai/runtime/
│   │   ├── LaiApplication.kt · MainActivity.kt            # logger configure + crash handler
│   │   ├── core/{AppContainer.kt, LaiLog.kt, LaiLogRedactor.kt}
│   │   └── ui/{LaiApp.kt, MainViewModel.kt, ToolsDashboard.kt,
│   │           QuickSettingsSheet.kt, WorkspaceSettingsCoordinator.kt, theme/Theme.kt}
│   ├── src/main/res/{drawable, values, values-bn}         # en/bn string parity
│   ├── src/test/.../core/LaiLogRedactorTest.kt
│   ├── build.gradle.kts · proguard-rules.pro
├── core/
│   ├── contracts/   # pure tool/inference/OCR/model/workspace/diagnostics contracts
│   ├── policy/      # consent, shell, zero-egress, settings, audit policy
│   ├── scheduler/   # thermal governor, memory estimator, backend routing
│   └── model/       # immutable reviewed-model catalog (rev 4 embedded)
├── platform/
│   ├── download/    # sole network owner; WorkManager downloads, import/verify
│   ├── audit/       # app-private no-backup hash-chained JSONL audit
│   ├── device/      # memory/battery/thermal environment
│   ├── accessibility/  # AccessibilityService + gateway + node snapshotter
│   ├── workspace/   # SAF grant, settings persistence, model discovery
│   ├── history/     # chat-history persistence (no-backup)
│   └── shizuku/     # UserService authority; argv allowlist
├── runtime/
│   ├── llama/       # JNI/C++ llama.cpp adapter
│   │   └── src/main/cpp/{llama_session.{h,cpp}, llama_cpu_backend.cpp,
│   │                     vulkan_backend.cpp, backend_registry.cpp, native_inference.cpp}
│   ├── ocr/         # replaceable Bangla OCR seam (honest placeholder)
│   └── orchestrator/# policy-gated tool dispatch (AgentRuntime)
├── plugins/api/     # versioned local-only plugin contract
├── scripts/
│   ├── validate_repo.sh · check_architecture_boundaries.py
│   ├── validate_documentation.py · validate_model_catalog.py
│   └── ci/{fetch_llama_cpp.sh, generate_test_keystore.py}
├── docs/                            # see docs/README.md map (architecture, ADRs, device-results…)
└── docs/LOGGING.md · docs/BUILD_AND_RELEASE.md · docs/DIAGNOSTICS_EXPORT.md
```

---

## 3. API & Module Specifications

### 3.1 LLM / Inference
- `core/contracts …/inference/InferenceEngine.kt`:
  ```kotlin
  interface InferenceEngine : AutoCloseable {
      val capabilities: RuntimeCapabilities          // nativeLibraryLoaded, compiledBackends: Set<BackendDescriptor>
      val contextSize: Int
      suspend fun load(modelPath: String, backend: BackendId? = null): Result<Unit>
      fun generate(conversation: List<ConversationMessage>, config: GenerationConfig = …): Flow<InferenceEvent>
      // InferenceEvent: Token(text) | Completed(metrics: GenerationMetrics?) | Failed(message)
  }
  ```
- Native boundary (`runtime/llama/src/main/cpp/include/lai/backend.h`): `Backend` (`name()`,
  `available()`, `open(model_path, context_size, error) → unique_ptr<BackendSession>`);
  `BackendSession` (`count_tokens`, `generate(conversation, options, on_token, is_cancelled)
  → GenerationResult` with honest `evaluated_prompt_tokens`; `set_thread_limit` thermal hook).
  `create_backends()` registers `cpu` (always) and `vulkan` (when `LAI_HAS_VULKAN`).
- Scheduler (`core/scheduler`): `InferenceScheduler.select(workload, profile)` gates non-CPU
  backends on `CapabilityEvidence.DEVICE_VALIDATED` + model-catalog compatibility + memory/
  battery/thermal admission; `ScheduleDecision(selected, evaluations, reason)`.
- Catalog (`core/model`): `ReviewedModelCatalog.recommendedCpuBaseline` + `embeddedDocument`
  (rev 4; `compatibleBackendIds=[llama-cpu, llama-vulkan]`, preferred `llama-cpu`).
- Build-time GPU enablement: `-Plai.validatedAccelerators` (default `llama-vulkan` in CI) →
  `BuildConfig.VALIDATED_ACCELERATORS` → `MainViewModel` grants `DEVICE_VALIDATED`.

### 3.2 Tools / Agent
- `core/contracts …/agent/ToolModels.kt`:
  `ToolCall(id, name, arguments: JsonObject)` · `ToolResult(success, output: JsonObject,
  error?)` · `ToolDefinition(name, risk: ToolRisk, requiresConfirmation, schema…)`.
- `core/policy …/agent/`: `AgentPolicy.review(tool, authority, userConfirmed)` →
  `Allow | Deny(code, reason)`; `BuiltInToolCatalog.definitions` (screen.*, ocr.current_screen,
  app.launch, system.global_action, shell.operation); `ToolInstructionGate`;
  `ToolAuditLedger` (hash chain, replay guard); `ToolCallParser` (bounded JSON).
- `runtime/orchestrator`: `AgentRuntime.execute(call, userConfirmed)` = validate → policy →
  dispatch to AccessibilityGateway / BanglaOcrService / ElevatedShell; audit decision +
  completion records.
- `platform/audit`: `ToolAuditRepository.snapshot()/recordDecision()/recordCompletion()` —
  content-free records, `auditIntegrityValid`.

### 3.3 UI / ViewModel
- `MainViewModel` (`MainUiState` StateFlow): `sendMessage`, `cancelGeneration` (Stop watchdog),
  `loadModel/unloadModel/deleteModel`, `downloadModel/pause/cancel`, `grantWorkspace/
  revokeWorkspace/scanWorkspaceModels` (auto-import), `setDeveloperMode`, tool proposal
  `approvePendingTool/denyPendingTool`, `exportDiagnostics(uri)`, `exportLogFile(uri)`,
  `maxNewTokensCeiling`, quick-settings `applyQuickSettings/saveDefaultSettings/resetSettings`.
- `MainUiState`: mode, input, messages, operation, activeModelId, schedulerDetail,
  installed/supported models, performanceHistory, toolProposalCounters, toolAuditHistory,
  workspace (grant state + counts), downloadProgress, lastGenerationFailure,
  emptyGenerationCount, developerMode, notice, diagnosticsStatus.
- `LaiApp.kt`: three-mode shell + Settings (Developer Mode → Support diagnostics: JSON export +
  log export via SAF `CreateDocument`).

### 3.4 Diagnostics / Logging
- `app/…/core/LaiLog.kt`: `configure(context, debugBuild)` (once; DEBUG for debug, INFO for
  release), `d/i/w/e(tag, message[, throwable])`, `recentEntries(limit)`, `exportText()`;
  sinks = logcat + app-private `files/logs/lai-{debug|release}.log` (512 KiB rotate) + ring
  (300). Every line passes `LaiLogRedactor.redact` (token/api-key/password/PEM/AWS patterns;
  unit-tested, repo token-scan passes).
- `DiagnosticsReportV1` (contracts): `app`, `device`, `runtime`, `models`, `performance`,
  `privacy` (excludedData), `automation`, `logs: List<LogEntryDiagnostics>` (bounded tail).

### 3.5 Build / Release
- Workflow: `android_build.yml` — `workflow_dispatch.build_type` default `release`; push/tag →
  signed release only; PR → debug; inputs `validated_accelerators` (default `llama-vulkan`);
  `Ensure release signing key` step generates the deterministic test keystore when secrets are
  absent; artifacts `lai-release-<run>` (30 d), `lai-release-mapping-<run>` (90 d), `sbom`,
  `lint`. Tags additionally publish a GitHub Release.
- `catalog_publish.yml`: validates + signs `catalog/models-v1.json` (rev 4) → `catalog-v1` assets.

---

## 4. Next Logical Implementation Phase

### 4.1 Immediate (device-qualification loop for GPU — this is THE open gate)
1. **Install `lai-release-172`+ on the Redmi Turbo 4 Pro** (updates over previous builds now —
   same cert `D3:A6:6C:E0:…`). Confirm the update succeeds in place (the signing fix).
2. **Retest GPU generation** (MMVQ disabled). Expected logcat: `offloaded 28/29 layers to GPU`;
   generation should run on `Vulkan0` (KV cache already lands there).
   - If it **works**: record device evidence in `docs/device-results/2026-08-19-redmi-turbo-4-pro-vulkan.md`
     (decode/prefill tok/s, thermal), mark GPU row Device Validated, and optionally make
     `llama-vulkan` the catalog `preferredBackendId`.
   - If a **different shader fails**: the in-app capture names it (`Generation failed:
     … Compute pipeline creation failed for <shader>`); disable that path or bump the pinned
     llama.cpp to a commit with the Adreno fix.
   - If GPU is fundamentally unstable: keep CPU as the default (`validated_accelerators=cpu` /
     catalog preferred `llama-cpu`) and log the decision.
3. **Tag `v0.9.8`** (or `v0.10.0`) once GPU/CPU is settled → production-signed release via the
   secrets (this is the only path to `PRODUCTION_SIGNED=true`).

### 4.2 Next features (in roadmap order, each needs a PR + device evidence)
1. **Bangla OCR real model** — unblock the owner's dataset/licence decision; then wire the
   engine into `BanglaOcrService` (contract + pipeline already scaffolded).
2. **GPU qualification artifacts** — `llama-vulkan` benchmark + thermal record; Vulkan
   `preferredBackendId` flip after evidence.
3. **QNN/HTP (NPU) Phase 3** — isolated adapter, converted DLC, licensed QAIRT CI. Boundary
   only, no code today.
4. Product backlog from `docs/ROADMAP.md`: autonomous multi-step tool loop (foreground
   binding + loop limits), RAG/STT/TTS plugins, encrypted vector DB — after runtime stability
   is proven.

### 4.3 Engineering hygiene (cheap, do alongside)
- Keep `docs/STATUS.md`/`CURRENT_STATUS.md` in sync (snapshot date, 187 tests, run numbers).
- Re-run `bash scripts/validate_repo.sh` before every push; keep docs-with-code.
- Never paste tokens in chat/repo; rotate `ghp_…` exposed earlier in this conversation.

---

**Definition of "full featured"** (unchanged): production-ready only when every capability has
physical-device evidence — see `docs/STATUS.md` § "Definition of full featured".
