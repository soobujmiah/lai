# GGEN Capability Contract Reference

**Status:** Cross-repository reference, v0.1 semantic mirror
**Date:** 2026-08-24

This document records the LAI-side implementation obligations for the GGEN↔LAI capability protocol. The GGEN repository owns the authoritative client-facing contract text; LAI owns provider/runtime semantics and evidence.

## Initial operations

| Capability | LAI-side responsibility | Initial status |
|---|---|---|
| `text.generate` | map to `InferenceEngine`, scheduler and runtime evidence | READY FOR ADAPTER DESIGN |
| `ocr.extract` | map to `OcrEngine` and explicit model-required failure | CONTRACT ONLY; MODEL PENDING |
| `vision.analyze` | future multimodal runtime | PLANNED |
| `image.generate` | provider/runtime integration | PLANNED |
| `image.edit` | provider/runtime integration | PLANNED |
| `embedding.create` | future embedding/RAG provider | PLANNED |
| `structured.generate` | schema-constrained inference | PLANNED |
| `workflow.plan` | agent/workflow planner | PLANNED |
| `agent.run` | policy-gated AgentRuntime | PLANNED FOR CROSS-REPO EXPOSURE |
| `tool.execute` | policy-gated typed tool gateway | PLANNED FOR CROSS-REPO EXPOSURE |

## Mapping rules

GGEN capability IDs must map to LAI public contracts, never to internal UI/ViewModel classes. For example:

```text
GGEN text.generate
  -> LAI provider adapter
  -> InferenceEngine
  -> InferenceScheduler
  -> BackendSession
  -> normalized response + evidence
```

The LAI provider adapter must not bypass scheduler evidence gates or policy.

## Evidence mapping

LAI reports the strongest state actually supported by evidence:

- `api_available`: provider endpoint/adapter exists
- `backend_available`: selected runtime backend can be opened/used
- `backend_accepted`: backend accepted the workload
- `operations_delegated`: actual operations reached that backend
- `execution_completed`: workload completed there
- `device_validated`: physical evidence exists for the device/backend/build
- `performance_measured`: benchmark evidence exists for this workload

Unknown remains unknown. No zero/default value may imply evidence.

## Privacy mapping

GGEN privacy classes map to LAI policy before inference or OCR execution. `LOCAL_ONLY` is a hard local boundary. LAI must not silently invoke a cloud or remote provider to satisfy a local-only request.

## Tool/agent boundary

`agent.run` and `tool.execute` are not generic model functions. They enter LAI policy, permission and audit infrastructure. Model-generated arguments are untrusted. Approval and authority checks remain mandatory.

## Current runtime truth

LAI currently has a provider-neutral `InferenceEngine`, device-aware scheduler, llama CPU execution and replaceable OCR seam. Accelerator/runtime claims remain evidence-gated. A future generic `AiGateway`/localhost server is a separate design milestone and must not be inferred from the existing inference interface.

## Implementation gate

The first adapter milestone is:

1. capability discovery;
2. `text.generate` request/response mapping;
3. streaming and cancellation;
4. structured error mapping;
5. evidence mapping;
6. contract fixtures;
7. Redmi Turbo 4 Pro validation;
8. only then `ocr.extract` once a real OCR model is available.
