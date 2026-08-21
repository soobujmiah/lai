# AI Provider Policy

## Purpose

This document defines how LAI decides whether an AI provider may be selected. It complements ADR-0011 and ADR-0012.

## Provider classes

| Class | Examples | Default network behavior |
|---|---|---|
| Local | CPU, qualified GPU, qualified NPU | No network required |
| Remote | OpenAI, Anthropic, compatible endpoint | Denied until explicitly enabled |

## User policies

- `LOCAL_ONLY`: only qualified local providers.
- `CLOUD_ONLY`: only explicitly enabled remote providers.
- `PREFER_LOCAL`: qualified local provider first; remote fallback only when the user has separately allowed it.
- `PREFER_CLOUD`: explicitly enabled remote provider first.
- `AUTO`: choose from providers permitted by privacy, network, capability, evidence and availability policy.

## Explicit backend requests

A request such as `VULKAN`, `QNN`, `OPENAI`, or `ANTHROPIC` is not a suggestion. If that provider/backend is unavailable, unqualified, disabled, or incompatible, LAI reports failure rather than silently selecting another provider.

## Fallback

Fallback is a policy decision, not an implementation side effect. If fallback is allowed, the resulting provenance must record the requested and actual provider/backend plus the fallback reason.

## Network boundary

Remote inference requires all of:

1. a network-capable provider;
2. explicit provider configuration;
3. valid credential/configuration where required;
4. user policy permitting network inference;
5. endpoint validation appropriate to the provider type.

Local inference must not gain a network path merely because a remote provider is configured.

## Privacy

Default LAI operation remains local-first and zero-egress. A local failure does not authorize cloud transmission. Sensitive content is never uploaded as an implicit recovery mechanism.

## Credentials

Credentials are managed outside UI state and inference events. Secrets are excluded from logs, diagnostics, provenance and content-free audit records.

## OpenAI-compatible endpoints

Users may configure a compatible endpoint explicitly. The endpoint is not trusted based only on URL format. Configuration must identify the endpoint, protocol compatibility and network policy.

## Qualification

Provider availability must distinguish:

- `UNQUALIFIED` — known/declared but not validated;
- `MEASURED` — evidence supports use for the declared environment;
- `FAILED` — a known validation or runtime failure exists.

Only `MEASURED` providers may be advertised as usable inference backends.
