# Redmi Turbo 4 Pro — Vulkan instance-init diagnostic: Vulkan ruled out as the `llama_backend_init()` blocker

**Date:** 2026-09-04
**Device:** Redmi Turbo 4 Pro (`25053RT47C`), Snapdragon 8s Gen 4 (SM8735), Adreno 825, arm64-v8a.
**Pinned llama.cpp commit:** `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`.
**Diagnostic patch commit:** `b626303` (`scripts/ci/ggml-vulkan-instance-init-diag.patch`, applied by `fetch_llama_cpp.sh` alongside the existing two Vulkan patches — temporary, debug-only).
**Build:** CI run `33858477529` (`lai-release-372`, `-Plai.validatedAccelerators=llama-cpu`).
**Follow-up to:** `docs/device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md` (where the `llama_backend_init()` hang was first localized) and the source-level trace that found `llama_backend_init()` internally triggers ggml's backend-registry construction (Vulkan → OpenCL → Hexagon → CPU, in that order, on the very first call in the process).

## Why this run happened

Four consecutive `qualify ... llama-cpu` attempts (documented in the OpenCL revalidation doc and its follow-ups) all hung at the same `LAI-diag: init_once: calling llama_backend_init()` log line, never returning. Reading the pinned llama.cpp source showed `llama_backend_init()` is not the trivial call its name suggests — it calls `ggml_backend_reg_count()`, whose first-ever invocation lazily constructs ggml's backend registry singleton, and that constructor eagerly registers every compiled backend in source order: Vulkan first (`ggml_backend_vk_reg()` → `ggml_vk_instance_init()`), then OpenCL, then Hexagon, then CPU. This happens regardless of which backend a caller actually asked for. This diagnostic run instruments the Vulkan step specifically, to determine whether *that* step is the blocker before assuming it is.

## What was instrumented

`ggml_vk_instance_init()` (`ggml/src/ggml-vulkan/ggml-vulkan.cpp`), every real driver-touching call in the function, each with a distinct `ENTER`/`EXIT` `GGML_LOG_INFO` line (routes through the same `ggml_log_internal` → LAI's `llama_log_set()` callback → `LAI-llama` logcat tag already proven to deliver `ggml_vulkan: Found N Vulkan devices` reliably):

1. `ggml_vk_default_dispatcher_instance.init(vkGetInstanceProcAddr)`
2. `vk::enumerateInstanceVersion()`
3. `vk::enumerateInstanceExtensionProperties()`
4. `ggml_vk_instance_layer_settings_available()` (layer enumeration)
5. `vk::createInstance(...)`
6. `VULKAN_HPP_DEFAULT_DISPATCHER.init(vk_instance.instance)` (per-instance dispatcher load, sits between 5 and 7)
7. `vk_instance.instance.enumeratePhysicalDevices()`

No behavior, threading, timing, synchronization, or backend-selection change — purely additive logging on top of the pinned commit, confirmed to apply cleanly and compile on CI.

## VERIFIED

- Every one of the seven instrumented calls completed — each has a matching `ENTER` and `EXIT` line.
- `vk::createInstance()` completed in ~58 ms (the longest single step; everything else was sub-millisecond to a few milliseconds).
- `instance.enumeratePhysicalDevices()` completed, `count=1`.
- Exactly one Adreno 825 device was found, and `ggml_vulkan: Found 1 Vulkan devices:` / `0 = Adreno (TM) 825 ...` printed, exactly as in every prior successful run this project has recorded.
- The entire traced sequence, start to finish, took ~166 ms (15:42:04.489 → .655 on-device wall clock).
- **Vulkan instance creation and physical-device enumeration are not the blocker.** This directly confirms, with the finest granularity attempted so far, the conclusion `docs/device-results/2026-09-03-redmi-turbo-4-pro-vulkan-warptile-clamp-crash.md` implied at a coarser level: Vulkan device access itself works on this hardware. (That earlier doc's crash — `vkCmdBindPipeline` — is a different, later, compute-time call; unaffected by and unrelated to today's finding.)

## CURRENT BLOCKER

- The qualify run still hit `LOAD_TIMEOUT` after the full 45 s budget, exit code 4 — the underlying process-wide hang is unresolved.
- The instrumented thread (tid 17641) produced zero further log output of any kind immediately after `ggml_vulkan: Found 1 Vulkan devices`, for the remainder of the 45 s window.
- Per the traced registry-construction order, the next call after Vulkan's registration returns is `ggml_backend_opencl_reg()` (OpenCL's own registration) — not yet instrumented.

