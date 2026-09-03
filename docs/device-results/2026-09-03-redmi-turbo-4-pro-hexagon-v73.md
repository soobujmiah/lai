# Redmi Turbo 4 Pro — `llama-hexagon` requalification: root cause found, was never a native hang

**Date:** 2026-09-03
**Package:** `dev.lai.runtime` (release)
**Builds referenced:** `versionCode=320`/`323` (pre-fix, apparent hang), `versionCode=325` (post-fix,
clean result) — all `-Plai.validatedAccelerators=llama-hexagon`
**Status supersedes:** an earlier version of this document (same filename) recorded a 4+ minute
apparent hang with no diagnosis. That version was wrong about the cause. This version replaces it
after a full instrument → isolate → diagnose → fix → requalify pass. The earlier raw observation
(process alive, no terminal log line, no crash) was real; the FastRPC/DSP-hang interpretation of
it was not.

## Summary

The original "hang" was a **Kotlin-level startup race in LAI's own qualification-intent
handling**, not a native/vendor/driver stall. `MainViewModel.loadModel()`'s
`installedModels.firstOrNull { it.id == modelId } ?: return` guard was silently returning before
ever touching native code, because `installedModels` is populated asynchronously
(`refreshModels()` in `init{}`) and the qualification intent — which fires essentially
immediately on cold app start via `MainActivity.onCreate()` — could win that race. The
qualification coroutine then waited forever on a state transition nothing had ever triggered:
indistinguishable, from outside the process, from a genuine hang.

With that race fixed (`MainViewModel.runBackendQualification` now waits for the model to
actually appear in `installedModels` before calling `loadModel`), the forced `llama-hexagon`
load now completes in **47 ms** with a clean, honest failure: no HTP device is registered by
ggml on this hardware, so LAI correctly refuses the request before ever touching the model file.
`llama-hexagon` is not device-validated, but it is now a *clean, fast, well-understood*
unavailability, not an open hang.

## Instrumentation added (kept permanently — `docs/TESTING.md` "Backend qualification")

- `LAI-diag` tag: stage-by-stage timing markers through `initialize_llama_once()`,
  `HexagonBackend`/`OpenCLBackend` `available()`/`open()`, and `build_llama_session()`'s
  device-pin / `MODEL_LOAD_BEGIN`-`END` / `CONTEXT_CREATE_BEGIN`-`END` brackets
  (`runtime/llama/src/main/cpp/{llama_session,hexagon_backend,opencl_backend}.cpp`).
- `MainViewModel.runBackendProbe()` + `qualify_probe=true` intent extra: a model-free capability
  probe (`scripts/device/lai_adb.sh probe <backend-id>`) that isolates enumeration from load.
- `MainViewModel.runBackendQualification()`: waits for the model to be visible in
  `installedModels` before loading (closes the actual race), and wraps the load-wait in
  `withTimeoutOrNull` as a safety backstop distinct from the fix itself — see "Timeout guard"
  below. New terminal states `MODEL_NOT_FOUND` and `LOAD_TIMEOUT`, alongside the existing `DONE`/
  `DENIED`/`LOAD_FAILED`.
- `MainViewModel.loadModel()`'s two early-return guards (`busy`, model-not-found) now log a
  warning instead of returning silently — the actual observability gap that let the race masquerade
  as a hang. Production behavior is unchanged: the UI's "Load" button is only ever tappable once
  `installedModels` is already populated and the app isn't busy, so neither branch is reachable
  from a real tap.

A second, independent tooling bug was found and fixed while re-testing: `scripts/device/lai_adb.sh`
polled with `adb logcat -d -e '<tag>'` at 1-2 s intervals, but each such invocation is itself
echoed back into the device's own logcat by `adbd` (`in ShellService: ... exec logcat ... '<tag>'`)
— since that echoed line literally contains the tag text, it matches the very filter it's
polluting, and at a fast polling interval this self-referential noise (plus ordinary system log
volume) can rotate a real target line out of the buffer before a later poll ever reads it.
Confirmed directly: a `probe: DONE` line present at T+0 was gone from `-e`-filtered output well
within 30 s of 1 s-interval polling. Fixed by filtering on the app's exact PID instead
(`adb logcat -d --pid=<pid>`), which cannot self-match and excludes unrelated system noise
entirely.

## Real-device evidence (versionCode 325, post-fix)

```text
20:34:29.649 LAI-qualify: STARTING model=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon loadTimeoutMs=45000
20:34:29.662 LAI-model:   Loading model id=qwen2.5-1.5b-instruct-q4-0 forcedBackend=llama-hexagon
20:34:29.709 LAI-model:   Model load failed id=qwen2.5-1.5b-instruct-q4-0: Backend llama-hexagon is not provided by the llama runtime
20:34:29.709 LAI-qualify: LOAD_FAILED model=... backend=llama-hexagon detail=Load rejected: Backend llama-hexagon is not provided by the llama runtime
```

