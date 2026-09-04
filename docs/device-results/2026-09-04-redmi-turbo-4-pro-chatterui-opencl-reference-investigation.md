# Redmi Turbo 4 Pro — ChatterUI as a reference for LAI's Adreno OpenCL hang

**Date:** 2026-09-04
**Reference app:** `com.Vali98.ChatterUI` (React Native + `llama.rn`), versionName `0.8.9-beta9b`,
already installed on-device, used via its own UI (no source access, no APK modification).
**Purpose:** LAI's `llama-opencl` backend is currently blocked on a real device-confirmed hang
(`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`) triggered by adding
`<uses-native-library android:name="libOpenCL.so">`. Before designing a fix, determine whether
ChatterUI — previously used only as Hexagon-path evidence
(`docs/device-results/2026-09-03-redmi-turbo-4-pro-chatterui-fastrpc-comparison.md`) — actually
exercises OpenCL as a *compute* backend on this same device, not just whether it declares/compiles
OpenCL support. Investigation only; no LAI source was modified.

## Summary of findings

1. **ChatterUI has an explicit, separate "Backend Device" selector (OpenCL / Hexagon / CPU) plus a
   numeric "GPU Layers" field**, both per-model, under Models → (model) → Show Settings → Model
   Settings. This is materially different from LAI's current design, where backend selection is a
   build-time flag (`-Plai.validatedAccelerators`) plus a scheduler decision, not a runtime,
   per-load user choice.
2. **Every model load observed before this session's deliberate test had `GPU Layers: 0`** —
   including the very first import of `qwen2.5-1.5b-instruct` (ChatterUI's own in-app Logs screen,
   entry timestamped `[22:24:05]`) and every subsequent load through today. With `GPU Layers: 0`,
   no backend actually receives any offloaded layers regardless of which Backend Device is
   selected — so **all of ChatterUI's owner-driven usage history up to this session was CPU-only**,
   contradicting the throughput-based "real accelerator engagement" inference in the earlier
   Hexagon comparison doc (that doc's evidence was real, but for Hexagon specifically, via a
   different, always-eager code path — see below — not for OpenCL, which had never actually been
   used).
3. **The real Hexagon FastRPC/DSP session-open sequence happens unconditionally at native library
   registration time** (`ggml_backend_load_all()`), independent of `GPU Layers` or which Backend
   Device is selected — confirmed by capturing it on a cold start where the persisted model config
   still had `GPU Layers: 0`. This means the earlier Hexagon comparison doc's FastRPC evidence
   shows real *hardware detection/registration*, not proof that a prior chat's tokens were actually
   computed on the NPU. (Doesn't retract that doc — LAI's own Hexagon PASS is separately
   qualified with `gpu_layers=999` via an explicit forced-backend intent — but the ChatterUI side
   of that comparison needs this correction.)
