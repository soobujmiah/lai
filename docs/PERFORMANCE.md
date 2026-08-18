# Performance & Resource Management

LAI shall be **resource-aware, thermal-aware, and measured** — never claim without log.

## Current — SM8735 (Redmi Turbo 4 Pro, 8 cores, Adreno 825)

*   **CPU (device-validated 0.1.139):** `Qwen 1.5B Q4_K_M` `570 ms` load, **16–22 tok/s prefill** (93 tok in 5.8s, 11 reuse in 0.54s), **8–12 tok/s decode**, `storage/LAI/models` + `install -r`, adaptive `little 7 idle (pin_to_little) → big 0-3 burst (pin_to_big)` (fixes `0.7 tok/s` on HyperOS little-core throttling), batch `32`, prompt `180` chars, hysteresis `ThermalGovernorPolicy` (`NOMINAL/LIGHT→ baseline 4, MODERATE 3, SEVERE 2, CRITICAL 2` min, `45s` watchdog, `fsync` audit).
*   **Memory:** `ModelMemoryEstimator` (`estimatedPeakBytes 1.93 GB` for 1.5B), `InferenceScheduler` preflight (`4.0 GB` free on SM8735 → `LLAMA-CPU` selected), `ContextWindowPolicy` (`keepLastTurns`), `ChatHistoryRepository` (`100` sessions, `512` msgs, atomic), `kv_tokens_` mirroring cache, `llama_memory_seq_rm(0, reused, -1)`.
*   **GPU:** Scaffold `dlopen libvulkan.so` → `Adreno 825` loader found (log 17:39:59), `GGML_VULKAN=ON` with `SPIRV-Headers` on CI (`0263d30`, proper way) — `available true` next, `n_gpu_layers=99` stub. **Device test mandatory** for `40–60 tok/s` prefill.
*   **NPU:** Planned `QNN/HTP` isolated `runtime:qnn` (licensed QAIRT, `GGUF→DLC` + INT4 calibration).

## Target

*   **Scheduling:** `CPU/GPU/NPU` via `InferenceScheduler` (capability/evidence/thermal/battery/memory, `AVAILABLE/SUPPORTED/ACTIVE/MEASURED`), `BackendId` opaque, `DeviceProfile` (`SM8735`, `arm64-v8a`, `8` cores, `Adreno 825`).
*   **Governor:** `PowerManager` thermal callback → `ThermalGovernorPolicy` (hysteresis: fall instantly, rise only at `NOMINAL`) → JNI atomic `setDecodeThreadLimit` between `llama_decode` + `sched_setaffinity` (`little 7` idle, `big 0-3` burst). Plain `Reduced CPU threads…` notice + `thermal: decode threads X→Y` trace.
*   **Memory:** `3B Q4_K_M ~2.8 GB peak` → `2P` on SM8735, `5B` needs NPU; `SQLCipher` RAG `4 GiB` file cap, `256`-file scan cap, `kv_tokens_.clear()` on cancel/error.
*   **Metrics:** `GenerationMetrics` (`promptTokens`, `generatedTokens`, `promptEvaluationMs`, `timeToFirstTokenMs`, `decodeMs`, `totalMs`, `evaluatedPromptTokens`, `promptTokensPerSecond`, `decodeTokensPerSecond`) — `promptTokensPerSecond` divides by `evaluatedPromptTokens`, never inflated total. `MEASURED` only with value, else `N/A`.

## Optimization Layer (not lock)

Snapdragon tuning is **optimization, not architecture lock**: `core` pure JVM, `platform` Android, `runtime` replaceable adapters, `app` composition. Vendor SDKs behind `LAI_HAS_LLAMA_CPP`/`LAI_HAS_VULKAN` guards, never in `core`.

## Testing

`coverageCheck` `169` + `JaCoCo`, `DEVICE TEST REQUIRED` for `MEASURED` (`prefill/decode` on SM8735, `Adreno 825` Vulkan `40–60 tok/s`, `QNN` DLC, `thermal: Reduced CPU threads…`, `big 0-3` pin).
