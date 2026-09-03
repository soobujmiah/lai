# Redmi Turbo 4 Pro — a real, working third-party Hexagon NPU path exists (corrects the "verified blocker" conclusion)

**Date:** 2026-09-03
**Status supersedes:** `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`'s "verified external
blocker" framing and `docs/HANDOFF-2026-09-03-npu-android-ecosystem-research.md`'s "no proven Android
app... ships working sandboxed third-party Hexagon NPU acceleration" conclusion. Both are corrected by
this document. The underlying device evidence in those documents (no non-secure `/dev/fastrpc-adsp`
node; `ggml-hexagon` finds zero HTP devices; the ecosystem research on MLC Chat and NNAPI) remains
accurate and is not retracted — only the conclusion drawn from it ("therefore NPU is blocked for any
third-party app on this device") is wrong.

## What actually happened

Two real, already-installed third-party apps (`io.github.xororz.localdream` "Local Dream" and
`ai.mlc.mlcchat` "MLC Chat") were launched and exercised end to end on this same physical device,
using the ADB-first methodology, at the owner's direction, specifically to check a claim already
sitting unreconciled in this repo's own history: the 2026-08-20 scoping note stating "Local Dream's
NPU-class Stable Diffusion generation already proved this functionally" (`docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md`).
That note was correct, and the same-day research that concluded NPU was industry-wide blocked should
have re-tested it before generalizing — it did not, and was wrong to state the conclusion as broadly
as it did.

## Local Dream: genuine Hexagon NPU acceleration, confirmed

**Process identity — a normal, unprivileged third-party app, not special:**

```text
$ adb shell ps -Z | grep localdream
u:r:untrusted_app:s0:c250,c258,c512,c768 u0_a762 ... io.github.xororz.localdream
$ adb shell dumpsys package io.github.xororz.localdream | grep installerPackageName
installerPackageName=com.miui.packageinstaller
```

Same SELinux domain as LAI (`untrusted_app`), sideloaded via the ordinary MIUI installer — no
system/privileged/vendor-signed status.

**Real QNN/HTP initialization, from live logcat during model load:**

```text
QnnDsp <I> QnnDevice_create done. device = 0x3. status 0x0
QnnDsp <I> QnnContext_createFromBinary done successfully. context = 0x3
QnnDsp <I> htpPerfInfrastructureCreatePowerConfigId done. status 0x0
QnnDsp <I> htpPerfInfrastructureSetPowerConfig done. status 0x0
BackendService: Runtime files: libQnnHtp.so, libQnnHtpV68.so, ..., libQnnHtpV73.so,
  libQnnHtpV73Skel.so, libQnnHtpV73Stub.so, ..., libQnnSystem.so
```

The `libQnnHtpV73*` files match this device's already-confirmed HTP architecture (v73). These are
**not** loaded from `/vendor/` — they are the app's own bundled/downloaded "Runtime files," loaded from
its own private storage, which is why they never need vendor-namespace bridging at all.

**A real generation, with the app's own backend label:**

```text
steps: 20 / CFG: 7.0 / seed: 3674045236
size: 512x512 / time: 5.7s / NPU
```

5.7 s for a 20-step 512×512 Stable Diffusion generation is consistent with real accelerator throughput,
not CPU (which would be far slower at this resolution/step count on this SoC).

**The critical nuance — it hits the same SELinux wall LAI does, and works anyway:**

```text
avc: denied { search } for comm="libstable_diffu" name="/" ... scontext=u:r:untrusted_app:s0:...
  tcontext=u:object_r:adsprpcd_file:s0 tclass=dir app=io.github.xororz.localdream
avc: denied { getattr } for comm="libstable_diffu" path="/vendor/dsp" ... tcontext=u:object_r:adsprpcd_file:s0
```

Local Dream's own native code also gets denied when it probes `/vendor/dsp` directly — the exact
restriction class already documented for LAI's `ggml-hexagon`. **This denial does not stop it.** QNN's
FastRPC session establishment succeeds anyway, almost certainly by falling through to a sanctioned,
Binder/HAL-mediated transport (consistent with the `vendor.qti.hardware.dsp::IDspService` mechanism
identified in the same-day ecosystem research) rather than requiring the denied raw vendor-directory
probe to succeed.

## MLC Chat: re-tested, no NPU evidence — the earlier research conclusion holds for this app specifically

`Llama-3.2-3B-Instruct-q4f16_0-MLC`, already downloaded, run via its normal chat UI:

```text
prefill: 11.2 tok/s, decode: 4.2 tok/s
```

No `QnnDsp`/`libQnnHtp`/Hexagon-related strings anywhere in its logcat across the load-and-generate
window. 4.2 tok/s decode for a 3B model is consistent with GPU(OpenCL)/CPU, not NPU — real Hexagon LLM
decode throughput is markedly faster (see the v79 reference numbers already cited in
`docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md`: ~51.5 tok/s for a 1.24B model on a different, faster
HTP generation). This matches, rather than contradicts, the same-day research finding that MLC-LLM's
Android backend is OpenCL, not NPU — that specific conclusion is not corrected by this document.

## Corrected conclusion

1. **Direct, raw FastRPC device-node access is genuinely restricted** for a third-party `untrusted_app`
   on this device — this part of the original finding stands, confirmed again by Local Dream hitting
   the identical `avc: denied` on `/vendor/dsp`.
2. **That restriction does not block NPU acceleration for a third-party app in general** — Qualcomm's
   real QNN SDK, integrated the way it's meant to be (bundled/downloaded `libQnnHtp*.so` files loaded
   from the app's own storage, real FastRPC session establishment through QNN's own runtime rather than
   a raw device-node open), works and was just observed working, end to end, producing a real image in
   5.7 seconds.
3. **`ggml-hexagon`'s specific approach is what doesn't work here** — not the Android/Qualcomm security
   model in general. It attempts device discovery/session establishment at a lower level than QNN's own
   runtime does, and that lower-level approach hits the wall QNN's own transport routes around.
4. **A real, evidenced path forward exists for LAI** if NPU acceleration is ever prioritized: integrate
   against the actual QNN SDK (matching Local Dream's proven approach) instead of relying on
   `ggml-hexagon`'s current direct-FastRPC implementation. This was previously ranked low ("QNN/LiteRT
   delegate... not recommended without new evidence it behaves differently on this device") — that
   evidence now exists. Re-rank this candidate up.

## What this does not change

CPU and Vulkan-first GPU strategy conclusions are untouched. The specific finding that `ggml-hexagon`
itself needs the raw device node and gets nothing on this device is untouched and still accurate — it's
the *generalization* from that single fact to "NPU is blocked industry-wide for this device" that was
wrong, corrected here with direct counter-evidence.
