# Redmi Turbo 4 Pro — traced `llama-hexagon`'s failure to an exact FastRPC error, in upstream source

**Date:** 2026-09-03
**Follow-up to:** `docs/device-results/2026-09-03-redmi-turbo-4-pro-chatterui-fastrpc-comparison.md`
(the `libcdsprpc.so` manifest fix). That fix genuinely helped — this document traces exactly how
far it got, why it still wasn't enough, and the second fix applied alongside it.

## Method

Fetched `ggml-hexagon.cpp`, `htp-drv.cpp`, `ggml-backend-reg.cpp`, and ggml's own
`CMakeLists.txt` files directly from `ggml-org/llama.cpp` at LAI's exact pinned commit
(`ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`) to read the real registration/session-open code
path, rather than continuing to guess from behavior alone. Cross-checked every hypothesis against
a fresh on-device qualification run before accepting it.

## What the `libcdsprpc.so` manifest fix actually changed

Before the fix: `find_htp_device()` returned nullptr in 2-6 μs with **zero** `ggml-hex:`-prefixed
log output at all — not even the unconditional log lines `ggml_hexagon_registry`'s own constructor
prints (`"Hexagon backend (experimental): allocating new registry"`, `"Hexagon Arch version
v%d"`). Source trace: `ggml_backend_hexagon_reg()` calls `htpdrv_init()` **first**, which itself
starts with `dlopen("libcdsprpc.so")` — if that fails, `ggml_backend_hexagon_reg()` returns `NULL`
immediately, before the registry (and its logging) is ever constructed. `libcdsprpc.so` was never
namespace-bridged into LAI's process, so this `dlopen` failed silently every time.

After the fix (`<uses-native-library android:name="libcdsprpc.so" .../>`), a fresh qualification
run showed real progress:

```text
ggml-hex: Loading driver libcdsprpc.so
ggml-hex: forcing ndev to 1 for SoCs archs lower than v75.
ggml-hex: Hexagon backend (experimental) : allocating new registry : ndev 1
ggml-hex: Hexagon Arch version v73
ggml-hex: HTP0 allocating new session
ggml-hex: failed to open session 0 : error 0x80000406
ggml-hex: releasing session: HTP0
ggml-hex: failed to create device/session 0
```

The driver loads. The registry constructs. `htpdrv_get_arch()` does a **real DSP round-trip**
and correctly identifies v73 (no fallback-to-default warning logged) — genuine bidirectional
FastRPC communication succeeded. "Enable Unsigned PD" also succeeds silently (no error). The
fix was real, not a no-op — it just wasn't the whole story.

## The remaining failure, traced to source

`ggml_hexagon_session`'s constructor (`ggml-hexagon.cpp`) calls `htp_iface_open(session_uri,
&this->handle)` as its final step — the actual FastRPC `remote_handle64_open`-class call that
ChatterUI's own logs showed succeeding on this device. Here it returns `0x80000406`.

That error code is a well-documented Hexagon FastRPC failure: **dynamic loading failed on the DSP
side** — the DSP-side loader could not find/open the requested skel file
(`libggml-htp-v73.so`), independent of signature/permission checks (which had already passed —
Unsigned PD was enabled without error). This is exactly the class of failure the Hexagon FastRPC
ecosystem attributes to a missing or wrong `ADSP_LIBRARY_PATH`.

**Device check confirmed why:** `adb shell dumpsys package dev.lai.runtime` reports
`legacyNativeLibraryDir=/data/app/.../lib`, but that directory is **empty** on disk
(`ls -la .../lib/arm64/` → only `.`/`..`). This build's native-library packaging loads `.so`
files straight out of the (compressed or uncompressed) APK zip at runtime rather than extracting
them to a real filesystem path — a normal, modern Android optimization for the app's own
`dlopen()`-based libraries, but fatal for the Hexagon DSP-side loader, which is a separate
execution context that cannot read from inside an APK zip and needs `ADSP_LIBRARY_PATH` pointed
at a real directory containing the skel file. LAI's `hexagon_backend.cpp` never set this
environment variable at all. ChatterUI's own logs (previous document) show it doing exactly this
after a manual extraction step: `RNLlama: Set ADSP_LIBRARY_PATH=/data/user/0/com.Vali98.ChatterUI/app_rnllama-htp`.

## Fix applied

1. `android:extractNativeLibs="true"` (`AndroidManifest.xml`, `<application>` tag) — makes
   Android's own package installer extract every bundled `.so` (including the four HTP skels) to
   `legacyNativeLibraryDir` at install time, avoiding a manual runtime-extraction step like
   ChatterUI's.
2. `LaiApplication.onCreate()` now calls a new native function,
   `NativeInferenceEngine.configureHexagonAdspPath(applicationInfo.nativeLibraryDir)`
   (`hexagon_backend.cpp`), which does `setenv("ADSP_LIBRARY_PATH", nativeLibraryDir, 1)` —
   mirroring the existing `configureOpenCLVendors` pattern exactly, run before any backend probe.

Both changes are additive and inert for every other path: CPU/Vulkan load the same way regardless,
and `extractNativeLibs="true"` only affects on-disk packaging (larger install footprint, no
behavior change for libraries that don't need DSP-style filesystem access).

## Result

See the qualification re-run recorded immediately after this document (same `docs/device-results/`
directory, or `PROJECT_STATE.md`, whichever carries the final on-device outcome) for whether this
closes the gap completely.
