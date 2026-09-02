# Session Handoff — Build 1: KleidiAI + Vulkan Warptile Clamp (2026-09-02)

**Read this first.** This session executed "Build 1" from
`docs/HANDOFF-2026-08-20-acceleration-sprint.md` (which remains the authoritative
evidence record for *why* this build exists — Vulkan crash signature, OpenCL
closed-track verdict, NPU door status — none of that is repeated here). This
doc covers only what changed and what's still open.

## What shipped (commit `6066bd2`, pushed to `main`)

1. **`runtime/llama/src/main/cpp/CMakeLists.txt`**: `GGML_CPU_KLEIDIAI` flipped
   `OFF → ON`. Rationale: ChatterUI measured ~28 tok/s on this device/model on
   the same llama.cpp version with KleidiAI on, vs LAI's validated 8–15 tok/s
   with it off.
2. **`scripts/ci/ggml-vulkan-clamp-warptile.patch`** (new): clamps `l_/m_`
   warptile `WM<=BM` for `subgroupSize>64`, targeting the confirmed SIGSEGV in
   `vkCmdBindPipeline` binding `MUL_MAT` on the Adreno 825 driver.
   **Deviation from the 2026-08-20 plan, evidence-based:** that doc named
   upstream PR #25735. This session verified #25735 was superseded by
   **PR #27726**, a more complete fix (also clamps `l_/m_warptile_id` and the
   `_mmqid_int_k` variants that #25735 missed). #27726 was used instead.
   Verified via **real local patch application** (not just diff inspection)
   against the exact pinned commit `ad1de39` — fetched the file at that exact
   commit from GitHub, ran `patch -p1 --forward` for real, confirmed clean
   application standalone and in sequence with the existing
   `ggml-vulkan-skip-mmvq.patch`, both orders.
3. **`scripts/ci/fetch_llama_cpp.sh`**: applies the new patch after the
   existing mmvq-skip patch.

No SDK/NDK/compileSdk/Gradle pins changed. OpenCL work untouched.

## CI results — both qualification artifacts exist

| Artifact | Run | Config | Status |
|---|---|---|---|
| `lai-release-292` | `33606821023` | `validated_accelerators=llama-vulkan` | ✅ success |
| `lai-release-293` | `33608792558` | default (CPU-only) | ✅ success |

Both compiled clean — this confirms **build qualification** for both the
KleidiAI flag and the warptile patch. It does **not** confirm the patch
actually fixes the runtime crash, or that KleidiAI measurably helps — that
needs on-device evidence, which this session could not collect (see below).

## Critical implementation detail for whoever runs the device test

**There is no manual "pick a backend" UI control, and none is needed.**
Traced `InferenceScheduler.selectInternal()` and `MainViewModel.loadModel()`
directly:

- CPU **never** receives `CapabilityEvidence.DEVICE_VALIDATED` in code
  (`MainViewModel.kt` only adds `COMPILED`/`RUNTIME_PROBED` for CPU),
  regardless of what `PROJECT_STATE.md`'s prose vocabulary claims elsewhere.
- Accelerators get `DEVICE_VALIDATED` only when their backend id appears in
  `BuildConfig.VALIDATED_ACCELERATORS` (i.e. the `-Plai.validatedAccelerators`
  build flag).
- The scheduler's primary sort key is
  `compareByDescending { DEVICE_VALIDATED in it.evidence }` — this outranks
  catalog `preferredBackendId` and measured tok/s entirely.

**Consequence:** on `lai-release-292`, an ordinary "load model → send message"
action will **automatically** route through Vulkan. On `lai-release-293`, the
same ordinary action can only route through CPU. No developer-mode toggle,
no long-press, no special flow — just the normal UI, on the right artifact.

The one existing "force CPU" code path
(`MainViewModel.fallbackToCpuAfterAcceleratorFailure`) is a **Kotlin-level**
catch triggered by an accelerator error message — it cannot fire on a raw
native SIGSEGV, which kills the whole process before any Kotlin code runs.
Don't expect it to save you from a hard crash; a hard crash just kills LAI.

## What did NOT get validated this session, and why

