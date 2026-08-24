# LAI ↔ GGEN Tool Ownership Inventory

**Status:** Architecture baseline
**Date:** 2026-08-24

## Purpose

This document defines which LAI capabilities may be exposed to GGEN and which remain internal LAI runtime concerns. GGEN is a Creative & Document Studio; LAI is the local-first AI/runtime/automation platform. The repositories remain independent.

## GGEN-facing capability families

| Capability | LAI responsibility | GGEN responsibility | Initial priority |
|---|---|---|---:|
| `text.generate` | model selection, inference, streaming, evidence | prompt UX, document insertion, validation | P0 |
| `ocr.extract` | OCR/model execution and runtime evidence | region/text-frame integration and editing | P0 |
| `vision.analyze` | multimodal inference | result presentation and project mutation | P1 |
| `structured.generate` | schema-constrained model execution | schema validation and domain mapping | P1 |
| `image.generate` | image model execution | asset ingestion/history/placement | P1 |
| `image.edit` | image model execution | masks/layers/history and editable result | P1 |
| `embedding.create` | embedding runtime/model lifecycle | indexing/search UX and project integration | P2 |
| `workflow.plan` | optional agent/model planning | review/edit/authorization and workflow ownership | P2 |
| `agent.run` | policy-gated agent execution | request/approval/result presentation | P2 |
| `tool.execute` | runtime policy and authorized tool execution | capability request only | P2 |

## Internal LAI capabilities

The following must remain LAI-owned and must not leak as GGEN implementation dependencies:

- llama.cpp internals;
- Vulkan/QNN/NNAPI/backend implementation details;
- model loading/quantization/runtime memory internals;
- device-aware scheduler implementation;
- thermal/memory/battery policy implementation;
- Android Accessibility and Shizuku authority;
- agent policy engine and privileged tool authorization;
- runtime audit/evidence storage internals;
- credential/secret storage;
- recovery and local runtime orchestration.

## Provider contract

GGEN should see a provider-neutral capability descriptor, not a Kotlin class hierarchy. A provider descriptor must expose stable ID/version, supported capabilities, modality/limits, model identifiers where safe, streaming/structured/tool support, locality, privacy/cost/retention metadata and health/auth state without secrets.

A request must carry a protocol version, request ID, capability, input, constraints, privacy policy, routing/context metadata, streaming preference and cancellation semantics. Responses must carry status, output, provider/model metadata where safe, usage where available, evidence, warnings and typed error information.

## Evidence rule

LAI must distinguish capability/API availability from actual execution. Minimum monotonic evidence states are:

`API_AVAILABLE → BACKEND_AVAILABLE → BACKEND_ACCEPTED → OPERATIONS_DELEGATED → EXECUTION_COMPLETED → DEVICE_VALIDATED → PERFORMANCE_MEASURED`

Unknown values remain unknown. GGEN must not convert a backend label into a claim of GPU/NPU execution.

## Security rule

GGEN requests capabilities; LAI decides whether execution is permitted. Model output is untrusted. Tool arguments require schema validation. Consequential Android actions require LAI policy and appropriate user consent. No model output can self-authorize privileged execution.

## Integration sequence

1. Freeze and review the cross-repository semantic contract.
2. Implement a mock provider in GGEN and contract fixtures.
3. Implement a provider adapter/discovery surface in LAI.
4. Validate `text.generate` end-to-end.
5. Test privacy, cancellation, typed errors and evidence.
6. Physically validate the applicable Redmi Turbo 4 Pro path.
7. Add OCR.
8. Add multimodal capabilities.
9. Add agent/tool capabilities only after explicit authority and consent contracts are validated.

No direct repository dependency, source sharing or runtime coupling is required by this plan.
