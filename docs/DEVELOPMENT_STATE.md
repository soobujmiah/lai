# Development State

**Snapshot:** 2026-08-21 — documentation state reconciled after the Redmi Turbo 4 Pro KGSL/app-access qualification evidence.

## Current Phase

**Documentation reconciliation / GPU qualification preparation.** The previous 2026-08-18 snapshot is superseded where it conflicts with the newer 2026-08-21 evidence. No GPU implementation claim is made from device-node access alone.

The repository follows the documentation-first sequence: review the documentation and architecture, reconcile the source-of-truth state, implement only the authorized milestone, test, then document the result.

## Current Status (evidence-based)

| Area | State | Evidence / interpretation |
|---|---|---|
| **CPU inference** | **Device validated; shipped default** | Redmi Turbo 4 Pro / SM8735: Qwen 1.5B Q4_K_M validated at ~20 tok/s reuse with adaptive big-core scheduling. |
| **GPU Vulkan** | **AVAILABLE / experimental; inference NOT validated** | `llama-vulkan` loads the model on Adreno 825, but generation crashes at `vkCmdBindPipeline+0x4` inside Qualcomm `vulkan.adreno.so`. This is a runtime/driver failure, not evidence of missing app access. |
| **GPU OpenCL** | **AVAILABLE at vendor-stack level; inference NOT validated** | Qualcomm OpenCL libraries are present and OpenCL-Z previously showed a healthy Adreno 825 stack, while the normal modern-app ggml-opencl loader path hit an Android linker-namespace boundary. A new native loading path is now explicitly under investigation; do not claim inference support yet. |
| **KGSL** | **AVAILABLE from LAI app identity** | On 2026-08-21 the installed debug package `dev.lai.runtime.debug` successfully opened `/dev/kgsl-3d0` from its Android app context. The observed UID `10675` is installation-specific and is NOT a stable device identifier. This proves device-node access only; it does not prove GPU compute. |
| **NPU QNN/HTP** | **PLANNED / investigation only** | Vendor runtime presence is not sufficient. Requires a real app-side loading and execution qualification path and licensed QAIRT where applicable. |
| **Model system** | **Verified** | Signed catalog, SHA-256 validation, resume/download and workspace model discovery remain the established path. |
| **Tool system** | **One-shot, user-confirmed** | Existing tool proposal validation, confirmation gate and hash-chained audit remain unchanged. |
| **Agent** | **One-shot** | No planner/memory/verification loop yet. |
| **Android automation** | **Device validated** | Accessibility + Shizuku integration remains validated. |
| **Diagnostics/logging** | **Implemented** | Centralized LaiLog/redaction/crash handling remains the diagnostic baseline. |
| **Linux/terminal** | **Missing** | PRoot/QEMU remains specification-only. |
| **OCR** | **Scaffold** | Dataset/licence decision remains required for printed Bangla OCR. |
| **RAG/Memory** | **Missing** | Planned; not part of the current accelerator qualification milestone. |
| **Cloud/Remote** | **Missing** | Local-first/local-only current state. |

## Accelerator evidence model

Use the explicit progression:

`AVAILABLE → SUPPORTED → ACTIVE → MEASURED`

A device node opening is **AVAILABLE**. A vendor library existing is **AVAILABLE**. A backend becomes **SUPPORTED** only after controlled runtime qualification; `DEVICE_VALIDATED` requires successful model execution with reproducible evidence.

## Immediate authorized work

The next implementation milestone is a **minimal native GPU qualification probe**, not a production backend rewrite:

1. Open `/dev/kgsl-3d0` and record safe results.
2. In the same native process, use `dlopen()`/`dlsym()` for the Qualcomm OpenCL implementation; never execute a shared library as a program.
3. Enumerate OpenCL platforms/devices and identify Adreno 825.
4. If enumeration succeeds, run a tiny non-destructive compute sanity test and record timing/results.
5. Separately perform one controlled Vulkan qualification build with the documented warptile/subgroup mitigation and capture success/crash evidence.
6. Keep CPU as the safe fallback and do not advertise acceleration from library or device-node presence alone.

## Guardrails

Do not disable SELinux, modify vendor files, lower targetSdk, hard-code an app UID, or use unsupported security bypasses as a qualification shortcut. The observed UID `10675` is historical evidence for the 2026-08-21 installation only.

## Canonical documentation

* `docs/CURRENT_STATUS.md` — current accelerator/device state.
* `docs/MASTER_ROADMAP.md` — roadmap and phase gates.
* `docs/ARCHITECTURE.md` — architecture boundaries.
* `docs/implementation/IMPLEMENTATION_PREPARATION.md` — implementation authorization and preparation gates.
* `docs/VENDOR_BACKEND_STRATEGY.md` — backend qualification and vendor-boundary policy.
* `docs/device-results/2026-08-21-redmi-turbo-4-pro-kgsl-app-access.md` — latest LAI KGSL evidence.
* `docs/device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md` — previous OpenCL evidence.
* `docs/device-results/2026-08-20-redmi-turbo-4-pro-gpu-npu-access-audit.md` — previous GPU/NPU access audit.

**Important:** This file supersedes the stale 2026-08-18 statements that described OpenCL as permanently closed and treated the next code step as unrelated M1 work. The newer 2026-08-21 documentation explicitly authorizes controlled GPU qualification preparation while preserving the documentation-first and evidence-gated architecture rules.
