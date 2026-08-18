# Benchmarking

LAI shall be **measured, not claimed** — `MEASURED` only with value, else `N/A`.

## Current

*   **Metrics:** `GenerationMetrics` (`promptTokens`, `generatedTokens`, `promptEvaluationMs`, `timeToFirstTokenMs`, `decodeMs`, `totalMs`, `promptTokensPerSecond`, `decodeTokensPerSecond`, `evaluatedPromptTokens`) — `promptTokensPerSecond` divides by `evaluatedPromptTokens` (reused-aware), not inflated total. `DeviceDiagnostics` (`availableMemoryBytes`, `batteryPercent`, `charging`, `thermalState`, `socModel SM8735`, `cpuCoreCount 8`, `supportedAbis arm64-v8a`).
*   **History:** `performanceHistory` (`≤20` samples) + `lastGenerationMetrics` in `MainUiState` + `DiagnosticsReportV1` (`performance[]`), plus `logcat -s LAI-llama` µs logs (`mutex wait`, `template`, `tokenize`, `reusing X/Y`, `prefill X/Y at us`, `prefill done X new in us (tok/s)`, `first token at us`, `thermal: decode threads`, `core: pinned to big 0-3 / little 7`).
*   **Evidence validated:** `0.1.139` SM8735 `Qwen 1.5B Q4_K_M` `570 ms` load, **16–22 tok/s prefill** (`93 in 5.8s`, `11 reuse in 0.54s`), **8–12 tok/s decode**, `sched_reserve` `151 MiB` `958` nodes `35 ms`, `28 tok/s` before `32`-batch fix.
*   **SBOM:** Lightweight `app/build/sbom/sbom-*.txt` (`releaseRuntimeClasspath` + `dependencies`) → `CycloneDX` future.

## Target (like NpuHub `benchmark`)

*   **Export:** Deterministic `JSON/Markdown/CSV` of `SchedulerDecisionTelemetry` + `GenerationMetrics` + `DeviceDiagnostics` (no `prompts/modelPath/bytes`), via SAF `CreateDocument` on explicit tap, `prettyPrint true`, `install -r` keeps `tool_audit.jsonl` but not export.
*   **Categories:** `perf` (prefill/decode/total, `evaluatedPromptTokens`), `memory` (`estimatedPeakBytes` vs `availableMemoryBytes`, `kv_tokens_` size), `thermal` (`NOMINAL→CRITICAL`, `pin_to_little` idle), `battery` (`39%→97%` on charger), `model` (`1,117,320,736` bytes, `Q4_K_M`), `backend` (`llama-cpu` vs `vulkan` `40–60 tok/s` vs `qnn`), `OCR` (`CER/WER` on Bangla printed/handwritten set), `RAG` (`BM25` lexical vs `384-dim` dense), `agent` (tool loop `MEASURED`).

## Rules

`MEASURED` requires `value`; missing = `N/A`. Never turn `AVAILABLE` (`libvulkan.so` found) into `SUPPORTED` (`n_gpu_layers=99` loaded) or `MEASURED` (`40 tok/s`). LiteRT `x3` fixture is diagnostic, not production-model benchmark.

## Device Test Mandatory

SM8735 `Adreno 825` Vulkan `40–60 tok/s` prefill, QNN DLC on HTP, `thermal: Reduced CPU threads…` notice, `core: pinned` log, `storage/LAI/models` auto-import `1.1 GB` discovery, `install -r` grant persistence.
