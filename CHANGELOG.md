# Changelog

All notable changes are documented here. The project follows semantic versioning after the first production-capable release.

## [Unreleased]

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

### v0.7 runtime reliability candidate

- Added multi-turn conversation arrays through Kotlin/JNI/model-native chat templates.
- Added native token counting and oldest-turn trimming against the 4,096-token context.
- Added explicit runtime operation states, Stop and New chat controls.
- Added native model-load, prompt-evaluation, TTFT, decode and total-time measurements; values remain local.
- Added Android memory-pressure model release and download/import storage preflight.
- Recorded Redmi Turbo 4 Pro evidence for Qwen installation, CPU scheduling, memory preflight, nominal thermal state and a 15-token local generation.
- Added explicit Storage Access Framework export of versioned, privacy-filtered diagnostics JSON.
- Added an in-memory rolling history of 20 generation performance samples; no automatic persistence or upload.

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
