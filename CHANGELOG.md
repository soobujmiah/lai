# Changelog

All notable changes are documented here. The project follows semantic versioning after the first production-capable release.

## [Unreleased]

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
