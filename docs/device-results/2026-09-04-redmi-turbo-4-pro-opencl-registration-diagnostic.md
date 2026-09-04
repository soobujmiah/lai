# Redmi Turbo 4 Pro — OpenCL registration diagnostic: `clGetPlatformIDs()` is the first call without a matching EXIT

**Date:** 2026-09-04
**Device:** Redmi Turbo 4 Pro (`25053RT47C`), Snapdragon 8s Gen 4 (SM8735), Adreno 825, arm64-v8a.
**Pinned llama.cpp commit:** `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`.
**Diagnostic patch commit:** `2397d3f` (`scripts/ci/ggml-opencl-reg-diag.patch`, applied by `fetch_llama_cpp.sh` as a fourth patch, alongside the existing three Vulkan patches — including the Vulkan diagnostic from `b626303`, which this commit does not modify).
**Build:** CI run `33863527989` (`lai-release-375`, `-Plai.validatedAccelerators=llama-cpu`).
**Follow-up to:** `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`, which ruled out Vulkan as the `llama_backend_init()` blocker and identified OpenCL's own registration (`ggml_backend_opencl_reg()` → `ggml_opencl_probe_devices()`) as the next untested boundary in the source-verified registration order (Vulkan → OpenCL → Hexagon → CPU).

## What was instrumented

`ggml_opencl_probe_devices()` (`ggml/src/ggml-opencl/ggml-opencl.cpp`), the real vendor-touching calls in this backend's registration path, each with a distinct `ENTER`/`EXIT` `GGML_LOG_INFO` line (same pipeline as the Vulkan diagnostic — `ggml_log_internal` → LAI's `llama_log_set()` callback → `LAI-llama` logcat tag):

1. `clGetPlatformIDs()`
2. The platform enumeration loop (per-iteration `ENTER`/`EXIT`, logging index `i`)
3. `clGetDeviceIDs()` (per platform, inside the loop)
4. `clCreateContext()`
5. `ggml_opencl_is_device_supported()` (per device, call-site only — not instrumented internally, matching the granularity used for Vulkan's post-enumeration device-property loop, which was left uninstrumented for the same reason: it's reached only after the calls above succeed)

Same constraints as the Vulkan patch: purely additive logging, no behavior/threading/timing/synchronization change, no backend disabling, no fallback or timeout changes. Confirmed to apply cleanly on top of the existing three Vulkan patches and compile on CI.

## VERIFIED

- The full Vulkan sequence (from `docs/device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md`) reproduced identically on this build: every Vulkan call completed, `vk::createInstance()` in 15ms this run, `ggml_vulkan: Found 1 Vulkan devices` printed. Vulkan is not the blocker, confirmed a second time on a different build.
- `ENTER clGetPlatformIDs()` was logged, at 16:43:32.403, immediately after Vulkan's registration returned — confirming execution reached OpenCL's registration path exactly as the source-traced registration order predicted.

## CURRENT BLOCKER

- **No matching `EXIT clGetPlatformIDs()` was ever logged.** This is the last line of output from this thread for the remainder of the 45s qualify window.
- None of the other instrumented calls (`ENTER platform loop`, `clGetDeviceIDs`, `clCreateContext`, `ggml_opencl_is_device_supported`) were reached — consistent with `clGetPlatformIDs()` itself being the point execution stops, not a call further down the same function.
- The qualify run hit `LOAD_TIMEOUT` after the full 45s budget (exit code 4), same as every prior attempt in this investigation.

**Per the decisive-evidence framework used for the Vulkan run: this is case (A) — `ENTER` logged, `EXIT` never appears. `clGetPlatformIDs()` is the first native call in the traced OpenCL registration path that does not return.**

## NOT YET PROVEN

