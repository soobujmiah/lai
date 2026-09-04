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

**The Hexagon work above was the first of four milestones this same day** — after it: (2) the
OpenCL app-launch main-thread hang was root-caused and fixed (`71319ef`), then device-revalidated
with the manifest entry reapplied (`8b72c12`); (3) a model-registry id-matching bug found during
that revalidation was fixed and closed (`ad0b80c`, milestone recorded at §1.5 "Model management");
(4) a source trace + two debug-only diagnostic patches (`b626303` for Vulkan, `2397d3f` for
OpenCL) ruled out Vulkan and narrowed a *separate*, still-open `llama_backend_init()` hang down to
one specific unexplained call, `clGetPlatformIDs()`, inside OpenCL's own backend registration —
see the session-close handoff below and §1.1 "Adreno OpenCL track" for the current,
correctly-hedged state of that investigation. Do not read the Hexagon narrative below as the
day's only or latest event.

Repository: `soobujmiah/lai` · Application ID: `dev.lai.runtime` · Public repo
Target device: Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), Android SDK 36, QTI **SM8735** (Snapdragon 8s Gen 4), arm64-v8a, 8 cores
Head as of session close: **`c72e717`** — `b53c9cd` (documentation audit close) followed by two
cleanup commits removing all three temporary hang-localization diagnostics: `51b2e00` (deletes and
unwires the two ggml-level patches `b626303`/`2397d3f` from `fetch_llama_cpp.sh`'s patch loop) and
`c72e717` (clean `git revert` of the JNI-boundary `LAI-diag` logging, `c93ebda`). Both cleanup
commits are **CI-green**: `51b2e00` → run `33871465548` success (`Source and documentation policy`
+ `Compile Kotlin, C++ and APK` both passed); `c72e717` → run `33872164259`, same two jobs, same
result. Hexagon PASS build: CI run `33812084740` green, release APK device-qualified for
`llama-hexagon` · Hexagon-track commits: `5c74806` (target-list gating), `a8954cb`+`21791ac`
(CMake 3.31.6 pin), `9ef2edd` (HTP skel packaging) · Later same-day commits: `71319ef` (OpenCL
main-thread fix), `8b72c12` (OpenCL manifest re-add), `ad0b80c` (model-id reclassification,
CLOSED milestone), `c93ebda`+`b626303` (Vulkan hang localization), `2397d3f` (OpenCL hang
localization), `b1f64ef`+`b53c9cd` (documentation audit and updates), `51b2e00`+`c72e717`
(diagnostic cleanup, CI-verified) · **~192 `@Test` annotations** (187 at the 2026-08-19 snapshot,
+14 from `ModelReclassificationPolicyTest`; counted by grep, not re-verified against the
CI-reported figure) · 16 Gradle modules

> **Session-close handoff (2026-09-04, end of session — supersedes the earlier same-day handoff
> below the Hexagon section, which only reflected the day's first milestone):** four milestones
> closed or advanced this session, in order:
>
> 1. **Hexagon HTP (`llama-hexagon`) — CLOSED, reproducibility-confirmed.** Two independent real
>    DSP passes; the "second catalog model" retry idea is N/A (`q4-k-m` was never a valid target
>    for `ggml-hexagon`'s kernel). No further action queued. Details: §1.1 and
>    `docs/device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-limit.md`.
> 2. **OpenCL app-launch main-thread hang — CLOSED and device-revalidated.** Root-caused to an
>    eager backend-capability probe on the main thread (`71319ef`), fixed, then confirmed on-device
>    with the manifest entry actually reapplied (`8b72c12`): the app itself no longer hangs at
>    launch or on any model load, regardless of backend. This is real and durable — do not reopen
>    without new evidence contradicting it.
> 3. **Model-registry id reclassification — CLOSED, real-device verified, idempotent.**
>    `ad0b80c`. Full milestone record: §1.5 "Model management" and `CHANGELOG.md`.
> 4. **OpenCL native-call localization — OPEN, narrowed to one call, not explained.** Debug-only
>    diagnostic patches (`b626303` for Vulkan, `2397d3f` for OpenCL) traced the *separate* residual
>    `LOAD_TIMEOUT` (present on every backend, since it happens during eager ggml backend-registry
>    construction, not backend selection) down to: Vulkan's entire registration completes cleanly
>    (~166ms, confirmed twice on two different builds) — **ruled out**; OpenCL's own registration
>    reaches `clGetPlatformIDs()` and that call never returns — **the current, unexplained
>    blocker**. Nothing further is instrumentable from LAI's own source at that exact call (a
>    single opaque call into the vendor ICD loader). *Do not* attribute this to a driver bug,
>    thermal state, contention, or anything else without new evidence — that question is open, not
>    answered. Full chain: `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md`,
>    `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md`,
>    `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`,
>    `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md`.
>
> **Next session's first action, if resuming this specific thread:** decide whether to (a) stop the
> OpenCL localization here — `llama-opencl` is not shipping regardless, gated behind
> `BuildConfig.VALIDATED_ACCELERATORS` (empty by default), so this is a low-priority curiosity, not
> a blocker — or (b) investigate the ICD loader / `<uses-native-library>` manifest-bridge
> interaction directly, since nothing further can be learned from ggml's own source at
> `clGetPlatformIDs()`. **Cleanup item — DONE:** all three debug-only diagnostics from this
> investigation have been removed. The two ggml-level diagnostic patches
> (`ggml-vulkan-instance-init-diag.patch`, `ggml-opencl-reg-diag.patch`) were deleted and unwired
> from `fetch_llama_cpp.sh`'s patch loop; the JNI-boundary `LAI-diag` logging added in `c93ebda`
> (`runtime/llama/src/main/cpp/native_inference.cpp`) was cleanly reverted (`git revert -n c93ebda`,
> no later commits touched that file, clean apply, no `LAI-diag`/`kDiagTag` references remain). All
> were additive logging only — their findings are fully preserved in the device-results docs cited
> above, and all three are still recoverable from git history (`c93ebda`, `b626303`, `2397d3f`) if
> the localization thread is resumed. Both cleanup commits (`51b2e00`, `c72e717`) were pushed and
> are CI-green (runs `33871465548` and `33872164259`, `Source and documentation policy` +
> `Compile Kotlin, C++ and APK` both passed on each). If not resuming this thread, the next roadmap
> item is Bangla OCR (blocked on the owner's dataset/licence decision) — see §4.2.
>
> **Portfolio note:** `soobujmiah.github.io` commit `5f8f2f6` (NPU/Hexagon claim wording updated to
> reflect the reproducible HTP finding above) was pushed this session as `95de6cc` (rebased onto an
> unrelated same-day test-count fix, `648fdd9`, that had landed on the remote first — non-conflicting,
> no content loss; see that repo's own `git log` for detail). SKB (`soobujmiah/skb`) was also
> updated the same session with a dated audit section covering this whole day's LAI work
> (`repositories/lai.md`, commit `9a40eee`) — both are current, no further sync needed.
>
> No local build was performed this session — confirmed via `git status` + absence of
> `build/`/`.gradle`/`.cxx` dirs; all APKs came from GitHub Actions. Delete pulled artifacts after
> use per the artifact-hygiene rule
> (`/home/sbj/.claude/projects/-home-sbj/memory/sobuj-engineering-workflow.md`).

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
| **Adreno OpenCL track (GPU primary path)** | Implemented; **main-thread hang fixed and device-revalidated (2026-09-04) — app no longer hangs, but the OpenCL backend itself still isn't viable on this device** | llama.cpp's Qualcomm-maintained OpenCL backend compiled into `liblai_runtime.so` (Adreno-optimized kernels embedded); Khronos headers + ICD loader fetched by immutable SHA on CI, ICD loader linked statically (no binaries committed); `OpenCLBackend` probes `GPUOpenCL`; catalog rev 5 declares `llama-opencl` fallback; `model_params.devices` pinned per backend. 2026-09-03: `/vendor/lib64/libOpenCL.so` exists but is only bridged to the vendor sphal namespace, not the app's default namespace — a `<uses-native-library>` manifest fix reached real vendor driver code but hung indefinitely on every launch (45+s, thread state `S`, no crash), reverted (`82949bb`). 2026-09-04: root cause traced to source — `NativeInferenceEngine.capabilities` is a `by lazy` property whose first-ever access (previously `MainViewModel`'s `_state` initializer, evaluated synchronously during `MainActivity`'s `onCreate`) ran the real, uninterruptible native backend-probe chain (`available()` → `initialize_llama_once()` → `dlopen("libOpenCL.so")`) **on the main thread** — confirmed by a working reference implementation (ChatterUI: `docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`). Fixed (`71319ef`): the eager probe now warms up on `Dispatchers.IO` in `MainViewModel.init {}`; `runBackendProbe()`, `exportDiagnostics()`/`buildDiagnosticsReport()`, and `loadModel()`'s pre-load section were fixed the same way. **Manifest entry reapplied and device-revalidated same day** (`8b72c12`, CI run `33839471230` with `-Plai.validatedAccelerators=llama-opencl`): the app now launches instantly and stays fully responsive with the exact manifest change that used to hang it completely — confirmed via `probe llama-opencl` and `qualify qwen2.5-1.5b-instruct-q4_k_m llama-opencl` on the Redmi Turbo 4 Pro. The app itself no longer hangs — now cleanly isolated on an orphaned background thread and correctly reported as `LOAD_TIMEOUT` after 45s (confirmed genuinely stuck via unchanged `utime`/`stime` over a 5s sample) instead of freezing. **What actually blocks past that point is under active re-investigation, not settled** — see below. Full trace and evidence: `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md`, `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md`. (Model-registry id-matching bug found *during* this OpenCL revalidation work is tracked at its own canonical location, §1.5 "Model management" — not duplicated here.) **2026-09-04, later same day — native call localization (source trace + diagnostic patch `b626303`, `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`):** `llama_backend_init()` (the call LAI's own logs show as the last thing entered before every hang) is not the trivial library init its name suggests — reading the pinned llama.cpp source (`ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`) shows it triggers ggml's backend-registry singleton construction on the first call in the process, which eagerly registers every compiled backend **in fixed order: Vulkan → OpenCL → Hexagon → CPU** — regardless of which backend was actually requested. A debug-only diagnostic patch instrumented every real Vulkan driver call inside Vulkan's registration (`ggml_vk_instance_init()`: instance version/extension/layer enumeration, `vk::createInstance()`, per-instance dispatcher init, `enumeratePhysicalDevices()`) with ENTER/EXIT logging. On a real device run, **every instrumented Vulkan call completed** (`vk::createInstance()` in ~58ms, full sequence ~166ms, `ggml_vulkan: Found 1 Vulkan devices` printed) — **ruling Vulkan out** as the blocker, correcting the previous entry's implication that OpenCL was already confirmed as the cause. The thread went silent immediately after Vulkan's registration returned. **Update, same day, next instrumentation pass (`2397d3f`, `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md`):** OpenCL's own registration path (`ggml_backend_opencl_reg()` → `ggml_opencl_probe_devices()`) was instrumented the same way. Result: `ENTER clGetPlatformIDs()` logs, then nothing — **no matching EXIT, ever, for the rest of the 45s window.** This is the first native call in the traced OpenCL path without a returning EXIT — a clean localization, reproduced on a fresh build. **Not yet proven:** why this specific call doesn't return; nothing further can be instrumented from LAI's own source at this exact call (it is a single opaque call into the ICD loader), so this is treated as sufficiently localized for now, not a causal explanation. | **Closed as a low-priority backlog item for shipping purposes** (`llama-opencl` is still not `DEVICE_VALIDATED` and still not reachable from any production build, gated behind `BuildConfig.VALIDATED_ACCELERATORS`, empty by default) **but open as a localization investigation, now narrowed to one specific call**: `clGetPlatformIDs()` is the first native call in the traced OpenCL registration path with no matching EXIT. Nothing further can be instrumented from LAI's own source at that exact call — it's a single opaque call into the ICD loader. Do not declare *why* it doesn't return (driver bug, ICD loader misconfiguration, contention, thermal, or anything else) without new evidence beyond this localization — that question is open, not answered. Keep the `libOpenCL.so` manifest entry — no observed downside, and it's what exposed this whole finding. |
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
| Model management | Build verified | catalog one-tap install, Load/Unload/Delete, Keep copy export, background WorkManager downloads, auto-import of `storage/LAI/models/*.gguf` (on startup, grant-active, manual grant, and manual scan — serialized to avoid a 3-way `.part` race). **Milestone — model-id reclassification, CLOSED 2026-09-04 (commit `ad0b80c`), do not reopen absent contradictory evidence:** *problem* — a model imported by an older build, before the reviewed catalog recognized its SHA-256, registered under a filename-derived id (`qwen2.5-1.5b-instruct-q4_k_m`, underscore) instead of the catalog's canonical id (`qwen2.5-1.5b-instruct-q4-k-m`, hyphens) and stayed there permanently; caught because it caused qualify attempts to fail at `MODEL_NOT_FOUND` even though the file and its SHA-256 were correct. *Root cause* — the matching code was already correct for *new* imports (`WorkspacePolicy.classify()`, `MainViewModel.importWorkspaceModels()`); nothing re-checked *already-installed* models against later catalog updates. *Fix* — new pure `ModelReclassificationPolicy` (`core/policy`, no Android/file I/O) + `ModelRepository.reclassify()`, invoked idempotently from `MainViewModel.refreshModels()`: upgrades an installed model's `id` to the canonical id on SHA-256 match, never renames the underlying file (only `id` changes; `fileName`, what actually resolves it, is untouched), never produces a duplicate id (first entry in registry order claims a canonical id; any other entry targeting the same one is left alone), and remaps `activeModelId` in the same state update so a live/loaded model's id can't go stale mid-session. Every persisted reference to model id was traced first — chat history and `SettingsDocumentV1` reference neither; the registry `id` field is the only one; `MainUiState.activeModelId` is in-memory-only. *Tests* — 14 regression tests in `ModelReclassificationPolicyTest` (stale-id reclassification, already-canonical no-ops, idempotency across repeated passes, duplicate-id prevention, legacy-reference remapping). *CI* — green, both the fix commit and its own build verified compilation + tests. *Device verification (Redmi Turbo 4 Pro, v368)* — fresh install with the pre-existing stale registry entry: first launch logged `Reclassified installed model id(s) to canonical catalog id: {qwen2.5-1.5b-instruct-q4_k_m=qwen2.5-1.5b-instruct-q4-k-m}`; second launch logged nothing further (**idempotent, confirmed on real device data**); three subsequent qualify attempts with the canonical id all found the model immediately, zero `MODEL_NOT_FOUND` recurrences. |
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
3. **Adreno OpenCL track — closed as a shipping concern, still open as a localization
   investigation.** The app-launch hang (`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`)
   was root-caused (eager backend-capability probe on the main thread, cross-checked against
   ChatterUI) and fixed at the source level (2026-09-04, `71319ef`), then device-revalidated the
   same day with the manifest entry actually reapplied (`8b72c12`): the app no longer hangs — it
   launches instantly and stays fully responsive. What actually blocks the native load past that
   point is **not yet proven** — a source trace plus a debug-only diagnostic patch (`b626303`,
   `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`) found
   that `llama_backend_init()` eagerly registers every compiled ggml backend in fixed order
   (Vulkan → OpenCL → Hexagon → CPU) on the very first call, and *ruled Vulkan out*: every real
   Vulkan driver call traced (instance creation, device enumeration) completed in ~166ms on-device.
   OpenCL's own registration was instrumented next, same day (`2397d3f`,
   `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md`): `ENTER
   clGetPlatformIDs()` logs, then no matching EXIT for the rest of the 45s window — the first
   native call in the traced OpenCL path that doesn't return. `llama-opencl` remains not viable for
   shipping regardless (gated behind `BuildConfig.VALIDATED_ACCELERATORS`, empty by default) — that
   part is settled — but *why* `clGetPlatformIDs()` doesn't return is not established; nothing
   further is instrumentable from LAI's own source at that call. Full chain: `docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`,
   `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md`,
   `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md`,
   `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`,
   `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md`. Separately,
   a model-registry id-matching bug found *during* this work is fixed and closed — see §1.5 "Model
   management" for that milestone, not duplicated here.
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
