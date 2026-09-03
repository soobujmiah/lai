# Redmi Turbo 4 Pro — ChatterUI proves the exact same `ggml-hexagon` LAI uses works here

**Date:** 2026-09-03
**Comparison app:** `com.Vali98.ChatterUI` (React Native + `llama.rn`), versionName `0.8.9-beta9b`
**Follow-up to:** `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md`
(Local Dream/QNN finding, same day). This document adds a second, independent real-app data
point, and it's a more direct one: ChatterUI's Hexagon path is not a separate NPU framework like
QNN — it is the **same upstream `ggml-hexagon` backend LAI itself compiles against**.

## What was checked

ChatterUI was already installed and in active use (owner-driven, live chat session) when
inspected. Its bundled native libraries include multiple `librnllama*` build variants selected at
runtime by CPU/accelerator capability, one of them named
`librnllama_jni_v8_2_dotprod_i8mm_hexagon_opencl.so` — a llama.cpp build with `ggml-hexagon`
(and OpenCL) compiled in, the same upstream component LAI's `runtime/llama` module builds.

## Result: real, successful FastRPC session — not a hang, not "no device found"

Live logcat during model load (`RNLlama` tag):

```text
RNLlama: Using /data/user/0/com.Vali98.ChatterUI/app_rnllama-htp for HTP libraries
RNLlama: Extracted HTP library: libggml-htp-v69.so
RNLlama: Extracted HTP library: libggml-htp-v73.so
RNLlama: Extracted HTP library: libggml-htp-v75.so
RNLlama: Extracted HTP library: libggml-htp-v79.so
RNLlama: Extracted HTP library: libggml-htp-v81.so
RNLlama: Set ADSP_LIBRARY_PATH=/data/user/0/com.Vali98.ChatterUI/app_rnllama-htp

nativeloader: Load .../librnllama_jni_v8_2_dotprod_i8mm_hexagon_opencl.so ... ok
nativeloader: Load .../librnllama.so ... ok

apps_std_imp.c:382: apps_std_fopen_fd done for .../app_rnllama-htp/./libggml-htp-v73.so
  with fopen:23us, read:94us, rpc_alloc:124us, mmap:557us fd 0xf0 error_code 0x0
apps_std_imp.c:1160: Successfully opened file .../app_rnllama-htp/./libggml-htp-v73.so

fastrpc_apps_user.c:1828: remote_handle64_open: opened handle 0xb4000076742cbc10
  (remote 0x2d8740) for file:///libggml-htp-v73.so?htp_iface_skel_handle_invoke
  &_modver=1.0&_dom=cdsp&_session=0 on domain 3 (spawn time 39147 us, load time 12150 us), refs 1
```

Total elapsed from library load to open handle: ~14 seconds. This is the exact skel filename
(`libggml-htp-v73.so`) and the exact same upstream code path LAI's `hexagon_backend.cpp` /
`find_htp_device()` tries and gets nothing from — here it succeeds, in a real sandboxed
`untrusted_app`, on this same device.

Live generation was already in progress (owner actively chatting). Reading the app's own UI
directly (not logcat, which doesn't carry this metric): **`Prompt: 48.80 t/s · Text Gen: 21.36
t/s`**. Decode is meaningfully faster than LAI's CPU-only baseline (8-15 tok/s) — consistent
with, though not definitive proof of, real accelerator engagement; the FastRPC handle success
above is the stronger evidence for that specifically.

## The one concrete difference found

Android's `nativeloader` log line when ChatterUI's `librnllama_jni_v8_2_dotprod_i8mm_hexagon_opencl.so`
loads:

```text
nativeloader: Configuring clns-11 for other apk .../com.Vali98.ChatterUI-.../base.apk.
  target_sdk_version=36, uses_libraries=libOpenCL.so:libcdsprpc.so, ...
```

`uses_libraries=libOpenCL.so:libcdsprpc.so` is Android's own report of that app's declared
`<uses-native-library>` manifest entries. ChatterUI requests the app-namespace bridge for
**`libcdsprpc.so`** (the FastRPC client library) — LAI's `AndroidManifest.xml` has never
requested this. LAI's only prior `<uses-native-library>` attempt was for `libOpenCL.so`
specifically, which caused a real hang on this device and was reverted (`82949bb`,
`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md`) — a different
vendor library with its own, separately-confirmed bad outcome. `libcdsprpc.so` has no such
evidence against it, and ChatterUI's success is a positive signal specifically for this library.

## Action taken

Added `<uses-native-library android:name="libcdsprpc.so" android:required="false" />` to
`app/src/main/AndroidManifest.xml`, independent of (not alongside) OpenCL's reverted entry.
`required="false"` keeps this a no-op wherever the library or the API-31+ mechanism is
unavailable — no install/launch risk, CPU/Vulkan paths unaffected either way. This is a
qualification step, not a claimed fix — see the follow-up entry in this same `docs/device-results/`
directory (or `PROJECT_STATE.md`) for the actual on-device requalification outcome.
