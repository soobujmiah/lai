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

### Phase 2 candidate

- Added immutable-commit llama.cpp acquisition in GitHub Actions without committing upstream source.
- Added real CPU GGUF loading with the pinned `LLAMA_LOAD_MODE_MMAP` API, model chat-template formatting, bounded prompt evaluation and sampling.
- Added cancellable per-token JNI streaming with UTF-8/UTF-16-safe conversion.
- Added explicit model load/unload controls and live Compose chat updates.
- Recorded successful Redmi Turbo 4 Pro Phase 1 physical-device evidence.

### Verified

- GitHub Actions run 31917533925 passed the Phase 1 source policy, toolchain, Kotlin/C++, tests, lint, APK, and artifact pipeline.
- GitHub Actions run 31919286438 passed immutable llama.cpp acquisition, Kotlin tests/lint, full arm64 llama.cpp/ggml/JNI compilation, APK assembly, and artifact upload.
