# Commercial product structure

**Status:** options comparison  
**Date:** 2026-08-18  
**Business decision:** **DECISION REQUIRED**  
**Legal review:** **LEGAL REVIEW REQUIRED**  
**Licensing analysis:** [`../legal/LICENSING_STRATEGY.md`](../legal/LICENSING_STRATEGY.md)

This document compares product-packaging structures. It does not select a final business model and does not change the single application ID established by ADR 0004.

## Evaluation criteria

Ownership control, commercial flexibility, community growth, developer adoption, security, privacy, maintainability, AI-agent development compatibility, future investment potential.

## Option A — Public LAI Core + commercial LAI Pro

One public runtime; a separately licensed Pro application or module set.

| Criterion | Assessment |
|---|---|
| Ownership control | Medium if Pro is entity-owned new work |
| Commercial flexibility | Subscription or one-time Pro |
| Community growth | High on the core |
| Developer adoption | High if the core remains useful |
| Security / privacy | Same local-first core; Pro must not weaken gates |
| Maintainability | Two products to keep in sync |
| AI-agent compatibility | Agents stay on the public core |
| Investment | Familiar open-core story |

Risk: Pro that is only a thin wrapper of the public Work may still be a Derivative Work. **LEGAL REVIEW REQUIRED.**

## Option B — Public LAI Runtime + commercial plugin marketplace

Public runtime hosts signed plugins. Commerce happens at the plugin artifact.

| Criterion | Assessment |
|---|---|
| Ownership control | High per plugin |
| Commercial flexibility | Add-on purchases, third-party sellers later |
| Community growth | High if the API is stable |
| Developer adoption | High for extension authors |
| Security / privacy | Requires the missing plugin manager, signing, isolation |
| Maintainability | API stability becomes a product |
| AI-agent compatibility | Agents implement the public host; plugins stay private |
| Investment | Marketplace operations cost |

Fits [`../architecture/plugin-architecture.md`](../architecture/plugin-architecture.md), which is currently PARTIAL.

## Option C — Public runtime + commercial cloud services

Client stays public. Revenue is hosted routing, sync, support.

| Criterion | Assessment |
|---|---|
| Ownership control | High for the service; none extra for the client |
| Commercial flexibility | Usage billing, seats |
| Community growth | High |
| Developer adoption | High |
| Security / privacy | Conflicts with current local-only invariants unless consent is explicit |
| Maintainability | Service reliability is a new org function |
| AI-agent compatibility | High |
| Investment | Requires operations, not just a license change |

Compatible with Apache-2.0 history without relicensing.

## Option D — Public core + commercial Enterprise edition

Enterprise policy, fleet, compliance packs on top of the public core.

| Criterion | Assessment |
|---|---|
| Ownership control | Medium–high for enterprise modules |
| Commercial flexibility | High ACV, longer sales cycle |
| Community growth | Core remains free |
| Developer adoption | Enterprise APIs must not fragment the core |
| Security / privacy | Enterprise telemetry is a privacy hazard if mis-designed |
| Maintainability | Policy matrix grows |
| AI-agent compatibility | High for the core |
| Investment | Needs organization identity, not present today |

## Option E — Hybrid combination of A–D

Public Apache-2.0 core and runtime; Pro capabilities as entity-owned modules or plugins; optional cloud services; Enterprise packs.

| Criterion | Assessment |
|---|---|
| Ownership control | Highest among options that do not rewrite history |
| Commercial flexibility | Supports Free / Pro / Enterprise / Cloud / add-on / support |
| Community growth | Preserved if the free tier stays meaningful |
| Developer adoption | Preserved if APIs stay public |
| Security / privacy | Achievable if cloud and enterprise remain explicit |
| Maintainability | Highest documentation and boundary burden |
| AI-agent compatibility | Public core remains the agent workspace |
| Investment | Clear layered story without claiming the public core is closed |

## Recommended technical direction

**Option E**, aligned with the recommended technical direction in [`../legal/LICENSING_STRATEGY.md`](../legal/LICENSING_STRATEGY.md).

This is an engineering packaging recommendation. It is not a selected legal or pricing decision.

ADR 0004 (one local-first application) remains the *runtime* product shape: one `dev.lai.runtime` upgrade path. Commercial SKUs, if any, should prefer entitlement and optional modules over a return to multiple application IDs, unless a later ADR supersedes ADR 0004. Dual application IDs were already tried (ADR 0003) and superseded.

## Decision

**DECISION REQUIRED**

**LEGAL REVIEW REQUIRED**
