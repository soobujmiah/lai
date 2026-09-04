# LAI Project State

Snapshot date: 2026-09-04 (updated same day): `llama-hexagon`'s first real device PASS is now
**reproducibility-confirmed** by a second independent qualify run with the same class of
unfakeable vendor DSP evidence. The catalog's second model (`q4-k-m`) was found not to be a valid
Hexagon test target at all (`ggml-hexagon` only supports Q4_0/Q4_1/Q8_0/IQ4_NL kernels, not Q4_K) —
see `docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-limit.md`.
The
HTP-skel-targets fix (`4d90f37`) needed three follow-on fixes before it produced a working,
packaged, device-loadable build — unconditional CMake target list breaking non-Hexagon builds
(`5c74806`), a CMake-version floor (`ggml-hexagon/htp` needs >=3.22.2, LAI had 3.22.1) fixed by
pinning CMake 3.31.6 in CI (`a8954cb`/`21791ac`), and an APK-packaging gap fixed via a CMake
`POST_BUILD` copy (`9ef2edd`). With all four HTP skels finally built, packaged, and
independently verified inside `lib/arm64-v8a/` of a downloaded-and-unzipped release APK (CI run
`33812084740`), `scripts/device/lai_adb.sh qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon` on
the Redmi Turbo 4 Pro reached `LOAD_OK` → generation → `DONE`/`READY`, with real Qualcomm
`adsprpc` FastRPC vendor-driver traces confirming `libggml-htp-v73.so` genuinely loaded onto the
DSP (live-queried `HTP0` hwinfo, real op-batching config) — not a no-op handshake. Full evidence
and caveats (single run, not yet repeated; NPU-vs-CPU compute attribution inferred from vendor
traces, not a per-op trace) in
`docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md`.
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Head: **`9ef2edd`** · CI run **`33812084740` green**, release APK device-qualified for
`llama-hexagon` · Hexagon-track commits: `5c74806` (target-list gating), `a8954cb`+`21791ac`
(CMake 3.31.6 pin), `9ef2edd` (HTP skel packaging) · **~192 `@Test` annotations** (187 at the
2026-08-19 snapshot; counted by grep, not re-verified against the CI-reported figure) · 16
Gradle modules

