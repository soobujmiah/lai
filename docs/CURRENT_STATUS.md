# Current Status

**Snapshot:** 2026-08-20 — `main` @ `0f7c8ab` + Adreno OpenCL track (earlier snapshots at `35d90ea`/`9ab9aff`/`17ad75b`)
**Build:** `Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10` — `validate_repo.sh` PASS (~1.35 MB <128 MB), `16` modules, `189` tests, main green at CI **#185**. Working rule: **default CI builds ONLY the signed release APK** (debug only on PR or explicit `build_type=debug/both`).

## Implemented — Device-validated or Build-verified

*   **CPU LLM:** `llama.cpp` mmap GGUF, `Qwen 1.5B Q4_K_M`, **10–20 tok/s prefill / 2.5–15 tok/s decode**, KV-prefix reuse (`evaluatedPromptTokens` 6–15; TTFT ~0.5–1.4 s), streaming, `45s` Stop watchdog, `storage/LAI/models` auto-import (startup, grant-active, manual grant, manual scan — serialized), `install -r` keeps grant.
*   **Vulkan GPU (unsupported on this device):** addr2line of the crash handler backtrace (release-183) shows SIGSEGV at `vkCmdBindPipeline+0x4` in `vulkan.adreno.so` while binding the **MUL_MAT** pipeline (ggml-vulkan.cpp:15635) — the core matmul of every token. No env/patch combination avoids it; llama.cpp pin is current (upstream already routes large matmul tiles away from Qualcomm, so the crash sits in the medium/small tile path itself). **Qualcomm driver bug — Vulkan stays opt-in for a qualified device/driver.**
*   **Adreno OpenCL track (CLOSED on this device — device-policy wall; backend dormant):** the stack itself is proven healthy (OpenCL-Z: platform `QUALCOMM Snapdragon(TM)`, OpenCL 3.0, `Adreno (TM) 825`, FULL_PROFILE) but HyperOS publishes `libOpenCL.so` to **no** app namespace (`/linkerconfig/ld.config.txt` grep empty) — only legacy targetSdk apps bypass that config. The linker trace shows LAI's dlopen refused at the classloader namespace. The backend stays compiled: probe → scheduler evidence → offload will self-activate with zero code change if a future HyperOS build publishes the library. Full chain: [`device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md`](device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md).
*   **CPU KleidiAI headroom (found 2026-08-20):** ChatterUI (newer llama.cpp, KleidiAI enabled) decoded the same Qwen Q4_K_M at ~28 tok/s on this device vs LAI's validated 8–15 tok/s; LAI builds with `GGML_CPU_KLEIDIAI OFF`. The pinned llama.cpp supports it natively (pinned archive v1.24.0 + MD5). One-line flip queued for the next build.
*   **Thermal/Governor:** `ThermalGovernorPolicy` hysteresis, `setDecodeThreadLimit` atomic between `llama_decode`, adaptive `little 7 idle → big 0-3 burst`, batch 32.
*   **Tool/Agent (one-shot):** 15 tools, `ToolInstructionGate`, hash-chained `ToolAuditLedger` (`APP_PRIVATE_HASH_CHAIN_V1`), `ToolsDashboard`, Xiaomi lock guide.
*   **Android:** `AccessibilityAutomationService` (400 nodes, `canTakeScreenshot`), `Shizuku UID 2000` argv allowlist (no raw shell), `Workspace` SAF (depth 4/256/8 GB, SHA streaming).
*   **Model:** Signed `models-v1.json` **rev 5** (`llama-cpu` preferred; `llama-opencl` + `llama-vulkan` fallbacks pending qualification), SHA-256 + resume, GGUF validate, `Keep copy` export, `Delete` guard.
*   **Diagnostics:** `LaiLog` (logcat + file + export; debug `DEBUG` / release `INFO`), `LaiLogRedactor` (11 unit tests), uncaught-crash handler, in-app log/JSON export — see [LOGGING.md](LOGGING.md).
*   **Delivery:** `JDK 17 + API 36 + NDK 27 + CMake 3.22.1`, pinned `llama.cpp ad1de39` + Vulkan-Headers `v1.4.311` + SPIRV-Headers `vulkan-sdk-1.4.357.0`, Actions majors current, R8 `SourceFile/LineNumberTable` + `lai-release-mapping-<run>` artifact, `sbom-*.txt`.

