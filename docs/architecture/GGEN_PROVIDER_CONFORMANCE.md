# GGEN Provider Conformance Requirements

**Status:** Proposed — documentation only
**Date:** 2026-08-24

This document defines what LAI must prove before its GGEN provider adapter is considered compatible.

## Required conformance areas

### 1. Capability advertisement

The provider must advertise only capabilities that are actually available in the current runtime configuration.

A capability may have states such as unavailable, supported, active or measured. Existing LAI evidence terminology should be preserved rather than collapsed into a generic boolean.

### 2. Request validation

Malformed, oversized or unsupported requests fail before expensive inference/tool execution whenever possible.

### 3. Privacy classification

Every operation has an explicit local/remote/cloud/custom classification. No silent egress is permitted.

### 4. Authorization

`tool.execute` and `agent.run` remain subject to LAI's existing permission and consent system.

### 5. Cancellation

Cancellation is tested for:

- queued requests;
- running local inference;
- streaming responses;
- tool preparation;
- tool execution where the underlying authority supports cancellation.

### 6. Error mapping

Internal LAI failures map to stable external error categories without leaking secrets or sensitive implementation state.

### 7. Artifact integrity

Returned artifacts are bounded, typed and validated before being exposed as completed outputs.

### 8. Version negotiation

The provider rejects incompatible required protocol versions deterministically.

### 9. Regression safety

The adapter must not change existing standalone LAI behavior. Existing CPU inference, model handling, automation policy, audit and privacy tests remain authoritative.

## Conformance test matrix

| Area | Required test |
|---|---|
| Discovery | Provider descriptor and capability list are deterministic |
| Text | Valid request returns typed text result |
| Vision | Unsupported modality fails explicitly |
| OCR | Structured OCR output validates against schema |
| Image | Artifact metadata validates |
| Embedding | Vector dimensions are validated |
| Tool | Permission denial prevents execution |
| Agent | Approval-required state is not reported as completed |
| Streaming | Ordered partial events and explicit finalization |
| Cancellation | Cancelled request never becomes completed |
| Error | Stable category mapping |
| Privacy | Local request has no remote fallback unless explicitly selected |
| Auth | Secrets never appear in responses/logs |
| Version | Incompatible protocol fails closed |
| Limits | Oversized request is rejected |

## Implementation rule

This document is a conformance target, not permission to implement. Implementation starts only after the shared protocol, transport and schemas are accepted as architecture decisions.
