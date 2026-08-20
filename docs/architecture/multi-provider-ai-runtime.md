# Multi-Provider AI Runtime Architecture

## Purpose

This document defines the target provider topology for LAI. It is an architecture target, not a claim that every provider is implemented or qualified today.

```text
                         LAI Application / Agent
                                  |
                           +------v------+
                           |  AI Gateway  |
                           +------+------+
                                  |
                    +-------------+-------------+
                    | Provider Registry/Router  |
                    +------+------+------+------+
                           |      |      |
                  +--------+  +---+---+  +--------+
                  |           |       |           |
               LOCAL CPU   LOCAL GPU LOCAL NPU   REMOTE
                  |           |       |           |
              llama.cpp     Vulkan   QNN/HTP   Cloud/HTTP
                                               /    |    \
                                            OpenAI Anthropic Custom
```

## Core principles

1. Application code depends on the Gateway, not provider SDKs.
2. Provider identity and model identity are separate.
3. Capability does not equal qualification.
4. Local CPU is the current validated baseline.
5. GPU and NPU providers remain evidence-gated until physical-device inference is qualified.
6. Remote providers are network-capable and require explicit network/user policy.
7. No provider may silently change a requested backend or privacy boundary.
8. Provider-specific controls remain behind provider/runtime-control interfaces.

## Provider descriptor

Conceptually each provider exposes:

```text
ProviderDescriptor
- id
- kind: LOCAL | REMOTE
- models
- backends
- capabilities
- evidence
- networkRequired
- credentialsRequired
- streaming
- cancellation
```

## Routing

Routing combines:

```text
request
 + model compatibility
 + backend capability
 + qualification evidence
 + network policy
 + user preference
 + privacy policy
 + provider availability
        -> selected provider
```

A requested provider/backend that is unavailable or unqualified must produce an explicit failure. A policy-authorized fallback must be observable in provenance.

## Remote providers

### OpenAI

Implemented as a dedicated provider adapter when enabled. Credentials belong to the credential store, not UI state or logs.

### Anthropic

Implemented as a dedicated provider adapter because its API contract differs from OpenAI-compatible APIs.

### OpenAI-compatible

A generic provider supports user-selected endpoints implementing the compatible contract. Endpoint configuration must be explicit; URL shape alone does not establish trust.

This provider can cover compatible hosted services and user-controlled LAN/local servers while keeping the core Gateway unchanged.

## Credential boundary

```text
Provider adapter -> CredentialStore -> encrypted Android storage
```

Secrets must not appear in:

- UI state;
- inference events;
- diagnostics;
- normal logs;
- provenance;
- content-free audit records.

## Runtime-control boundary

Inference and runtime control are separate contracts.

```text
AI Gateway
   |
   +--> InferenceEngine
   |
   +--> RuntimeControl (optional provider-specific implementation)
```

CPU may use thread limits; GPU/NPU may use other mechanisms. The application must not branch on provider-specific APIs.

## Security boundary

The AI Gateway does not acquire shell, Accessibility, Shizuku, project or filesystem authority. Remote providers do not bypass tool-consent or authorization policy.

## Evidence boundary

A provider may be present in the registry as `UNQUALIFIED` without being selectable as a usable backend. This allows the architecture to be ready for Vulkan/QNN without making unsupported acceleration claims.

## Implementation status

| Provider family | Architecture | Implementation | Qualification |
|---|---|---|---|
| Local CPU / llama.cpp | Target | Current baseline | Measured |
| Local GPU / Vulkan | Target | Pending | Unqualified |
| Local NPU / QNN/HTP | Target | Pending | Unqualified |
| OpenAI | Target | Pending | Remote policy required |
| Anthropic | Target | Pending | Remote policy required |
| OpenAI-compatible endpoint | Target | Contract planned | Endpoint-specific |

This table must be updated whenever implementation or evidence changes; architecture presence alone must never be reported as implementation availability.
