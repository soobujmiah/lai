# LAI ↔ GGEN Integration Boundary

**Status:** Proposed authoritative integration boundary
**Date:** 2026-08-24

## 1. Ownership

GGEN is the user-facing Creative & Document Studio. It owns project artifacts, canvas/document semantics, creative tools, templates, exports, workflow authoring and AI-assisted creative UX.

LAI is the local-first AI intelligence/runtime platform. It owns inference/runtime execution, model lifecycle, accelerator backends, scheduling, agent policy, Android automation authority, OCR/model execution, AI-runtime evidence and diagnostics.

Neither repository becomes a submodule or source dependency of the other.

## 2. Consumer/provider relationship

```text
GGEN
  Creative + Document domain
          |
          | capability contract
          v
LAI / Cloud / Custom provider
          |
     inference/runtime
          |
     CPU / GPU / NPU
```

GGEN may use LAI as its preferred local provider, but GGEN remains functional without LAI.

## 3. Current LAI boundary

LAI already has provider-neutral inference contracts, a scheduler, device evidence, model lifecycle, native llama runtime, OCR seam and policy-gated tools. Current physical evidence must remain distinct from roadmap claims. The present architecture explicitly treats a future `AiGateway`, localhost server, remote providers, RAG and multimodal systems as planned boundaries rather than implemented features.

## 4. Capability contract

Initial shared capability vocabulary:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `structured.generate`
- `workflow.plan`
- `agent.run`
- `tool.execute`

The first LAI-facing implementation should expose only the smallest stable subset required by GGEN, beginning with `text.generate` and `ocr.extract`.

## 5. LAI provider requirements

A future LAI provider endpoint/adapter shall expose:

- protocol and operation versions
- provider identity/version
- capability discovery
- input/output schema identifiers
- streaming/cancellation support
- limits
- privacy class support
- model/runtime metadata where permitted
- honest execution evidence
- stable error codes
- health/readiness

The provider must not expose internal Kotlin module types or JNI objects as the GGEN API.

## 6. Execution evidence

Evidence must distinguish:

`API_AVAILABLE → BACKEND_AVAILABLE → BACKEND_ACCEPTED → OPERATIONS_DELEGATED → EXECUTION_COMPLETED → DEVICE_VALIDATED → PERFORMANCE_MEASURED`

LAI must never report GPU/NPU execution merely because a backend exists or was accepted. GGEN must render evidence supplied by LAI without upgrading its certainty.

## 7. Privacy

GGEN communicates content-routing intent. LAI enforces local execution and runtime policy. `LOCAL_ONLY` requests must never silently leave the device. Cloud/custom providers require explicit routing permission under GGEN's privacy policy.

## 8. Tool and agent authority

GGEN may request an AI plan or explicitly defined tool capability, but selecting LAI must not grant GGEN arbitrary Android authority. `agent.run` and `tool.execute` remain policy-gated LAI capabilities. Model output is untrusted and cannot self-authorize execution.

## 9. Failure semantics

If LAI is unavailable, GGEN must continue manual operation and may select another configured provider when privacy/policy permits. No silent cloud fallback. Provider errors must remain typed and actionable.

## 10. Security and credentials

GGEN and LAI must keep API keys/tokens out of project files, source, ordinary logs and diagnostic exports. Cross-process authentication, if needed, is a later design task and must precede production localhost/remote implementation.

## 11. Transport

The semantic contract is transport-neutral. Candidate transports are loopback HTTP, Android IPC where justified, and remote HTTPS. No transport is selected as mandatory by this document.

## 12. Implementation gate

Before production integration:

1. approve the shared capability contract;
2. implement GGEN mock provider and contract fixtures;
3. implement LAI capability discovery/adapter;
4. validate text generation and OCR end-to-end;
5. verify privacy, cancellation, error and evidence semantics;
6. validate on Redmi Turbo 4 Pro;
7. document measured behavior;
8. only then add image/embedding/agent/tool capabilities.

No direct dependency from GGEN to LAI internals is permitted.
