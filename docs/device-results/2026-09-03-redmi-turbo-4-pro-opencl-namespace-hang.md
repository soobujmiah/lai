# Redmi Turbo 4 Pro — Adreno OpenCL app-namespace bridge attempt

**Date:** 2026-09-03
**Package:** `dev.lai.runtime` (release, `versionCode=303`, commit `a23c340`)
**Purpose:** re-investigate the 2026-08-20 "OpenCL locked by HyperOS for modern apps" verdict against LAI's actual compiled OpenCL backend, rather than accepting a two-week-old finding as final — per direct instruction not to declare GPU acceleration a dead end on the strength of a single backend's failure (Vulkan) without exhausting the realistic alternatives first.

## Starting point: what the 08-20 verdict actually established, re-checked

Confirmed still current before doing anything else: `ro.build.display.id` unchanged (`BP2A.250605.031.A3`) since 2026-08-20 — no HyperOS OTA has landed that could have changed anything.

Fresh, direct evidence from relaunching the currently-installed build and capturing `OpenCLBackend::available()`'s own probe output (not re-reading old docs):

```text
ggml_opencl: platform IDs not available.
opencl: dlopen(libOpenCL.so) failed: dlopen failed: library "libOpenCL.so" not found
opencl probe: device 0 type=2 name='Vulkan0' description='Adreno (TM) 825'
opencl probe: device 1 type=0 name='CPU' description='CPU'
opencl: compiled but no OpenCL GPU device registered (no vendor ICD?)
```

This confirmed the 08-20 verdict still holds for LAI's actual shipped code (not just a different, untested code path), but also surfaced the precise mechanism: `/vendor/lib64/libOpenCL.so` exists on disk (confirmed via `adb shell find`), with SELinux type `same_process_hal_file` — the same category as the Adreno EGL/Vulkan libraries this app already loads successfully — and is declared in `/vendor/etc/public.libraries.txt`. That file only bridges a library into the vendor **sphal** namespace, not automatically into the app's default linker namespace. That distinction, not a blanket "OpenCL is inaccessible," is the actual restriction.

## Attempted fix: `<uses-native-library>`

Android's documented (API 31+) mechanism for exactly this situation — an app requesting the OS bridge a vendor-declared library into its own linker namespace — is a manifest declaration:

```xml
<uses-native-library android:name="libOpenCL.so" android:required="false" />
```

Confirmed via web search that this is a real pattern other Android/Adreno-OpenCL projects use, not a speculative guess. Added to `AndroidManifest.xml`, built (`lai-release-303`), installed.

## Result: real device evidence — a genuine hang, not a fix

Relaunched fresh **three times** (`am force-stop` + `am start`, confirmed foreground via `dumpsys activity activities` each time) with a clean logcat capture. All three times, startup logging halted completely right after:

```text
ggml_vulkan: Found 1 Vulkan devices:
ggml_vulkan: 0 = Adreno (TM) 825 (Qualcomm Technologies Inc. Adreno Vulkan Driver) | ...
```

— exactly the point where `ggml_opencl: platform IDs not available.` used to log instantly in every prior build. No further log lines, ever, across a 45+ second observation window on the longest attempt.

Confirmed this is a genuine block, not a slow-but-completing driver init:

- Process stayed alive throughout (`pidof` unchanged).
- No crash logged (`LAI-crash` tag silent), no new tombstone in `/data/tombstones/`.
- Main thread state: `S` (sleeping) via `/proc/<pid>/task/<pid>/status`, not `R` (running).
- Main thread `utime`/`stime` from `/proc/<pid>/stat` were unchanged (18/11 → 18/13, negligible) across a 3-second sampling window, then unchanged again after a further 30 seconds — the thread is blocked on something, not spinning.

## Why this is worse than the prior state, and why it was reverted

Before this change: OpenCL detection failed **instantly** and cleanly — `dlopen` returned "library not found" in under a millisecond, logged, and the app proceeded normally with CPU (and, if flagged, Vulkan) unaffected. This is the fail-closed behavior the architecture requires.

After this change: **app launch itself hangs**, unconditionally, regardless of which `validated_accelerators` the build was flagged with. This breaks the project's own non-negotiable principle that GPU/NPU experimentation must never take down the CPU guaranteed fallback — a hung launch means CPU never gets the chance to load either. Reverted immediately (commit `82949bb`) rather than leave a hang on `main`.

## What this evidence actually means

This is more precise than "OpenCL is inaccessible." The prior instant failure suggests the app-namespace lookup for the bare `libOpenCL.so` soname genuinely came back empty before reaching any vendor code. After declaring `uses-native-library`, the failure mode changed from an instant negative to a deep hang — consistent with the linker now actually attempting real resolution work against the vendor sphal bridge, reaching into the vendor driver's own initialization path, and blocking there. In plain terms: **the request can reach the vendor OpenCL driver on this device, but the driver (or the bridge into it) is not safe to invoke this way from an untrusted app process on this HyperOS build.**

This does not prove OpenCL is fundamentally impossible on this hardware — Qualcomm's own upstream backend documentation (`docs/backend/OPENCL.md` at LAI's exact pinned llama.cpp commit) lists real verified Adreno GPUs (750, 810, 830, 840, X1-85, X2-90) with working OpenCL, and real third-party projects (llama.rn on Adreno 740, llama.cpp core issues) show it working on other Adreno generations/Android builds with their own rough edges. It shows this specific device/ROM combination's linker-namespace bridge for OpenCL is not currently safe to invoke synchronously.

## Path not attempted this session, and why

The `available()` probe runs inside `initialize_llama_once()`, which is a **shared** one-time native initializer also used by the CPU and Vulkan backends' own detection — it is not isolated to OpenCL. A proper fix would run the platform-enumeration probe on a background thread with a hard timeout, so a driver-level hang can never block app or model-load startup regardless of cause. That is a real native-threading change to a shared initialization path, not a quick patch, and rushing it risked introducing a genuine race condition into code that CPU and Vulkan also depend on. Documented here as the correct next step rather than attempted blind under time pressure.

## Status transition

`llama-opencl`: **`Implemented; device qualification pending`** → **`Implemented; device-validated HANG on synchronous app-namespace bridge attempt — reverted, not currently viable without an async/timeout-guarded probe`**. Does not change `llama-cpu` (unaffected, confirmed working again after the revert) or `llama-vulkan` (separate, already-crashed track, unaffected either way).
