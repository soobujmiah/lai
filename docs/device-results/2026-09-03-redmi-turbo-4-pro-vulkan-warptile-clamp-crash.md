# Redmi Turbo 4 Pro — Vulkan warptile-clamp qualification (Build 1 follow-up)

**Date:** 2026-09-03
**Package:** `dev.lai.runtime` (release, `versionCode=292`, `versionName=0.1.292`)
**Commit under test:** `6066bd2` ("feat: enable KleidiAI and qualify Vulkan warptile clamp"), artifact `lai-release-292` (run `33606821023`, `validated_accelerators=llama-vulkan`)
**Purpose:** complete the on-device validation left open by `docs/HANDOFF-2026-09-02-build1-kleidiai-vulkan-clamp.md` — does upstream PR #27726's warptile clamp fix the confirmed `vkCmdBindPipeline` SIGSEGV on the Adreno 825 driver?

## Device

- Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), codename `onyx`
- QTI SM8735 (Snapdragon 8s Gen 4), Adreno 825, arm64-v8a
- 4679 MB available RAM, 90% battery, thermal NOMINAL at test time

## Model

`qwen2.5-1.5b-instruct-q4_k_m.gguf` (1065 MB, Q4_K_M), auto-imported from the workspace folder.

## Procedure

Driven entirely via `adb` (no non-UI control surface exists for load/send — confirmed by manifest/source inspection: `MainActivity` is the only exported component, no intent extras, no broadcast receivers, no debug service). Model load and message send required UI automation; `uiautomator dump` was unusable for the whole session (`could not get idle state` / `null root node` — root cause traced to a competing always-on-top fullscreen window from the host Termux session, not a device or app defect), so raw `input tap`/`input text`/screenshots were used instead.

1. Enabled Developer Mode in Settings to expose model load controls.
2. Tapped **Load** on the installed Qwen 2.5 1.5B Instruct entry.
3. Sent a short chat message once "Send" enabled.
4. Captured `adb logcat` throughout.

## Result: load succeeded, generation crashed

Load (clean, full GPU offload):

```text
09-03 05:02:34.075 LAI-model: Loading model id=qwen2.5-1.5b-instruct-q4-k-m
09-03 05:02:34.106 LAI-llama: vulkan: device 0 type=2 name='Vulkan0'
09-03 05:02:34.107 LAI-llama: device: pinned offload to 'Vulkan0' (+ CPU for the remainder)
09-03 05:05:35.841 LAI-model: Model loaded id=qwen2.5-1.5b-instruct-q4-k-m backend=llama-vulkan in 1743 ms
```

Generation (prefill completed, first decode crashed):

```text
09-03 05:05:16.480 LAI-llm: Generation start model=qwen2.5-1.5b-instruct-q4-k-m
09-03 05:05:24.666 LAI-llama: generate: prefill done, 93 new of 93 total tokens in 8119804 us (11.5 tok/s)
09-03 05:05:24.678 LAI-llama: generate: first token sampled at 8136814 us from start
09-03 05:05:24.796 LAI-crash: NATIVE CRASH signal=11 (SIGSEGV (invalid memory access))
09-03 05:05:24.802 LAI-crash:   #03 pc=0x173a24 _ZN11qglinternal17vkCmdBindPipelineE...+0x4  [/vendor/lib64/hw/vulkan.adreno.so]
09-03 05:05:24.802 LAI-crash:   #04-#12 ggml_backend_sched_graph_compute_async -> llama_context::graph_compute -> ...::decode -> llama_decode -> Java_dev_lai_runtime_inference_NativeBindings_generate
```

Full native backtrace preserved in the run's logcat capture; crash site is the same `vkCmdBindPipeline` call in `vulkan.adreno.so` as every prior Vulkan crash report on this device.

Process 475 died; Android restarted the app (`LaiApplication onCreate (process start)`, new pid 10407) automatically.

## Verdict

**The warptile clamp (upstream PR #27726) did not fix the crash.** It moved the failure point later — the app now survives model load and the full prefill pass (previously unconfirmed how far earlier attempts got), but still SIGSEGVs in the driver's `vkCmdBindPipeline` on the very first decode step. This is a genuine completed-generation test, not a compile/install/launch-only result — it satisfies the evidence bar the handoff doc set ("only on an actual completed generation with no crash").

Per `docs/HANDOFF-2026-08-20-acceleration-sprint.md`'s own ordering: the warptile-clamp mitigation lever is now spent. Do not stack another speculative Vulkan patch without new upstream evidence that specifically addresses this call site. **NPU/QNN (Phase 3) becomes the next priority track.**

## Correction: no persisted UI hang (retracts this doc's earlier draft claim)

The screenshot taken immediately after the crash showed an empty assistant bubble with **Stop** still active, which this doc's first draft mischaracterized as a "silent hang" surviving the crash/restart. A live recheck of the same device state minutes later, after the process had fully restarted, showed a clean chat screen with no stray message and no stuck button.

Tracing `MainViewModel.persistChat()` explains why: it is only invoked on a definite outcome (reply completed, reply failed, or a canned notice), and it filters out blank-text messages before writing to disk. A native SIGSEGV kills the process before any such Kotlin-level callback can run, so **neither the user's message nor the empty placeholder bubble was ever persisted** — the doc-comment on `persistChat()` states this is intentional ("a process death never loses more than the message currently streaming"). What the earlier screenshot captured was the doomed process's last in-memory frame, not a surviving app-level bug. No UX fix is warranted here; this section is left in the doc as a record of the correction rather than deleted.

## Status transition

`llama-vulkan`: **`Implemented; device qualification pending`** → **`Implemented; device-validated CRASH (not viable on this driver as of PR #27726)`**. Does not change `llama-cpu` (unaffected) or `llama-opencl` (separate track, still pending its own qualification build per `docs/HANDOFF-2026-08-20-acceleration-sprint.md`).
