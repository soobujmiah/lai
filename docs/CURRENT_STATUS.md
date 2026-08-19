# Current Status

**Snapshot:** 2026-08-19 — `main` @ `35d90ea` (initialization complete; earlier snapshots at `9ab9aff`/`17ad75b`)
**Build:** `Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10` — `validate_repo.sh` PASS (~1.31 MB <128 MB), `16` modules, `187` tests, main green at CI **#173**. Working rule: **default CI builds ONLY the signed release APK** (debug only on PR or explicit `build_type=debug/both`).

## Implemented — Device-validated or Build-verified

*   **CPU LLM:** `llama.cpp` mmap GGUF, `Qwen 1.5B Q4_K_M`, **10–20 tok/s prefill / 2.5–15 tok/s decode**, KV-prefix reuse (`evaluatedPromptTokens` 6–15; TTFT ~0.5–1.4 s), streaming, `45s` Stop watchdog, `storage/LAI/models` auto-import (startup, grant-active, manual grant, manual scan — serialized), `install -r` keeps grant.
*   **Vulkan GPU (opt-in, CPU default):** real `VulkanBackend` (full layer offload, IGPU probe) + LAI patch skipping MMVQ compile (fixed the pipeline error), but the **Adreno 825 driver crashes natively during Vulkan compute** on this llama.cpp revision (process restart on send, 0.1.175). Default `validated_accelerators` is now **empty = CPU-only**; set `llama-vulkan` only on a qualified device.
*   **Thermal/Governor:** `ThermalGovernorPolicy` hysteresis, `setDecodeThreadLimit` atomic between `llama_decode`, adaptive `little 7 idle → big 0-3 burst`, batch 32.
*   **Tool/Agent (one-shot):** 15 tools, `ToolInstructionGate`, hash-chained `ToolAuditLedger` (`APP_PRIVATE_HASH_CHAIN_V1`), `ToolsDashboard`, Xiaomi lock guide.
*   **Android:** `AccessibilityAutomationService` (400 nodes, `canTakeScreenshot`), `Shizuku UID 2000` argv allowlist (no raw shell), `Workspace` SAF (depth 4/256/8 GB, SHA streaming).
*   **Model:** Signed `models-v1.json` **rev 4** (`llama-cpu` preferred, `llama-vulkan` fallback), SHA-256 + resume, GGUF validate, `Keep copy` export, `Delete` guard.
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

Install **`lai-release-176`**+ (default CPU — stable, no crash) → load model → send a message → confirm normal CPU generation. GPU is opt-in only; if you want to pursue it, capture `adb logcat -d` during the crash (look for `F libc`/tombstone naming the native function) so we can decide whether a llama.cpp bump or driver-side fix makes Adreno usable.