## Scaffold — Compiles, honestly unavailable

*   **Bangla OCR:** `OcrEngine` contracts (`OcrResult` blocks/polygon/confidence), `Placeholder` fails `OcrModelRequiredException` — blocked on dataset/licence.
*   **Linux/Terminal:** No PRoot/QEMU.

## Planned — Not built

QNN/HTP NPU (licensed QAIRT), `core:tokenization` (SentencePiece unigram), `core:rag` (BM25 + Granite 107M 384-dim LiteRT embedder), `features:rag` doc store, `core:pipeline` DAG, full `core:agent` loop (`Plan→Memory→Approve→Execute→Verify`), LiteRT backend, Tesseract 5.5.3 `ben.traineddata` full OCR, cloud/remote hybrid, knowledge graph, STT/TTS, benchmark CycloneDX.

## Evidence States

`AVAILABLE` (loader found) → `SUPPORTED` (model validated) → `ACTIVE` (executing) → `MEASURED` (latency with value) → `UNKNOWN` — unmeasured renders `N/A`. Never claim without log.

## Next Device Test

**No GPU qualification test is currently actionable on this device** — both accelerator paths are closed by external walls, recorded with full evidence: Vulkan = Qualcomm driver bug (`vkCmdBindPipeline` MUL_MAT bind, release-183 addr2line), OpenCL = HyperOS publishes `libOpenCL.so` to no modern-app namespace (2026-08-20 device-facts record). The dormant Vulkan + OpenCL backends remain compiled and will self-activate when the walls move. **Strategy handoff with the complete decision table (incl. the new Vulkan warptile-clamp lever from upstream PR #25735 and the one remaining NPU/NNAPI HAL check): [`device-results/2026-08-20-redmi-turbo-4-pro-gpu-npu-access-audit.md`](device-results/2026-08-20-redmi-turbo-4-pro-gpu-npu-access-audit.md).** Triggers to re-test:

1. **Land the warptile-clamp LAI patch** (dry-run verified against the pin) → one `validated_accelerators=llama-vulkan` qualification build — the only Vulkan lever LAI controls today.
2. **NNAPI HAL check** (`ls /vendor/bin/hw | grep -iE "neural"`) — decides whether the NPU has a sanctioned side door before any QNN engineering is spent.
3. **HyperOS/Xiaomi OTA** → re-grep the linker config; the dormant OpenCL backend self-activates if published.

CPU remains the device-validated shipped default; no build is needed until one of the triggers above happens.

## 2026-08-21 — Phase 2 UI integration status

Completed in this slice:

- Audited current `main` after PR #15 merge (`022b1da`).
- Preserved existing `MainViewModel`, `MainUiState`, runtime, model, workspace, Accessibility/Shizuku, tool approval, chat/history, streaming and cancellation responsibilities.
- Bound Models, Workspace, and Providers screens to real `MainUiState` via presentation adapters instead of placeholder state.
- Added adaptive Material 3 navigation in `LaiApp` using bottom navigation on compact layouts and rail navigation on expanded layouts.
- Added real Models actions for catalog refresh, install recommended/supported model, load, unload, and delete.
- Added real Workspace actions for SAF grant, revoke, refresh, and bounded model scanning while keeping path/file details private.
- Added Provider status UI that documents current local runtime/CPU/GPU/cloud boundaries without claiming fake cloud or GPU support.
- Removed duplicate provider/model/workspace presentation types that would otherwise conflict at compile time.

Validation in this environment:

- `bash scripts/validate_repo.sh` passed.
- `./gradlew` could not be run because the repository does not currently commit a Gradle wrapper.
- A downloaded Gradle 9.5.0 was attempted with a downloaded JDK 17; the sandbox Gradle daemon was killed during dependency instrumentation before tests could execute. This is an environment limitation, not a passing build.
- No physical Android device was available here, so Redmi Turbo 4 Pro / Adreno 825 validation is still pending and must not be claimed.

Remaining:

- Run CI Android build/test/lint with the configured GitHub Actions toolchain.
- Complete physical-device validation, especially CPU fallback, model load/unload, streaming/cancellation, workspace scan, Screen Reader, Automator, and tool approval.
- Continue polishing Chat and Settings once Models + Workspace pass CI/device validation.