- *Why* `clGetPlatformIDs()` does not return. No instrumentation inside the OpenCL ICD loader or the vendor driver itself was added or is possible from LAI's own source — `clGetPlatformIDs()` is a single opaque call into the ICD loader (`libOpenCL.so`, bridged via the `<uses-native-library>` manifest entry from `8b72c12`), which in turn dispatches to whatever vendor driver the loader resolves.
- Any causal explanation — driver bug, contention, thermal, ICD loader misconfiguration, or anything else. None of these were tested by this run.
- Whether this is the same underlying issue the original `docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md` investigation found (a real vendor call newly reachable via the manifest bridge, previously failing instantly with "platform IDs not available" before that bridge existed) — the symptom matches closely (same call, same "reaches real code now, doesn't return" pattern), but this run did not independently re-verify that document's own findings; it is a plausible connection, not a re-confirmed one.

## Process/crash/ANR

- Process remained alive throughout and after the timeout (confirmed via `pidof` before the deliberate force-stop that ended this session's device activity).
- No tombstone generated in this timeframe (most recent on-device tombstone predates this session by weeks).
- No ANR trace generated.

## Raw evidence

```
16:43:32.386 I LAI-diag: jni: runtimeInfo checking available() ENTER backend=vulkan
16:43:32.387 I LAI-diag: init_once: calling llama_backend_init()
16:43:32.387 D LAI-llama: lai-diag: ENTER dispatcher.init(vkGetInstanceProcAddr)
16:43:32.387 D LAI-llama: lai-diag: EXIT dispatcher.init(vkGetInstanceProcAddr)
16:43:32.387 D LAI-llama: lai-diag: ENTER vk::enumerateInstanceVersion()
16:43:32.387 D LAI-llama: lai-diag: EXIT vk::enumerateInstanceVersion() api_version=4210688
16:43:32.387 D LAI-llama: lai-diag: ENTER vk::enumerateInstanceExtensionProperties()
16:43:32.387 D LAI-llama: lai-diag: EXIT vk::enumerateInstanceExtensionProperties() count=14
16:43:32.387 D LAI-llama: lai-diag: ENTER ggml_vk_instance_layer_settings_available()
16:43:32.387 D LAI-llama: lai-diag: EXIT ggml_vk_instance_layer_settings_available() result=0
16:43:32.387 D LAI-llama: lai-diag: ENTER vk::createInstance()
16:43:32.402 D LAI-llama: lai-diag: EXIT vk::createInstance()
16:43:32.402 D LAI-llama: lai-diag: ENTER per-instance DEFAULT_DISPATCHER.init()
16:43:32.403 D LAI-llama: lai-diag: EXIT per-instance DEFAULT_DISPATCHER.init()
16:43:32.403 D LAI-llama: lai-diag: ENTER instance.enumeratePhysicalDevices()
16:43:32.403 D LAI-llama: lai-diag: EXIT instance.enumeratePhysicalDevices() count=1
16:43:32.403 D LAI-llama: ggml_vulkan: Found 1 Vulkan devices:
16:43:32.403 D LAI-llama: ggml_vulkan: 0 = Adreno (TM) 825 (Qualcomm Technologies Inc. Adreno Vulkan Driver) | ...
16:43:32.403 D LAI-llama: lai-diag: ENTER clGetPlatformIDs()
[ 45,000 ms of silence on this thread — no EXIT, no further output of any kind ]
16:44:17.503 E LAI-qualify: LOAD_TIMEOUT model=qwen2.5-1.5b-instruct-q4-k-m backend=llama-cpu after 45000ms ...
```

## Next step (not run here)

Per the same registration order, Hexagon's own registration (`ggml_backend_hexagon_reg()`) is the next boundary if OpenCL's is ever cleared — not applicable here since OpenCL did not clear. The concrete next step is deciding whether to instrument *inside* `clGetPlatformIDs()`'s call path from the LAI/ICD-loader side (there is nothing left to instrument in ggml's own source at this call — it is a single opaque library call), or to treat this as sufficiently localized and investigate the ICD loader / manifest-bridge interaction directly instead. Not decided or attempted in this document.
