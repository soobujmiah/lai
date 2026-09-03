# NPU/Hexagon ecosystem research (2026-09-03)

**Read this first.** Follows the same-day Hexagon investigation
(`docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`), which fixed a Kotlin startup
race that was masquerading as a native hang, and found — with strong device evidence — that this
device exposes no non-secure `/dev/fastrpc-adsp` node accessible to a third-party app's
`untrusted_app` SELinux domain. This document asks the follow-up question: *do other real Android
apps solve this, and should LAI copy their approach?* It is research only; no LAI architecture
change is proposed or made here.

## Method

Three independent research passes (MLC Chat's actual architecture; how Android apps legitimately
reach Hexagon NPU; other Android local-LLM apps + the GPU compute stack), each citing primary
sources (repo code, issue trackers, AOSP/Google/Qualcomm docs) wherever possible. No new device
testing was performed — this device has no MLC Chat or comparable app installed
(`adb shell pm list packages` confirmed), so binary/APK inspection wasn't possible; findings are
source/docs-based. Every claim below is labeled VERIFIED / STRONGLY SUPPORTED / HYPOTHESIS.

## Findings

**MLC Chat does not use Hexagon NPU on Android.** [VERIFIED — `mlc-ai/mlc-llm` repo/issues]
Its Android backend is OpenCL (TVM-compiled kernels) + CPU fallback; Vulkan is an open feature
request, and Hexagon NPU support is an unimplemented, zero-engagement feature request (#1689,
#2673). A secondary review source claims "verified NPU on Snapdragon 8 Elite" — this conflicts
with the primary-source finding and is unresolved; flagged, not adjudicated. No other surveyed
app (PocketPal AI, Maid, Layla) ships working Hexagon NPU acceleration either; PocketPal's NPU
request (#460) is open and unassigned. Even upstream `ggml-hexagon`'s own "working" benchmarks
are ADB-shell-launched CLI tools, not sandboxed-APK evidence, and its supported-chipset list
does not include SM8735 (8s Gen 4).

**The FastRPC restriction LAI hit is industry-wide, not HyperOS-specific.** [VERIFIED] Google
enforces the identical SELinux restriction on Pixel devices (Check Point security research).
TensorFlow's own Hexagon-delegate documentation states manufacturers intentionally restrict
non-system apps from the DSP, with no supported workaround short of disabling SELinux
enforcement. A public TensorFlow issue (Realme X50 Pro) shows the same failure class one layer
up: `avc: denied` on the *HAL lookup itself* for `vendor.qti.hardware.dsp::IDspService`, meaning
even Qualcomm's "official" QNN integration path — which brokers FastRPC access through a
privileged HAL service rather than the app opening the device node directly — is OEM-sepolicy
gated and fails the same way on other real devices.

**The one AOSP-guaranteed-accessible NPU path is NNAPI, not QNN.** [STRONGLY SUPPORTED] AOSP's
SELinux policy carves out `hal_neuralnetworks_server` as an explicit exception to the rule
blocking untrusted apps from talking to HAL servers. Qualcomm ships an NNAPI driver targeting
Hexagon as part of the OEM software image. This is architecturally different from — and more
robust than — direct QNN/FastRPC integration. Caveat: NNAPI itself is in a deprecation-leaning
state (Google scaled back updatable-NNAPI-driver plans around Android 13), and whether LiteRT's
Qualcomm-QNN delegate (as opposed to its NNAPI delegate) inherits the OEM-gating risk was not
verified — flagged as a HYPOTHESIS needing device testing before being load-bearing.

**LAI's GPU strategy is correct and should not change.** [VERIFIED / STRONGLY SUPPORTED] Vulkan
compute is standard NDK surface, zero vendor-namespace friction — the same reason Maid ships it
successfully on Adreno. OpenCL requires `dlopen`-ing a vendor library bridged only into the
vendor sphal namespace by default, matching LAI's own already-documented OpenCL friction.
LAI's `vkCmdBindPipeline` crash is a known, recurring class of Adreno/Qualcomm Vulkan driver bug
reported independently by multiple llama.cpp users (issues #6395, #8455, discussion #8336), not
an LAI-specific defect.

## Conclusion

No proven Android local-LLM app — MLC Chat included — actually ships working, sandboxed
third-party Hexagon NPU acceleration today. LAI's `ggml-hexagon` failure reflects a real Android
security boundary that every app in this space hits, not an LAI implementation gap. Recommendation:
no runtime replacement, no new NPU integration effort under the current approach. Keep CPU
(working) and Vulkan-first GPU (architecturally correct, blocked only by a known driver bug
class). If NPU acceleration becomes a genuine product priority later, the only path worth a
time-boxed spike is NNAPI/LiteRT — not more direct-FastRPC work — and it would slot in as a
fifth `Backend` behind the existing evidence-gated scheduler with no architectural rework needed.

## What this does not do

No LAI code, backend, or scheduler behavior changed. No new dependency added. This is a
documentation record for the next engineering decision, not a decision itself.
