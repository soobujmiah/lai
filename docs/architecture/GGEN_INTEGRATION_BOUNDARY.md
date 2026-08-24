# LAI ↔ GGEN Integration Boundary

**Status:** Canonical integration boundary
**Date:** 2026-08-24

## 1. Ownership

GGEN is the user-facing Creative & Document Studio. It owns project artifacts, canvas/document semantics, creative tools, templates, exports, workflow authoring and AI-assisted creative UX.

LAI is the AI/device execution and Android runtime platform. It owns inference/runtime execution, model lifecycle, accelerator backends, scheduling, provider adapters/connectivity, provider routing/failover, agent policy/runtime, Android automation authority, OCR/model execution where supplied by runtime, runtime evidence, diagnostics, secrets and runtime audit.

Neither repository becomes a submodule or source dependency of the other.

## 2. Consumer/provider relationship

```text
GGEN
  Creative + Document domain
          |
          | versioned capability contract
          v
LAI Runtime
          |
   +------+----------------+
   |                       |
 Local execution      Cloud/custom providers
 CPU/GPU/NPU
          |
          v
 normalized result + evidence
          |
          v
         GGEN
```

GGEN may use LAI as its preferred local/runtime provider, but GGEN remains functional without LAI.

## 3. Canonical capability boundary

Initial shared capability vocabulary:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `document.transform`
- `tool.execute`
- `agent.run`

The contract identifies the capability; LAI owns execution authority. GGEN owns the user-facing intent and result/document integration.

## 4. LAI runtime/provider requirements

LAI runtime/provider interfaces should expose, as applicable:

- protocol and operation versions;
- provider identity/version;
- capability discovery;
- input/output schema identifiers;
- streaming/cancellation support;
- limits;
- privacy/execution locality support;
- model/runtime metadata where permitted;
- honest execution evidence;
- stable error codes;
- health/readiness.

Provider credentials remain inside LAI's secure runtime boundary. Provider SDK/JNI/internal Kotlin types must not leak into the GGEN contract.

## 5. Execution evidence

Evidence must distinguish:

`API_AVAILABLE → BACKEND_AVAILABLE → BACKEND_ACCEPTED → OPERATIONS_DELEGATED → EXECUTION_COMPLETED → DEVICE_VALIDATED → PERFORMANCE_MEASURED`

LAI must never report GPU/NPU execution merely because a backend exists or was accepted. GGEN must render evidence supplied by LAI without upgrading its certainty.

## 6. Privacy and routing

GGEN communicates content-routing intent and user-facing constraints. LAI enforces runtime policy.

Supported intent may include:

- `LOCAL_ONLY`
- `LOCAL_PREFERRED`
- `CLOUD_ALLOWED`
- `REMOTE_ALLOWED`
- `NETWORK_REQUIRED`

`LOCAL_ONLY` must never silently leave the device. Provider routing/failover is LAI-owned and must respect privacy, capability, cost, side-effect and idempotency constraints.

## 7. Tool and agent authority

GGEN may request an AI plan or explicitly defined tool capability, but selecting LAI must not grant GGEN arbitrary Android authority. `agent.run` and `tool.execute` remain policy-gated LAI capabilities. Model output is untrusted and cannot self-authorize execution.

Accessibility, Shizuku, terminal, privileged Android operations, tool registry, confirmation and audit remain LAI-owned.

## 8. Failure semantics

If LAI is unavailable, GGEN must continue manual/non-AI operation and may select another explicitly configured runtime/provider only when the integration contract permits it. No silent policy-violating cloud fallback.

Provider/runtime errors remain typed and actionable. LAI owns retry/failover; GGEN owns user-facing recovery UX.

## 9. Security and credentials

GGEN and LAI must keep API keys/tokens out of project files, source, ordinary logs and diagnostic exports. LAI owns runtime secret handling. Cross-process authentication, if needed, must be specified and tested before production transport deployment.

## 10. Transport

The semantic contract is transport-neutral. Candidate transports include loopback HTTP, Android IPC where justified, and remote HTTPS. No transport is mandatory merely because it is convenient for a prototype.

## 11. Implementation gate

Before production integration:

1. freeze the shared capability contract;
2. verify current GGEN/LAI implementations against it;
3. implement GGEN mock/contract fixtures where useful;
4. implement the smallest LAI capability endpoint/adapter justified by existing runtime evidence;
5. validate text generation and OCR end-to-end;
6. verify privacy, cancellation, errors and evidence semantics;
7. validate on the target Android device where applicable;
8. document measured behavior;
9. only then expand to image, embeddings, tools and agent capabilities.

No direct dependency from GGEN to LAI internals is permitted.

## 12. Research requirement

Before implementing a non-trivial capability, agents must follow the SKB Android best-in-class research protocol and inspect Android, professional and open-source benchmarks. This research must inform the capability specification without creating duplicate runtime infrastructure.

## 13. Relationship to existing LAI roadmap

The architecture must preserve the distinction between implemented, validated, experimental, planned, blocked and not-started work. In particular, future gateway/RAG/multimodal/agent capabilities must not be described as implemented until source and evidence establish them.

## 14. Completion criterion

The boundary is aligned when each capability has one canonical owner, both repositories reference compatible contracts, GGEN remains independently useful as a creative/document product, LAI remains independently useful as an Android AI runtime, no duplicate provider/runtime/tool authority is introduced, and future implementation follows the research + ownership preflight.