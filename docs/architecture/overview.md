# LAI architecture overview

**Last audited:** 2026-08-24  
**Status:** Canonical implementation-state overview

## Purpose

LAI is the Android AI/device execution runtime: local inference, runtime model lifecycle, device-aware execution, cloud/custom provider connectivity, privileged Android tools, and bounded agent execution. GGEN is an independent consumer/product layer; LAI does not become a GGEN dependency or creative/document engine.

This overview distinguishes current implementation from roadmap intent. Source code and validation evidence are authoritative for maturity.

## Responsibilities and layers

```text
App / UI / composition root
        |
        +-- Core contracts + policy + scheduler + model metadata
        |
        +-- Platform authority
        |     download · audit · device · Accessibility · Shizuku · workspace
        |
        +-- Runtime
        |     llama.cpp · OCR · orchestration · future accelerator adapters
        |
        +-- Provider/runtime integration
              local execution · cloud/custom adapters · routing/failover

        +-- Tool / Agent authority
              typed tools · permission/confirmation · bounded execution · evidence

        <------ versioned capability contract ------>
                         GGEN
```

### Current implementation layers

- **Core:** serializable contracts, policy, scheduler and model decisions.
- **Platform:** Android authority and persistence boundaries.
- **Runtime:** replaceable inference/OCR/orchestration implementations.
- **Tools:** typed, policy-gated Android automation capabilities.
- **App:** composition root and product shell.

Future layers must not be described as implemented until source and evidence establish them.

## Canonical LAI responsibilities

LAI owns:

- local AI execution;
- model lifecycle for runtime models;
- CPU/GPU/NPU backend selection and qualification;
- device/thermal/memory-aware scheduling;
- cloud/custom provider adapters and connectivity;
- provider routing/retry/failover;
- runtime streaming/cancellation semantics;
- Android tool registry and execution authority;
- Accessibility/Shizuku/privileged-operation policy;
- bounded agent planning/execution;
- runtime security, evidence and audit;
- runtime diagnostics and secrets.

GGEN owns user-facing creative/document semantics and consumes LAI through a stable capability contract.

## Principal interfaces

Current interfaces include `InferenceEngine`, `OcrEngine`, `ToolCall`, `ToolResult`, `ToolDefinition`, `AutomationCommand`, `PrivilegedCommand`, workspace contracts and `LaiPlugin`.

A generic cloud gateway/provider manager is an architectural target, not proof of current implementation.

## Current maturity snapshot

- **CPU local inference:** implemented and device-validated baseline.
- **Vulkan/GPU acceleration:** experimental/qualification-blocked where current device evidence requires it; do not claim production readiness from source presence.
- **QNN/NPU:** planned/experimental only where source and device evidence establish the exact state; no blanket production claim.
- **OCR:** runtime contract/adapter exists; production completeness depends on the specific engine/model/device evidence.
- **Android Accessibility/Shizuku authority:** implemented capabilities exist, subject to permission/policy state.
- **Agent:** bounded/one-shot execution exists; autonomous multi-step agent runtime remains planned.
- **Plugin API:** contract exists; loader/installation/sandbox/lifecycle management is not implied.
- **Cloud/custom providers, generic AI Gateway, remote server, RAG, workstation/Linux runtime:** roadmap unless separately proven by source and evidence.

## Runtime lifecycle

1. Compose/application creates the runtime container.
2. Policies, repositories, schedulers and authority gateways are composed.
3. Models are explicitly imported/downloaded, verified and loaded.
4. Inference produces typed streamed events where supported.
5. Tool proposals are treated as untrusted model output.
6. Permission/confirmation and audit policy precede consequential tool execution.
7. Runtime sessions close on explicit unload, lifecycle teardown or critical memory pressure.

## Security boundaries

Trust boundaries include model output, screen content, downloaded artifacts, SAF documents, Accessibility authority, Shizuku authority, plugin input, native code and CI secrets. Fail-closed behavior is required: missing authority or failed validation yields a typed failure, never broader implicit authority.

Credentials/secrets remain inside LAI's runtime security boundary and must not leak into GGEN contracts, project files or ordinary logs.

## GGEN integration boundary

The shared contract is semantic and versioned. GGEN may request capabilities such as:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `document.transform`
- `tool.execute`
- `agent.run`

LAI owns execution authority and evidence; GGEN owns user intent, creative UX and result integration. Provider SDKs, secrets and internal runtime types must not cross into the GGEN contract.

`LOCAL_ONLY` must never silently leave the device. LAI owns retry/failover and must respect privacy, capability, cost, side-effect and idempotency constraints.

## Testing and evidence

Unit/integration tests cover pure contracts, policy, scheduler and applicable runtime components. Hardware-dependent claims require device evidence. Evidence should progress through:

`API_AVAILABLE → BACKEND_AVAILABLE → BACKEND_ACCEPTED → EXECUTION_COMPLETED → DEVICE_VALIDATED → PERFORMANCE_MEASURED`

The presence of an implementation or backend is never sufficient to claim validated hardware execution.

## Documentation rule

Every roadmap or research document must preserve the distinction:

`IMPLEMENTED / VALIDATED / EXPERIMENTAL / PLANNED / BLOCKED / NOT_STARTED`.

If documentation conflicts with source or evidence, reconcile the canonical documentation before extending implementation. Non-trivial implementation must follow current Android/professional/open-source benchmark research and the cross-project ownership boundary.

## Extension rule

Add behavior behind core contracts and isolated adapters. New Android authority requires a dedicated platform owner, explicit permission/policy, tests, documentation and an ADR. Do not introduce duplicate provider registries, routing/failover systems, model runtimes, tool authorities or runtime audit systems in GGEN.

## Completion criterion

LAI architecture documentation is aligned when each capability has one canonical owner, maturity is evidence-backed, GGEN integration uses a stable contract, LAI remains independently useful as an Android AI runtime, and no roadmap claim is presented as implemented without source/test/device evidence.
