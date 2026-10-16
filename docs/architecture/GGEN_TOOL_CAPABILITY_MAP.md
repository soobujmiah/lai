# LAI ↔ GGEN Tool Capability Map

**Status:** Cross-repository implementation planning reference
**Date:** 2026-08-24

GGEN owns creative/document semantics and user-facing workflows. LAI owns policy-gated AI/runtime/automation capabilities. This map is not a license to expose every LAI tool to GGEN.

## Capability mapping

| GGEN capability | LAI-side building block | Current LAI truth | Exposure rule |
|---|---|---|---|
| `text.generate` | `InferenceEngine` + scheduler + backend session | CPU device-validated; accelerator qualification remains evidence-gated | First cross-repo adapter |
| `ocr.extract` | `OcrEngine` / OCR pipeline | Contract/scaffold; real model pending | Expose only with explicit model-required failure |
| `vision.analyze` | future multimodal runtime | Planned | Do not expose as available |
| `image.generate` | model/provider runtime | Planned | Provider-specific later |
| `image.edit` | model/provider runtime | Planned | Provider-specific later |
| `embedding.create` | RAG/embedder path | Planned/future | Expose only after runtime evidence |
| `structured.generate` | constrained inference contract | Planned | Contract-first |
| `workflow.plan` | pipeline/agent planner | Planned | Planning only until policy contract exists |
| `agent.run` | `AgentRuntime` + `AgentPolicy` + audit | Local tool runtime exists; cross-repo exposure planned | Never bypass LAI policy |
| `tool.execute` | typed tool catalog + authority gate + audit | Implemented tools exist | Explicit capability allowlist only |

## Existing LAI tool inventory relevant to GGEN

Current implemented or build/device-verified tools include:

- accessibility screen snapshot/click/type/scroll;
- global Android actions;
- app launch;
- device information;
- typed settings access;
- bounded Shizuku shell operations;
- package operations;
- key events;
- diagnostic/audit infrastructure.

The canonical detailed tool catalog remains `docs/TOOL_CATALOG.md`. Every tool is typed, permission-aware, risk-rated, auditable and status-labeled. No raw shell is exposed as a generic tool.

## Developer-tool family

LAI's planned developer surface includes repository inspection, code search, generation/edit/refactor, debugging, test generation, build/log/static-analysis/dependency-analysis, Git/GitHub operations and Linux/PRoot/QEMU execution. These are LAI developer-workstation capabilities, not GGEN product semantics.

If GGEN ever requests them, the boundary must be a narrow declared capability (for example `workflow.plan` or an explicitly approved development operation), not direct access to LAI internal modules or arbitrary filesystem authority.

## Security invariants

1. Model output is untrusted.
2. Selecting LAI never grants arbitrary Android authority.
3. Approval precedes privileged execution.
4. Tool calls are bounded, typed and auditable.
5. Replay is rejected.
6. `LOCAL_ONLY` requests never silently route remotely.
7. Provider/runtime evidence is propagated without certainty inflation.
8. Tool status is `AVAILABLE / PLANNED / EXPERIMENTAL / FUTURE / PLATFORM-LIMITED / DEVICE-DEPENDENT`, not a roadmap claim disguised as availability.

## First adapter sequence

1. Capability discovery.
2. `text.generate` request/response mapping.
3. Streaming + cancellation.
4. Typed error mapping.
5. Evidence mapping.
6. Contract fixtures shared by both repositories.
7. Redmi Turbo 4 Pro end-to-end validation.
8. OCR only after a real OCR model is integrated and validated.
9. Later capability expansion one contract at a time.
