# LAI commercial ownership and intellectual property policy

**Status:** permanent project policy (documentation)  
**Date:** 2026-08-18  
**Current project license:** Apache License 2.0 — [`../../LICENSE`](../../LICENSE)  
**Current audit:** [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md)  
**Decision register:** [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md)  
**This document is not legal advice.** Interpretive items are marked **LEGAL REVIEW REQUIRED**.

This policy is the canonical commercial-IP specification. Other legal documents implement a single topic; they must not contradict this file.

## Purpose

LAI is developed as a commercial software product. Selected basic capabilities may remain free. Advanced capabilities may later be paid, premium, proprietary, enterprise, or subscription-based.

The repository is public for development and AI-agent workflow reasons. Public visibility is not permission to treat every future capability as open source.

The engineering objective is to preserve the maximum **legally controllable** ownership of LAI-owned original work, without claiming rights LAI does not have.

## Rights-holder terminology

Until a formal legal entity or named copyright owner is established in a recorded instrument, this documentation uses:

**LAI project owner / rights holder**

That phrase is a placeholder for the person or entity that will hold LAI-owned rights. It is **not** a company name, assignment, or proof of ownership. Creating or naming a legal entity is **DECISION REQUIRED**.

AI agents, bots, and commit author strings are not rights holders. See [`AI_CODE_PROVENANCE.md`](AI_CODE_PROVENANCE.md).

## Classes of work

### A. Existing publicly released Apache-2.0 work

Includes the public Git history, tags, GitHub Release APKs, and documentation already distributed under [`../../LICENSE`](../../LICENSE).

Facts:

- The grant is perpetual, worldwide, non-exclusive, royalty-free, and irrevocable (copyright; patent grant as stated in section 3).
- Recipients of those copies retain the rights granted by Apache-2.0, subject to its conditions.
- This history **cannot** be converted retroactively into exclusive proprietary software.

Forbidden:

- pretending previous Apache-2.0 releases never existed;
- removing Apache-2.0 obligations from already distributed copies;
- claiming exclusive rights over those copies;
- rewriting Git history solely to hide the previous license;
- deleting [`../../LICENSE`](../../LICENSE) as a shortcut.

**IP-001:** preserve existing Apache-2.0 obligations. Status: **ACCEPTED.**

### B. New LAI-authored work

Original source, documentation, tests, original icons, and original design-system tokens created after a recorded ownership decision, and not copied from third parties.

Ownership of new work vests according to applicable law and any contract with the human who directed it. This policy does not invent that vesting. **LEGAL REVIEW REQUIRED.**

New work may be placed under Apache-2.0, a different license, or kept unpublished, **only** if the LAI project owner / rights holder actually controls that work. See [`OWNERSHIP_MODEL.md`](OWNERSHIP_MODEL.md) and [`CONTRIBUTOR_RIGHTS.md`](CONTRIBUTOR_RIGHTS.md).

### C. Third-party dependencies

AndroidX, Kotlin, OkHttp, WorkManager, Shizuku, llama.cpp, build tools, and transitives retain their own licenses. LAI does not own them. Intake: [`THIRD_PARTY_INTAKE.md`](THIRD_PARTY_INTAKE.md). Inventory: [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md).

### D. External model weights

Qwen and any future model remain under the upstream model license. Catalog metadata and SHA-256 are not a redistribution grant. Policy: [`MODEL_IP_POLICY.md`](MODEL_IP_POLICY.md).

### E. Vendor SDKs

Qualcomm QAIRT/QNN and similar SDKs remain under vendor agreements. Those terms never become LAI’s project license. Policy: [`VENDOR_LICENSE_POLICY.md`](VENDOR_LICENSE_POLICY.md).

### F. Community contributions

Inbound terms are currently Apache-2.0 section 5 unless a separate agreement exists. No CLA or DCO is in force. Policy: [`CONTRIBUTOR_RIGHTS.md`](CONTRIBUTOR_RIGHTS.md).

A contribution that lacks ownership clearance must not enter a future proprietary core.

### G. Proprietary / premium modules