## NOT YET PROVEN

- That OpenCL registration is the actual root cause. It is the next unlogged call in the traced sequence — a localization boundary, not a confirmed cause.
- Which exact native call inside OpenCL registration (if that is indeed where execution goes next) blocks.
- Any driver-specific, thermal, or contention-based causal explanation for the underlying hang. None of these were tested or ruled in by this run.
- Whether removing or altering the `<uses-native-library android:name="libOpenCL.so">` manifest entry (present since commit `8b72c12`, active in every build tested since) would change this outcome — noted as a plausible next experiment, not run here.

## Process/crash/ANR

- Process remained alive throughout and after the timeout (confirmed via `pidof` post-run).
- No tombstone generated in this timeframe (most recent on-device tombstone predates this session by weeks).
- No ANR trace generated (consistent with the main/UI thread never being blocked — only the background registration thread).

## Raw evidence

```
15:42:04.589 D LAI-llama: lai-diag: ENTER dispatcher.init(vkGetInstanceProcAddr)
15:42:04.589 D LAI-llama: lai-diag: EXIT dispatcher.init(vkGetInstanceProcAddr)
15:42:04.589 D LAI-llama: lai-diag: ENTER vk::enumerateInstanceVersion()
15:42:04.589 D LAI-llama: lai-diag: EXIT vk::enumerateInstanceVersion() api_version=4210688
15:42:04.589 D LAI-llama: lai-diag: ENTER vk::enumerateInstanceExtensionProperties()
15:42:04.596 D LAI-llama: lai-diag: EXIT vk::enumerateInstanceExtensionProperties() count=14
15:42:04.596 D LAI-llama: lai-diag: ENTER ggml_vk_instance_layer_settings_available()
15:42:04.596 D LAI-llama: lai-diag: EXIT ggml_vk_instance_layer_settings_available() result=0
15:42:04.596 D LAI-llama: lai-diag: ENTER vk::createInstance()
15:42:04.654 D LAI-llama: lai-diag: EXIT vk::createInstance()
15:42:04.654 D LAI-llama: lai-diag: ENTER per-instance DEFAULT_DISPATCHER.init()
15:42:04.654 D LAI-llama: lai-diag: EXIT per-instance DEFAULT_DISPATCHER.init()
15:42:04.654 D LAI-llama: lai-diag: ENTER instance.enumeratePhysicalDevices()
15:42:04.654 D LAI-llama: lai-diag: EXIT instance.enumeratePhysicalDevices() count=1
15:42:04.654 D LAI-llama: ggml_vulkan: Found 1 Vulkan devices:
15:42:04.655 D LAI-llama: ggml_vulkan: 0 = Adreno (TM) 825 (Qualcomm Technologies Inc. Adreno Vulkan Driver) | uma: 1 | fp16: 0 | ...
[ 45,000 ms of silence on this thread ]
15:42:49.669 E LAI-qualify: LOAD_TIMEOUT model=qwen2.5-1.5b-instruct-q4-k-m backend=llama-cpu after 45000ms ...
```

## Next step (not run here)

Extend the same ENTER/EXIT diagnostic pattern (same patch mechanism, same log pipeline) to `ggml_backend_opencl_reg()`'s registration path, since that is the next call in the traced, source-verified registry-construction order. This is a localization step, not a fix attempt — the manifest/threading changes already in place stay untouched.
