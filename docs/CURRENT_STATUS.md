# Current Status

> The authoritative handoff snapshot is [`PROJECT_STATE.md`](../PROJECT_STATE.md); the canonical
> full roadmap is [`ROADMAP.md`](ROADMAP.md). This file is a frozen snapshot from the date below,
> retained for historical context — several items below (notably the Hexagon/QNN scoping task)
> have since been completed; see `PROJECT_STATE.md` for current state, not this file.

**Snapshot:** 2026-08-21 — GPU/NPU qualification resumed after new stock-app KGSL access evidence.
**Build:** Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10; `validate_repo.sh` remains the documentation/build gate.

## Device-validated baseline

* **CPU LLM:** device-validated and shipped default. Qwen 1.5B Q4_K_M remains the reliable path.
* **Vulkan GPU:** application access is proven, model loading on `llama-vulkan` is proven, but generation currently SIGSEGVs at `vkCmdBindPipeline+0x4` inside Qualcomm `/vendor/lib64/hw/vulkan.adreno.so` during ggml `MUL_MAT`. This is a driver/kernel interaction failure, not an app-permission failure.
* **OpenCL:** Qualcomm OpenCL libraries are present and the legacy OpenCL-Z path proves the Adreno OpenCL stack is healthy. The existing modern-app ggml-opencl path previously failed at Android linker namespace loading. Do not claim OpenCL inference support yet.
* **KGSL device-node access:** **NEW, OBSERVED 2026-08-21.** On the exact LAI debug package `dev.lai.runtime.debug`, UID 10675, SELinux app context `u:r:untrusted_app:s0:c163,c258,c512,c768`, `run-as` can read/write and successfully `open()` `/dev/kgsl-3d0` (`gpu_device`, mode 0666). This proves the raw Qualcomm KGSL character-device boundary is accessible from LAI's app identity. It does not prove GPU computation.

Evidence: `docs/device-results/2026-08-20-redmi-turbo-4-pro-gpu-npu-access-audit.md` and `devices/redmi-turbo-4-pro` evidence in the user's `skb` knowledge base.

## Acceleration strategy — current

The project must retain GPU acceleration as a first-class goal. CPU-only is not the target architecture.

### Immediate qualification tracks

1. **Native KGSL probe:** add a minimal native diagnostic probe that opens `/dev/kgsl-3d0` and records safe driver/device results. Do not issue undocumented destructive ioctls.
2. **Qualcomm OpenCL qualification:** in the same native process, use `dlopen()` (never execute `.so` directly) for the vendor OpenCL implementation, resolve `clGetPlatformIDs` / `clGetDeviceIDs`, enumerate Adreno 825, then run a tiny compute sanity test if enumeration succeeds. This is exploratory; the existing linker-namespace evidence remains valid for the normal ggml-opencl route.
3. **Vulkan qualification:** land the upstream ggml-vulkan warptile clamp already identified in the device strategy document, build exactly one `validated_accelerators=llama-vulkan` qualification APK, and capture the real subgroup size plus success/crash outcome.
4. **QNN/Hexagon:** scope the existing `ggml-hexagon` backend and Qualcomm QNN/HTP route after confirming the SM8735 DSP/HTP architecture and a real app-side loading path. Do not assume vendor library presence equals support.

### Evidence rule

`AVAILABLE` → `SUPPORTED` → `ACTIVE` → `MEASURED`.

A device node opening is **AVAILABLE**, not GPU support. A library existing is **AVAILABLE**, not backend support. Only successful model execution with captured evidence can grant `DEVICE_VALIDATED`.

## Current backend policy

CPU remains the shipped safe default. Vulkan/OpenCL/NPU remain opt-in until device validation. The scheduler must never silently advertise acceleration from library presence alone.

## Documentation requirements for accelerator work

Every device experiment must record:

- exact device/build identity;
- command or test procedure;
- raw result/evidence file;
- Android UID/SELinux context when access control matters;
- backend/library identity and loading mechanism;
- failure boundary (permissions, linker, driver, runtime, kernel, model);
- explicit status transition.

Do not disable SELinux, modify vendor files, lower targetSdk, or use unsupported security bypasses as a qualification shortcut.

## Next action

**Do not stop at the previous “GPU unsupported” conclusion.** The new KGSL result re-opens controlled native GPU investigation. The first implementation milestone is the minimal native KGSL + OpenCL qualification probe; the second is the single Vulkan warptile-clamp qualification build; QNN/Hexagon follows as a separate acceleration path.
