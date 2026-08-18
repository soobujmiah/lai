# Diagnostic logging

LAI's centralized diagnostic logger makes signed/release builds debuggable on a real device
without adb/root, while keeping the debug build as verbose as before.

## Where the implementation lives

| Piece | Location | Notes |
|---|---|---|
| Centralized logger | `app/src/main/java/dev/lai/runtime/core/LaiLog.kt` | `object LaiLog`; mirrors every call to Android Logcat + a bounded app-private log file + a bounded in-memory ring buffer (included in the diagnostics export). |
| Redactor | `app/src/main/java/dev/lai/runtime/core/LaiLogRedactor.kt` | Pure JVM; last line of defense — every line passes through it before it can reach logcat or the file. Unit-tested in `app/src/test/.../LaiLogRedactorTest.kt`. |
| Startup wiring | `LaiApplication.onCreate` | Picks the level from the build type, writes the context header, installs a crash handler that logs fatal stack traces. |
| Key event call sites | `MainViewModel`, `MainActivity` | Subsystem events: permissions, model load/unload/delete, generation start/completed/failed, downloads, tool approval/execution, audit integrity, workspace grant, developer mode, activity lifecycle, memory pressure. |
| Native runtime | `runtime/llama/src/main/cpp/*` | The C++ layer logs to logcat under the `LAI-llama` tag (stall tracing, thermal thread changes, Vulkan probe). `initialize_llama_once` also redirects `std::cerr` to `LAI-llama` so ggml-vulkan hard errors (e.g. `Compute pipeline creation failed for <name>`) appear in logcat instead of being dropped to /dev/null. |

Levels are `DEBUG < INFO < WARN < ERROR`.

## Debug vs signed release

| Build | `LaiLog` minimum level | Rationale |
|---|---|---|
| **debug** (`BuildConfig.DEBUG == true`) | `DEBUG` | Maximum useful diagnostic information: lifecycle, permission changes, every state transition. |
| **release / signed** | `INFO` | Production diagnostics without noise: model load, backend selection, generation outcomes, downloads, tool execution, permission failures, audit failures, memory pressure, uncaught crashes. `WARN`/`ERROR` (with stack traces) always pass. |

The level is chosen once in `LaiApplication.onCreate` from `BuildConfig.DEBUG`. There is no
runtime toggle; a future `-Plai.logLevel` Gradle property would slot into `LaiLog.Level.parse`.

The native `LAI-llama` logcat output is independent of the Kotlin level and is present in both
build types (Android filters logcat by priority at the OS level).

## Retrieving logs from a signed build

1. **In-app export (no adb needed):** Settings → Developer Mode → **Support diagnostics** →
   **Export diagnostic log**. Android's document picker lets you save `lai-diagnostic-log.txt`
   anywhere (Files, Drive, email…) — or share it directly. Every line is redacted by
   `LaiLogRedactor` before it is written.
2. **Logcat:** `adb logcat -s LAI-llm LAI-model LAI-app LAI-lifecycle LAI-perms LAI-workspace
   LAI-download LAI-agent LAI-audit LAI-mem LAI-crash LAI-llama` (add `*:V` for full verbosity
   on a debug build).
3. **Log file on device:** the logger writes to the app-specific external directory
   (`/sdcard/Android/data/dev.lai.runtime*/files/logs/lai-<buildtype>.log`), so `adb pull`
   works without root on both build types. It is rotated at 512 KiB (`lai-<buildtype>.log.1`).

## What is intentionally logged

- App/process start, activity lifecycle, developer-mode toggles, diagnostics exports.
- Accessibility and Shizuku authority connection changes; workspace grant/revoke.
- Model load/unload/delete (model id, selected backend, load time), generation
  start/completed/failed (token counts, tok/s, TTFT, totals — never content), downloads
  (model id + state + failure reason), memory-pressure model unloads.
- Tool proposal approval/denial and execution results (tool name + coarse outcome — never
  arguments or outputs), audit integrity failures.
- Uncaught exceptions with full stack traces (via `Thread.setDefaultUncaughtExceptionHandler`).
- A context header at the top of the log file: LAI version/version code, build type,
  Android SDK, device manufacturer/model, supported ABIs.

## What is intentionally excluded

- Prompt text, generated text, screenshots, OCR text, accessibility trees, foreground
  packages, documents, tool arguments/outputs, audit hashes, typed automation text.
- Credentials: tokens, API keys, passwords, `Authorization` headers, private keys. The
  redactor strips these even if a call site accidentally includes them; the diagnostics export
  additionally lists them in `privacy.excludedData`.
- File contents and arbitrary paths are never logged by call sites; the logger does not
  capture logs from other processes or the system.

## Release mapping / symbol files (R8/ProGuard)

Minification and resource shrinking stay **enabled** for release — this feature does not weaken
them. To keep release crashes debuggable:

- `app/proguard-rules.pro` keeps `SourceFile` and `LineNumberTable` attributes, so release
  stack traces carry real line numbers and can be mapped back to source.
- R8 emits `app/build/outputs/mapping/release/mapping.txt`; the CI workflow now uploads it as
  the `lai-release-mapping-<run>` artifact (retention 90 days) whenever a release APK is built.
- To de-obfuscate a crash from the field: download the matching `lai-release-mapping-<run>`
  artifact and the release APK's `mapping.txt`, then use Android Studio's **Build → Analyze
  Stack Trace** or `retrace.sh` with the mapping file. Match the mapping artifact to the same
  CI run number as the installed APK.

## How to add diagnostic logging

1. Prefer an existing subsystem tag: `LAI-llm`, `LAI-model`, `LAI-download`, `LAI-agent`,
   `LAI-audit`, `LAI-perms`, `LAI-workspace`, `LAI-mem`, `LAI-lifecycle`, `LAI-app`, `LAI-crash`.
2. `LaiLog.i("LAI-model", "Model loaded id=$modelId backend=$backend")` for an important event;
   `LaiLog.e("LAI-model", "Load failed: ${error.message}", error)` for failures (include the
   throwable so the stack trace is captured).
3. Use `w` for recoverable problems and `e` for failures/errors. Reserve `d` for debug-only detail.
4. Never log user content or secrets; the redactor is a safety net, not a license to be sloppy.
5. Follow the documentation-with-code rule: update this file and `docs/DIAGNOSTICS_EXPORT.md`
   when the log vocabulary or the export schema changes.
