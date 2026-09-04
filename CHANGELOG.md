# Changelog

All notable changes are documented here. The project follows semantic versioning after the first production-capable release.

## [Unreleased]

### Adreno OpenCL track — the primary GPU qualification path

- **Why:** the Adreno 825 Vulkan driver bug is addr2line-verified (release-183): SIGSEGV at `vkCmdBindPipeline+0x4` in `vulkan.adreno.so` while binding the MUL_MAT pipeline. Every compile-time failure mode was already disabled (coopmat/2, MMVQ incl. the LAI compile-skip patch, f16, integer dot product, async, fusion, flash attention), and upstream llama.cpp already routes large matmul tiles away from Qualcomm — the crash sits in the remaining medium/small tile path and is a driver bug. Vulkan stays opt-in for qualified devices, but GPU qualification itself moves to Qualcomm's own acceleration path.
- **New `opencl` backend** (`runtime/llama/src/main/cpp/opencl_backend.cpp`): wraps llama.cpp's OpenCL backend — maintained with Qualcomm/Codelinaro, Adreno-optimized matmul kernels, full llama graph coverage for Qwen Q4_K_M, same GPU through the mature OpenCL driver stack instead of the crashing Vulkan driver. Registered as `llama-opencl` in `NativeInferenceEngine`; auto CPU fallback now recognizes OpenCL failures too.
- **Source-only policy preserved:** CI fetches `KhronosGroup/OpenCL-Headers` @ `15b536b7` and `KhronosGroup/OpenCL-ICD-Loader` @ `45cdbda4` by immutable SHA into runner temp, builds the ICD loader as a STATIC arm64 library with the NDK, and links it into `liblai_runtime.so` (`find_package(OpenCL)` satisfied via `OpenCL_INCLUDE_DIR`/`OpenCL_LIBRARY` hints; `GGML_OPENCL_EMBED_KERNELS=ON` embeds the kernels). No OpenCL byte is committed; at runtime the ICD loader discovers the vendor Adreno driver through `/vendor/etc/OpenCL/drivers/*.icd` like every Android OpenCL app. Devices without a vendor ICD report the backend unavailable and stay on CPU.
- **Deterministic dual-GPU offload:** `build_llama_session` now pins `llama_model_params.devices` per backend (substring `GPUOpenCL` for OpenCL, `Vulkan` for Vulkan, always + CPU for the remainder), so a single artifact compiling both GPU backends can never offload to the wrong device.
- **Model catalog rev 5:** `qwen2.5-1.5b-instruct-q4-k-m` declares `compatibleBackendIds=[llama-cpu, llama-vulkan, llama-opencl]`, `fallbackBackendIds=[llama-opencl, llama-vulkan]`, `preferredBackendId=llama-cpu` (embedded catalog + signed `catalog/models-v1.json` together; `catalog_publish.yml` re-signs on push). The scheduler evidence gate is unchanged: no accelerator runs without `DEVICE_VALIDATED`, granted per build via `validated_accelerators` (now accepting `llama-opencl`, comma-separable). 2 new scheduler tests cover the gate with OpenCL present.
- **Known first-run cost:** OpenCL program compilation happens at first model load (cl-program cache needs a writable temp dir that Android app contexts do not provide by default) — a slow first load is expected evidence, not a failure.
- **Device qualification pending:** qualification build = Actions → Android build → Run workflow with `validated_accelerators=llama-opencl`; evidence protocol in `docs/BUILD_AND_RELEASE.md` § "GPU enablement — Adreno OpenCL track" and `docs/DEVICE_TESTING.md`.
- **CLOSED 2026-08-20 (device-policy wall, recorded in `docs/device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md`):** the vendor-discovery work was proven correct end-to-end, but the final probe showed HyperOS publishes `libOpenCL.so` to NO modern-app namespace (OpenCL-Z — a legacy 2015 app — sees the full OpenCL 3.0 Adreno 825 stack; LAI's dlopen is refused by the classloader namespace; `/linkerconfig/ld.config.txt` contains no OpenCL entry). The backend stays compiled and dormant: it self-activates if a future HyperOS build exposes the library. GPU acceleration priorities: Vulkan driver-fix watch, then QNN/HTP (bundled QNN runtime bypasses the wall).
- **Android vendor discovery fix (device evidence, build #188):** the first qualification build compiled and linked OpenCL correctly but probed zero platforms on the Redmi Turbo 4 Pro — the Khronos ICD loader's default Android path is only `/system/vendor/Khronos/OpenCL/vendors`, which this device does not populate. `LaiApplication.onCreate` now runs native `prepare_opencl_vendor_dir(filesDir)` before any backend probe: when no system vendor dir has `.icd` files it synthesizes an app-private vendor directory (`libOpenCL.so` via the public-library namespace first, plus absolute `/vendor/lib64` fallbacks) and sets `OCL_ICD_VENDORS` (single-directory semantics, one library per `.icd`, loader skips failures). `available()` now dlopens `libOpenCL.so` directly and logs the linker result, and the probe logs every registered ggml device — any remaining failure is diagnosable from `adb logcat -s LAI-llama` without a rebuild.
- **Reopened and re-investigated, 2026-09-04** (superseding the 2026-08-20 "dormant until a future HyperOS build" verdict above — a same-day fix, not a new HyperOS release, is what reopened it): a `<uses-native-library android:name="libOpenCL.so">` manifest bridge was re-added, exposing a real app-launch hang that root-caused to `MainViewModel`'s eager backend-capability probe running synchronously on the main thread during `MainActivity.onCreate` — fixed (`71319ef`) by warming it up on `Dispatchers.IO`, cross-checked against a working reference (ChatterUI, `docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`). Device-revalidated the same manifest change (`8b72c12`): the app no longer hangs at launch or on model load. What still blocks the native load past that point turned out **not** to be settled by that fix alone — a source trace of the pinned llama.cpp commit plus a debug-only diagnostic patch (`b626303`) found `llama_backend_init()` eagerly registers every compiled ggml backend in fixed order (Vulkan → OpenCL → Hexagon → CPU) on the very first call, and *ruled out Vulkan* specifically as the blocker (every real Vulkan driver call traced completed in ~166ms on-device, `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`). A second diagnostic patch (`2397d3f`) then instrumented OpenCL's own registration path (`ggml_backend_opencl_reg()` → `ggml_opencl_probe_devices()`) the same way: `ENTER clGetPlatformIDs()` logs, then no matching EXIT for the rest of the 45s qualify window — the first native call in the traced OpenCL path that doesn't return (`docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md`). *Why* that specific call doesn't return is not established, and nothing further is instrumentable from LAI's own source at that call (a single opaque call into the ICD loader) — this is where the localization stopped for this session. `llama-opencl` remains not viable for shipping either way (gated behind `BuildConfig.VALIDATED_ACCELERATORS`, empty by default); full chain in `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md` and `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md`. **All three diagnostics from this investigation removed the same day**, once their findings were fully captured in the device-results docs above: `ggml-vulkan-instance-init-diag.patch` and `ggml-opencl-reg-diag.patch` deleted and unwired from `fetch_llama_cpp.sh`'s patch loop; the JNI-boundary `LAI-diag` logging (`c93ebda`, `native_inference.cpp`) cleanly reverted. All were additive logging only (no behavior change either applied or removed), and all remain recoverable from git history (`c93ebda`, `b626303`, `2397d3f`) if the `clGetPlatformIDs()` localization thread is resumed.

### Model-ID reclassification — self-healing catalog id migration (CLOSED, 2026-09-04)

- **Problem:** a model imported by an older LAI build, before the reviewed catalog recognized its SHA-256, registered under a filename-derived id (`qwen2.5-1.5b-instruct-q4_k_m`, underscore) instead of the catalog's canonical id (`qwen2.5-1.5b-instruct-q4-k-m`, hyphens) and stayed there permanently — surfaced because it made qualify/load attempts fail at `MODEL_NOT_FOUND` even though the underlying file and its SHA-256 were correct.
- **Root cause:** the matching logic was already correct for *new* imports (`WorkspacePolicy.classify()`, `MainViewModel.importWorkspaceModels()` in `core/policy`/the app module) — nothing re-checked *already-installed* models against later catalog updates, so a model imported before its catalog entry existed just never got reclassified.
- **Fix (`ad0b80c`):** new pure `ModelReclassificationPolicy` (`core/policy`, zero Android/file I/O) plus `ModelRepository.reclassify()` (`platform/download`), invoked idempotently from `MainViewModel.refreshModels()`. Upgrades an installed model's `id` to the canonical catalog id on SHA-256 match; never renames the underlying file (only `id` changes — `fileName`, what actually resolves the file, is untouched); never produces a duplicate id (first entry in registry order claims a canonical id, any other entry targeting the same one is left alone, never merged or dropped); remaps `activeModelId` in the same state update so a live/loaded model's id can't go stale mid-session. Every persisted reference to model id was traced first: chat history and `SettingsDocumentV1` reference neither; the registry `id` field is the only persisted reference; `MainUiState.activeModelId` is in-memory-only.
- **Tests:** 14 regression tests in `ModelReclassificationPolicyTest` — stale-id reclassification, already-canonical no-ops, idempotency across repeated passes, duplicate-id prevention (two variants), legacy-reference remapping (including the null/no-active-model case).
- **CI:** green — compilation and the full test suite verified on the fix commit.
- **Real-device verification (Redmi Turbo 4 Pro):** fresh install with the pre-existing stale registry entry logged `Reclassified installed model id(s) to canonical catalog id: {qwen2.5-1.5b-instruct-q4_k_m=qwen2.5-1.5b-instruct-q4-k-m}` on first launch; a second launch logged nothing further, confirming idempotency on real device data, not just in unit tests; three subsequent qualify attempts using the canonical id all found the model immediately with zero further `MODEL_NOT_FOUND` occurrences.
- **Status: CLOSED.** Do not reopen this investigation unless new evidence directly contradicts the real-device verification above.

### Closed-loop thermal governor + model management

- `ThermalGovernorPolicy` (`core:scheduler`, 9 tests): live thermal status → decode-thread budget with hysteresis (threads fall immediately at MODERATE/SEVERE/CRITICAL, rise again only at fully NOMINAL, so a status boundary cannot flap speeds). Wired end-to-end: `PowerManager` thermal callback flow in `platform:device` → ViewModel → JNI `setThreadLimit` → atomic consumed by the native decode loop between `llama_decode` calls, never concurrently with one. Intervention reasons surface as plain-language notices.
- Installed models can finally be **deleted** from Settings (guarded: the active model must be unloaded first), and the Installed section shows total on-device model storage.

### Chat history

- New `platform:history` module: `ChatHistoryRepository` persists conversations as one JSON file per session in app-private **no-backup** storage — the only content-bearing store in the app, and it never crosses SAF, network, or diagnostics boundaries. Bounded (≤100 sessions, ≤512 messages each, 32 KiB per message), atomic writes, corrupt-file tolerant, id-validated against path escape. 7 pure-JVM tests.
- Conversations save automatically after every completed or failed reply; "New chat" now archives instead of destroying. The History sheet (bilingual, en/bn 68/68) lists sessions newest-first with delete; tapping one restores it on screen and the chat continues from where it stopped.

### Background model downloads survive app exit (WorkManager)

- `ModelDownloadWorker` (CoroutineWorker) + `ModelDownloadCoordinator` in `platform:download`; the app module observes typed `BackgroundDownloadStatus` contract states and never imports androidx.work. Unique-per-model work, CONNECTED constraint, exponential backoff.
- No foreground service needed by design: `ModelRepository.download` already resumes `.part` staging files with HTTP Range, so a stopped worker loses nothing — it retries from the last flushed byte. Transport errors retry (max 8); policy/integrity failures (size ceilings, SHA-256 mismatch) are final and never loop.
- UI: Pause (keeps the resumable partial; pressing Download continues it), Cancel (also discards the partial via new `ModelRepository.discardPartial`), a bilingual "continues in background" hint (en/bn string parity 65/65), and `adoptBackgroundDownloads()` reattaches the progress UI to a download that kept running while the app was closed.

### Bangla output quality: tuned system prompt + repetition penalty

- Rewrote the native `kSystemPrompt`: explicit bilingual instruction to answer Bangla in short, simple, everyday sentences, never literal translations or unprompted English mixing, and to admit ignorance directly — the failure modes visible in the 0.9.0 field screenshot. Costs ~40 extra prompt tokens once per conversation (KV-prefix reuse absorbs it afterwards).
- Added a mild repetition penalty (1.1 over the last 64 tokens) to the sampling chain, placed after top-p per the pinned header's performance note for the 151k Qwen vocabulary; greedy path untouched. 1.5B-class models loop worst in low-resource languages; this is the standard conservative mitigation.

### Rolling Context Window — `keepLastTurns` is now real

- `ContextWindowPolicy` (`core:policy`, pure JVM, 6 tests incl. Bangla content): keeps the last N completed turns plus the in-flight request, dropping oldest turns from the front so the kept history always starts at a USER message. Applied in `prepareConversation` before token counting; the quick-settings "conversation memory" slider now actually bounds the prompt. Reported honestly as `windowedConversationTurns` in state and diagnostics (default 0), distinct from token-overflow `trimmedConversationTurns`. Interaction with KV-prefix reuse documented: stable prefix (maximal reuse) under the window; bounded re-prefill once it slides.

### KV-cache prefix reuse — flat TTFT across a conversation

- `LlamaCpuSession` now tracks the exact token sequence resident in the KV cache (`kv_tokens_`, updated strictly after each successful `llama_decode`). Each request reuses the longest common prefix between the cache and the new templated prompt, removes only the divergent tail via `llama_memory_seq_rm`, and prefills only the suffix — turn-N TTFT becomes proportional to the new turn, not the whole history (device data showed 6.2 s → 17.0 s growth over six turns at ~28 tok/s prefill). At least one prompt token is always re-decoded for fresh sampler logits; any exception invalidates the cache wholesale so a failed decode can never leave stale bookkeeping.
- Honest metrics for reuse: `GenerationResult`, JNI array (now 7 slots), `GenerationMetrics`, and diagnostics `performance[]` gained `evaluatedPromptTokens`; `promptTokensPerSecond` divides by tokens actually evaluated, never the inflated total. Both new fields default to the no-reuse value so existing tests and old exports stay valid. New contract test covers both semantics.

### Fixed

- **Bottom mode bar shoved off-screen when the keyboard closes** (0.9.0 field report): bar visibility was keyed on `WindowInsets.isImeVisible`, the *current* IME inset, which only reaches zero at the very end of the close animation — the composer had already slid to the bottom, then the bar popped in and displaced the layout for a frame. Visibility now keys on `WindowInsets.imeAnimationTarget`, which flips at animation start in both directions, so the bar rides the animation smoothly. Steady-state behaviour is unchanged.

### Device milestone — first chat replies ever (v0.9.0, 2026-08-17)

- Six completed generations on the Redmi Turbo 4 Pro, English and Bangla, on the production-signed `v0.9.0` build: prefill 25–31 tok/s, decode 15–19 tok/s, model load 721 ms, thermal `NOMINAL`. Root cause of the five failed reports confirmed: prefill genuinely needs ~14 s for a ~407-token prompt, so the former 4 s cancel watchdog aborted every healthy generation. `ToolInstructionGate` held "hi" to 159 prompt tokens and all six proposal examinations were correctly `NOT_TOOL_CALL`. New top priority: TTFT grows linearly with history (6.2 s → 17.0 s in six turns) because each generate re-prefills the whole conversation — KV-prefix reuse is next.

### Prefill cost cut + native stall tracing (P0: chat has never replied on device)

- **First production-signed release `v0.9.0`** (run 88, green): a permanent `lai-release` RSA-4096 PKCS12 keystore was generated, stored only in GitHub Actions secrets (`ANDROID_KEYSTORE_BASE64/_PASSWORD`, `ANDROID_KEY_ALIAS/_PASSWORD`) and in the owner's offline copy. The published `app-release.apk` (8.6 MB, minified + shrunk, V1–V4 signed) was downloaded and its certificate verified byte-for-byte against the keystore (SHA-256 `80:03:8D:3E…7E:8E`). No keystore byte is tracked in the repository.
- **Gated the tool instruction on relevance** (`ToolInstructionGate`, `core:policy`, pure JVM): the tool-proposal instruction is now prepended only when the latest user message plausibly requests an Android action (English word-boundary regex + inflection-tolerant Bangla stems). A plain "hi" previously paid a ~314-token instruction inside a ~407-token prefill — 7–27 s of CPU before the first token on 4 threads; it now carries no tool tokens at all. Recall-biased by design: a false positive costs prefill time only, and authority is unchanged — proposals are still parsed, validated twice, and user-confirmed before dispatch. 4 new gate tests.
- **Compressed `BuiltInToolCatalog.modelInstruction`** from ~314 to roughly half the tokens while keeping every schema, the single-JSON-object envelope rule, the no-self-confirmation rule, and the never-claim-success rule.
- **Added native `LAI-llama` stall tracing** (`llama_cpu_backend.cpp`): µs-precision `__android_log_print` around mutex acquisition (the suspected `COUNTING_TOKENS` stall point), chat-template application, tokenization, per-chunk prefill progress, prefill completion with tok/s, first sampled token, and total generation. Four device reports could not distinguish "prefill is slow" from "prefill is wedged"; the next `adb logcat -s LAI-llama` will name the exact blocking call and elapsed time.
- No build-flag, NDK, or workflow change was required; versioning remains CI-derived (`0.6.<run_number>`).

### Phase 2A vertical slice complete: quick settings, workspace UI and one-request overrides

- Added pure `SettingsSession` / `SettingsSessionPolicy` (`core:policy`) that models the two distinct notions of "current settings": persisted defaults and a one-request quick-sheet override. Validation happens before every transition, so an invalid candidate is rejected with typed issues instead of being silently sanitized.
- Added pure `WorkspaceGrantPort` / `SettingsStorePort` / `ModelDiscoveryPort` (`core:contracts`); `platform:workspace` adapters now implement them, so the composition seam is testable without SAF, Robolectric, or a device. No Android type crosses the port.
- Added `WorkspaceSettingsCoordinator` (`app`): grant state, settings session, save/reset, and coarse discovery counts, depending only on the ports and pure policy. `MainViewModel` is a thin binder over it.
- Chat now honours the effective settings: `sendMessage` consumes any armed override exactly once and maps typed `LlmSettings` onto `GenerationConfig`, clamped to the live native context. Clearing the conversation discards an unspent override. The hard-coded 256-token `GENERATION_CONFIG` is gone.
- Added the Chat ⚙ quick-settings `ModalBottomSheet`: creativity, focus, reply length and conversation memory, in plain bilingual language, with **Apply once** / **Save default** / **Reset**. Slider ranges mirror `SettingsPolicy`, so the sheet cannot compose a document the store would reject. Image/voice/search controls are deliberately not rendered while no real adapter exists.
- Added the Settings workspace card: `ACTION_OPEN_DOCUMENT_TREE` grant/revoke, a status line, and a manual scan showing **coarse counts only** (reviewed / local unreviewed) — never a file name, path, or digest.
- Added 29 pure-JVM tests (11 session-policy, 15 coordinator, 3 codec): session-policy transitions, coordinator behaviour against fake ports (absent workspace → defaults, valid file restores typed values, malformed/oversized falls back without crashing, override never mutates saved defaults, rejected document is never written, failed write keeps previous defaults, save/reset round-trips, revoked grant clears counts), plus truncated-write/unsupported-version/free-text-rejection codec cases.
- No build-flag or NDK change was required; the Qwen 1.5B / llama.cpp APK path is untouched.
- **Build verified**: GitHub Actions run `31975456732` (commit `475f942`) passed policy, coverage ratchets, all module unit tests, lint, the native arm64 build, and debug APK assembly.

### Fixed

- **Reply length could make generation impossible** (`03de2f5`, regression from `c150eb0`): `maxNewTokensCeiling` allowed the reply budget to equal the whole context, making `promptTokens + maxNewTokens <= contextSize` unsatisfiable — every send trimmed the entire conversation and threw, so chat silently never replied. The ceiling now reserves half the context for the prompt, with a regression test. Also declared `windowSoftInputMode=adjustResize` (the missing declaration caused the keyboard to flash the UI over the status bar), reduced the per-token JNI cancellation upcall to one per 8 tokens, and added `lastGenerationFailure`/`emptyGenerationCount` to diagnostics so a silent failure explains itself.

- **Device regression fixes from the 2026-08-17 Redmi Turbo 4 Pro run** (`e4ad398`): halved CPU worker threads so prompt batches stop saturating the little cores and heating the SoC; added a Stop watchdog so a cancel blocked inside a non-suspending JNI call can no longer strand the UI on "Stopping…"; dropped the empty assistant bubble left by a stopped or failed reply; added a `SEVERE`+ thermal admission check to `sendMessage`; fixed the edge-to-edge keyboard inset that pushed the app into the status bar; added a real back arrow plus `BackHandler` for Settings. Analysis in `docs/device-results/2026-08-17-redmi-turbo-4-pro-chat-regression.md`.

- Restored `ModelRepository.PROGRESS_STEP_BYTES` (512 KiB), deleted by `dcc42d2` while the retained-copy export at line 166 still referenced it. `main` had been red since that commit (run `31973297070` failed `:platform:download` compilation with `Unresolved reference 'PROGRESS_STEP_BYTES'`); this was a pre-existing break, not a Phase 2A regression.

### Documentation-first repository audit

- Added the source-verified current-state audit and class/interface inventory; architecture/system/module/AI/agent/security/plugin documents; complete feature matrix; canonical Phase 0–14 roadmap; implementation/testing plans; ADR index; definition of done; development policy; and documentation map.
- Added a section-by-section audit of all 48 PDF directive sections while preserving useful legacy documentation instead of creating empty roadmap placeholders.
- Added `THIRD_PARTY_NOTICES.md`, `THIRD_PARTY_LICENSES.md`, `MODEL_LICENSES.md`, and the legal distribution map; production still requires generated exact resolved notices, SBOM, and provenance.
- Added `scripts/validate_documentation.py` to enforce required substantive files, exact Phase 0–14 roadmap structure, feature-matrix columns/statuses/26 targets, PDF sections 1–48, and local Markdown links.
- Recorded implementation/documentation conflicts, actual module/test evidence, security/privacy boundaries, and explicit MISSING/PARTIAL roadmap capabilities.

### Bounded model stream hardening candidate

- Added a pre-write byte ceiling to model download/import copying: reviewed expected size when known, otherwise currently writable storage minus the 256 MiB reserve, always capped at 8 GiB.
- Rejects oversized declared responses before reading and deceptive/unknown-length streams before writing beyond the ceiling; limit violations remove unsafe staging bytes.
- Validates expected/provider model sizes against the GGUF minimum and hard maximum and discards stale staging files already beyond policy.
- Added `:platform:download` unit coverage for inclusive bounds, overflow rejection, resumed-transfer base accounting, and invalid limits; added the module test task to CI.
- This candidate is not build-verified until the updated CI workflow passes.

### Verified

- GitHub Actions run 31968037878 (commit `34e1281`) passed the fifteen-module source/architecture/catalog policy, pure-JVM coverage ratchets (typed settings + workspace classifier/codec + GGUF magic detector), `:platform:audit`/`:platform:workspace`/`:app` unit tests, lint, the native llama.cpp arm64 build, and debug APK assembly for the Phase 2A settings + workspace foundation.
- Intermediate commits (`7904ce3`, `f4eee3f`, `a362b4d`) failed `:core:policy:compileKotlin`: `SettingsPolicy.validate(document)` called `checkVoice`/`checkSearch` without the required `path` argument, and `WorkspacePolicyTest` missed three settings imports. Fixed in `34e1281`; the local source gate does not compile Kotlin, so these were caught only by CI.

### Phase 2A workspace SAF adapter candidate

- Added the `platform:workspace` Android module: `WorkspaceRepository` (SAF tree grant via `ACTION_OPEN_DOCUMENT_TREE`, persistable read/write permission, canonical child resolution through `DocumentsContract` document IDs, idempotent layout creation), `WorkspaceSettingsStore` (bounded read with fallback-to-defaults, temp-write-then-replace write that verifies the exact v1 schema first), and `WorkspaceDiscovery` (depth/count/size-bounded traversal that streams SHA-256 and classifies via the pure `WorkspacePolicy`).
- A `content://` URI is never translated into a raw path; `MANAGE_EXTERNAL_STORAGE` is never requested. Discovery registers metadata only and never auto-loads inference.
- Added a pure JVM test for the GGUF magic detector.
- Registered the module in the build graph, wired it into `AppContainer`, and added `:platform:workspace:testDebugUnitTest` to the CI test command. No NDK/build-flag changes were needed; the Qwen 1.5B / llama.cpp APK path is unchanged.
- Remaining: the minimal Chat ⚙ settings bottom sheet, `MainViewModel` integration, and physical SAF grant/discovery device tests.

### Phase 2A workspace decision layer candidate

- Added `core:contracts` workspace contracts: `WorkspaceGrantState`, canonical `WorkspaceLayout`, `ModelCandidate`, `DiscoveredModel`, `ModelDiscoveryStatus`, and bounded `DiscoveryLimits`.
- Added `core:policy` `WorkspacePolicy`: a pure discovery classifier that turns SAF-observed files into REVIEWED / LOCAL_UNREVIEWED / REJECTED statuses with size/format/digest validation, case-insensitive SHA-256 deduplication, reviewed-catalog matching, a deterministic path ordering, and a registration count limit. Registration never allocates weights or auto-loads inference.
- Added `core:policy` `WorkspaceSettingsCodec`: bounded encode/decode of the non-secret `settings.json` with a strict maximum byte size and strict JSON; lenient read falls back to embedded defaults, while strict write verification (`verifyForStorage`) requires the exact v1 schema — no unknown fields — so the file can never become a prompt/document/credential dump.
- Added pure-JVM coverage in both modules (catalog match, unknown digest, oversized/unsupported/missing-digest/duplicate rejection, count limit, ordering, codec round trip, absent/oversized/malformed/out-of-range handling, and write-time rejection of unknown fields).
- No new Gradle module, Android/JNI/native code, SDK, NDK, or build flag was required; exercised by the existing `coverageCheck` test step, so the Qwen 1.5B / llama.cpp APK build path is unchanged. The Android SAF workspace adapter (`platform:workspace`) and minimal UI slice remain pending.

### Phase 2A typed settings foundation candidate

- Added `SettingsDocumentV1` to `core:contracts`: versioned, typed, non-secret product settings for LLM, image generation, voice, and search, each with bounded numeric/boolean ranges and embedded defaults.
- Added `SettingsPolicy` to `core:policy`: pure validation, default-merging, sanitization and a deterministic v1 migration seam.
- Preserved the reviewed Qwen 2.5 1.5B product defaults (temperature `0.7`, top-P `0.9`, maximum new tokens `256`, random-seed sentinel) and a context window default of `4096`.
- Validation treats unknown fields as forward-compatible warnings (never silent, never fatal); range, finite-number, and type violations are errors.
- Enforced the context-dependent max-token limit (`maxNewTokens` cannot exceed `maxContextTokens`) in addition to absolute ranges.
- Hardened load behavior: an unsupported or future schema version, malformed JSON, wrong types, or out-of-range values fall back to safe embedded defaults rather than crashing or persisting an unsafe document.
- Privacy invariant preserved by construction: the settings schema has no free-text field, so a document can never absorb a prompt, conversation, document, model output, selector, package name, or credential.
- Added pure-JVM coverage in `core:contracts` and `core:policy` (round trip, defaults, every range boundary, NaN/infinity, unknown fields, malformed input, schema-version handling, precedence/reset, Bangla-Unicode-safe parse).
- No new Gradle module, no Android/JNI/native code, and no new SDK/NDK/build flags were required; the additions are exercised by the existing `coverageCheck` test step, so the Qwen 1.5B/llama.cpp APK build path is unchanged.

### Product architecture documentation update

- Merged the dual Standalone Tools Dashboard / Unified Chat **+ Attach Tools** UX into the existing architecture specification.
- Specified contextual Chat ⚙ quick settings and typed Image Generation, LLM, Voice and Vector Search parameter ranges/defaults.
- Defined the planned SAF-backed `/sdcard/LAI/` workspace, versioned `config/settings.json`, bounded startup restore and SHA-256 GGUF auto-discovery while preserving private runtime copies.
- Expanded the existing model document with a categorized Model Center, foreground-capable background pause/resume downloads and manual/local-unreviewed GGUF handling.
- Documented a future native C++ memory-admitted `TaskGraph` for 3B–5B LLM plus Embedding/Whisper micro-model work without changing the Qwen 1.5B baseline.
- Added roadmap gates for Rolling Context Window, dynamic thermal/battery throttling, expanded Shizuku automation, parallel streaming TTS with Interrupt VAD barge-in, and SQLCipher-encrypted vector storage.

### Added

- Phase 1 Compose application with Chat, Screen Reader, and Automator modes.
- Bangla resource localization and bilingual onboarding.
- Accessibility tree snapshot, click, text, scroll, global action, app launch, and screenshot foundations.
- Shizuku connection/permission state, dedicated UserService, and allowlisted argv-only elevated operations.
- Resumable, hashed Hugging Face GGUF model repository in app-private storage.
- Bangla OCR plugin contract and structured JSON schema.
- C++20/JNI inference session and backend registry scaffold.
- GitHub-only Android/NDK/CMake build, lint, tests, artifacts, and tag release workflow.
- Source-only 128 MB and documentation policy checks.
- Architecture, safety, backend, OCR, automation, build, and device-test documentation.

### Backbone hardening candidate

- Compared LAI with the private NpuHub architecture without copying private source.
- Extracted pure contracts, policy and evidence-aware scheduler modules.
- Isolated download, Accessibility and Shizuku authority into platform modules.
- Isolated llama.cpp, OCR and tool orchestration into runtime modules.
- Added a versioned local-only plugin API.
- Made model SHA-256 mandatory and added a central no-outbound-data policy.
- Added static network/authority/JNI/dependency boundary enforcement.
- Added per-module pure-JVM coverage ratchets.
- Added an immutable reviewed model catalog and one-tap digest-pinned baseline installation.
- Added Android memory/battery/thermal observation and conservative pre-load estimation for scheduler routing.
- Added reviewed local GGUF import through Android's system file picker.
- Superseded the short-lived dual-edition experiment with one local-first app and upgrade path.
- Added an ECDSA-signed web supported-model catalog, encrypted CI signing key, schema validation, verified cache and embedded offline fallback.

### v0.8.0 physical evidence

- Verified **Keep copy** survived uninstall and the reviewed Qwen GGUF was restored and loaded through offline **Import file** with exact size and SHA-256.
- Recorded four multi-turn CPU samples averaging 9,174 ms TTFT, 47.36 prefill tok/s, 20.70 decode tok/s and 10,421 ms total; prompt tokens grew 382→510 with no trimming.
- Confirmed the v0.8.0 diagnostics export contained no prompts, generated text, screen/OCR content, foreground packages, documents, tool arguments/results, typed text, credentials or network identifiers.
- Recorded a negative tool-compliance result: proposal mode was enabled, but Qwen produced no valid recognized proposal and the in-memory audit remained empty.
- Stop/recovery, New chat, forced trimming and sustained thermals were not exercised.

### Privacy-safe proposal diagnostics candidate

- Strengthened the trusted system instruction so explicit single-step Android requests require one bare JSON proposal when a built-in schema can perform the next action.
- Added session-only content-free counters for examined, accepted, rejected and ordinary model responses, last coarse outcome, and rejection-code counts.
- Added Developer Mode visibility and diagnostics export fields without retaining model output, prompts, arguments, selectors or typed text.

### Persistent tool-audit candidate

- Added concrete `platform:audit` ownership for app-private, no-backup, content-free JSONL security events.
- Added pure-JVM SHA-256 call fingerprints, full record hash chaining, schema/sequence/tool-risk/transition verification and corruption tests.
- Fsyncs `USER_APPROVED` before invoking Android authority; an audit write failure blocks execution.
- Rejects a second approval or completion for the exact same call fingerprint across process restarts.
- Records bounded success/failure after execution and best-effort denials without storing arguments, selectors, typed text, packages, model/tool output or screen content.
- Bounds audit parsing to 2 MiB, 4,000 events and 4 KiB per record; unknown fields, partial writes and chain mismatches disable proposals.
- Exposes only the latest 50 content-free projections and integrity state to UI/user-exported diagnostics; fingerprints and record hashes remain private.
- Added ADR 0007 with root-attacker, capacity, keying and future archive/reset limitations.

### One-shot local tool proposal candidate

- Added an opt-in trusted system instruction for eight built-in local Android tools.
- Added a 16 KiB whole-response JSON parser with exact envelope keys, per-tool argument schemas, bounded selectors/text/path depth, package/enumeration checks and allowlisted shell compilation.
- Revalidates every `ToolCall` at `AgentRuntime` dispatch so plugins/manual paths cannot bypass argument schemas.
- Rejects model-authored confirmation, sensitive-input flags, mixed prose/JSON, wrong JSON types, unknown fields/tools and shell argument mismatches.
- Added a trusted Compose review dialog with **Approve once** / **Do not run**; all model proposals, including read-only calls, require review and no autonomous chain runs.
- Added a 50-record in-memory content-free tool audit and privacy-filtered diagnostics fields; arguments, typed text and tool output are excluded.
- Merges trusted Kotlin system instructions into the native model system message and preserves that prefix during context trimming.

### Snapdragon-first vendor-neutral architecture candidate

- Replaced the closed core backend enum with validated, adapter-owned opaque backend IDs and generic descriptors.
- Added a vendor-neutral `DeviceProfile` with Android/SoC/ABI/CPU/environment facts and adapter-reported capabilities.
- Made scheduler acceleration policy depend on compute class, evidence, artifact/backend/ABI compatibility, memory, battery, thermal state and real measurements rather than QNN/Vulkan names.
- Extended signed catalog revision 3 with artifact format, context, compatible/preferred/fallback backend IDs, memory estimate and ABI requirements; older signed revisions cannot replace a newer embedded catalog.
- Namespaced llama backends as `llama-cpu` / future `llama-vulkan` and removed QNN flags/placeholders from `runtime:llama`.
- Added a CI source boundary that rejects vendor SDK terminology in generic inference and scheduler code.
- Documented the dedicated future Qualcomm runtime boundary, model-artifact compatibility, vendor onboarding, test evidence and migration path in ADR 0005.
- Extended privacy-filtered device diagnostics with optional SoC manufacturer/model and CPU core count.

### v0.7 runtime reliability candidate

- Added multi-turn conversation arrays through Kotlin/JNI/model-native chat templates.
- Added native token counting and oldest-turn trimming against the 4,096-token context.
- Added explicit runtime operation states, Stop and New chat controls.
- Added native model-load, prompt-evaluation, TTFT, decode and total-time measurements; values remain local.
- Added Android memory-pressure model release and download/import storage preflight.
- Recorded Redmi Turbo 4 Pro evidence for Qwen installation, CPU scheduling, memory preflight, coherent Bangla output, 578 ms load, 4,533 ms TTFT, 45.76 prefill tok/s and 20.35 decode tok/s.
- Added explicit Storage Access Framework export of versioned, privacy-filtered diagnostics JSON.
- Added an in-memory rolling history of 20 generation performance samples; no automatic persistence or upload.
- Recorded v0.7.1 Diagnostics JSON evidence: four multi-turn samples, 5.58 s average TTFT, 38.60 prefill tok/s and 20.43 decode tok/s with no user content in the export.
- Added **Keep copy**: user-selected SAF export, destination re-open and SHA-256 verification so a GGUF survives app uninstall and can be imported after reinstall.

### Phase 2 candidate

- Added immutable-commit llama.cpp acquisition in GitHub Actions without committing upstream source.
- Added real CPU GGUF loading with the pinned `LLAMA_LOAD_MODE_MMAP` API, model chat-template formatting, bounded prompt evaluation and sampling.
- Added cancellable per-token JNI streaming with UTF-8/UTF-16-safe conversion.
- Added explicit model load/unload controls and live Compose chat updates.
- Recorded successful Redmi Turbo 4 Pro Phase 1 physical-device evidence.

### Verified

- GitHub Actions run 31917533925 passed the Phase 1 source policy, toolchain, Kotlin/C++, tests, lint, APK, and artifact pipeline.
- GitHub Actions run 31919286438 passed immutable llama.cpp acquisition, Kotlin tests/lint, full arm64 llama.cpp/ggml/JNI compilation, APK assembly, and artifact upload.
- GitHub Actions run 31921021303 passed architecture/privacy boundaries, coverage ratchets, all eleven modules, lint, native llama.cpp linkage, APK assembly, and artifact upload.
- GitHub Actions run 31936642189 passed the thirteen-module catalog/device/scheduler graph, memory and routing tests, lint, native llama.cpp, APK assembly, and artifact upload.
- GitHub Actions run 31937804443 verified the superseded dual-edition experiment before product unification.
- GitHub Actions runs 31939187858 and 31939187860 published/verified the signed catalog and built the unified local-first app.
- GitHub Actions run 31941189803 passed v0.7 multi-turn Kotlin/JNI, native metrics/cancellation, memory lifecycle, lint and APK assembly.
- GitHub Actions run 31942344604 passed diagnostics privacy schema/tests, coverage, lint, native runtime and APK assembly.
- GitHub Actions run 31944742526 passed retained-model export/reopen/hash, catalog v2, lint, native runtime and APK assembly.
- GitHub Actions run 31951341085 passed opaque backend/device-profile/catalog-v3 contracts, architecture boundaries, coverage, lint, native runtime and APK assembly.
- Catalog run 31951341094 signed and published revision 3; exact published bytes and detached signature were independently verified.
- GitHub Actions run 31953199936 passed strict tool-call parser/rejection tests, trusted-review UI compilation, diagnostics privacy, coverage, lint, native runtime and APK assembly.
- Release run 31953763295 published v0.8.0 with an 8,355,060-byte temporary/debug-signed APK.
- GitHub Actions run 31956572135 passed persistent-ledger and Android file/reopen/replay/corruption tests, fourteen-module boundaries, coverage, lint, native runtime and APK assembly.
- Release run 31957058631 published v0.8.1 with an 8,371,584-byte temporary/debug-signed APK.
- GitHub Actions run 31958557120 passed proposal outcome telemetry/privacy tests, strengthened prompt integration, persistent audit tests, coverage, lint, native runtime and APK assembly.
- Release run 31958838061 published v0.8.2 with an 8,371,584-byte temporary/debug-signed APK.
