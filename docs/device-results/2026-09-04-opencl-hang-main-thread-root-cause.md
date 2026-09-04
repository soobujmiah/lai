# OpenCL app-launch hang — root cause: eager backend probe on the main thread

**Date:** 2026-09-04
**Follow-up to:**
`docs/device-results/2026-09-03-redmi-turbo-4-pro-opencl-namespace-hang.md` (the original device
evidence: adding `<uses-native-library android:name="libOpenCL.so">` made app launch hang
indefinitely instead of the previous instant "library not found") and
`docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`
(ChatterUI, a comparable app, runs the equivalent native calls off the main thread and does not
hang on this same device). This document closes the gap those two left open — the prior hang doc
inferred the fix from symptoms and precedent; this one traces the actual call chain in LAI's own
source to confirm it. Source-reading only originally; the fix from this document has since been
implemented in the same commit.

## The call chain

1. `InferenceEngine.capabilities` (`core/contracts/.../inference/InferenceEngine.kt`) is a plain
   property in the pure contracts interface — synchronous, not `suspend`.
2. Its implementation, `NativeInferenceEngine.capabilities`
   (`runtime/llama/src/main/kotlin/dev/lai/runtime/inference/NativeInferenceEngine.kt:106`), is a
   Kotlin `by lazy { ... }` property (default `LazyThreadSafetyMode.SYNCHRONIZED`). The lazy block
   calls `NativeBindings.runtimeInfo()` — a synchronous JNI call — which internally runs
   `create_backends()` and `available()` on every compiled backend. For OpenCL specifically,
   `OpenCLBackend::available()` (`runtime/llama/src/main/cpp/opencl_backend.cpp:98-119`) calls
   `dlopen("libOpenCL.so")` and `initialize_llama_once()` (`llama_session.cpp:206`, which itself
   calls `llama_backend_init()` + `ggml_backend_load_all()`), then `find_opencl_device()`. None of
   this has a timeout or cancellation point — it is a normal, uninterruptible native call chain.
3. **`by lazy`'s synchronization means only the very first access anywhere in the process actually
   runs that chain; every later access (any thread) blocks on the same result** — so where that
   first access happens, and on what thread, is the entire question.
4. Before this fix, the first access was `MainViewModel`'s `_state` property initializer
   (`app/src/main/java/dev/lai/runtime/ui/MainViewModel.kt`, previously
   `runtimeDetail = container.inferenceEngine.capabilities.detail` directly in the
   `MutableStateFlow(MainUiState(...))` constructor call). This is a plain property initializer,
   not inside `init {}`, not inside a coroutine — it runs synchronously as part of constructing the
   `MainViewModel` instance.
5. `MainViewModel` is constructed via the standard Android `viewModels()` delegate, on first access
   from `MainActivity`/Compose `setContent {}` — during `MainActivity.onCreate()`, on the **main/UI
   thread**, before the first frame is drawn.

So: the eager OpenCL (and Vulkan, and Hexagon) availability probe ran synchronously on the main
thread as an unavoidable side effect of the app starting at all — every time, on every launch,
regardless of whether `capabilities` was ever actually needed yet. When the previously-instant
`dlopen("libOpenCL.so")` failure became a real, slow-or-blocking vendor call after the
`uses-native-library` manifest change, this is exactly the code path that froze `MainActivity`
before it could draw its first frame — matching the observed symptom precisely (main thread state
`S`, no crash, no forward progress, 45+ second observation window in the original hang doc).

## Two more call sites with the same latent bug

Reading `capabilities` synchronously is only safe *after* the above warm-up has completed. Two
other places in `MainViewModel.kt` read it via `viewModelScope.launch { ... }`, whose default
dispatcher is `Dispatchers.Main.immediate`, not a background thread:

- `runBackendProbe()` — the ADB `probe` diagnostic command, whose own KDoc comment claimed "reading
  capabilities is already always safe." It wasn't: if this probe is exactly the tool used to check
  a suspected hang, and it itself blocks the main thread while doing so, it defeats its own purpose
  (and hangs the app it's meant to be diagnosing without hanging).
- `loadModel()` — reads `capabilities` (for scheduler evidence) before its own call to
  `InferenceEngine.load()`, which internally does switch to `Dispatchers.IO`, but only *after* the
  `capabilities` read that precedes it.
- `exportDiagnostics()` → `buildDiagnosticsReport()` — reads `capabilities` before the existing
  `withContext(Dispatchers.IO)` that wraps only the file-write step.

All were "safe in practice" only as an accident of the (buggy) eager main-thread warm-up already
having memoized `capabilities` by the time a user could reach them.

## The fix (implemented, same commit as this doc)

`MainViewModel.kt`:

- `_state`'s initializer no longer touches `capabilities` at all; `runtimeDetail` starts as a
  placeholder (`"Probing native runtime…"`).
- `init {}` now launches `viewModelScope.launch(Dispatchers.IO) { ... }` to read `capabilities` off
  the main thread and update `_state.runtimeDetail` once resolved (or with an error message on
  failure). This is the actual warm-up; everything downstream benefits from `by lazy`'s memoization
  without needing to know about threading itself.
- `runBackendProbe()` and `loadModel()`'s coroutine now explicitly launch on `Dispatchers.IO`.
- `exportDiagnostics()` now builds the diagnostics report (which reads `capabilities`) inside the
  same `withContext(Dispatchers.IO)` block that already wrapped the file write, instead of before
  it.

## What this does and does not fix

- **Does fix:** the app can no longer hang before drawing its first frame due to a slow/blocking
  vendor backend-enumeration call, on any of the paths audited above. This directly enables safely
  re-attempting the `<uses-native-library android:name="libOpenCL.so">` manifest change that was
  reverted in the original hang doc — that attempt is still not made in this commit (device
  re-validation is a separate step, not done here).
- **Does not fix:** a genuinely stuck vendor call still permanently blocks whichever background
  thread first touches `capabilities` — Kotlin coroutine cancellation cannot interrupt a blocked JNI
  call, the same accepted, already-documented cost as `runBackendQualification`'s `LOAD_TIMEOUT`
  handling. If the vendor call never returns, any *later* access to `capabilities` (e.g. a user
  tapping "Load" after the warm-up itself got stuck) will also block — but only that specific
  action, not the whole app at launch.
- **Does not re-validate OpenCL on-device.** This commit is a source-level fix based on tracing the
  actual call chain and the ChatterUI comparison; it has not yet been exercised with the
  `uses-native-library` manifest change re-applied and tested on the Redmi Turbo 4 Pro. That's the
  next step, not this one.