> **Session-close handoff (2026-09-04, updated same day):** reproducibility is now confirmed — a
> second `qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon` run against the same installed build
> reached `LOAD_OK` → generation → `DONE`/`READY` again, with the same class of unfakeable vendor
> FastRPC/DSP evidence as the first PASS. The "try the second catalog model" step is **closed as
> N/A, not a retry target**: `qwen2.5-1.5b-instruct-q4-k-m` was never a valid `llama-hexagon`
> candidate — `ggml-hexagon`'s kernel only accepts Q4_0/Q4_1/Q8_0/IQ4_NL, not Q4_K (documented in
> the catalog's own q4-0 entry), and `q4-k-m`'s `compatibleBackendIds` doesn't list
> `llama-hexagon` at all. Running it anyway surfaced a separate device-testing hazard, not a
> Hexagon bug: MIUI's `MiuiFreeFormGestureController` finished the activity ~29s in (before the
> app's own 45s internal timeout), which `lai_adb.sh` correctly reported as exit 3 ("no terminal
> state at all"). Full evidence and analysis in
> `docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-limit.md`.
> **Next session's first action:** there is no queued Hexagon retry; move to whichever of (a)
> per-op NPU-vs-CPU attribution logging, (b) the `llama-hexagon` catalog-default product decision,
> or (c) the next roadmap item (Bangla OCR / OpenCL retry) the owner prioritizes. If precise
> NPU-vs-CPU compute attribution becomes important, add ggml per-op backend-assignment logging
> rather than inferring from vendor driver traces alone. Whether to move `llama-hexagon` from
> empty-by-default to catalog-preferred/fallback in `core/model` is an open product decision, not
> made yet. Delete the pulled artifact after use per the artifact-hygiene rule
> (`/home/sbj/.claude/projects/-home-sbj/memory/sobuj-engineering-workflow.md`). No local build
> was performed this session — confirmed via `git status` + absence of `build/`/`.gradle`/`.cxx`
> dirs; all APKs came from GitHub Actions. A separate, unrelated finding from this session (not
> yet acted on): the ADB-first device-testing harness's short/no-op operations (`probe`, a
> denied `qualify`) are dominated by fixed ADB process/transport overhead (~1.3-1.5s across 5-6
> serial `adb` invocations per run), while real model-load/qualify runs remain LAI/model-bound —
> flagged as a future engineering task to reduce serial ADB round-trips, not yet started.

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
| **Vulkan (GPU) adapter** | Implemented; **device-validated CRASH — not viable on this driver** | Real `VulkanBackend::open()` with full layer offload (`LLAMA_LOAD_MODE_NONE` for GPU, mmap kept for CPU); IGPU device probe (Adreno 825 is integrated); `GGML_VK_DISABLE_COOPMAT/_2` + `GGML_VK_DISABLE_MMVQ` + (2026-09-02) upstream warptile clamp PR #27726 (`ggml-vulkan-clamp-warptile.patch`) for the Adreno driver's confirmed failing shaders; `std::cerr` → `LAI-llama` logcat + in-app capture of the failing pipeline name; auto **CPU fallback** on accelerator failure/stall (fail-closed, no acceleration claim until device-validated) | **2026-09-03 device evidence (`docs/device-results/2026-09-03-redmi-turbo-4-pro-vulkan-warptile-clamp-crash.md`): the warptile clamp did NOT fix the crash.** `lai-release-292` loaded the model cleanly on `Vulkan0` and completed prefill (93/93 tokens, 11.5 tok/s), then SIGSEGV'd in `vkCmdBindPipeline` (`vulkan.adreno.so`) on the first decode — same crash site as every prior report, just later in the pipeline. Mitigation lever spent; do not stack another speculative Vulkan patch without new upstream evidence. Vulkan stays opt-in/non-default; **NPU/QNN (Phase 3) is now the next acceleration priority**, ahead of the OpenCL track below. Also found: on this crash the Compose chat UI hangs silently on an empty "Stop" bubble with no error surfaced — open UX bug, independent of the backend decision |
| **Adreno OpenCL track (GPU primary path)** | Implemented; **main-thread hang root-caused and fixed in source (2026-09-04), manifest change not yet reapplied/device-revalidated** | llama.cpp's Qualcomm-maintained OpenCL backend compiled into `liblai_runtime.so` (Adreno-optimized kernels embedded); Khronos headers + ICD loader fetched by immutable SHA on CI, ICD loader linked statically (no binaries committed); `OpenCLBackend` probes `GPUOpenCL`; catalog rev 5 declares `llama-opencl` fallback; `model_params.devices` pinned per backend. 2026-09-03: `/vendor/lib64/libOpenCL.so` exists but is only bridged to the vendor sphal namespace, not the app's default namespace — a `<uses-native-library>` manifest fix reached real vendor driver code but hung indefinitely on every launch (45+s, thread state `S`, no crash), reverted (`82949bb`). 2026-09-04: root cause traced to source — `NativeInferenceEngine.capabilities` is a `by lazy` property whose first-ever access (previously `MainViewModel`'s `_state` initializer, evaluated synchronously during `MainActivity`'s `onCreate`) ran the real, uninterruptible native backend-probe chain (`available()` → `initialize_llama_once()` → `dlopen("libOpenCL.so")`) **on the main thread** — confirmed by a working reference implementation (ChatterUI, React Native, dispatches the equivalent native calls off the main thread by construction and does not hang on this same device: `docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`). Fixed: the eager probe now warms up on `Dispatchers.IO` in `MainViewModel.init {}`; two other call sites (`runBackendProbe()`, `exportDiagnostics()`/`buildDiagnosticsReport()`, and `loadModel()`'s pre-load section) that assumed reading `capabilities` was always safe were also moved off `Main.immediate`. Full trace: `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md` | **Not yet done:** reapply the reverted `<uses-native-library android:name="libOpenCL.so">` manifest change and re-run the device qualify loop now that the main-thread hang class is fixed — this is a device-revalidation step, not done as part of this source fix. |
| Backend scheduler | Device validated (CPU) | `InferenceScheduler`: evidence gates — accelerators need `DEVICE_VALIDATED` (granted per build via `-Plai.validatedAccelerators`, default empty = CPU-only); memory/battery/thermal admission; model catalog declares compatible backends (rev 5: `llama-cpu` preferred; `llama-opencl` + `llama-vulkan` fallbacks) | — |
| Rolling context window / Bangla pass / thermal governor | Build verified | `ContextWindowPolicy`, tuned bilingual system prompt + repetition penalty, closed-loop thermal governor (min 2 threads) | Device re-validation |
| Hexagon NPU (HTP) via `ggml-hexagon` | **Device validated, reproducibility-confirmed (2026-09-04)** — `scripts/device/lai_adb.sh qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon` reached `LOAD_OK` → generation → `DONE`/`READY` on the Redmi Turbo 4 Pro; full unfiltered logcat shows real Qualcomm `adsprpc` FastRPC traces loading `libggml-htp-v73.so` onto the DSP, a genuine `HTP0` session with live-queried hwinfo (`threads 4, hvx 4, hmx 1, vtcm 8 MB`) and real op-batching config (`n-ops 1024`) — not a no-op handshake. Single run, not yet repeated; NPU-vs-CPU compute attribution inferred from vendor driver evidence, not a per-op backend-assignment trace — see `docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md` for full evidence and caveats. SM8735's HTP confirmed **v73**. Chronological findings, all 2026-09-03: (1) apparent 4+ min hang was a Kotlin-level startup race in `MainViewModel.loadModel()`, not native/vendor — fixed. (2) Follow-up "industry-wide unfixable platform restriction" conclusion was itself wrong — Local Dream (real third-party app, plain `untrusted_app`, sideloaded) genuinely uses this device's Hexagon NPU via Qualcomm's real QNN SDK, proving NPU access is possible in principle (`docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md`). (3) ChatterUI (same upstream `ggml-hexagon`, real device) got further than LAI: `libcdsprpc.so` app-namespace bridge fix (`<uses-native-library>`) got LAI's own `ggml-hexagon` to a real DSP round-trip (`Hexagon Arch version v73` correctly detected), then failed at `htp_iface_open` with FastRPC `0x80000406` ("dynamic loading failed" on the DSP side) — traced to `ADSP_LIBRARY_PATH` needing a real on-disk directory containing the HTP skel, which LAI's default native-lib packaging never provided; fixed via `jniLibs.useLegacyPackaging` + `configureHexagonAdspPath()` (`docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-session-open-diagnosis.md`). (4) Even with all of the above fixed, `libggml-htp-v73.so` (and v75/v79/v81) were **never produced by any build at all** — confirmed absent across a fully uncached rebuild too, ruling out Gradle build-cache staleness. Root cause found via `--info` Gradle output (CI run `33787760131`): AGP's `externalNativeBuild` CMake integration only builds targets that are link-dependencies of the module's own `.so` output (`lai_runtime`) when `cmake.targets` is unset — the four HTP skels are standalone DSP-side shared objects never linked into `lai_runtime`, so AGP silently dropped them ("not building target htp-v73 because no targets are specified"). Fixed: `runtime/llama/build.gradle.kts` now sets `externalNativeBuild.cmake.targets` explicitly. Full record: `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-htp-skels-not-built-root-cause.md`. (5) That fix (`4d90f37`) was itself unconditional and broke every non-Hexagon build variant ("Unexpected native build target htp-v75") — fixed by gating the four HTP targets on the same `lai.hexagonSdkRoot`/`lai.hexagonToolsRoot` check that already flips `GGML_HEXAGON` (`5c74806`). (6) With targets correctly gated, the Hexagon-enabled build reached real configure and failed on `ggml-hexagon/htp/CMakeLists.txt`'s own `cmake_minimum_required(VERSION 3.22.2)` — LAI's CMake was `3.22.1`. Fixed by pinning a checksum-verified CMake 3.31.6 via `lukka/get-cmake` (SHA-pinned) and `cmake.dir` in a CI-only `local.properties`, with `runtime/llama/build.gradle.kts`'s `externalNativeBuild.cmake.version` matched to `3.31.6` (`a8954cb`, path-derivation bug fixed in `21791ac`) — confirmed via AGP's own `CmakeLocator.kt` source that `cmake.dir` is checked before the SDK-managed package and short-circuits it, so the existing `sdkmanager "cmake;3.22.1"` install is untouched and simply unused. (7) With the build finally succeeding end-to-end, the four skels still weren't reaching the APK: they're standalone DSP-side outputs of `ggml-hexagon`'s own nested `ExternalProject_Add` sub-build (separate toolchain, never linked into `lai_runtime`), landing outside anything AGP's native-lib packaging scans. Fixed via a `POST_BUILD` custom command in `runtime/llama/src/main/cpp/CMakeLists.txt` that copies them into `$<TARGET_FILE_DIR:lai_runtime>` (the same directory `liblai_runtime.so` lands in), ordered with `add_dependencies()`, using only CMake-resolved paths — no hardcoded `.cxx/<hash>`, no Gradle glob, no AGP-internal task-name dependency (`9ef2edd`). Independently verified: downloaded `lai-release-358` from CI run `33812084740` and unzipped it directly — `lib/arm64-v8a/libggml-htp-v7{3,5,9,1}.so` all present, correctly sized, no unrelated `.so` swept in. | **Done:** reproducibility confirmed (second independent PASS, same vendor-evidence class). **Not applicable:** the catalog's only other model (`qwen2.5-1.5b-instruct-q4-k-m`) is not a valid `llama-hexagon` target — `ggml-hexagon` only supports Q4_0/Q4_1/Q8_0/IQ4_NL kernels, not Q4_K, and that model's `compatibleBackendIds` excludes `llama-hexagon`; a real second data point needs a new model in a supported quant, not a retry of this one. **Not yet done:** if precise NPU-vs-CPU op attribution is needed, add ggml per-op backend-assignment logging rather than inferring from vendor driver traces alone; decide (product decision, not made here) whether `llama-hexagon` should move from empty-by-default to catalog-preferred/fallback in `core/model` now that it has two real passing runs. |

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
├── catalog/{catalog-public-key.pem, models-v1.json}      # signed catalog revision 5
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
│   └── model/       # immutable reviewed-model catalog (rev 5 embedded)
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
  (rev 5; `compatibleBackendIds=[llama-cpu, llama-vulkan, llama-opencl]`, preferred `llama-cpu`; fallbacks `llama-opencl`, `llama-vulkan`).
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
- `catalog_publish.yml`: validates + signs `catalog/models-v1.json` (rev 5) → `catalog-v1` assets.

---

## 4. Next Logical Implementation Phase

### 4.1 GPU qualification loop — CLOSED, 2026-09-03 (result: crash confirmed, not fixed)

The device-qualification loop that used to be "the open gate" is done: `lai-release-292`
(warptile-clamp build) was installed and exercised end-to-end on the Redmi Turbo 4 Pro —
model load succeeded on `Vulkan0`, prefill completed, and the first decode step SIGSEGV'd in
`vkCmdBindPipeline` (`vulkan.adreno.so`). Full evidence in
`docs/device-results/2026-09-03-redmi-turbo-4-pro-vulkan-warptile-clamp-crash.md`. Consequences:

- Do not attempt another speculative Vulkan patch without new upstream evidence targeting this
  exact call site — the mitigation lever (warptile clamp, PR #27726) is spent.
- Keep `llama-vulkan` opt-in/non-default; CPU (`llama-cpu`) remains the shipped default.
- **Hexagon NPU was qualified on-device same day; the apparent hang was root-caused and fixed —
  it was never a native/vendor issue.** Full story in 4.2 and
  `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`: a Kotlin-level startup race
  in `MainViewModel.loadModel()` made a clean, fast, honest "no HTP device" rejection look like
  an indefinite hang from outside the process. Fixed; the forced load now fails cleanly in 47 ms
  via `ggml-hexagon`. **Same-day follow-up correction:** that clean failure does not mean NPU is
  unreachable in general — `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md`
  shows a real third-party app (Local Dream) genuinely using this device's Hexagon NPU via
  Qualcomm's actual QNN SDK. `ggml-hexagon`'s specific low-level approach is what fails, not the
  platform. All three non-CPU backends (Vulkan, OpenCL, Hexagon-via-ggml-hexagon) remain
  unviable as currently implemented; `llama-cpu` remains the only device-validated backend, but a
  QNN-based Hexagon path is now a real, evidenced candidate rather than a dead end.
- A "silent hang" UX bug was suspected from the crash screenshot (empty response bubble, "Stop"
  still active) but was **retracted after a live recheck**: `MainViewModel.persistChat()` only
  writes to disk on a definite outcome and drops blank-text messages, so a native SIGSEGV never
  persists the in-flight exchange at all — the screenshot just caught the doomed process's last
  frame. No fix needed here; see the correction section in
  `docs/device-results/2026-09-03-redmi-turbo-4-pro-vulkan-warptile-clamp-crash.md`.
- Once NPU direction is decided (or explicitly deferred), tag `v0.9.8`/`v0.10.0` for a
  production-signed release via the `ANDROID_KEYSTORE_*` secrets (the only path to
  `PRODUCTION_SIGNED=true`).

### 4.2 Next features (in roadmap order, each needs a PR + device evidence)
1. **Bangla OCR real model** — unblock the owner's dataset/licence decision; then wire the
   engine into `BanglaOcrService` (contract + pipeline already scaffolded).
2. **Hexagon NPU (`llama-hexagon`) — device-validated, reproducibility-confirmed, no further
   qualify work queued.** Two independent real-DSP passes now (2026-09-04); the only other catalog
   model is not a valid target for this backend (Q4_K unsupported by `ggml-hexagon`'s kernel — see
   §1.1 and `docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-
   limit.md`). Remaining Hexagon-track work is no longer "get it working" but: per-op NPU-vs-CPU
   attribution logging (if needed) and the empty-by-default-vs-catalog-preferred product decision
   (§1.1). Local Dream already proved real QNN-based NPU access is possible on this device in
   principle (`docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md`) —
   still relevant background, not an active fallback plan since `ggml-hexagon` is now working.
3. **Adreno OpenCL track retry** — unrelated to the Hexagon finding above (that was a genuine
   Kotlin bug, not a shared native-init issue as previously suspected). OpenCL's own hang
   (`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`) has been
   root-caused and fixed at the source level (2026-09-04): the eager backend-capability probe was
   running on the main thread via `MainViewModel`'s `_state` initializer, confirmed by tracing
   `capabilities` (a `by lazy` property) back to its first-access site and cross-checked against
   ChatterUI, a comparable app, which doesn't hang on this device because its equivalent native
   calls run off the main thread by construction
   (`docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`,
   `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md`). **Remaining:** reapply
   the reverted `<uses-native-library android:name="libOpenCL.so">` manifest change and re-run the
   device qualify loop on the Redmi Turbo 4 Pro to confirm OpenCL now loads and generates
   successfully — not yet done, this was a source-only fix.
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