4. **With OpenCL explicitly selected as Backend Device and GPU Layers set to 28 (all layers of
   `qwen2.5-1.5b-instruct`, the same file LAI uses for its own Hexagon qualify test), the model
   loaded successfully — no hang, no crash.** A burst of ~90 real Adreno GPU driver log lines
   (empty-payload `I Adreno` entries, standard for this driver's release-build logging) occurred on
   a dedicated thread (tid distinct from the main/UI thread) in a 588ms window strictly between
   `RNLlama: loadModel:212` and `RNLlama: loadModel:228 Context initialized` — i.e., inside the
   native model-load call, at exactly the point a backend would allocate device buffers / compile
   compute kernels. This is consistent with real OpenCL kernel compilation (`clBuildProgram`),
   though no `ggml_opencl`-prefixed text log was captured (ChatterUI's build doesn't appear to
   route llama.cpp's own log output to Android `Log`/logcat the way LAI's custom `LAI-llama`
   redirect does — the Adreno driver's own logging is independent of that and appears regardless).
5. **A real chat generation was sent and completed normally** on this OpenCL/GPU-Layers=28
   configuration: `Prompt: 89.83 t/s`, `Text Gen: 16.80 t/s`. For comparison, the same model
   (`qwen2.5-1.5b-instruct`) on this same device with `GPU Layers: 0` (CPU-only, logged moments
   earlier the same session) measured `Prompt: 44.58 t/s`, `Text Gen: 11.97 t/s`. The OpenCL run
   was ~2x faster on prompt eval and ~40% faster on decode. Consistent with real compute offload;
   not treated as definitive on its own (per the same caveat as the Hexagon doc — no per-op
   backend-assignment trace was captured), but it corroborates the driver-log evidence rather than
   contradicting it.

## Direct answers

**A. What backend does ChatterUI actually use?**
By default (as configured across all of the owner's prior usage on this device): **CPU only**
(`GPU Layers: 0` on every historical load, both models). ChatterUI *can* use OpenCL or Hexagon, but
only when a user explicitly sets `GPU Layers > 0` and picks that Backend Device for a given model —
neither had ever been done before this session's deliberate test.

**B. Is that backend directly comparable to LAI's OpenCL path?**
Yes for the deliberate test performed this session: same device, same model file
(`qwen2.5-1.5b-instruct-q4-0`/`qwen2.5-1.5b-instruct-q4_k_m` — ChatterUI's own file is `q4_k_m`,
not the `q4-0` LAI had to special-case for Hexagon; OpenCL has no such quant restriction), same
underlying `llama.cpp`-family OpenCL backend (ChatterUI's `librnllama_jni_v8_2_dotprod_i8mm_hexagon_opencl.so`
bundles the same upstream OpenCL implementation LAI compiles into `liblai_runtime.so`), same vendor
driver (`/vendor/lib64/libOpenCL.so`, same Adreno 825). The comparison is apples-to-apples for "does
OpenCL work on this hardware/ROM combination at all" — which is exactly the question LAI's hang
left open.

**C. Why does ChatterUI succeed where LAI's attempt hung?**
The most load-bearing concrete difference found: **threading**. Every native call ChatterUI made in
this investigation — HTP extraction, FastRPC init, the model load itself (`RNLlama: loadModel:*`),
the Adreno driver burst — ran on a background thread (tid distinct from, and never equal to, the
main/UI thread's tid, which by Android convention equals the process pid). This is inherent to
React Native's architecture: native module calls are dispatched off the JS/UI thread by the bridge,
not something ChatterUI's own code had to opt into. LAI's hang, by contrast, was described in the
prior doc as blocking "app launch itself" — the whole `MainActivity`, main thread state `S`
(sleeping), no crash, no forward progress — which only happens if the failing call is on, or is
synchronously blocking, the main thread. LAI's `initialize_llama_once()` (which contains the
backend-capability probes, including OpenCL's) is called eagerly during startup; whether that
specific eager call path runs on the main thread or a background one is the exact next thing to
verify in LAI's own source before writing a fix (this investigation deliberately did not touch LAI
source). A secondary, lower-confidence difference: ChatterUI's manifest requests the app-namespace
bridge for `libOpenCL.so` *and* `libcdsprpc.so` together (`uses_libraries=libOpenCL.so:libcdsprpc.so`,
confirmed via `nativeloader` logcat), while LAI's reverted attempt requested `libOpenCL.so` alone —
possibly irrelevant, but a real difference worth ruling in or out rather than assuming.

**D. Is the previously proposed background-thread + timeout-guarded OpenCL probe still justified?**
Yes, and more strongly than before. This was already the prior session's own conclusion
(`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`, "Path not attempted
this session" section) reached from first principles. This investigation adds a working existence
proof: a real app, real device, real OpenCL backend, real generation, with the *only* structurally
guaranteed-different property being that its native init never runs synchronously on the UI thread.
That's now direct evidence for the specific mechanism, not just a plausible theory.

**E. Smallest safe next engineering step**
Before writing any fix: read `runtime/llama/src/main/cpp/native_inference.cpp` (or wherever
`initialize_llama_once()` lives) and confirm which thread it actually runs on for the eager,
non-forced startup probe path (as opposed to the `hexagon: open()` call, which the 2026-09-04
Hexagon qualify logs show already running on a background thread via
`viewModelScope.launch { ... }` / `Dispatchers.IO`). If the eager OpenCL/Vulkan/Hexagon
*availability* probe (not the forced-load path) is what's still on the main thread, that's the
actual bug, and the fix is exactly what the prior doc proposed: move that probe to a background
thread with a hard timeout, so a stuck vendor call can never block `MainActivity`. Re-attempt the
`<uses-native-library android:name="libOpenCL.so">` manifest addition only after that guard exists,
not before — this investigation does not license retrying the same change unguarded.

## Raw evidence

Cold-start capture (`GPU Layers: 0`, model `qwen2.5-1.5b-instruct`, backend irrelevant at 0 layers)
— real Hexagon FastRPC session opened eagerly at registration time, unconditionally:
```
D RNLlama : Using /data/user/0/com.Vali98.ChatterUI/app_rnllama-htp for HTP libraries
D RNLlama : Extracted HTP library: libggml-htp-v73.so
D RNLlama : Set ADSP_LIBRARY_PATH=/data/user/0/com.Vali98.ChatterUI/app_rnllama-htp
I vendor/qcom/proprietary/adsprpc/.../fastrpc_apps_user.c:5112: multidsplib_env_init: libcdsprpc.so loaded
D nativeloader: Load .../librnllama_jni_v8_2_dotprod_i8mm_hexagon_opencl.so ... ok
...(13s later, before RNLlama: loadModel:212 fires at all)...
D apps_std_imp.c:382: apps_std_fopen_fd done for .../app_rnllama-htp/./libggml-htp-v73.so ... error_code 0x0
I fastrpc_apps_user.c:1828: remote_handle64_open: opened handle 0xb400007674299590 ... refs 1
I RNLlama : loadModel:212 Using n_parallel: 1 (enables up to 1 parallel slots)
I RNLlama : loadModel:228 Context initialized with n_seq_max = 1
```

In-app Logs screen (JS-side, not logcat), confirming `GPU Layers: 0` on both the very first import
and the most recent explicit load before this session's test:
```
[INFO] [22:24:05]:
------ MODEL LOAD -----
 Model Name: qwen2.5-1.5b-instruct
...
GPU Layers: 0

[INFO] [10:12:31]:
------ MODEL LOAD -----
 Model Name: qwen2.5-1.5b-instruct
...
GPU Layers: 0
```

Deliberate OpenCL test (`Backend Device: OpenCL`, `GPU Layers: 28`) — model load, no hang:
```
I RNLlama : loadModel:212 Using n_parallel: 1 (enables up to 1 parallel slots)
I SKIA    : CreateGraphicsPipeline pipeline cache hit. elpased time: 0.37 ms.
I Adreno  :                                                         [x~90, 588ms, tid 27624]
W RenderInspector: QueueBuffer time out on com.Vali98.ChatterUI/com.Vali98.ChatterUI.MainActivity, count=1, avg=60 ms, max=60 ms.
I RNLlama : loadModel:228 Context initialized with n_seq_max = 1
I RNLlama : loadModel:237 ctx_shift: enabled
I RNLlama : attachThreadpoolsIfAvailable:183 Attached ggml threadpool (n_threads=4, n_threads_batch=4)
```

Generation result (in-app, same OpenCL/GPU-Layers=28 config) vs. the CPU-only baseline logged
minutes earlier in the same session, same model:
```
OpenCL, GPU Layers=28:  Prompt: 89.83 t/s   Text Gen: 16.80 t/s
CPU,    GPU Layers=0:   Prompt: 44.58 t/s   Text Gen: 11.97 t/s
```

## What this does not establish

- No per-op backend-assignment trace was captured (same caveat as every prior device-results doc in
  this series) — the Adreno driver burst plus the throughput delta are strong circumstantial
  evidence of real OpenCL compute, not a formal proof every matmul ran on the GPU.
- The exact thread LAI's own eager backend-probe path runs on was not checked in this investigation
  (deliberately, since it requires reading/reasoning about LAI source, out of scope for a
  device-only investigation) — that's the first action item for whoever picks up the fix.
- This is a single OpenCL load + single generation, not a repeated/stability pass.
