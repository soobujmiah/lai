# Snapdragon-first, vendor-agnostic backend strategy

> Build LAI for Snapdragon today, but design LAI so Snapdragon is only one backend tomorrow.

## Scope and priority

LAI currently prioritizes modern arm64 Snapdragon phones, Hexagon NPU acceleration through QAIRT/QNN, Adreno GPU acceleration where useful, and a reliable CPU reference path. This priority determines optimization and physical-test order. It does not allow Qualcomm APIs, types, model assumptions, or scheduling rules to leak into generic LAI features.

Other vendors are not implementation targets during this phase. LAI will not create fake MediaTek, Exynos, Tensor, NNAPI, or other adapters for architectural appearance. Extensibility is provided by stable boundaries, opaque identifiers, truthful capability evidence, and documentation—not by untested modules.

## Layering

```text
Compose features: Chat / OCR / RAG / Speech / Agents / Automation
                              |
                    Generic LAI contracts
                              |
             Capability + compatibility scheduler
                              |
               Runtime composition / backend manager
                              |
          +-------------------+-------------------+
          |                   |                   |
   llama runtime       Qualcomm runtime      future runtime
    CPU / Vulkan          QNN / HTP            provider
          |                   |                   |
         CPU              Hexagon NPU       other hardware
```

Today, `app` composes the scheduler directly with the one concrete `NativeInferenceEngine`. A separate backend-manager implementation is deferred until a second real runtime provider exists. The contracts already avoid a closed vendor enum so that addition does not require redesigning core.

## Enforced ownership

| Concern | Generic owner | Vendor/runtime owner |
|---|---|---|
| Conversation, streaming, cancellation, metrics | `core:contracts` | implements the contract |
| Backend identity | opaque `BackendId` | defines stable namespaced ID |
| Compute class | generic CPU/GPU/NPU/DSP/other value | declares actual class |
| Selection policy | `core:scheduler` | provides evidence and preference |
| Manufacturer/SoC/ABI/environment | `platform:device` | may add a runtime probe |
| Model artifact trust | `core:model` + `platform:download` | declares format compatibility |
| SDK APIs and native types | none | dedicated adapter only |
| Conversion/calibration | none | vendor build/tooling boundary |
| Buffers, RPC, graph/context cache | none | dedicated adapter only |
| Feature UX and tool policy | app/core features | never imports vendor SDKs |

`scripts/check_architecture_boundaries.py` rejects vendor backend terminology in generic inference and scheduler source. Existing module-direction checks continue to prevent core from importing Android, platform, runtime, or app implementations.

## Backend contract

An adapter publishes facts rather than asking core to recognize its brand:

```text
BackendDescriptor
|- stable BackendId
|- compute class
|- supported model formats
|- explicitly known quantizations
`- adapter default priority

BackendCapability
|- descriptor facts
|- compiled evidence
|- runtime-probe evidence
|- device-validation / benchmark evidence
|- estimated peak memory
`- measured performance, if real
```

The inference contract remains feature-oriented:

- load a compatible local artifact;
- count formatted conversation tokens;
- stream complete text pieces;
- cooperate with cancellation;
- return timing/token metrics;
- bound context and allocations;
- destroy sessions deterministically;
- report failures without claiming unavailable acceleration.

QNN graph names, HTP architecture numbers, RPC handles, tensors, context binaries, and SDK status codes are not part of this contract.

## Device capability model

`DeviceProfile` now records:

- manufacturer and model;
- SoC manufacturer/model when Android exposes them;
- Android API and supported ABIs;
- reported CPU core count;
- available memory, battery/charging, and thermal state;
- backend capabilities supplied by compiled runtime adapters.

The manufacturer or SoC name is diagnostic context, not proof that an accelerator is usable. A backend becomes selectable only when it is compiled, its runtime probe passes, its model format is compatible, memory policy passes, and required physical validation exists. Unknown GPU/NPU properties stay unknown.

A Qualcomm adapter may make Snapdragon probing much more complete by checking QAIRT runtime version, HTP architecture, firmware compatibility, required libraries, and model context compatibility. Those probe structures remain in that adapter and are projected into generic evidence.

## Model and artifact compatibility

A logical model can have multiple hardware-specific artifacts. LAI must not treat these as interchangeable:

