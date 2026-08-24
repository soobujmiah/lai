# GGEN Integration Contract

**Status:** Proposed architecture contract — documentation only
**Date:** 2026-08-24
**Consumer:** GGEN AI Creative & Document Studio
**Provider:** LAI when selected

## 1. Product boundary

LAI remains an independent AI platform/runtime. GGEN remains an independent creative and document application.

LAI shall expose provider capabilities without importing or depending on GGEN's Flutter UI, canvas implementation, project schema, or document model.

GGEN may use LAI as one provider among local, cloud, remote and custom providers.

## 2. Provider role

When connected to GGEN, LAI acts as an AI capability provider and, where explicitly requested and authorized, an agent/tool execution provider.

LAI owns:

- local inference;
- backend selection and scheduling;
- CPU/GPU/NPU execution;
- model lifecycle;
- device/thermal/memory constraints;
- agent runtime;
- Android automation authorities;
- tool permission and audit policy;
- provider diagnostics.

GGEN owns the user-facing creative/document workflow and acceptance of returned artifacts.

## 3. Capability surface

LAI may advertise:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `tool.execute`
- `agent.run`

A capability is not considered available merely because its code exists. Availability must be runtime-advertised and evidence-backed.

The LAI provider must report unsupported or unavailable capabilities explicitly rather than fabricating output.

## 4. Provider descriptor

The external provider descriptor should expose, at minimum:

```text
providerId
providerName
protocolVersion
capabilities[]
modalities[]
streaming
cancellation
privacyClass
localOrRemote
authenticationMode
limits
```

The descriptor must not expose internal implementation classes as part of the stable external contract.

## 5. Request boundary

Requests from GGEN should use a provider-neutral envelope:

```text
requestId
protocolVersion
capability
inputs[]
options
context
privacyPolicy
requestedOutput
cancellation
```

LAI maps the envelope into its internal inference, agent, model and tool contracts.

Provider-specific options must remain namespaced and optional.

## 6. Response boundary

LAI should return:

```text
requestId
status
outputs[]
usage?
providerMetadata?
error?
```

Output artifacts must be typed. A provider metadata field may expose LAI-specific facts without making them part of GGEN's canonical document schema.

## 7. Status model

The provider must distinguish at least:

- accepted;
- queued;
- running;
- streaming;
- completed;
- cancelled;
- denied;
- failed;
- unavailable.

For tool and agent operations, `completed` means execution actually occurred. `prepared`, `suggested`, or `approved` must never be reported as completed.

## 8. Tool/agent safety

LAI's existing consent, permission and audit architecture remains authoritative for Android automation and consequential tools.

A GGEN request cannot bypass LAI's policy layer.

For agent operations, LAI should expose enough state for GGEN to display whether execution requires approval or has been blocked.

No integration design may create a generic raw-shell escape hatch.

## 9. Local-first and privacy

When an operation is executed locally by LAI, GGEN may display the provider's local classification.

LAI must preserve its existing privacy invariants: prompts, screen structures, captures, models and user intelligence remain local unless an explicitly supported remote/cloud path is selected.

A GGEN UI label such as `local` must be backed by actual LAI provider state; it must not be inferred from the presence of a LAI connection alone.

## 10. Failure behavior

LAI should map internal failures to stable external categories:

- unsupported capability;
- invalid request;
- authentication required;
- permission denied;
- resource unavailable;
- timeout;
- cancelled;
- quota exceeded;
- policy blocked;
- provider unavailable;
- execution failed;
- invalid output.

Internal stack traces, model paths, tokens and sensitive device state must not become mandatory external payloads.

## 11. Streaming/cancellation

LAI may stream partial results.

Requirements:

- one request ID per operation;
- explicit sequence/finalization semantics;
- idempotent cancellation;
- no false completion after cancellation;
- partial artifacts distinguishable from final results.

Cancellation must propagate toward the underlying runtime when technically supported and must report the actual resulting state.

## 12. Artifact boundary

GGEN may accept returned image, text, OCR, embedding or structured artifacts into its own project model.

LAI retains ownership of:

- models;
- model caches;
- runtime sessions;
- device profiles;
- tool audit records;
- internal agent state unless explicitly exported.

Large artifacts should prefer explicit references/transfer mechanisms over embedding unbounded binary payloads in control messages.

## 13. Authentication and secrets

LAI must not require GGEN to place secrets inside project documents or request logs.

If LAI uses authentication for a remote service, credentials remain provider configuration and secure storage. They must never be returned as provider metadata.

## 14. Transport independence

This document intentionally does not select HTTP, WebSocket, Unix/domain sockets, Android Binder, or another transport.

The transport must satisfy the capability contract and preserve:

- request identity;
- typed payloads;
- streaming;
- cancellation;
- version negotiation;
- structured errors;
- authentication boundaries.

Transport selection is a separate architecture decision.

## 15. Versioning

The external capability protocol must have explicit version negotiation.

LAI should support additive evolution without breaking existing GGEN clients. Semantic incompatibility requires a major protocol revision.

Unknown optional fields may be ignored. Unknown required semantics must fail closed.

## 16. Non-dependency rule

LAI shall not add a dependency on GGEN merely to implement this provider boundary.

The provider contract belongs to a stable interoperability layer. GGEN-specific UI concepts, Flutter classes and GGEN project internals are prohibited from the LAI core/runtime.

## 17. Implementation gate

Implementation remains blocked until the shared architecture work documents:

1. canonical protocol version;
2. transport;
3. schemas;
4. provider discovery/registration;
5. authentication configuration;
6. streaming/cancellation;
7. artifact transfer/reference rules;
8. tool/agent authorization state;
9. privacy classification;
10. compatibility and conformance tests.

The corresponding GGEN document is `docs/architecture/ggen-lai-ai-capability-contract.md` in GGEN.
