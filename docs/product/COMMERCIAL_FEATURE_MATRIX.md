# Commercial feature matrix

**Status:** architectural starting point  
**Date:** 2026-08-18  
**Implementation:** none. No entitlement, payment, or feature lock exists in source  
**Capability classification:** **DECISION REQUIRED**  
**Related ADR:** [`../decisions/ADR-0104-free-vs-paid-capability-model.md`](../decisions/ADR-0104-free-vs-paid-capability-model.md)

This document sketches an entitlement taxonomy. It does not finalize pricing, SKU names, or the exact free set. It must not be hard-coded into the application in this phase.

The functional feature matrix remains [`feature-matrix.md`](feature-matrix.md). The canonical roadmap remains [`../ROADMAP.md`](../ROADMAP.md). This file adds a commercial overlay only.

## Product principle

LAI provides a meaningful free foundation. Paid layers correspond to additional engineering, infrastructure, enterprise controls, or advanced capability. Artificial limitations whose only purpose is to frustrate free users are out of scope.

Public Apache-2.0 source that implements a capability can be rebuilt by recipients even if a commercial SKU gates that capability in official binaries. Entitlement is a product-distribution control, not a revocation of Apache-2.0. See [`../legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md`](../legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md).

## Entitlement tiers (starting point)

```text
FREE
├── Basic local inference
├── Basic agent
├── Basic tools
├── Basic device automation
└── Basic local workspace

PRO
├── Advanced agents
├── Advanced workflows
├── Advanced RAG
├── Advanced OCR
├── Advanced developer tools
├── Advanced model routing
└── Premium integrations

ENTERPRISE
├── Enterprise policy
├── Fleet / device management
├── Advanced security
├── Organization controls
├── Audit / compliance
└── Enterprise support

CLOUD / OPTIONAL SERVICES
├── Cloud AI routing
├── Hosted models
├── Cloud RAG
├── Sync
├── Remote orchestration
└── Usage-based services
```

Supported commercial motions (architecture only): Free, Pro, Premium, Enterprise, one-time license, subscription, add-on / module purchase, cloud/API usage billing, optional commercial support.

## Suggested mapping of current and planned capabilities

Statuses in the “Source status” column come from [`feature-matrix.md`](feature-matrix.md) and related docs. “Candidate tier” is not approved.

| Capability | Source status | Candidate tier | Notes |
|---|---|---|---|
| Local llama.cpp CPU chat | IMPLEMENTED | FREE | Meaningful foundation |
| Model download / import / Keep copy | PARTIAL | FREE | User-owned weights; not a LAI SKU |
| One-shot confirmed tools | IMPLEMENTED | FREE | Safety gates stay on in every tier |
| Accessibility snapshot / click / type / scroll | PARTIAL | FREE basic; PRO for expanded recipes | Authority remains user-confirmed |
| Shizuku named operations | PARTIAL | FREE basic; PRO/ENTERPRISE for broader recipes | No raw shell in any tier |
| Typed settings + SAF workspace | PARTIAL | FREE | Local workspace is foundational |
| Diagnostics export | IMPLEMENTED | FREE | Privacy-filtered |
| Persistent tool audit | IMPLEMENTED | FREE core; ENTERPRISE export/retention packs | Content-free ledger already exists |
| Multi-step agent / task center | MISSING | PRO | Real additional runtime |
| Diff / checkpoint / rollback | MISSING | PRO | Additional storage design |
| Local RAG | MISSING | PRO | Additional models + index |
| Printed Bangla OCR (licensed engine) | PARTIAL placeholder | PRO | Engine/dataset license required |
| Handwriting OCR | PLANNED | PRO / add-on | Dataset license required |
| Developer / Git / terminal / Linux runtime | MISSING | PRO | Large additional surface |
| Vulkan / QNN backends | PARTIAL / PLANNED | FREE when local device-owned; PREMIUM if vendor pack | Vendor SDK terms may force a pack |
| Cloud / remote providers | MISSING | CLOUD / BYO key in PRO | User-supplied keys; no silent upload |
| Plugin manager / marketplace | MISSING | add-on / marketplace | Separate artifacts |
| Knowledge graph UI | MISSING | PRO | After RAG |
| Organization policy / fleet | MISSING | ENTERPRISE | No current source |
| Hosted sync / cloud RAG | MISSING | CLOUD | Conflicts with current privacy invariants unless explicit consent |
| Commercial support | n/a | support SKU | Contract, not code |

## Capability identifiers

Future implementation, if approved, uses stable capability IDs rather than hardcoded tier names in feature code. Example identifiers (not implemented):

| ID | Description |
|---|---|
| `cap.inference.local.basic` | Load one reviewed local model and stream chat |
| `cap.agent.oneshot` | One-shot confirmed tool proposals |
| `cap.agent.multistep` | Multi-step plan / verify loop |
| `cap.rag.local` | Local retrieval |
| `cap.ocr.printed_bn` | Licensed printed Bangla OCR |
| `cap.dev.workstation` | Editor / Git / terminal pack |
| `cap.cloud.provider` | User-configured remote provider |
| `cap.org.policy` | Organization policy pack |

Tiers are mappings from SKU → set of capability IDs. Mappings can change without renaming IDs. See [`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md).

## What remains undecided

- Exact FREE set.
- Whether Pro is subscription, one-time, or both.
- Whether official binaries gate capabilities that remain present in public source.
- Whether premium modules live in a private tree, a plugin artifact, or a service.
- Price.

**DECISION REQUIRED.**