Not present. Future modules require an explicit ownership and licensing review before implementation (**IP-002**, **ACCEPTED** as process). Boundaries: [`PROPRIETARY_BOUNDARIES.md`](PROPRIETARY_BOUNDARIES.md). Directory names do not create legal separation. **LEGAL REVIEW REQUIRED** per module.

### H. Future commercial infrastructure

Entitlement, license servers, billing, hosted routing, and organization controls are not implemented. Designs: [`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md), [`../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md). Secrets for that infrastructure must never enter the public tree ([`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md)).

### I. Trademarks and branding

Copyright licensing does not grant trademark rights. No registration is verified. Policy: [`TRADEMARK_POLICY.md`](TRADEMARK_POLICY.md).

### J. Documentation and other creative assets

Project documentation, original vector `ic_lai.xml`, and the LAI design system ([`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md)) are LAI-authored creative works unless a third-party source is identified. They follow the same license as the tree they are published in. Third-party visual assets must not be copied.

## Commercial control method

Future commercial control is established by:

1. leaving published Apache-2.0 obligations in place;
2. owning new original work through a documented LAI project owner / rights holder;
3. using module, plugin, and service boundaries so new work is not accidentally published under Apache-2.0 without review;
4. controlling inbound contributions before they enter a proprietary core;
5. reviewing every third-party, model, dataset, and SDK;
6. protecting trademarks and secrets separately from copyright.

This is not “anti-theft” protection of already published Apache-2.0 code. Recipients of that code may use it as Apache-2.0 allows.

## Free versus premium (product principle)

LAI provides a meaningful free foundation. Paid layers correspond to additional engineering, infrastructure, or enterprise capability. Artificial frustration of free evaluation is out of scope. Mapping: [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md). Exact FREE set: **DECISION REQUIRED.**

## Development sequence

```text
DOCUMENTATION FIRST
→ ARCHITECTURE REVIEW
→ LICENSING / IP REVIEW
→ IMPLEMENTATION
→ TESTING
→ RELEASE REVIEW
```

Implementation must not precede licensing review. Documentation of a planned premium module is not authorization to implement it.

Every major feature defines, before code: purpose, architecture, dependencies, ownership, license, data flow, privacy, security, commercial status, free/premium status, extension point, and tests.

## Authoritative files

| Question | File |
|---|---|
| Current license text | [`../../LICENSE`](../../LICENSE) |
| What was already published | [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md) |
| This policy | this file |
| Ownership facts / gaps | [`OWNERSHIP_MODEL.md`](OWNERSHIP_MODEL.md) |
| Recorded IP decisions | [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md) |
| Contributors | [`CONTRIBUTOR_RIGHTS.md`](CONTRIBUTOR_RIGHTS.md) |
| AI provenance | [`AI_CODE_PROVENANCE.md`](AI_CODE_PROVENANCE.md) |
| Dependency intake | [`THIRD_PARTY_INTAKE.md`](THIRD_PARTY_INTAKE.md) |
| Models | [`MODEL_IP_POLICY.md`](MODEL_IP_POLICY.md) |
| Vendor SDKs | [`VENDOR_LICENSE_POLICY.md`](VENDOR_LICENSE_POLICY.md) |
| Premium boundaries | [`PROPRIETARY_BOUNDARIES.md`](PROPRIETARY_BOUNDARIES.md) |
| Trademarks | [`TRADEMARK_POLICY.md`](TRADEMARK_POLICY.md) |
| Release gates | [`RELEASE_COMPLIANCE.md`](RELEASE_COMPLIANCE.md) |
| Public Git | [`../governance/PUBLIC_DEVELOPMENT_POLICY.md`](../governance/PUBLIC_DEVELOPMENT_POLICY.md) |

## What must not change without explicit approval

- [`../../LICENSE`](../../LICENSE)
- Historical tags and already published Apache-2.0 grants
- Privacy invariants that forbid silent egress ([`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md))
- Authority isolation (Accessibility, Shizuku, JNI)
- Acceptance of unknown-license dependencies

## Decision

**IP-001** and **IP-002** are accepted process/fact records. Strategy for new-work licensing, entity formation, CLA adoption, and the exact free set remain **DECISION REQUIRED**.
