# Current implementation state

**Audit update:** 2026-08-21  
**Source baseline:** current `main` source is authoritative.  

This document is a source-of-truth status record. Status describes checked-in implementation, while device/CI evidence is stated separately.

## M1 conclusion

The previously planned SAF settings reliability work is already implemented in the current source:

- typed settings v1 and validation/migration exist;
- `AtomicNamedDocumentReplace` implements temp-file, backup, finalize, and recovery behavior;
- `WorkspaceSettingsStore` integrates the atomic replacement helper;
- dedicated tests cover the atomic replacement path.

Therefore, M1 must not be reimplemented as duplicate code. The remaining M1 responsibility is **source/documentation/CI reconciliation**.

## Current engineering rules

- Preserve existing working functionality.
- Treat checked-in source as authoritative over stale planning snapshots.
- Do not claim Vulkan, OpenCL, QNN, or another accelerator as device-qualified without reproducible evidence.
- Avoid new dependencies unless an approved implementation task requires them.
- Every implementation change needs an explicit test/verification path.

## Next engineering gate

Before starting the next accelerator milestone, verify:

1. current CI baseline is green;
2. architecture/module documentation matches the source;
3. acceleration claims are evidence-backed;
4. the selected implementation target has tests and a reproducible verification procedure.

The next implementation task should then be selected from the current source state rather than from obsolete M1 planning text.

## Important current source facts

- The Gradle graph contains 16 modules, including `platform:history`.
- The native runtime has CPU inference plus Vulkan/OpenCL integration points; llama.cpp and optional acceleration are controlled by the native CMake configuration.
- The current CMake keeps `GGML_CPU_KLEIDIAI` disabled by default.
- The current CI/build scripts already contain llama.cpp/Vulkan patching infrastructure.
- Device qualification remains separate from compilation support.

## Evidence policy

A source change is not a device qualification claim. Device/backend readiness requires reproducible runtime evidence on the target hardware, with correctness and stability checks in addition to compilation success.
