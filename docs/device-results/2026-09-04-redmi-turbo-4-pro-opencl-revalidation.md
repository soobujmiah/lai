# Redmi Turbo 4 Pro — Adreno OpenCL re-validation: main-thread hang fixed, backend itself still not viable

**Date:** 2026-09-04
**Package:** `dev.lai.runtime` (release), installed from CI run `33839471230` (`lai-release-365`,
built with `-Plai.validatedAccelerators=llama-opencl`), HEAD `8b72c12` (manifest re-add) on top of
`71319ef` (the main-thread threading fix).
**Follow-up to:** `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md` (source
fix) and `docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md` (original
device hang this re-validates against).

## Result summary

- **The main-thread app-launch hang is confirmed fixed.** With the exact same
  `<uses-native-library android:name="libOpenCL.so">` manifest entry that caused a 45+ second total
  app freeze on 2026-09-03, the app now launches instantly, draws its UI normally, and stays fully
  responsive — repeatedly confirmed across a probe run, two qualify attempts, and direct interaction
  after a timeout.
- **The underlying OpenCL backend itself is still not usable on this device.** The native
  `available()`/session-open call still does not return — but now it's isolated on an orphaned
  background thread instead of blocking the main thread, and the app correctly detects and reports
  this as `LOAD_TIMEOUT` after 45s instead of hanging indefinitely or silently.
- **Recommendation: keep the manifest entry, do not mark `llama-opencl` viable.** The fix is real
  and worth keeping — it converts an unconditional app-breaking hang into a contained, cleanly
  diagnosed backend failure with zero impact on CPU/Vulkan/Hexagon. `llama-opencl` remains gated
  behind `BuildConfig.VALIDATED_ACCELERATORS` (empty on ordinary signed releases), so no production
  user can ever reach this path regardless.

## Evidence

### 1. `probe llama-opencl` — the lightweight, model-free capability check

```
=== probe evidence (backend=llama-opencl) ===
I LAI-lifecycle: LaiApplication onCreate (process start)
I LAI-lifecycle: OpenCL vendor discovery configured under files
I LAI-lifecycle: MainActivity onCreate
I LAI-diag: Probe intent received: backend=llama-opencl
I LAI-diag: init_once: call (may be a no-op if already run)      [tid 14475]
I LAI-diag: init_once: ENTER (first call in this process)         [tid 14475]
I LAI-diag: probe: ENTER backend=llama-opencl                     [tid 28923, main]
I LAI-diag: init_once: calling llama_backend_init()               [tid 14475]
===
TIMED OUT waiting for 'probe: DONE' within 30s
```

Note the thread split: `LaiApplication onCreate`, `MainActivity onCreate`, and `probe: ENTER` all
ran on the main thread (28923) and completed instantly (`TotalTime: 530`, `WaitTime: 536` — a cold
launch, not a hang). `init_once` and everything downstream ran on a separate thread (14475) — this
is the fix working exactly as designed.

A screenshot taken while this background thread was still blocked shows LAI's normal home screen
(`Local AI, under your control`, Bangla greeting, chat input, bottom nav) fully rendered — something
that was categorically impossible under the pre-fix behavior, where the app never finished drawing
its first frame.

Sampling the blocked thread's CPU time twice, 5 seconds apart, well after the script's own 30s
gave up:
```
utime=0 stime=3   (sample 1)
utime=0 stime=3   (sample 2, +5s)
```
No progress — genuinely blocked, not slow-but-working.

### 2. `qualify qwen2.5-1.5b-instruct-q4_k_m llama-opencl` — the real model-load path

First two qualify attempts used the wrong model id (`qwen2.5-1.5b-instruct-q4-k-m`, matching the
catalog's declared id, with hyphens) against what the workspace auto-importer actually registered
it as (`qwen2.5-1.5b-instruct-q4_k_m`, with an underscore, derived from the on-disk filename) —
both failed at `MODEL_NOT_FOUND` after 45s, never reaching `loadModel()` at all. This is a real,
separate naming-mismatch worth fixing (catalog-declared id vs. filename-derived import id can
diverge), noted here but not fixed in this commit — out of scope for the OpenCL re-validation.

The corrected run, with the id workspace import actually uses:
```
=== qualification evidence (model=qwen2.5-1.5b-instruct-q4_k_m backend=llama-opencl) ===
I LAI-qualify: STARTING model=qwen2.5-1.5b-instruct-q4_k_m backend=llama-opencl loadTimeoutMs=45000
I LAI-diag: init_once: call (may be a no-op if already run)       [tid 25061]
I LAI-diag: init_once: ENTER (first call in this process)         [tid 25061]
I LAI-diag: init_once: calling llama_backend_init()               [tid 25061]
I LAI-model: Loading model id=qwen2.5-1.5b-instruct-q4_k_m forcedBackend=llama-opencl   [tid 16885, main]
...(45,000ms later, nothing else logged)...
E LAI-qualify: LOAD_TIMEOUT model=qwen2.5-1.5b-instruct-q4_k_m backend=llama-opencl after 45000ms — the native load call has not returned; it is left running orphaned rather than force-killed (see LAI-diag tag for the last stage reached)
```
Script exit code 4 (`LOAD_TIMEOUT`) — the documented, correct classification for exactly this case.

Post-timeout: the same process (PID unchanged) was re-brought to foreground via a fresh
`am start` and rendered a completely normal, fresh UI — direct on-device confirmation that a
genuinely stuck vendor call on a background thread no longer takes the app down. The orphaned
thread (tid 25061) was re-sampled and showed the identical zero-progress signature as the probe
thread above.

## What this confirms and what it doesn't

- **Confirms:** the source-level diagnosis in
  `docs/device-results/2026-09-04-opencl-hang-main-thread-root-cause.md` was correct and complete —
  moving the eager `capabilities` probe off the main thread is sufficient to eliminate the
  app-launch hang, verified with the actual manifest change re-applied on the actual device that
  originally hung.
- **Confirms:** this was never purely a LAI threading bug from the vendor's perspective — there is
  a real, separate, still-unresolved vendor/ROM-level issue where this device's OpenCL app-namespace
  bridge does not complete in reasonable time. That part of the original 2026-09-03 doc's conclusion
  (*"the driver (or the bridge into it) is not safe to invoke this way from an untrusted app process
  on this HyperOS build"*) still stands.
- **Does not establish** why the vendor call blocks (no further vendor-side tracing was attempted —
  out of scope; this device/app process cannot introspect the vendor driver's own internal state).
- **Does not change** `llama-opencl`'s status: still not `DEVICE_VALIDATED`, still not reachable by
  any production build. `llama-cpu` remains the sole shipped, validated backend for this track;
  `llama-hexagon` remains separately validated (see the 2026-09-04 Hexagon docs).

## Recommendation

Keep the manifest entry and this fix. It has no observed downside (CPU/Vulkan/Hexagon unaffected,
confirmed across every run in this session) and converts a catastrophic, undiagnosable failure mode
into a clean, correctly-classified one. If a future HyperOS/driver update changes the underlying
vendor behavior, `llama-opencl` becomes re-testable with zero further LAI-side change — just re-run
`qualify` with the correct model id. Do not spend further engineering time on the OpenCL track
itself unless new vendor-side evidence (a ROM update, a different device) appears; this is now a
closed, understood, low-priority backlog item rather than an open hang investigation.
