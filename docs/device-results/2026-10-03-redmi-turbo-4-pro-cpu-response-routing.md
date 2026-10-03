# Redmi Turbo 4 Pro: CPU response correctness and hidden Vulkan scheduling

**Date:** 2026-10-03. **Build tested:** GitHub Actions `lai-release-411`,
app `0.1.411`/versionCode `411`, source `ea3d681`. **Device:** Redmi Turbo 4 Pro
(`onyx`, Snapdragon 8s Gen 4, Adreno 825). The owner installed and operated
the app. An assistant captured scoped LAI logcat and inspected two screenshots
the owner explicitly identified. Screenshots and raw logs remain local and are
not committed because they include private chat content.

## Observed

- The previous OpenCL registration hang was absent. Capability warm-up returned,
  and Q4_K_M loaded on `llama-cpu` in 1410 ms. It generated two 256-token replies.
  The owner showed that the first, to `hey`, was repetitive and unrelated (blog
  fragments, repeated `u` and `[Sub]`).
- The owner started a fresh chat, selected Q4_0, and sent `hi`. Q4_0 loaded on
  `llama-cpu` in 1194 ms. It also generated 256 tokens, but the reply discussed
  unrelated words and companies. Both quantizations fail the basic correctness
  check. A temperature-zero comparison was requested but unavailable in this
  session; no sampling-only conclusion is justified.
- The process stayed alive. Streaming delivered text; neither load nor decode
  failed. These facts confirm execution, not correctness.

## Source and runtime cause found

`LlamaCpuBackend::open()` passed `gpu_layers=0` into `build_llama_session()`.
That set `model_params.n_gpu_layers` but left `model_params.devices` null. The
[pinned llama.cpp `llama_model_params` contract](https://github.com/ggml-org/llama.cpp/blob/ad1de39e0708e3ced9c71bb3c82d93a2c046a73f/include/llama.h#L283-L289)
says null means *all available devices*. The on-device log confirms the effect:

```text
load_tensors: offloaded 0/29 layers to GPU
llama_context: backend_ptrs.size() = 2
sched_reserve: Vulkan0 compute buffer size = 353.46 MiB
sched_reserve: Vulkan_Host compute buffer size = 5.01 MiB
sched_reserve: graph splits = 396 (with bs=256), 1 (with bs=1)
```

Q4_0 repeated the two-backend layout with a 350.08 MiB Vulkan0 compute
buffer. Thus the `llama-cpu` label and zero GPU **weight layers** did not mean
CPU-only computation. This is a confirmed LAI routing defect. It is a strong
candidate for the corrupted replies because Vulkan generation on this Adreno
driver has already failed qualification, but causation of the text corruption
requires the next CPU-only device comparison; this run alone cannot prove it.

## Fix and acceptance

`build_llama_session()` now sets a CPU-only, null-terminated device list when
`gpu_layers=0`. Accelerator paths retain their existing explicit GPU/HTP
selection. The next release must show all of the following on-device:

1. `device: pinned CPU session to 'CPU' only`.
2. A single context backend and CPU compute buffers, with no `Vulkan0` buffer.
3. Q4_K_M loads, streams, and gives a coherent answer to a simple greeting.
4. A second turn preserves context and responds coherently; Q4_0 gets the same
   basic check if installed.

Do not mark the response issue fixed, or advance GPU/NPU qualification, until
these device checks pass. No GitHub artifact was downloaded or installed by the
assistant during this source change.