47 ms total, model correctly found this time (`forcedBackend=llama-hexagon` present in the log,
proving the race is closed), clean `IllegalArgumentException` from
`NativeInferenceEngine.load()`'s own guard (`capabilities.compiledBackends.firstOrNull { it.id ==
backend }` found nothing) — never even reached the native `createSession`/`open()` call.

Confirmed identically three separate times (a model-free `probe`, inside a full pre-fix `loadModel`
call, and inside the post-fix `loadModel` call): `find_htp_device()` enumerates the ggml device
registry in **2-4 microseconds** and finds exactly two devices — `Vulkan0` (Adreno 825) and `CPU`.
No `HTP*`-named device is ever registered. This is instant and deterministic, not a hang at any
enumeration stage.

## Why no HTP device registers (strong evidence, not a captured denial — stated at that
confidence level)

```text
$ adb shell ls -la /dev/ | grep -iE 'fastrpc|adsp|qdsp'
crw-r--r--  system  system  fastrpc-adsp-secure
crw-rw-r--  system  system  fastrpc-cdsp
crw-r--r--  system  system  fastrpc-cdsp-secure

$ adb shell ls -Z /dev/fastrpc-adsp-secure /dev/fastrpc-cdsp /dev/fastrpc-cdsp-secure
u:object_r:vendor_xdsp_device:s0  /dev/fastrpc-adsp-secure
u:object_r:vendor_qdsp_device:s0  /dev/fastrpc-cdsp
u:object_r:vendor_xdsp_device:s0  /dev/fastrpc-cdsp-secure

$ adb shell ps -Z | grep dev.lai.runtime
u:r:untrusted_app:s0:c244,c258,c512,c768  u0_a756  ...  dev.lai.runtime
```

This device exposes **no plain, non-secure `/dev/fastrpc-adsp` node at all** — only
`fastrpc-adsp-secure` (SELinux type `vendor_xdsp_device`) and `fastrpc-cdsp` (a different DSP
domain, type `vendor_qdsp_device`, DAC-read-only for anything outside the `system`
user/group). LAI's process runs in the standard third-party `untrusted_app` SELinux domain.
AOSP/HyperOS policy does not grant `untrusted_app` access to `vendor_xdsp_device`/
`vendor_qdsp_device` types. No explicit `avc: denied` line was captured in logcat for this
specific attempt — `ggml-hexagon`'s registration probe most likely fails via its own internal
domain-availability check (closed-source FastRPC client library behavior, not directly
instrumentable from here) rather than a raw `open()` syscall reaching the kernel audit log — so
this is recorded as **strong, device-verified circumstantial evidence**, not an absolutely
proven mechanism. It is the same general class of platform restriction already conclusively
documented on this device for OpenCL
(`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`) and implicated for
Vulkan's driver behavior: HyperOS restricting a general-purpose compute path that upstream
tooling assumes is openly available to any app.

## Timeout guard (added regardless, as a safety backstop — not the fix)

`NativeInferenceEngine.load()`'s native call is synchronous and uninterruptible from Kotlin;
cooperative coroutine cancellation cannot stop a thread genuinely blocked in vendor C++/driver
code, and forcibly killing that thread risks corrupting native global state (the ggml backend
registry, a partially-opened session) for the rest of the process. `runBackendQualification()`
therefore wraps only the *wait* for `loadModel()`'s result in `withTimeoutOrNull` (default 45 s,
matching the existing `CANCEL_GRACE_MS` convention) — if it fires, the native call (if one is
even still running) is left orphaned on its own `Dispatchers.IO` thread rather than force-killed,
and a `LOAD_TIMEOUT` terminal state is logged. Production `loadModel()` itself is completely
unaffected; this lives entirely inside the qualification-only coroutine. This did not end up
being what actually happened here — the real issue resolved in 47 ms — but it is now in place for
any future case where a native call genuinely does stall.

## Test matrix

| Case | Result |
|---|---|
| Control — CPU load (unforced, same model) | Unaffected by this fix (code path untouched); confirmed working repeatedly this session, ~660 ms–1.2 s |
| Target — forced `llama-hexagon` | Clean `LOAD_FAILED` in 47 ms post-fix (was an apparent hang pre-fix, now understood as the race) |
| Regression — `llama-opencl` enumeration (`probe`) | Unaffected: `find_opencl_device()` still returns nullptr in 3-4 μs, consistent with the existing documented OpenCL state |
| Failure — unvalidated backend (`llama-vulkan` requested against a `llama-hexagon`-only build) | Clean `DENIED` in <10 ms, exit code 1 |
| Failure — backend genuinely unavailable (`llama-hexagon`, no HTP device) | Clean `LOAD_FAILED`, exit code 2 |

## Consequences

- `llama-hexagon` is **not** device-validated. Root cause of unavailability is a platform/vendor
  restriction (best evidence: no accessible FastRPC ADSP domain for third-party apps on this
  HyperOS build), not a LAI defect — this reaches the "verified external blocker" bar for the
  *acceleration* question specifically, while the *apparent-hang* question is fully resolved as a
  genuine LAI-side bug that is now fixed.
- Do not retry Hexagon speculatively without new evidence — specifically, evidence that a
  non-secure ADSP FastRPC path is actually reachable from a third-party app context on this or a
  similar HyperOS build (e.g., a HyperOS update, a different Xiaomi device, or a documented vendor
  allowlist mechanism).
- `initialize_llama_once()`'s registration probe (`ggml_backend_load_all()`) itself is proven fast
  (microseconds) on this device; the earlier working theory that it or the accelerator-probe
  sequence needed a timeout-guarded rewrite was based on the mistaken hang diagnosis and does not
  apply. No change to that function's core logic was needed beyond the added diagnostics.
- Fallback remains `llama-cpu`, unaffected throughout.