```text
Logical model
|- GGUF artifact -> llama CPU / tested Vulkan
|- converted QNN artifact -> compatible Qualcomm QNN/HTP adapter
`- future artifact -> future adapter
```

Catalog revision 3 now describes, per artifact:

- model/artifact ID and format;
- architecture, quantization, and context requirement;
- exact bytes, digest, license, and provenance;
- compatible backend IDs;
- preferred backend and ordered fallback options;
- estimated peak memory and required ABIs;
- validation evidence and known limitations.

Future artifact types can extend precision/input and runtime requirements without changing generic scheduling concepts. The current catalog remains a reviewed GGUF CPU baseline and does not advertise direct QNN compatibility. Schema evolution remains signed and backward compatible with deployed clients; a signed remote revision older than the embedded catalog is rejected.

## Selection policy

The generic path is:

```text
workload + artifact requirements
            |
      DeviceProfile
            |
 compiled and runtime-probed?
            |
 model format / quantization compatible?
            |
 memory, battery, thermal policy passes?
            |
 required device evidence exists?
            |
 measured performance, then adapter preference
            |
 select or fail with explicit rejection reasons
```

Accelerators require physical-device validation before selection. CPU remains the correctness fallback when compatible and safe. An explicitly preferred backend may fall back only according to product policy; a backend failure must never be relabeled as successful acceleration.

The scheduler contains no Qualcomm-specific branch. A future Qualcomm adapter can prefer QNN through its descriptor/model metadata after validation. Another vendor can do the same without changing scheduler logic.

## Qualcomm boundary and migration questions

For each Snapdragon-specific change, its ADR or backend document must answer:

1. Why is this Qualcomm-specific and necessary?
2. Which module owns the SDK/API/type?
3. What generic LAI capability does it implement?
4. Could another adapter implement that capability?
5. Which artifact format, conversion, precision, and hardware constraints apply?
6. What fallback occurs when probing, loading, or execution fails?
7. Which licensing and redistribution restrictions apply?
8. Which physical devices and SoCs produced the evidence?

A future `runtime:qnn` boundary may depend on QAIRT/QNN and JNI/C++; `core`, generic features, Accessibility, model download, and Compose must not.

## Performance and validation record

Every important workload should record:

| Field | Required content |
|---|---|
| Device | manufacturer/model, Android version |
| SoC | exact model; firmware/runtime where relevant |
| Backend | stable ID, runtime/SDK version, evidence level |
| Artifact | model ID, format, quantization/precision, digest |
| Input | prompt/input size and context |
| Performance | load, prefill, TTFT, decode/throughput, total latency |
| Memory | available and estimated/measured peak |
| Utilization | CPU/GPU/NPU when a trustworthy measurement exists |
| Sustained behavior | duration, thermal states, throttling, failures |
| Energy | battery impact when measured with a stated method |
| Correctness | output/quality comparison with the reference backend |
| Recovery | cancellation, unload, backend failure, and fallback result |

One Snapdragon device does not validate all Snapdragon devices. QNN claims require at least the exact tested SoC/runtime/artifact combination and must state their scope.

## Progressive implementation gates

1. Keep CPU behavior correct, recoverable, and measured.
2. Validate sustained CPU thermals and current lifecycle gates.
3. Add and qualify llama Vulkan only if it produces a real benefit.
4. Acquire QAIRT/QNN legally in GitHub CI; never install it in the source-generation workspace.
5. Build a real converted-model and runtime-probe path in a dedicated adapter.
6. Compare QNN output and fallback with the CPU reference on physical Snapdragon hardware.
7. Optimize tensors, shared buffers, cache invalidation, and thermal routing inside the Qualcomm boundary.
8. Add other vendors only after core maturity or an explicit product requirement.

## Design review checklist

Before merging an AI/runtime change, answer:

- Is it generic or hardware-specific?
- If hardware-specific, is every dependency isolated?
- Is the interface based on LAI needs rather than one SDK's vocabulary?
- Does the Snapdragon path remain efficient?
- Is this real functionality rather than a placeholder vendor implementation?
- Are capability and performance claims backed by the correct evidence?
- Can another backend implement the contract without rewriting LAI Core?
- Were architecture, limitations, migration path, tests, and device protocol updated?

See [ADR 0005](adr/0005-snapdragon-first-vendor-neutral-backends.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [MODELS_AND_BACKENDS.md](MODELS_AND_BACKENDS.md).
