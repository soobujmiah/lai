# AI Runtime

LAI shall provide a **unified, evidence-based, fail-closed AI runtime** for `CPU/GPU/NPU` + `Cloud/Remote`.

## Current

**CPU only:** `runtime:llama` `NativeInferenceEngine` → `JNI` `native_inference` → `llama_cpu_backend` (`llama.cpp` `ad1de39`, `mmap`, `chat template`, `batch 32`, `kv_tokens_` + `llama_memory_seq_rm`, ` little 7 → big 0-3` `sched_setaffinity`, `little` idle) → `BackendId("llama-cpu")` `BackendDescriptor` (`computeClass`, `supportedModelFormats: gguf`, `supportedQuantizations: Q4_K_M`, `preference`). `InferenceScheduler` selects `LLAMA-CPU: Selected using evidence…` vs `4.0 GB` free, `NOMINAL`, `64%` battery. `Vulkan` scaffold `dlopen libvulkan.so` → `Adreno 825` loader found, `GGML_VULKAN=ON` with `SPIRV-Headers` on CI (`0263d30`), `available true` pending, `n_gpu_layers=99` stub.

**No Gateway, no Cloud/Remote yet.**

## Target — Hybrid (per §4)

Local `CPU (llama.cpp)` + `GPU (Vulkan, Adreno 825, GGML_VULKAN)` + `NPU (QNN/HTP, QAIRT DLC)` + Cloud `OpenAI/Anthropic/Gemini/OpenRouter` (OpenAI-compatible) + Remote `Ollama/LAN/desktop` — `AI Gateway` (`registry/router/context/model/tool/permission/usage/audit`) with `embedded provider` → `unified backend/provider orchestration` → `Intelligent model selection` (capability detection) → `Hybrid` routing.

**Routing (like NpuHub `TELEMETRY.md`):** `capability` (from `BackendDescriptor` + `ModelSpec` `compatibleBackendIds`/`preferredBackendId`/`fallbackBackendIds`) + `privacy` (local first) + `latency` (measured `16 tok/s` vs `40 tok/s` Vulkan) + `cost` (cloud `provider` quota) + `model availability` + `task complexity` + `device resources` (`availableMemoryBytes`, `estimatedPeakBytes`) + `network` (`AVAILABLE` vs `UNKNOWN`) + `user preference` + `battery` + `thermal` — all recorded in `SchedulerDecisionTelemetry` (no `requestId/input/modelPath/bytes`), `install -r` keeps `audit`.

## Contracts

`InferenceEngine` (`load`/`generate` `Flow<InferenceEvent>` `Token/Completed/Failed` + `GenerationMetrics` + `evaluatedPromptTokens`/`countTokens` + `close` + `setDecodeThreadLimit`), `Backend` (`name`/`available`/`open`), `BackendSession` (`count_tokens`/`generate`/`set_thread_limit`), `GenerationConfig` (`maxNewTokens`, `temperature`, `topP`, `seed`), `RuntimeCapabilities` (`nativeLibraryLoaded`, `compiledBackends`, `detail`).

## Device Test Mandatory

`CPU 16–22 tok/s` (done `0.1.139`), `Vulkan 40–60 tok/s` on Adreno 825 (next), `QNN DLC` on HTP (licensed). No claim without `MEASURED`.

## Fallback

`CPU` truthful fallback always — `Vulkan`/`QNN` `open` failure returns `CPU` `reason` + `N/A` metrics, never fabricates.
