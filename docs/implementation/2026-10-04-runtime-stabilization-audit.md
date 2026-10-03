# LAI runtime stabilization audit — 2026-10-04

## Scope and source of truth

This record follows the current `fix/cpu-only-device-20261003` source, the installed
`0.1.411` app, scoped 2026-10-03 device logs, the reference-app investigations in
`docs/device-results/`, and read-only inspection of the currently installed
ChatterUI and Local Dream APKs. Source/build success and device correctness are
separate gates. The owner operates all application UI during qualification.

## Runtime map

`LaiApp` → `MainViewModel` → `AppContainer` → `NativeInferenceEngine` → JNI
`native_inference.cpp` → `backend_registry.cpp` → CPU/Vulkan/OpenCL/Hexagon
backend → `llama_session.cpp` → GGUF model, tokenizer, chat template, KV cache,
sampler and streamed token callback. `InferenceScheduler` and the signed model
catalog restrict selection before native load. CPU is the normal default;
accelerators require explicit build evidence flags.

OCR follows `ToolsDashboard` / Screen reader → `MainViewModel` → `AgentRuntime`
→ `AccessibilityGateway.captureScreen()` → `BanglaOcrService` →
`TesseractBanglaOcrEngine` → `OcrResult` JSON. The engine verifies bundled
Apache-2.0 `ben` and `eng` `tessdata_fast` hashes, runs printed-text OCR on a
bitmap, and returns text, line bounds and confidence. This existing OCR pipeline
does not use a GGUF vision encoder, projector or LLM. It has CI source/build
evidence but no recorded real-image device result yet.
The primary printed-text OCR model remains the existing `tessdata_fast` Bengali
plus English pair: it is small, offline, hash-verified, Apache-2.0 and has a CPU
fallback on this phone. It does not meet a llama.cpp/GGUF-vision criterion;
switching to a vision LLM would replace a completed OCR implementation and
requires separate evidence that its recognition quality and memory are better.

## CPU response defect

Installed release 411 loaded both Qwen 2.5 1.5B Q4_K_M and Q4_0 and generated
256 tokens, but the owner-observed replies to `hey` and `hi` were unrelated and
repetitive. The GGUF loader reported Qwen tokenizer metadata and a chat template;
`llama_session.cpp` applies that template and uses the GGUF vocabulary for
tokenization. Earlier source work corrected sampler ordering (penalties before
top-p). Neither model load nor token streaming proves response correctness.

The release 411 log also showed `llama-cpu` with `0/29` GPU weight layers **and**
two context backends plus a roughly 350 MiB `Vulkan0` compute buffer. In the
pinned llama.cpp API, a null `model_params.devices` list permits all registered
devices. Commit `e2fe6c7` sets an explicit CPU-only device list when
`gpu_layers == 0`. This is a confirmed routing defect and a plausible cause of
bad text because the same Adreno Vulkan path crashed during LAI qualification.
The causal connection and coherent output remain unverified until the fixed APK
runs on the device. A deterministic greedy prompt and repeated turn are required.
The native adapter now rejects a missing or unapplicable GGUF chat template
with an actionable error instead of silently serializing role labels in a
generic format. The reviewed Qwen models have template metadata, so this guard
does not itself explain their bad release-411 replies.
The fix commit `e2fe6c7` passed Android CI (push run `37137362566`); a
CPU-only signed release build also passed (dispatch run `37156520247`, artifact
`lai-release-413`). Neither result is a device inference test.

## GPU reference and LAI boundary

ChatterUI `0.8.9-beta9b` previously loaded Qwen Q4_K_M with OpenCL selected
and 28 GPU layers, then generated a reply (89.83 prompt / 16.80 decode tok/s,
versus 44.58 / 11.97 with zero GPU layers). Adreno driver activity supported
offload, although a per-operation trace was not recorded. A fresh read-only ELF
inspection of its installed APK found direct `DT_NEEDED` entries for
`libOpenCL.so` and `libcdsprpc.so` in
`librnllama_v8_2_dotprod_i8mm_hexagon_opencl.so`.

