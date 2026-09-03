# Redmi Turbo 4 Pro — found why the HTP skel `.so` files were never produced at all

**Date:** 2026-09-03
**Follow-up to:** `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-session-open-diagnosis.md`
(the `ADSP_LIBRARY_PATH` + `jniLibs.useLegacyPackaging` fix). That fix was necessary but not
sufficient: it correctly wires up how the DSP-side loader *would* find the skel files, but the
build was never producing `libggml-htp-v73.so` (or v75/v79/v81) in the first place, on any build
attempted, cached or not.

## Method

Two prior hypotheses were ruled out with real evidence before this one:
1. **Gradle build-cache staleness** — ruled out by forcing `--no-build-cache`; zero skels either
   way (CI run `33786227486`).
2. **CMake configure never reaching `add_subdirectory(ggml-hexagon)`** — this was actually
   unfalsifiable with the tooling used so far, because AGP's `externalNativeBuild` CMake
   integration suppresses essentially all of CMake's own configure/build stdout by default, even
   on success. Confirmed by finding a separate, standalone CMake invocation elsewhere in the same
   log (the OpenCL ICD loader's own build, run directly via shell, outside Gradle's control) that
   DOES print `-- Configuring done` / `-- Generating done`, while LAI's own CMake configure never
   has, in any build so far. So the absence of Hexagon-related `message()` output was inconclusive,
   not evidence either way.

Added `--info` to the `gradle :app:assembleRelease` invocation (CI run `33787760131`) to force
AGP to print its own native-build target evaluation. That surfaced the real cause directly.

## Root cause

```text
C/C++: executing build commands for targets that produce .so files or executables
C/C++: evaluate library ggml (arm64-v8a)
C/C++: not building target library ggml because static libraries are not build by default.
C/C++: evaluate library htp-v73 (arm64-v8a)
C/C++: not building target htp-v73 because no targets are specified and library build output file is null
C/C++: evaluate library ggml-hexagon (arm64-v8a)
C/C++: not building target library ggml-hexagon because static libraries are not build by default.
C/C++: evaluate library htp-v75 (arm64-v8a)
C/C++: not building target htp-v75 because no targets are specified and library build output file is null
C/C++: evaluate library htp-v81 (arm64-v8a)
C/C++: not building target htp-v81 because no targets are specified and library build output file is null
C/C++: evaluate library lai_runtime (arm64-v8a)
C/C++: building target library lai_runtime because no targets are specified.
```

CMake's own configure step was fine all along — `ggml-hexagon`'s `CMakeLists.txt` **does** define
all four HTP skel targets (`htp-v73`, `htp-v75`, `htp-v79`, `htp-v81`). The problem is entirely on
the Gradle/AGP side: `externalNativeBuild.cmake.targets` (`runtime/llama/build.gradle.kts`) was
never set, and AGP's default behavior when that list is empty is to build **only** whatever is
needed to produce the module's own declared `.so` output (`lai_runtime`) — every static library
that `lai_runtime` links against (`ggml`, `ggml-hexagon`, `llama`, `ggml-base`, `ggml-vulkan`,
`ggml-opencl`) gets pulled in for free as a normal CMake link dependency, so those "not building
target library X because static libraries are not build by default" lines are harmless noise.

The four HTP skel `.so` files are different in kind: they are **standalone DSP-side shared
objects**, meant to be loaded by the Hexagon FastRPC skel loader (via `ADSP_LIBRARY_PATH`) as a
completely separate execution context, not linked into `lai_runtime` at all. Because nothing in
the ARM64 link graph depends on them, AGP's "only build what the module's own `.so` needs"
heuristic silently drops them — with no warning, no error, just a `C/C++: not building target …`
line, which was invisible without `--info`.

This is not a llama.cpp/ggml-hexagon bug at all; it's a standard AGP CMake integration pitfall for
any target that produces a `.so` that isn't itself a link dependency (plugins, DSP skels, tool
helpers). Same explanation also accounts for `qidlTargethtp_ifaceVYExy`, `vulkan-shaders-gen`, and
`htp_iface` all being skipped — none of them are link dependencies of `lai_runtime` either, but
`htp_iface`'s actual object code IS already linked into `ggml-hexagon`/`lai_runtime` via a
different target (its stub `.c` file compiles as part of the normal build — confirmed via compiler
warnings for `htp_iface_stub.c` present in every build log), so it was never actually missing.

## Fix applied

`runtime/llama/build.gradle.kts`, inside `defaultConfig.externalNativeBuild.cmake`:

```kotlin
targets += listOf("lai_runtime", "htp-v73", "htp-v75", "htp-v79", "htp-v81")
```

Explicitly telling AGP which extra `.so` targets to build, beyond the one it infers from the
module's own output. `lai_runtime` is listed too since setting `targets` at all switches AGP from
"infer automatically" to "build exactly this list" — omitting it would have silently dropped the
app's own runtime library.

Also reverted the now-unneeded `--info` flag on the `:app:assembleRelease` invocation
(`.github/workflows/android_build.yml`) — it served its purpose (finding this root cause) and just
adds log noise going forward.

## Result

Pending: next CI build with this fix, followed by inventory confirmation that
`libggml-htp-v73.so` (and v75/v79/v81) now appear in the CXX build output / final APK, followed by
a real on-device `scripts/device/lai_adb.sh qualify` run for `llama-hexagon`. Recorded in a
follow-up document once run.
