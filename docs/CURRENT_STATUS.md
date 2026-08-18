# Current Status

**Snapshot:** 2026-08-18 — `main` @ `17ad75b` (workspace auto-import) + `0263d30` (proper Vulkan)  
**Build:** `Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10` — `validate_repo.sh` PASS (937 KB <128 MB), `16` modules, `169` tests, `NOMINAL` on SM8735.

## Implemented — Device-validated or Build-verified

*   **CPU LLM:** `llama.cpp` mmap GGUF, `Qwen 1.5B Q4_K_M` 570 ms load, **16–22 tok/s prefill / 8–12 tok/s decode** (0.1.139), KV reuse `25×`, streaming `callbackFlow`, `45s` watchdog, `storage/LAI/models` auto-import, `install -r` keeps grant.
*   **Thermal/Governor:** `ThermalGovernorPolicy` hysteresis (`NOMINAL→MODERATE→SEVERE→CRITICAL`, `1→2` threads min), `setDecodeThreadLimit` atomic between `llama_decode`, adaptive `little 7 idle → big 0-3 burst` (`16bf06b`/`c4732fe`), batch `32`, prompt `180` chars.
*   **Tool/Agent (one-shot):** 15 tools, `ToolInstructionGate`, hash-chained `ToolAuditLedger` (`APP_PRIVATE_HASH_CHAIN_V1`), `ToolsDashboard`, Xiaomi lock guide (`Lock + No restrictions + Autostart`).
*   **Android:** `AccessibilityAutomationService` (400 nodes, `canTakeScreenshot`), `Shizuku UID 2000` argv allowlist (no raw shell), `Workspace` SAF `storage/LAI` (depth 4/256/8 GB, SHA streaming).
*   **Model:** Signed `models-v1.json` rev3, SHA-256 + resume, GGUF validate, `Keep copy` export, `Delete` guard.
*   **Delivery:** `JDK 17 + API 36 + NDK 27 + CMake 3.22.1`, pinned `llama.cpp ad1de39`, `gradle/actions` majors current, `ProGuard` keep for `WorkDatabase_Impl` (`a6f2ab0`), lightweight `sbom-*.txt`.

## Scaffold — Compiles, honestly unavailable

*   **Vulkan GPU:** `dlopen libvulkan.so` probe → `Adreno 825` loader found (log 17:39:59), `GGML_VULKAN=ON` with `SPIRV-Headers` on CI (`0263d30`), `available true` pending, `open n_gpu_layers=99` stub — **device test mandatory** for 93 tok prefill on Adreno.
*   **Bangla OCR:** `OcrEngine` contracts (`OcrResult` blocks/polygon/confidence), `Placeholder` fails `OcrModelRequiredException` — blocked on dataset/licence.
*   **Linux/Terminal:** No PRoot/QEMU.

## Planned — Not built

GPU real `generate()`, QNN/HTP (licensed QAIRT), `core:tokenization` (SentencePiece unigram), `core:rag` (BM25 + Granite 107M 384-dim LiteRT embedder), `features:rag` doc store, `core:pipeline` DAG, full `core:agent` loop (`Plan→Memory→Approve→Execute→Verify`), LiteRT backend, Tesseract 5.5.3 `ben.traineddata` full OCR, cloud/remote hybrid (`OpenAI/Anthropic/Gemini/Ollama`), knowledge graph, STT/TTS, benchmark CycloneDX.

## Evidence States

`AVAILABLE` (loader found) → `SUPPORTED` (model validated) → `ACTIVE` (executing) → `MEASURED` (latency with value) → `UNKNOWN` — unmeasured renders `N/A`. Never claim without log.

## Resource Constraints (AI Arena)

`~128 MB` workspace, `2–4` cores, no final device — use for `inspect/docs/code/static checks`, not for downloading 1.1 GB models, large builds, or `install` without `-r`. Real-device tests (Vulkan, QNN, thermal, OCR quality) are `DEVICE TEST REQUIRED`.

## Next Device Test

`0.1.139`-level `hi` (93 tok) + `11 tok` reuse on SM8735 after `install -r` — expect `big 0-3` pin + `20 tok/s`, `little 7` idle, `vulkan: available true`.
