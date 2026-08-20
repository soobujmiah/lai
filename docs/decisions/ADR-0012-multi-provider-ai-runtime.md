# ADR-0012 — Multi-provider AI runtime

## Status
Proposed

## Context
LAI must support more than embedded local inference. The long-term runtime needs one stable application-facing AI boundary that can host local CPU, GPU and NPU execution as well as explicitly enabled cloud APIs and arbitrary compatible endpoints.

The architecture must therefore be backend/provider neutral without pretending that every provider is currently qualified. Current SKB evidence establishes CPU as the validated local baseline; Vulkan and QNN/HTP remain qualification-gated. Remote providers are network-capable and must never bypass LAI's local-first, zero-egress-by-default policy.

## Decision
Extend the AI Gateway introduced by ADR-0011 into a multi-provider runtime boundary. Providers are registered behind a common contract and are selected through explicit capability, evidence, compatibility, privacy, network and user-policy checks.

The target provider families are:

```text
LocalCpuProvider       -> llama.cpp CPU
LocalGpuProvider       -> qualified GPU runtime (for example Vulkan)
LocalNpuProvider       -> qualified NPU runtime (for example QNN/HTP)
OpenAiProvider         -> OpenAI API
AnthropicProvider      -> Anthropic API
OpenAiCompatibleProvider -> user-selected compatible HTTP endpoint
```

Only providers with the required qualification/evidence state may advertise themselves as usable. Adding a provider type to the architecture does not claim that its implementation or device qualification exists.

## Provider model

Every provider has:

- stable provider ID;
- provider kind (`LOCAL` or `REMOTE`);
- supported model identifiers/patterns;
- supported backend/runtime identifiers;
- capability metadata;
- evidence/qualification state;
- network requirement;
- credential requirement;
- supported streaming/cancellation semantics;
- provenance metadata;
- explicit lifecycle and error mapping.

Model identity and provider identity remain separate. A model such as a local GGUF model is not itself a provider; the provider describes how that model is executed.

## Routing policy

The Gateway must support explicit policies such as:

- `LOCAL_ONLY` — never send inference to a network provider;
- `CLOUD_ONLY` — use an explicitly enabled remote provider;
- `PREFER_LOCAL` — local provider first; remote fallback only when separately allowed;
- `PREFER_CLOUD` — remote provider first when explicitly enabled;
- `AUTO` — select according to declared capability, privacy, availability and user policy.

`AUTO` does not mean silent fallback. A request for a specific backend/provider must fail visibly when that provider is unavailable or unqualified. Any policy-authorized fallback must be represented in provenance.

## OpenAI-compatible endpoints

LAI will provide a generic OpenAI-compatible HTTP provider for services exposing the compatible request/response contract. This can cover OpenAI-compatible hosted services, LAN servers and user-selected custom endpoints without creating a new adapter for every compatible service.

Compatibility must be detected/configured explicitly; an arbitrary URL must never be treated as trusted merely because it resembles an OpenAI endpoint.

## Native/cloud provider isolation

Provider-specific SDKs, HTTP clients, authentication formats and native runtime details must remain inside provider adapters. UI, agent and application orchestration code must depend only on Gateway contracts.

Cloud providers are network-capable and therefore cannot be invoked by the local inference path implicitly. The Gateway must enforce network policy before provider invocation.

## Credential security

API credentials must be owned by a dedicated credential store and must not be stored in UI state, inference events, diagnostics, provenance, normal logs or model metadata.

Authorization headers and secrets must never be included in content-free audit records. Provider identifiers are not credentials.

## Provenance

Every completed or failed inference session must be able to identify, without storing prompt/response content:

- requested provider/backend;
- selected provider;
- actual backend/runtime;
- evidence state;
- local vs remote classification;
- fallback decision, if any;
- model identifier/version where appropriate;
- failure category when unsuccessful.

## Security and privacy

The Gateway cannot grant Accessibility, Shizuku, shell, project or filesystem authority. Remote providers cannot bypass tool-consent rules.

Default behavior remains local-first and zero-egress. Remote inference requires an explicit policy/configuration path. Sensitive content must not be transmitted merely because a local provider failed.

## Thermal and runtime control

Thermal/resource controls are workload-level policies, not provider-specific APIs exposed to the UI. Providers may implement a separate runtime-control interface for their execution mechanism. For example, CPU may expose decode-thread limits while GPU/NPU providers may expose different controls.

This prevents the application from accumulating CPU/Vulkan/QNN-specific conditionals.

## Implementation order

1. Keep `InferenceEngine` stable.
2. Define the provider-neutral Gateway, provider descriptor, capability, evidence, provenance and routing policy contracts.
3. Wrap the validated CPU llama.cpp runtime as the first provider.
4. Migrate application composition to the Gateway while preserving native runtime controls behind a separate control boundary.
5. Add tests for routing, privacy policy, credentials, provenance, streaming, cancellation and unavailable providers.
6. Add the OpenAI-compatible provider contract and credential/configuration boundary without enabling network inference by default.
7. Add OpenAI and Anthropic adapters only after their security/license records and API behavior are validated.
8. Add Vulkan and QNN/HTP providers only after physical-device qualification evidence exists.
9. Add localhost/LAN server providers under the same remote/custom endpoint policy.

## Consequences

### Positive
- CPU, GPU, NPU and cloud providers share one stable application boundary.
- Future provider additions do not require UI or agent rewrites.
- OpenAI-compatible endpoints reduce adapter duplication.
- Privacy and network policy are enforced centrally.
- Provider-specific runtime controls stay isolated.
- Backend claims remain evidence-driven.

### Negative
- The Gateway and provider registry become more sophisticated.
- Capability, evidence and routing metadata require careful testing.
- Cloud credentials, SDK licenses and network error handling add operational complexity.

## Alternatives rejected

- Separate application paths for local and cloud AI: rejected because it duplicates routing, streaming, cancellation and audit behavior.
- One vendor-specific universal API: rejected because OpenAI, Anthropic, local runtimes and custom endpoints have materially different contracts.
- Automatic cloud fallback after local failure: rejected because it violates local-first/privacy expectations unless explicitly authorized by policy.
- Treating GPU/NPU presence as qualification: rejected because hardware/runtime presence is not inference evidence.
