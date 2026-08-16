# Redmi Turbo 4 Pro — Phase 2A chat regression report

Test date: 2026-08-17 (05:05 Asia/Dhaka)
Tested build: `0.6.78-debug` (versionCode 78), debug-signed
Result source: user screenshot + exported `lai-diagnostics-v1.json`
Fix commit: `e4ad398` (CI run [`31979369975`](https://github.com/soobujmiah/lai/actions/runs/31979369975) — green)

## Device

| Field | Value |
|---|---|
| Model | Xiaomi 25053RT47C (Redmi Turbo 4 Pro) |
| SoC | QTI SM8735 (Snapdragon 8s Gen 4), 8 cores |
| Android SDK | 36 |
| ABI | arm64-v8a |
| Free memory at export | 3.85 GB |
| Battery | 37 %, discharging |
| Shizuku | READY (UID 2000) |

## Reported symptoms

1. Device became noticeably hot during chat.
2. Chat never replied — the assistant bubble stayed empty.
3. **Stop** stuck on "Stopping…" and never recovered.
4. Keyboard pushed the layout so the app overlapped the status bar.
5. No back affordance in Settings — leaving required pressing the same **Settings** button.

## Evidence

Diagnostics were exported at 05:09:56, roughly four minutes after the screenshot.

| Field | Value | What it means |
|---|---|---|
| `models[0].active` | `false` | Qwen was installed but **not loaded** at export time |
| `activeBackendDecision` | `"No model has been scheduled"` | scheduler held no decision |
| `performance` | `[]` | **no generation ever completed** |
| `thermalState` | `NOMINAL` | measured after the CPU had gone idle — not proof heat was fine during the run |
| `nativeLibraryLoaded` | `true` | the native path was available |
| `trimmedConversationTurns` | `0` | context trimming was not involved |

The screenshot shows a user bubble (`হাই`) followed by an **empty** assistant bubble. That bubble
is only created on the generation path — the "install a model" / "load the model" guidance path
appends explanatory text instead. So inference **did** start and produced zero tokens, and the
model was released some time between the screenshot and the export.

## Root causes and fixes

| # | Cause | Fix |
|---|---|---|
| 1 | CPU backend used `hardware_concurrency - 2` decode threads and **all 8** cores for prompt batches. On a 1+3+4 big.LITTLE SoC this saturates the little cores; they finish late, every batch waits on the slowest core, and the whole package heats. | Half the cores, clamped 2–4, for both decode and batch. Decode on a 1.5B Q4_K_M is memory-bandwidth bound before it is core bound, so the throughput cost is small. |
| 2 | `Job.cancel()` only sets a flag. A coroutine blocked in a **non-suspending JNI call** (`llama_decode`, `countTokens`) never observes it, so the `catch` that restores the UI never ran and `CANCELLING` was terminal. | Watchdog joins the job and, if native has not yielded within 4 s, releases the engine and returns control to the user. Prompt batches also 512 → 128 tokens so cancellation is seen sooner. |
| 3 | A stopped/failed reply left a blank placeholder bubble, which reads as a broken app. | Trailing empty assistant message is dropped. |
| 4 | Nothing re-checked thermal state **during** a chat; the scheduler only gated model load. | `sendMessage` refuses at `SEVERE`+ with a bilingual explanation. |
| 5 | Edge-to-edge `Scaffold` kept reserving the status-bar inset while the IME was up. | `contentWindowInsets`/top-bar insets set to `safeDrawing`; composer gets `imePadding()`. |
| 6 | Settings had no exit affordance. | Real back arrow (`material-icons-core`, already on the classpath), redundant action hidden, `BackHandler` wired for system back. |

## Still unresolved — for the next device run

**Why the generation produced zero tokens, and why the model was unloaded.** Two candidates, not
yet distinguishable from the available evidence:

- **Memory pressure.** `AppContainer.onTrimMemory` closes the engine and emits
  `ModelUnloadedForMemory`. If that fired, the notice was overwritten by the stuck-cancel state
  before the user saw it. 3.85 GB free at export does not rule this out — the export is post-hoc.
- **A native generation failure** whose `InferenceEvent.Failed` was masked by the same stuck state.

Fix 2 makes both cases recoverable and visible, but the underlying cause needs confirmation.

### What to capture next time

1. Load the model, send one short message, and **immediately** export diagnostics — before pressing
   Stop and before backgrounding the app.
2. If it happens again, capture `adb logcat -s LAI-llama:* AndroidRuntime:*` during the reply.
3. Note whether the assistant bubble stays empty or shows an "Inference failed: …" message — with
   fix 3 in place, a failure now surfaces text instead of a blank bubble.

## Retest checklist for `e4ad398`

- [ ] Sustained chat no longer heats the device to the same degree.
- [ ] **Stop** always returns to a usable chat within ~4 s, worst case with an explicit
      "model was released" message.
- [ ] No empty grey bubble remains after Stop or a failure.
- [ ] Keyboard no longer pushes the app into the status bar; composer sits directly above the IME.
- [ ] Settings shows a back arrow, and system back also leaves Settings.
- [ ] Phase 2A acceptance list in `PROJECT_STATE.md` §4.1 (still outstanding).
