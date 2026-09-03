# LAI Project State

Snapshot date: 2026-09-03 (ADB-first device-testing methodology added; first `llama-hexagon` device qualification: load hangs, does not crash, does not silently fall back)
Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Head: **`2e54778`** · CI run 33758789683 green (signed release APK `versionCode=320`, `-Plai.validatedAccelerators=llama-hexagon`) · **~192 `@Test` annotations** (187 at the 2026-08-19 snapshot; counted by grep this session, not re-verified against the CI-reported figure) · 16 Gradle modules

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
| **Adreno OpenCL track (GPU primary path)** | Implemented; **device-validated HANG, reverted (2026-09-03)** | llama.cpp's Qualcomm-maintained OpenCL backend compiled into `liblai_runtime.so` (Adreno-optimized kernels embedded); Khronos headers + ICD loader fetched by immutable SHA on CI, ICD loader linked statically (no binaries committed); `OpenCLBackend` probes `GPUOpenCL`; catalog rev 5 declares `llama-opencl` fallback; `model_params.devices` pinned per backend. Root cause found: `/vendor/lib64/libOpenCL.so` exists (`same_process_hal_file`, same class as the Adreno EGL/Vulkan libs this app already loads) but is only bridged to the vendor sphal namespace, not the app's default namespace — a `<uses-native-library>` manifest fix was tried and reached real vendor driver code, but hung indefinitely on every launch (confirmed: 45+s, thread state `S`, no crash) instead of failing fast, which broke the CPU guaranteed-fallback principle. Reverted (`82949bb`) | **Not currently viable without a background-thread + timeout-guarded probe** — the hang is inside `initialize_llama_once()`, shared with CPU/Vulkan detection, so this needs careful native-threading design, not a quick patch. Full evidence: `docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md` |
| Backend scheduler | Device validated (CPU) | `InferenceScheduler`: evidence gates — accelerators need `DEVICE_VALIDATED` (granted per build via `-Plai.validatedAccelerators`, default empty = CPU-only); memory/battery/thermal admission; model catalog declares compatible backends (rev 5: `llama-cpu` preferred; `llama-opencl` + `llama-vulkan` fallbacks) | — |
| Rolling context window / Bangla pass / thermal governor | Build verified | `ContextWindowPolicy`, tuned bilingual system prompt + repetition penalty, closed-loop thermal governor (min 2 threads) | Device re-validation |
| Hexagon NPU (HTP) | Build-verified; **device-validated HANG (2026-09-03) — not viable yet, root cause not isolated** | SM8735's HTP confirmed **v73** via on-device evidence. CI extracts the real Hexagon SDK 6.6.0.0 from the public `ghcr.io/snapdragon-toolchain/arm64-android:v0.7` image (opt-in only via `validated_accelerators=llama-hexagon`), wires it into the `runtime:llama` CMake build; `hexagon_backend.cpp` mirrors Vulkan/OpenCL (fail-closed, probes for an "HTP*" ggml device). A Q4_0 catalog variant (`qwen2.5-1.5b-instruct-q4-0`) was added since `ggml-hexagon`'s `MUL_MAT` kernel only accepts Q4_0/Q4_1/Q8_0/IQ4_NL, not Q4_K. **First real device qualification (2026-09-03), using the new ADB-first `MainViewModel.runBackendQualification` + `scripts/device/lai_adb.sh qualify` path**: the load call hangs indefinitely — no crash, no `LAI-model` success/failure line, no `hexagon probe:` device-enumeration log line ever appears, process stays alive/sleeping for 4+ minutes. Same shared-native-init hang class already found for OpenCL that same day. Full record: `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`; scoping doc: `docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md` | Root cause not isolated between two candidates (FastRPC/DSP session open blocking vs. HTP device enumeration itself blocking before any log line). Needs a timeout-guarded probe — the same fix direction already identified for the OpenCL hang, since both share `initialize_llama_once()` — before another qualification attempt. Do not retry this exact path speculatively without new evidence, per the existing Vulkan/OpenCL discipline in this file |

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
- **Hexagon NPU was qualified on-device same day and also failed — a hang, not a crash** (see
  4.2 and `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`): the load call
  never returns, no crash/no fallback, same shared-native-init hang class already found for
  OpenCL. All three non-CPU backends (Vulkan, OpenCL, Hexagon) are now device-tested and none
  is viable as shipped; `llama-cpu` remains the only device-validated backend.
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
2. **`initialize_llama_once()` timeout-guarded probe** — now the shared blocker behind *both*
   remaining accelerator tracks (OpenCL and Hexagon each hang inside this same shared native
   init, not their own backend-specific code). A background-thread + timeout wrapper around
   this call is the actual next acceleration-track prerequisite, ahead of retrying either
   backend individually. See `docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`
   and `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`.
3. **Hexagon NPU (`llama-hexagon`) retry** — blocked on item 2 above. Root cause not yet
   isolated between FastRPC/DSP session-open blocking vs. HTP device enumeration blocking
   before any log line; a timeout-guarded probe would at minimum convert the hang into a clean,
   diagnosable failure. Do not retry the current code path speculatively without new evidence.
4. **Adreno OpenCL track retry** — same prerequisite (item 2). Reverted `<uses-native-library>`
   change is not currently applied; do not reapply without the timeout guard in place first.
5. Product backlog from `docs/ROADMAP.md`: autonomous multi-step tool loop (foreground
   binding + loop limits), RAG/STT/TTS plugins, encrypted vector DB — after runtime stability
   is proven.

### 4.3 Engineering hygiene (cheap, do alongside)
- Keep `docs/STATUS.md`/`CURRENT_STATUS.md` in sync (snapshot date, 187 tests, run numbers).
- Re-run `bash scripts/validate_repo.sh` before every push; keep docs-with-code.
- Never paste tokens in chat/repo; rotate `ghp_…` exposed earlier in this conversation.

---

**Definition of "full featured"** (unchanged): production-ready only when every capability has
physical-device evidence — see `docs/STATUS.md` § "Definition of full featured".
