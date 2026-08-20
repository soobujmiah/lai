# Current Status

**Snapshot:** 2026-08-20 — `main` @ `0f7c8ab` + Adreno OpenCL track (earlier snapshots at `35d90ea`/`9ab9aff`/`17ad75b`)
**Build:** `Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10` — `validate_repo.sh` PASS (~1.35 MB <128 MB), `16` modules, `189` tests, main green at CI **#185**. Working rule: **default CI builds ONLY the signed release APK** (debug only on PR or explicit `build_type=debug/both`).

## Implemented — Device-validated or Build-verified

*   **CPU LLM:** `llama.cpp` mmap GGUF, `Qwen 1.5B Q4_K_M`, **10–20 tok/s prefill / 2.5–15 tok/s decode**, KV-prefix reuse (`evaluatedPromptTokens` 6–15; TTFT ~0.5–1.4 s), streaming, `45s` Stop watchdog, `storage/LAI/models` auto-import (startup, grant-active, manual grant, manual scan — serialized), `install -r` keeps grant.
*   **Vulkan GPU (unsupported on this device):** addr2line of the crash handler backtrace (release-183) shows SIGSEGV at `vkCmdBindPipeline+0x4` in `vulkan.adreno.so` while binding the **MUL_MAT** pipeline (ggml-vulkan.cpp:15635) — the core matmul of every token. No env/patch combination avoids it; llama.cpp pin is current (upstream already routes large matmul tiles away from Qualcomm, so the crash sits in the medium/small tile path itself). **Qualcomm driver bug — Vulkan stays opt-in for a qualified device/driver.**
*   **Adreno OpenCL track (GPU qualification primary path, build-verified):** llama.cpp's Qualcomm-maintained OpenCL backend is now compiled into `liblai_runtime.so` (`GGML_OPENCL` + Adreno-optimized kernels, Khronos headers/ICD-loader fetched by immutable SHA on CI only — no binaries committed). Registered as backend `llama-opencl`, declared in model catalog **rev 5** (`llama-cpu` preferred; `llama-opencl`/`llama-vulkan` fallbacks), gated behind `DEVICE_VALIDATED` like Vulkan (`validated_accelerators=llama-opencl`). `build_llama_session` pins `model_params.devices` per backend (`GPUOpenCL` vs `Vulkan` + CPU) so dual-GPU builds offload deterministically. **Device qualification pending** — first test: workflow_dispatch with `validated_accelerators=llama-opencl`.
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

**OpenCL GPU qualification (the open gate):** trigger Actions → Android build → Run workflow with `validated_accelerators=llama-opencl`, install the signed release artifact, load Qwen, send a message.

Expected evidence when it works: logcat `opencl: dlopen(libOpenCL.so) OK`, `opencl probe: device … name='GPUOpenCL' description='Adreno (TM) 825'`, `device: pinned offload to 'GPUOpenCL'`, `offloaded 28/29 layers to GPU`, decode/prefill tok/s — record them in `docs/device-results/2026-08-20-redmi-turbo-4-pro-opencl.md`, then mark `llama-opencl` Device Validated. The FIRST model load compiles OpenCL programs (expect a slow load, that is not a failure). If generation is garbled or crashes: export diagnostics + `adb logcat -b crash -d`; the Kotlin fallback should have reloaded the model on CPU automatically. Build #188 proved the probe path end-to-end but found zero OpenCL platforms because the Khronos ICD loader's default Android vendor dir is unpopulated on this device; the startup vendor-directory synthesis (see `docs/BUILD_AND_RELEASE.md`) is the fix. Build #190 then logged `opencl: GGML_OPENCL not compiled` — traced to a workspace snapshot regression that silently rolled back `android_build.yml` in `fda2c24` (OpenCL CI step missing), NOT a device problem; the workflow was restored from `e210732`. Permanent capture of the device's OpenCL layout (vendor lib path, public.libraries entry, absent ICD dirs, diagnostic-line dictionary): [`device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md`](device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md).
