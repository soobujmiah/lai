# Proprietary and premium boundaries

**Status:** canonical commercial-boundary policy  
**Date:** 2026-08-18  
**Engineering table:** [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md)  
**Product overlay:** [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md)  
**Register:** [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md) IP-002, IP-006

No premium lock, DRM, or second license is implemented. This file exists so future code does not accidentally place intended proprietary functionality inside an irreversible open-source publication.

Legal classification of a derivative work is fact-specific. A separate directory or Gradle module is not, by itself, a proprietary work. **LEGAL REVIEW REQUIRED** before any proprietary label.

## Intended product split

### Free core (evaluation foundation)

Candidate only — exact set **DECISION REQUIRED** (IP-006):

- basic local LLM execution
- basic agent capability (today: one-shot confirmed tools)
- basic device interaction (confirmed Accessibility / named Shizuku)
- basic local workspace and model import
- functionality required to evaluate LAI

### Premium / commercial (not present)

Candidates only:

- advanced agent orchestration and multi-step workflows
- advanced automation recipe packs
- advanced RAG and knowledge management
- premium OCR
- advanced model management
- cloud / API orchestration
- advanced developer tools
- enterprise controls and advanced telemetry
- premium backends (including vendor NPU packs)
- commercial integrations and hosted services
- workflow marketplace / paid modules

## Open-core shape

```text
LAI Open Core (published Apache-2.0 Work)
        |
        +-- Runtime interfaces          (InferenceEngine, BackendId)
        |
        +-- Plugin / API boundaries     (plugins/api — PARTIAL)
        |
        +-- Free capabilities           (implemented local foundation)
        |
        +-- Proprietary LAI modules     (not present; review before code)
        |
        +-- Premium services            (not present)
        |
        +-- Optional cloud providers    (not present; consent required)
        |
        +-- Vendor-specific integrations (not present; vendor terms)
```

Engineering goal: keep future proprietary functionality **clearly separated** from already-published open-source components at an API, artifact, or service boundary.

Recommended technical packaging remains hybrid open-core (public core + modules/plugins + optional services). That is not a selected legal strategy. See [`LICENSING_STRATEGY.md`](LICENSING_STRATEGY.md).

## What must not enter the public core without clearance

- New modules intended to be proprietary
- Vendor SDK source or binaries
- Uncleared external PRs destined for a proprietary tree
- Copied private-project source
- Unreleased commercial algorithms intentionally kept secret
- Entitlement signing keys

## What can remain open

The already published Work remains Apache-2.0 (IP-001). Continuing to publish improvements to the free foundation under Apache-2.0 is compatible with a later premium layer **if** that layer is new, owned, and reviewed work.

## Knowledge graph

The visual knowledge graph / mind-map remains a **future modular** capability (`features:knowledge`), after RAG, local-first, optional later cloud intelligence only with consent. It must not be implemented in the core runtime. Premium possibility is recorded in [`../KNOWLEDGE_GRAPH.md`](../KNOWLEDGE_GRAPH.md). Not implemented.

## Hybrid AI business model

Device actions + local AI + optional cloud AI. Local holds private device data, basic inference, basic automation, local OCR/knowledge, and device control. Cloud/API, if added, is explicit, user-configured, key-protected, budgeted, and consent-gated. See [`../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md). Current source is local-only.

## Implementation prohibition

Do not implement entitlement, payment, feature locks, or a premium tree in this phase.
