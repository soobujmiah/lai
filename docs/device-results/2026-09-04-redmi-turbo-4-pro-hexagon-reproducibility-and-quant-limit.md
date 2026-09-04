# Redmi Turbo 4 Pro — `llama-hexagon`: reproducibility confirmed, Q4_K quant not viable

**Date:** 2026-09-04 (follow-up to `2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md`)
**Package:** `dev.lai.runtime` (release), same installed build as the first PASS: `lai-release-358`
from CI run `33812084740`, HEAD `9ef2edd`.

## 1. Reproducibility — confirmed

`scripts/device/lai_adb.sh qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon` was run a second time
against the same installed APK.

**Result:** `DONE` — script exit 0. `LOAD_OK` → generation → `DONE`/`READY`, decode 6.50 tok/s,
prefill 36.09 tok/s, ttft 2758 ms (first run was 5.36/33.36/2984 ms — consistent, same order of
magnitude).

Pulled the full unfiltered, pid-scoped logcat again (not just the `LAI-*` tag filter) and found the
same class of unfakeable vendor evidence as the first PASS: `libcdsprpc.so` loaded, a real
`remote_handle64_open` FastRPC handle for `libggml-htp-v73.so` (`apps_std_fopen_fd` succeeding,
`fd 0x96 error_code 0x0`), a genuine `HTP0` session (`session-id 0 domain-id 3`), live `HTP0
hwinfo` query (`threads 4, hvx 4, hmx 1, vtcm 8 MB`), and the same `HTP0 op batching` config
(`n-ops 1024`). This is a second independent real DSP engagement, not a fluke — the first PASS is
reproducible.

## 2. Second catalog model — not applicable, not a retry target

The handoff note (`PROJECT_STATE.md`, 2026-09-04) said to "try the second catalog model
(`qwen2.5-1.5b-instruct-q4-k-m`) on `llama-hexagon`." This was based on a premise that turns out to
be wrong: **there is no second Hexagon-compatible model in the current catalog.**

`catalog/models-v1.json`'s own `qwen2.5-1.5b-instruct-q4-0` entry documents why:

> "quantized Q4_0 instead of Q4_K_M specifically to qualify llama-hexagon (NPU): ggml-hexagon's
> MUL_MAT kernel only accepts Q4_0/Q4_1/Q8_0/IQ4_NL, not Q4_K."

Consistent with that, `qwen2.5-1.5b-instruct-q4-k-m`'s `compatibleBackendIds` in the same catalog
file is `[llama-cpu, llama-vulkan, llama-opencl]` — `llama-hexagon` was never declared compatible
for this file. Running the qualify command against it anyway was attempting an invalid combination,
not a real second test of the Hexagon path.

**Correction to the handoff note:** this open item should be closed as N/A, not left pending for a
future session to retry. `qwen2.5-1.5b-instruct-q4-0` remains the only catalog model that can
exercise `llama-hexagon`, by ggml-hexagon's own kernel-format constraint.

## 3. What actually happened when the invalid combination was run (environmental, not a code bug)

Running `qualify qwen2.5-1.5b-instruct-q4-k-m llama-hexagon` anyway (to see how it failed) did not
produce a clean app-level rejection. Sequence observed:

- `LAI-qualify: STARTING ... loadTimeoutMs=45000` at t+0.
- The qualification coroutine's first step (`runBackendQualification` in `MainViewModel.kt`) waits
  for the model id to appear in `installedModels`, bounded by the same 45s budget, before ever
  calling `loadModel()`. No `LAI-model: Loading model id=...` line ever appeared, meaning that wait
  never resolved before the process was interrupted — separate from the Q4_K kernel-support
  question, since the wait is purely on catalog/workspace state, not backend compatibility.
- At t+~29s, system logs (`ActivityManager`/`WindowManager`, pid 3250, not the app's own pid) show
  `MiuiFreeFormGestureController: deliverResultForFinishActivity ... resultFrom: ActivityRecord{...
  MainActivity ...}`, immediately followed by `VRI[MainActivity]: visibilityChanged ... false` and
  `LAI-lifecycle: MainActivity onDestroy` — the activity was finished by MIUI's window-management
  layer, not by app code (no crash, no exception, no `LAI-qualify` terminal state logged) and not by
  `scripts/device/lai_adb.sh` (it only issues one `am start -W`, no follow-up `am force-stop` during
  the wait). Device was awake, screen on, `always_finish_activities` is `0` (off) — this was not a
  screen-lock or a "don't keep activities" dev-option artifact.
- Earlier in the same launch, `ANDR-PWR-OPT: GAMEPOWEROPT: isGame() 348: dev.lai.runtime is Game =
  1` — MIUI's power-optimization layer classified this app as a "Game" (likely from its fullscreen/
  immersive Compose rendering pipeline), which is the same code path `MiuiFreeFormGestureController`
  belongs to. This is circumstantial, not a confirmed causal link.
- Net: `lai_adb.sh` correctly reported this as its worst-case outcome — exit 3, "no terminal state
  at all" — which is the right classification for "the app never got a chance to report anything,"
  distinct from `LOAD_TIMEOUT`/`LOAD_FAILED`/`MODEL_NOT_FOUND`.

## Conclusions

- **Hexagon HTP PASS is reproducible** — two independent real-DSP passes now, not a single
  fluke. `PROJECT_STATE.md`'s "single run, not yet repeated" caveat is resolved.
- **The "try a second model" next step is closed as N/A**, not deferred — no second
  Hexagon-compatible model exists in the catalog today. A future model added specifically in
  Q4_0/Q4_1/Q8_0/IQ4_NL would be a real second data point; `q4-k-m` is not.
- **The MIUI activity-finish behavior is a noted device-testing hazard**, not a Hexagon-path bug:
  if a future `qualify` run against a *validly* Hexagon-compatible model also gets cut short this
  way, don't mistake it for a native/vendor hang — check for `MiuiFreeFormGestureController` /
  `ActivityManager` activity-finish log lines (system pid, not the app pid) before concluding the
  backend itself is at fault. Not chased further here since finding #2 already fully explains why
  this specific run was never going to succeed regardless.
