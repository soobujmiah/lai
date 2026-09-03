# Redmi Turbo 4 Pro — first `llama-hexagon` device qualification: load hangs, no crash, no fallback

**Date:** 2026-09-03
**Package:** `dev.lai.runtime` (release, `versionCode=320`, commit `2e54778`)
**Build:** `-Plai.validatedAccelerators=llama-hexagon` (CI run 33758789683), Hexagon SDK 6.6.0.0 fetched and `GGML_HEXAGON` compiled in
**Purpose:** first real physical-device test of the `llama-hexagon` NPU backend registered in `docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md`, using the new ADB-first backend-qualification path (`MainViewModel.runBackendQualification`, `scripts/device/lai_adb.sh qualify`) added the same day specifically to make this test possible — the catalog's CPU-first preference otherwise makes an unvalidated backend unreachable through the normal UI.

## Method

```bash
scripts/device/lai_adb.sh qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon "Say hello in one short sentence." 180
```

This force-stops the app, launches it with `qualify_backend=llama-hexagon`/`qualify_model=qwen2.5-1.5b-instruct-q4-0` intent extras, and polls for a terminal `LAI-qualify` log line. The Q4_0 model variant was already downloaded and present on-device (1016 MB) from an earlier attempt to qualify the same model on `llama-cpu` (baseline: loaded successfully in 661 ms).

## Result: the load call never returns — a genuine hang, not a crash or a slow success

```text
09-03 19:16:48.942 14260 14260 I LAI-lifecycle: LaiApplication onCreate (process start)
09-03 19:16:48.969 14260 14260 I LAI-lifecycle: MainActivity onCreate
09-03 19:16:48.973 14260 14260 I LAI-qualify: Qualification intent received: model=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon
09-03 19:16:49.016 14260 14260 I LAI-qualify: STARTING model=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon
```

No further `LAI-*` log line ever appeared — not `LAI-model: Model loaded...`, not `LAI-model: Model load failed...`, not the `LAI-qualify: LOAD_FAILED`/`DONE` terminal lines the qualification path always logs on either outcome. The script's 180 s poll timed out (exit code 3). Manual follow-up over several more minutes confirmed:

- The process stayed alive throughout (`pidof` unchanged, `14260`).
- No crash logged (`LAI-crash` tag silent); `adb logcat -b crash -d` empty.
- Main process state stayed `S` (sleeping) via `/proc/<pid>/status`, with 44 threads, for the entire observation window (~4.5 minutes) — consistent with a thread genuinely blocked on a syscall/driver call, not spinning or making progress.
- `am start -W` itself reported `Complete`/`Status: ok` at launch — the *Activity* drew normally; the hang is specifically in the model-load coroutine's call into `InferenceEngine.load()` → native `createSession(..., "hexagon", ...)`, not in app startup generally.

At 19:18:09 (≈80 s after `STARTING`) an unrelated `MainActivity onDestroy` fired — the device's own foreground focus moved to another app (confirmed separately via `dumpsys activity activities`: `topResumedActivity` had become a different, unrelated foreground app) during the test window, most likely an incoming-notification interruption on this daily-use device, not an action taken by the qualification script itself. Per SKB's `standards/agent-device-testing.md` single-actor-rule/evidence-rules discipline, this is recorded as an honest confound rather than silently omitted: the observation window was not exclusively foregrounded end-to-end. It does not change the finding — the load call had already failed to return for the full 80 s *before* this occurred (CPU-backend load of the identical model completes in 661 ms), the `viewModelScope` coroutine and its underlying native call are independent of Activity foreground state, and the process was still alive and silent minutes later.

## What this evidence actually means

`MainViewModel.loadModel(forcedBackend=...)` correctly bypassed the scheduler and called `InferenceEngine.load()` directly onto `llama-hexagon` — the qualification path itself worked exactly as designed, and per its own contract it did **not** silently substitute CPU when Hexagon was explicitly requested (confirmed: no `backend=llama-cpu` load-success line ever appeared). The hang is downstream, inside the native call chain: `NativeBindings.createSession(path, "hexagon", contextSize)` → `backend_registry.cpp`'s `create_hexagon_backend()` → `HexagonBackend::open()`/`available()` → `initialize_llama_once()` + `find_htp_device()` (`hexagon_backend.cpp`). This is the same shared native-init code path already implicated in the 2026-09-03 OpenCL finding (`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`): a call that reaches real vendor/DSP driver code but blocks there indefinitely instead of failing fast, on this specific HyperOS build.

Two mechanisms are plausible and not yet distinguished by this evidence:
1. **FastRPC/DSP session open blocks.** `ggml-hexagon`'s device init opens a Hexagon RPC session (`libcdsprpc.so`/`libadsprpc.so`, confirmed present at `/vendor/lib64/` per the 2026-09-03 scoping doc) — if the DSP domain the app requests is unavailable, permission-gated, or the skel (`libggml-htp-v73.so`) fails to load into the DSP-side process without a clear error return, the FastRPC call can block rather than fail.
2. **HTP device enumeration itself blocks** inside `find_htp_device()`'s `ggml_backend_dev_count()`/`ggml_backend_dev_get()` loop, before any per-device log line is even reached — no `hexagon probe: device N ...` log line appeared either, which would be expected to print once per registered ggml device if the loop executed at all. This absence is itself evidence: the block is at or before the very first device enumeration call, earlier than the OpenCL hang (which at least logged its Vulkan device list before stalling).

## Consequences

- `llama-hexagon` is **not** device-validated. It stays out of the catalog's `compatibleBackendIds` selection path for real use (it already requires an explicit qualification-only force to be reachable at all, so no production surface is affected).
- Do not retry this exact path speculatively. The next attempt needs either: (a) upstream `ggml-hexagon` guidance on this specific failure mode, (b) a timeout-guarded probe (mirroring the fix direction already identified for the OpenCL hang, since both share `initialize_llama_once()`), or (c) confirmation from a from-scratch, screen-on, do-not-disturb, single-actor test run that the ≈80 s pre-backgrounding window is reproducible without the confound noted above.
- This is the second native-init hang found in the same shared code path in one day (OpenCL, then Hexagon). That is itself a signal: `initialize_llama_once()`'s accelerator-probe sequence may need a hard timeout/watchdog thread as a general fix, rather than patching each backend individually — worth a dedicated design pass before the next accelerator qualification attempt (Hexagon retry, or any future backend).
- Fallback remains `llama-cpu`, unaffected by this finding — it loads correctly regardless of what other backends are compiled in, since `NativeInferenceEngine.load()` only invokes the requested backend's native path.
