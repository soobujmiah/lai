# Current implementation state

**Audit update:** 2026-08-21  
**Source baseline:** current `main` source is authoritative.  

This document is a source-of-truth status record. Status describes checked-in implementation; device/CI evidence is stated separately.

## Reconciled state

The previous M1 SAF-settings task is already implemented in the current source. The remaining responsibility is source/documentation/CI reconciliation, followed by the newly authorized accelerator qualification work described below.

### Existing implementation

- typed settings v1, validation/migration, and atomic SAF replacement/recovery exist;
- `platform:history` is part of the 16-module Gradle graph;
- CPU inference is the validated production path;
- native llama integration contains CPU plus optional Vulkan/OpenCL integration points;
- CI/build scripts contain llama.cpp and Vulkan patching infrastructure;
- device qualification remains separate from compilation support.

## Accelerator qualification state

The project retains GPU acceleration as a first-class goal. CPU-only is not the target architecture.

| Capability | Current state | Meaning |
|---|---|---|
| CPU | **DEVICE_VALIDATED** | Reliable shipped baseline on Redmi Turbo 4 Pro / SM8735. |
| Vulkan | **AVAILABLE / EXPERIMENTAL** | Model loading on `llama-vulkan` works, but generation crashes in Qualcomm `vulkan.adreno.so` at `vkCmdBindPipeline+0x4`. Not device-validated. |
| Qualcomm OpenCL stack | **AVAILABLE** | Vendor libraries exist; prior OpenCL-Z evidence shows the Adreno stack is healthy. Normal modern-app ggml-opencl loading hit an Android linker-namespace boundary. A native `dlopen()` qualification route is now under investigation. |
| KGSL | **AVAILABLE** | The exact installed `dev.lai.runtime.debug` app identity successfully opened `/dev/kgsl-3d0` from its app context on 2026-08-21. The observed UID is installation-specific and must never be hard-coded. This proves device-node access, not GPU computation. |
| QNN/HTP | **PLANNED / QUALIFICATION LATER** | Requires real app-side runtime loading/execution evidence and applicable QAIRT licensing. |

Evidence progression is mandatory:

`AVAILABLE → SUPPORTED → ACTIVE → MEASURED`

A device node or library being present is never enough to grant `DEVICE_VALIDATED`.

## Next implementation gate

Before production backend changes, the first accelerator implementation is a **minimal native KGSL + OpenCL qualification probe**:

1. open `/dev/kgsl-3d0` and record safe results;
2. use `dlopen()`/`dlsym()` in the LAI native process for the Qualcomm OpenCL implementation;
3. enumerate platforms/devices and identify Adreno 825;
4. if enumeration succeeds, run a tiny non-destructive GPU compute sanity test and capture timing/results;
5. separately perform one controlled Vulkan qualification build using the documented warptile/subgroup mitigation and capture the real success/crash boundary;
6. preserve CPU fallback and do not advertise acceleration from library/device-node presence alone.

## Engineering rules

- Preserve existing working functionality.
- Treat checked-in source as authoritative over stale planning snapshots.
- No vendor-specific types in `core`.
- New inference backends belong behind the runtime/native boundary and existing backend contracts.
- Every implementation change needs an explicit test/verification path.
- Do not disable SELinux, modify vendor files, lower targetSdk, or hard-code app UIDs as a qualification shortcut.
- Shared libraries are loaded through a process (`dlopen`/linker); executing a `.so` directly is not a valid runtime test.

## Documentation sources

See `docs/CURRENT_STATUS.md`, `docs/DEVELOPMENT_STATE.md`, `docs/ARCHITECTURE.md`, `docs/architecture/module-map.md`, `docs/MASTER_ROADMAP.md`, `docs/VENDOR_BACKEND_STRATEGY.md`, `docs/implementation/IMPLEMENTATION_PREPARATION.md`, and the dated device-result files for evidence and phase gates.

## Verification policy

A source change is not a device qualification claim. Device/backend readiness requires reproducible runtime evidence on the target hardware, with correctness and stability checks in addition to compilation success.
