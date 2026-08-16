# ADR 0005: Snapdragon-first, vendor-neutral backend boundary

- Status: Accepted
- Date: 2026-08-16

## Context

LAI's first physical target is Snapdragon, and the intended acceleration path is Qualcomm QAIRT/QNN on Hexagon HTP. The currently proven backend is llama.cpp CPU on a Snapdragon 8s Gen 4 device. The earlier generic core used an `InferenceBackend` enum containing `QNN`, and the llama native module contained an unavailable QNN placeholder and build flag. That was honest about availability, but it made a vendor SDK name part of generic contracts and placed a future non-llama runtime inside the llama adapter.

LAI needs fast Snapdragon progress without making QNN concepts part of Chat, OCR, agents, model storage, scheduling, or other core features. It also must not build speculative implementations for hardware that is not currently being targeted.

## Decision

Adopt **Snapdragon-first, not Snapdragon-locked** as an enforced architecture rule.

1. Generic core uses validated, opaque `BackendId` values instead of a closed vendor enum.
2. Each concrete adapter publishes a `BackendDescriptor` containing compute class, supported model formats, declared quantizations, and its default preference.
3. `InferenceScheduler` evaluates evidence, model compatibility, memory, battery, and thermal state. It does not contain hardware-vendor names or SDK terminology.
4. `DeviceProfile` contains Android/SoC identity, CPU/ABI facts, runtime constraints, and adapter-reported backend capabilities. Unknown capabilities remain unknown; device branding alone never proves NPU support.
5. The llama adapter owns only llama.cpp execution (`llama-cpu` now and a tested `llama-vulkan` later). QNN flags and placeholders are removed from that module.
6. A real Qualcomm implementation will live behind a dedicated runtime adapter boundary (conceptually `runtime:qnn`) and will publish an implementation-owned ID such as `qualcomm-qnn-htp`. No empty module is created before licensed code and a real integration exist.
7. Future vendor adapters implement the same LAI contracts and capability evidence flow. Their SDK types, libraries, model conversion, buffer handling, and device probes stay inside their adapter modules.
8. CI rejects Qualcomm/QNN/Hexagon and other vendor terminology in generic inference and scheduler source.

## Why QNN is vendor-specific

QNN graph/context formats, HTP architecture identifiers, RPC memory, runtime libraries, SDK versions, operator partitioning, and context-binary cache rules are Qualcomm contracts. They are necessary for efficient Hexagon execution but are not generic inference concepts. LAI core may request generation, cancellation, metrics, memory bounds, and supported-format evidence; it must not manipulate those QNN details.

## Consequences

Positive:

- Snapdragon/QNN can be optimized without placing SDK dependencies in core.
- A future backend can be added by publishing descriptors and implementing the inference contract rather than editing a central vendor enum.
- Model-format incompatibility (for example GGUF versus a converted context binary) can be rejected before load.
- The generic scheduler remains small and testable on the JVM.
- Capability claims remain evidence-based rather than inferred from a manufacturer string.

Costs:

- Backend IDs must be stable, namespaced, and mapped at each adapter boundary.
- Adapter metadata and model metadata must remain synchronized.
- A future backend manager will be required when more than one runtime provider is actually compiled.
- Qualcomm integration still requires licensed CI acquisition, conversion tooling, supported-device probes, and physical tests.

## Alternatives considered

- **Keep a `CPU/VULKAN/QNN` enum in core:** simple today but every vendor expands a central implementation list and encourages vendor-specific scheduler branches.
- **Put QNN inside the llama C++ registry:** convenient packaging but incorrect ownership because converted QNN artifacts and QNN sessions are not arbitrary GGUF llama backends.
- **Create placeholder modules for every vendor now:** rejected as misleading and premature over-engineering.
- **Use only generic `CPU/GPU/NPU` IDs:** rejected because multiple providers can target the same compute class and need distinct compatibility, lifecycle, and evidence.
- **Implement all vendors immediately:** rejected; it would slow the physical Snapdragon path without producing validated support.

## Current limitation

Only `llama-cpu` is compiled and device validated. `llama-vulkan` remains planned. No QNN adapter, QNN model artifact, or QNN performance claim exists yet. Android public APIs do not provide a trustworthy universal NPU inventory, so adapter runtime probes are authoritative.

## Migration path

When Qualcomm integration begins:

1. review SDK/runtime redistribution and CI licensing;
2. create the dedicated adapter only when it can compile real code;
3. define converted artifact metadata and hash/signature requirements;
4. publish runtime and HTP compatibility probes;
5. implement bounded session lifecycle, cancellation, metrics, and deterministic cleanup;
6. compare correctness with the CPU reference;
7. add QNN only to adapter-owned source and Snapdragon-specific documentation;
8. physically validate fallback, sustained thermal behavior, memory, and battery impact.

Adding another vendor later follows the same path without changing the generic scheduler policy or feature APIs.