Neither artifact was actually exercised. `lai-release-292` was installed and
its task briefly appeared in the window manager (14:33–14:36), but **zero
`LAI-`tagged log lines appeared anywhere** in the logcat buffer — not even the
unconditional `LaiApplication onCreate` line — and the process was not running
when checked. No model load or generation was captured on either build.

Root cause: the device was in a **split-screen / multi-window state** with
another of the user's own active AI agent sessions sharing the display. This
blocked both safe options:
- Screenshots were explicitly ruled out (established earlier this session:
  a screenshot taken mid-investigation captured private chat content from an
  unrelated app before this split-screen state existed at all — a hard
  lesson to not repeat).
- `uiautomator dump` (structural/accessibility-tree read, no visual capture)
  failed twice with `ERROR: could not get idle state` — this happens when
  something on screen keeps animating/rendering, consistent with the other
  session still being active.

Neither blind taps nor guessed coordinates were used. The session ended
`WAITING_FOR_HUMAN` rather than guessing.

## Next steps (ordered)

1. **Get LAI into exclusive full-screen foreground** (not split-screen) —
   this alone will likely unblock `uiautomator dump` for a future agent, or
   just makes manual testing unambiguous.
2. **Test `lai-release-293` first** (CPU/KleidiAI baseline): load Qwen 2.5
   1.5B Q4_K_M, send one message, let it finish. Capture:
   `adb logcat -d -s LAI-llama LAI-model LAI-llm` for the load line
   (`Model loaded id=... backend=llama-cpu in ... ms`) and generation metrics
   (prompt/decode tok/s) from the in-app "Last generation" status card or the
   diagnostics export. Compare against the previous 8–15 tok/s baseline.
3. **Install `lai-release-292`** (`adb install -r`), repeat the same load +
   send. This build's scheduler will select Vulkan automatically — watch for
   `device: pinned offload to 'Vulkan0'` / `offloaded N/29 layers to GPU` on
   success, or a process death + `adb logcat -b crash -d` / `/data/tombstones`
   entry naming `vulkan.adreno.so` / `vkCmdBindPipeline` / `SIGSEGV` on
   failure. **Do not declare Vulkan fixed on compile/install/launch alone —
   only on an actual completed generation with no crash.**
4. **Record the outcome** in `docs/device-results/` (new dated file, following
   the existing convention) regardless of which way it goes. If Vulkan works:
   mark `llama-vulkan` `DEVICE_VALIDATED`, consider a catalog preference
   change. If it still crashes: the warptile clamp lever is now spent: don't
   stack another speculative mitigation without new evidence — the NPU track
   becomes the next priority per the 2026-08-20 doc's own ordering.
5. Update `PROJECT_STATE.md`'s snapshot date/head/CI-run-number — it's
   currently dated 2026-08-19 (`e80bd1e`, CI #172) and is now stale relative
   to `main` (currently `6066bd2`, several sessions ahead: NDK alignment, doc
   policy fix, duplicate-declaration fixes, this Build 1). Not done this
   session — out of scope for a focused build/qualification task, flagged
   here as hygiene debt per the project's own `PROJECT_STATE.md` §4.3.

## Operational notes for the next assistant (don't rediscover)

- GitHub-centric repo: all compilation happens in GitHub Actions; local PRoot
  workstation edits/pushes/watches CI/installs artifacts via adb. No local
  Android/NDK builds.
- A push to `main` auto-triggers CI via the workflow's own trigger config —
  if you also need a specific `workflow_dispatch` input (like
  `validated_accelerators`), dispatch explicitly; don't rely on the
  auto-triggered run, and expect it to get cancelled by the workflow's
  `concurrency: cancel-in-progress: true` group if a dispatch follows closely
  behind a push on the same ref (observed this session: run `33606792020`
  was cancelled this way).
- The user may have another of their own agent sessions active on this same
  device concurrently, sharing the screen. This is expected and authorized —
  do not treat it as anomalous, do not interact with it, and account for it
  as a possible cause of `uiautomator` idle-state failures.
- Before any screenshot: confirm explicitly with the user first if there's
  any chance another app or session might be visible. Prefer
  `uiautomator dump` (text/structural, no visual capture) wherever it's
  viable instead.

Session closed. Everything needed to resume is in this file plus
`docs/HANDOFF-2026-08-20-acceleration-sprint.md`.
