# Hybrid inference and provider architecture

**Status:** planning overlay on existing AI docs  
**Date:** 2026-08-18  
**Current source:** local-only `InferenceEngine` via `NativeInferenceEngine`; no AI Gateway  
**Canonical product docs:** [`../AI_PROVIDERS.md`](../AI_PROVIDERS.md), [`../AI_RUNTIME.md`](../AI_RUNTIME.md), [`../CLOUD.md`](../CLOUD.md), [`../architecture/ai-architecture.md`](ai-architecture.md)  
**Privacy invariants:** [`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md)

This file records the commercial and permission boundary for future local + cloud routing. It does not replace the documents above and does not implement providers.

## Architectural sketch

```text
User
  ↓
LAI Orchestrator
  ↓
Policy / Permission Layer     (consent, local-first, future entitlement)
  ↓
Model Router
  ├── Local CPU        (llama-cpu)          IMPLEMENTED
  ├── Local GPU        (llama-vulkan)       PARTIAL / planned generate
  ├── Local NPU        (dedicated QNN module, ADR 0005)  PLANNED
  ├── User local server (loopback OpenAI-compatible)     MISSING
  └── Cloud provider   (user-configured)                 MISSING
  ↓
Agent Planner                 MISSING (one-shot tools IMPLEMENTED)
  ↓
Tool Registry                 IMPLEMENTED (built-in)
  ↓
Permission Gate               IMPLEMENTED for one-shot tools
  ↓
Android / File / Network / Developer tools
  ↓
Audit                         IMPLEMENTED (content-free)
  ↓
Verification
```

## Three brains

| Layer | Role | Data leaving device |
|---|---|---|
| Local brain | On-device CPU/GPU/NPU inference | No |
| Cloud brain | User-configured remote model API | Only after explicit permission for that request/session |
| Device action layer | Accessibility, Shizuku, files, future developer tools | No, except user-initiated reviewed downloads |

Default remains local. Cloud is never selected because it is faster or cheaper unless the user configured that provider and the permission layer allows egress.

## Provider-agnostic chain

```text
Provider → Model → Capability → Router → Agent → Tool system → Permission → Audit
```

Core types stay provider-neutral. UI must not import a provider SDK. Backend IDs remain opaque (ADR 0005). Candidate providers listed in product docs (OpenAI, Anthropic, Gemini, OpenRouter, Ollama, local OpenAI-compatible servers, future providers) are examples, not hardcoded core enumerations.

Bring-your-own-key is the default commercial posture for third-party cloud models. LAI-hosted models, if offered later, are a separate service SKU under [`../legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md`](../legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md).

Business-model elements, when implemented, include: provider abstraction, API-key protection (Keystore), cost controls, token budgets, provider-specific terms review, data-transfer consent, and an honest local/cloud badge. None of those clients exist in current source.

## Permission boundary for any cloud transmission

A cloud or LAN request requires all of the following, when that path is implemented:

1. User-configured provider endpoint (no built-in hidden host for prompts).
2. Explicit consent that data will leave the device, shown at configuration and at first use.
3. Per-request or per-session `localOnly` override that forces local.
4. Denial when the tool or document class is marked SENSITIVE unless a narrower, documented exception exists.
5. Secrets in Android Keystore (or equivalent), never in `registry.json` or diagnostics.
6. No silent fallback from local to cloud, or from cloud to LAN.
7. Honest UI badges: Local / Cloud / Remote ([`../CLOUD.md`](../CLOUD.md)).
8. Failure of cloud returns a visible error or an explicit local fallback notice—not a silent swap.

Until that path exists, [`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md) remains the binding rule: user intelligence never leaves the device.

## What must remain local

Even after a cloud adapter exists:

- Accessibility trees, screenshots, passwords, and OCR of the screen, unless the user later opts into a narrowly scoped export (none exists today);
- Shizuku output;
- tool arguments and results in the persistent audit (already content-free);
- entitlement secrets and signing keys;
- catalog private keys;
- user documents by default.

Documents and RAG corpora must not auto-upload.

## Commercial intersection

| Function | Entitlement candidate | Privacy |
|---|---|---|
| Local CPU inference | FREE foundation | Local |
| Local GPU/NPU | Device-owned; vendor pack if SDK terms require | Local |
| BYO cloud provider | PRO or CLOUD add-on | Explicit egress |
| LAI-hosted routing | CLOUD service SKU | Service terms + consent |
| Usage billing | CLOUD | Meter tokens, not prompt text, in LAI-operated billing |

## Decision

Provider adapters and an AI Gateway remain **MISSING**. Implementation requires an ADR from the existing “next decisions” list in [`../decisions/README.md`](../decisions/README.md), plus privacy and legal review of each provider’s terms.

**LEGAL REVIEW REQUIRED** for provider terms before any adapter ships.
