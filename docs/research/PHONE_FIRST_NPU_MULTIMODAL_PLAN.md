# Phone-First NPU and Multimodal Research Plan

**Status:** Research plan; does not authorize runtime changes by itself  
**Target:** Redmi Turbo 4 Pro / Snapdragon 8s Gen 4 (SM8735), Android 16, 12 GB RAM  
**Constraint:** No paid GPU compute, dedicated NVIDIA workstation, or cloud-training dependency  
**Date:** 2026-10-10

## Objective

Qualify Qualcomm Hexagon HTP/NPU for local inference while preserving the validated CPU path. Build toward one local-first assistant experience for text, code, images/documents, audio and video. Do not claim that all modalities or frontier-model capability already exist.

## Evidence baseline

The [2026-09-04 HTP qualification report](../device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md) records one physical-device llama-hexagon run using Qwen2.5 1.5B Instruct Q4_0: 256 output tokens, approximately 5.36 tokens/s decode, 33.36 tokens/s prefill, and 2.98 s time to first token. Vendor traces indicated HTP V73 engagement.

Qualification is still experimental: one run, no formal per-operation assignment trace, no repeated thermal/stability/energy qualification, and no clear decode-speed advantage established over CPU. This demonstrates an execution path, not production readiness or proven energy savings. A separate Local Dream QNN image-generation result is not evidence of LLM performance.

## Required work

### 1. Read-only implementation audit
Inspect the backend adapter, model compatibility metadata, scheduler, QAIRT/QNN acquisition and licensing, CI status, and dated device reports. Reconcile stale documents while preserving historical evidence.

### 2. Reproducible CPU-versus-HTP test
Use the same model hash, quantization, prompts, context, generation cap, APK and commit. Record load time, prefill, time to first token, decode rate, output correctness, failures, peak memory, thermal state, battery/power observations, HTP/DSP engagement and per-operation placement where feasible. Repeat runs and document variance. Claim energy improvement only if measured with a defensible method.

### 3. Runtime decision gate
- CPU remains the known-good default and fallback.
- HTP remains experimental until correctness, repeatability, memory and workload-level performance are reviewed.
- Fallbacks must be explicit and observable in generation provenance.
- Keep Qualcomm-specific code inside a replaceable backend adapter.
- Library presence or a successful device handle alone is not qualification.

### 4. Multimodal roadmap
Target one user-facing assistant and a shared language/reasoning core. Prefer a suitable unified multimodal model if license, capability, memory and runtime are verified. Otherwise use internal modality components only as needed, capability-gated and lazily loaded. Qualify in sequence:
1. Text, Bangla and code.
2. Image/document understanding and Bangla OCR.
3. Audio input/STT, then speech output/TTS.
4. Video via bounded frame sampling and temporal summarization.

Do not present a text-only model as multimodal or require all modality components to remain resident in RAM.

### 5. Training boundary
Do not plan to train a frontier model from scratch on the phone. NPU inference capability does not prove gradient-training support. Prefer inference/runtime optimization and legally redistributable pretrained models. Any future fine-tuning experiment requires a compatible training stack, lawful data and model licenses, and measured memory/thermal feasibility.

## Acceptance criteria
- Repeatable physical-device runs with exact commit, artifact and model hashes.
- Output correctness compared with CPU baseline.
- Stability, memory and thermal behavior reviewed.
- Backend attribution and fallback behavior documented.
- Performance and energy claims limited to measured results.
- SDK, runtime and model licensing reviewed.

## Non-goals
- No paid GPU or cloud-training prerequisite.
- Do not commit model weights, SDK binaries, firmware or proprietary libraries without review.
- No autonomous UI testing; follow owner-operated device testing policy.
- No claim of Claude Sonnet parity or a fixed energy-reduction percentage.
- Do not make HTP the production default based on the existing single PASS.

## Evidence
- [2026-09-04 HTP qualification](../device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md)
- [2026-09-03 real HTP path report](../device-results/2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md)
- [Models and backends](../MODELS_AND_BACKENDS.md)
- [Vendor backend strategy](../VENDOR_BACKEND_STRATEGY.md)