LAI's experimental OpenCL build instead statically links the Khronos ICD loader;
the last device trace enters `clGetPlatformIDs()` and never returns. Direct
vendor-library linking versus static ICD loading is a concrete difference to
test, **not** an established cause. LAI Vulkan previously loaded and prefetched
but crashed at `vkCmdBindPipeline` on first decode. No LAI GPU path meets the
load → offload → inference → correct-output gate, so production builds keep GPU
selection disabled.

## NPU reference and LAI boundary

Local Dream `2.8.1` currently packages `libQnnHtp.so`, `libQnnSystem.so`, and
v68/v69/v73/v75/v79/v81 HTP skel/stub assets. Earlier real-device logs show
QNN DSP device/context creation and a 20-step 512×512 NPU image generation in
5.7 seconds. This proves a QNN-based route for that image workload, not GGUF
LLM compatibility. Its app-private QNN binaries must not be copied into LAI.

LAI already implements a different direct GGUF route through `ggml-hexagon`:
`hexagon_backend.cpp` selects `HTP0`, sets full layer offload for the reviewed
Qwen Q4_0 model, and configures `ADSP_LIBRARY_PATH` for packaged v73 skels.
Two independent September device runs reached `LOAD_OK` → generation → `DONE`,
with Qualcomm FastRPC logs loading `libggml-htp-v73.so` and reporting live HTP0
hardware information. Q4_K_M is incompatible with this backend's matrix-multiply
kernel. These runs establish DSP engagement and generation; the actual reply
content was not captured in the evidence, so output sanity is UNKNOWN. The
normal build does not select Hexagon automatically.

## Current acceptance matrix

| Path | Load | Inference | Correct output | Actual hardware |
| --- | --- | --- | --- | --- |
| CPU, release 411 | PASS | PASS | FAIL (two owner-observed replies) | Vulkan compute buffer allocated despite `llama-cpu` label; per-op execution unknown |
| CPU, `e2fe6c7` | CI PASS; device UNKNOWN | UNKNOWN | UNKNOWN | CPU-only pin implemented; device verification pending |
| OCR, bundled Tesseract | CI PASS; device UNKNOWN | UNKNOWN on a real image | UNKNOWN | CPU intended |
| LAI Vulkan | PASS | FAIL at first decode | UNKNOWN | Adreno observed before crash |
| LAI OpenCL | BLOCKED at `clGetPlatformIDs()` | BLOCKED | UNKNOWN | No LAI offload proof |
| ChatterUI OpenCL reference | PASS | PASS | Content sanity not recorded | Adreno evidence, no per-op trace |
| LAI Hexagon Q4_0 (September qualification) | PASS | PASS twice | UNKNOWN | HTP0 FastRPC/DSP session observed |
| Local Dream QNN image reference | PASS | PASS | Image generated; visual quality not scored | QNN HTP observed |

## Exact remaining gates

1. Install the signed release built from `e2fe6c7` through the owner artifact
   handoff. Confirm one CPU backend and no Vulkan compute buffer; test a simple
   greedy greeting, a factual deterministic prompt, a second turn, malformed
   input, and unload/reload. If output remains wrong, compare the same GGUF,
   prompt, template, seed and sampler settings against ChatterUI/llama.cpp CLI.
2. Owner-test one printed Bangla/English image through the existing OCR app flow;
   capture extracted text, model initialization and timing. Load alone is not OCR
   acceptance.
3. For GPU, test the OpenCL loader/link difference in an isolated qualification
   build before changing production defaults. Require device logs showing actual
   offloaded layers and a sensible repeated reply.
4. For NPU, a new qualification build can repeat the Q4_0 HTP test and record
   reply sanity. Keep QNN context-binary conversion as a separate evaluated path;
   Local Dream's image context is not an LAI LLM artifact.
