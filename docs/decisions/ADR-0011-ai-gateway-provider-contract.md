# ADR-0011 — AI Gateway provider contract

## Status
Proposed

## Context
LAI currently exposes a provider-neutral `InferenceEngine` contract and a working embedded llama.cpp CPU runtime. The application still composes the embedded inference path directly. The canonical roadmap places an AI Gateway before a managed localhost server, multi-step agent runtime, and additional inference providers.

SKB evidence establishes the current device/backend constraints: CPU inference is the validated baseline; Vulkan and QNN remain unqualified and must not be represented as available acceleration. The Gateway must therefore improve provider composition without changing the truthfulness of backend availability.

## Problem
We need a stable application-facing boundary that can route AI requests without allowing UI code to depend on provider-specific SDKs or allowing an unavailable backend to masquerade as a working fallback.

The design must preserve:

- local-first and zero-egress invariants;
- existing CPU chat behavior;
- streaming and cancellation semantics;
- backend provenance and evidence state;
- explicit model/backend compatibility;
- user-controlled provider permissions;
- rollback to the current embedded inference path.

## Options
### Option A — Keep direct `InferenceEngine` composition
Minimal change, but every future provider/server integration would couple application orchestration to provider selection and make routing policy harder to test.

### Option B — Introduce an AI Gateway over the existing inference contract
Create a pure gateway contract and registry/router, with the existing embedded llama adapter as the first provider. The Gateway owns provider selection and lifecycle policy while provider implementations remain behind adapters.

### Option C — Replace the existing inference contract with a vendor-specific gateway
This would simplify one implementation but violate the vendor-neutral architecture and make future Vulkan/QNN/remote providers harder to isolate.

## Decision
**Proposed for review: Option B.**

The AI Gateway should be a policy-facing, provider-independent orchestration boundary. It must not become a vendor SDK abstraction leak.

Initial responsibilities:

1. register provider capabilities and evidence state;
2. resolve a requested model/backend against explicit compatibility metadata;
3. create a streaming inference session;
4. expose cancellation and deterministic session close;
5. report requested backend separately from actual backend/provenance;
6. distinguish unavailable, unsupported, failed, and successfully measured providers;
7. preserve content-free usage/audit records;
8. enforce local-first/network policy before any provider is invoked.

The first implementation provider is the existing embedded llama.cpp CPU adapter. No Vulkan, QNN, cloud, or LAN provider becomes implicitly enabled by this ADR.

The Gateway must not silently fall back from a requested unavailable backend to another backend. Fallback, when allowed by product policy, must be an explicit scheduler decision and must be observable in the resulting provenance.

## Consequences

### Positive
- UI and future task/agent layers depend on stable AI contracts rather than provider implementations.
- Backend qualification remains evidence-driven.
- CPU remains a safe rollback path.
- Future localhost/server and remote-provider work can reuse the same gateway boundary.
- Routing and lifecycle behavior become independently testable.

### Negative
- Adds a layer and migration work around the current direct composition.
- Provider capability metadata and provenance require more explicit state.
- Existing tests must be updated to exercise both the Gateway and the embedded adapter.

## Security implications

- The Gateway cannot grant shell, Accessibility, Shizuku, project, or filesystem authority.
- Provider registration cannot bypass `LocalFirstPolicy` or tool-consent policy.
- Network-capable providers must be explicitly identified and denied by default unless a later accepted policy enables them.
- Provider/model identifiers must not be treated as authorization credentials.

## Privacy implications

The Gateway must preserve the existing invariant that prompts, screens, documents, generations, and telemetry have no outbound path by default. Usage records must remain content-free.

## Licensing implications

The Gateway core remains Apache-2.0-compatible and vendor-neutral. A provider with a separate SDK/license must be isolated in its adapter/module and registered in the third-party/license records before distribution.

## Migration

1. Define pure Gateway contracts without deleting `InferenceEngine`.
2. Wrap the current embedded CPU provider behind the Gateway.
3. Migrate `MainViewModel`/application composition to request inference through the Gateway.
4. Keep the existing direct embedded adapter available as the rollback implementation during migration.
5. Add routing, streaming, cancellation, provenance, unavailable-provider, and regression tests.
6. Only after CI is green consider additional provider implementations.

No model storage format or existing user-owned GGUF format changes are required.

## Alternatives rejected

- Direct UI-to-provider dependencies: rejected because they violate the intended provider-neutral architecture.
- Generic automatic fallback: rejected because it can hide backend failure and produce false acceleration claims.
- Introducing Vulkan/QNN during Gateway migration: rejected because physical backend qualification is a separate evidence gate.
- Cloud-first Gateway design: rejected because LAI is local-first and remote providers belong to the later ecosystem phase.
