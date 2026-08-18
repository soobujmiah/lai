# Licensing strategy

**Status:** planning document  
**Current project license:** Apache License 2.0 ([`../../LICENSE`](../../LICENSE))  
**Current audit:** [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md)  
**Strategy decision:** **DECISION REQUIRED**  
**Legal interpretation:** **LEGAL REVIEW REQUIRED**  
**Date:** 2026-08-18  

This document compares candidate future licensing structures. It does not change [`../../LICENSE`](../../LICENSE), relicense any commit, or select a final legal strategy.

## Binding constraints

The following are recorded facts, not optional preferences:

1. The public repository currently declares Apache License 2.0. GitHub metadata reports `spdx_id: Apache-2.0`.
2. Apache-2.0 permits commercial use, modification, sublicensing, and distribution of the licensed Work, including in Object form, subject to the conditions in [`../../LICENSE`](../../LICENSE) sections 4–9.
3. The grant is perpetual, worldwide, non-exclusive, royalty-free, and irrevocable (copyright grant; patent grant is irrevocable except as stated in section 3).
4. Code already distributed under Apache-2.0 cannot be retroactively declared proprietary-only. Recipients of those copies retain the rights granted by that license.
5. Third-party components retain their own licenses regardless of any future LAI product license.
6. No Contributor License Agreement or Developer Certificate of Origin is currently in force. See [`CONTRIBUTOR_LICENSING_POLICY.md`](CONTRIBUTOR_LICENSING_POLICY.md) and [`OWNERSHIP_MODEL.md`](OWNERSHIP_MODEL.md).
7. A commercial product is intended. A meaningful free capability tier is a product principle. See [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md).

## Recommended technical direction

**Recommended technical direction:** hybrid open-core with separately authored commercial modules and optional commercial services (combination of Options 4, 6, 7, and 8 below).

This direction is an engineering recommendation only. It is **not** a selected legal strategy.

Rationale that stays inside documented facts:

- It does not require relicensing already published Apache-2.0 commits.
- It preserves a public development repository compatible with the current GitHub and AI-agent workflow.
- It matches existing module seams (`core/*`, `runtime/*`, `plugins/api`) described in [`../architecture/module-map.md`](../architecture/module-map.md) and [`../architecture/plugin-architecture.md`](../architecture/plugin-architecture.md).
- It allows a meaningful free tier implemented from the existing public Work.
- It keeps premium value in new modules, hosted services, support, and enterprise controls that are not automatically the same Work.
- Directory separation alone does not create legal proprietary status. Module boundaries are technical candidates. **LEGAL REVIEW REQUIRED** before treating any module as separately licensable.

**DECISION REQUIRED.** **LEGAL REVIEW REQUIRED.**

---

## Option 1 — Continue Apache-2.0 for the whole product

Keep the public tree and future LAI-authored product code under Apache-2.0.

| Dimension | Assessment |
|---|---|
| Advantages | Lowest process cost; matches current `LICENSE`, README, and GitHub metadata; easy inbound contributions; commercially usable; compatible with AndroidX/Kotlin/OkHttp/Shizuku Apache-2.0 families; no relicensing event. |
| Disadvantages | No exclusive control of the public Work; forks and competing redistributions remain lawful if section 4 is met; paid features implemented in the same Apache-2.0 tree are also redistributable by recipients. |
| Ownership requirements | Current collective notice is sufficient to continue; a named owner is still useful for trademarks and contracts. |
| Contributor implications | Apache-2.0 §5 already treats intentional submissions as Apache-2.0 unless stated otherwise. |
| Compatibility with Apache history | Full. |
| Commercial implications | Paid apps, support, and services remain allowed. Exclusive lock-down of the same code is not available. |
| Distribution implications | Continue shipping license text and required third-party notices. |
| Fork implications | Forks are expected and lawful under Apache-2.0. |
| Competitive implications | A competitor may reuse the published Work subject to Apache-2.0. Brand and trademark, if later registered, remain separate. |
| Future re-licensing implications | Later relicensing of the whole history still needs all copyright holders. |
| Technical architecture implications | No new entitlement or module split is legally required. Entitlement may still be added as product policy, but source availability limits enforcement against a rebuild from public source. |
| Legal review requirements | Notice/SBOM/APK attribution review for production releases. |

---

## Option 2 — Proprietary future modules

Continue Apache-2.0 for already published code. Author new modules under a proprietary license owned by a defined legal entity.

| Dimension | Assessment |
|---|---|
| Advantages | Preserves history; allows paid-only capabilities; does not claim the public core is closed. |
| Disadvantages | Requires clean API boundaries; risk that a “proprietary” module is a Derivative Work that still must comply with Apache-2.0 §4; contributor inbound terms must not place premium modules under Apache-2.0 by accident. |
| Ownership requirements | Named legal entity; clear authorship of new modules; usually a CLA or work-for-hire arrangement. **LEGAL REVIEW REQUIRED.** |
| Contributor implications | Public-core contributions stay Apache-2.0. Premium-module contributions need an explicit inbound grant to the entity. |
| Compatibility with Apache history | Compatible if new modules are not treated as a relicense of old commits. |
| Commercial implications | Supports Pro / Enterprise / add-ons. |
| Distribution implications | Commercial binaries must still honor Apache-2.0 and third-party notices for the included public Work. |
| Fork implications | Public core remains forkable. Proprietary modules are not part of that grant if they are separately authored and separately licensed. Whether a given file qualifies is **LEGAL REVIEW REQUIRED**. |
| Competitive implications | Competitors can fork the core, not automatically the premium modules. |
| Future re-licensing implications | Entity-owned new work can be dual-licensed later. |
| Technical architecture implications | Requires the boundaries in [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md). |
| Legal review requirements | Derivative-work analysis per module; dependency license compatibility; distribution notices. |

---

## Option 3 — Dual licensing

Offer the same owner-controlled code under Apache-2.0 (or another open license) and a paid proprietary license.

| Dimension | Assessment |
|---|---|
| Advantages | Familiar commercial open-source pattern; allows customers who cannot use the open terms to buy a different grant. |
| Disadvantages | Dual licensing of *the same* files requires unified copyright. This repository does not currently document unified ownership. Dual licensing does not cover llama.cpp, AndroidX, Shizuku, or model weights. |
| Ownership requirements | Single owner or CLA covering every file to be dual-licensed. |
| Contributor implications | CLA or assignment is effectively required. |
| Compatibility with Apache history | Already-granted Apache-2.0 copies remain Apache-2.0. Dual licensing applies going forward to owner-controlled work. |
| Commercial implications | Revenue from customers who want a different grant or indemnity, not from blocking Apache-2.0 use. |
| Distribution implications | Two license texts; careful product SKU wording. |
| Fork implications | The Apache-2.0 copy remains forkable. |
| Competitive implications | Does not prevent Apache-2.0 reuse. |
| Future re-licensing implications | Owner may add or withdraw the proprietary offer for *new* versions. |
| Technical architecture implications | Low if the whole owner-controlled tree is dual-licensed; higher if only some modules are. |
| Legal review requirements | Ownership chain; CLA enforceability; customer contract terms. |

---

## Option 4 — Open-core

A public Apache-2.0 core plus separately licensed premium capabilities.

| Dimension | Assessment |
|---|---|
| Advantages | Aligns with the free-foundation product principle; compatible with public GitHub development; does not rewrite history. |
| Disadvantages | Boundary disputes; temptation to cripple the free tier; contributors may refuse to work on premium modules; accidental commit of premium source into the public tree. |
| Ownership requirements | Entity ownership of premium modules; public core can remain collective Apache-2.0. |
| Contributor implications | Two inbound paths. |
| Compatibility with Apache history | High. |
| Commercial implications | Matches Free / Pro / Enterprise / add-on sketches in [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md). |
| Distribution implications | Two (or more) artifacts or an entitlement-gated single artifact that still ships Apache-2.0 notices for the core. |
| Fork implications | Core forks expected. Premium modules remain outside the public grant if legally separate. |
| Competitive implications | Differentiation moves to premium engineering, services, brand, and models—not to locking the published core. |
| Future re-licensing implications | Core stays Apache-2.0 unless a later, separately approved relicensing event occurs. |
| Technical architecture implications | Entitlement interfaces described in [`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md). No DRM in this phase. |
| Legal review requirements | Per-module derivative-work and dependency analysis. |

---

## Option 5 — Source-available licensing

Publish source under a license that restricts production use, competition, or hosted service.

| Dimension | Assessment |
|---|---|
| Advantages | Visible source with more use restrictions than Apache-2.0. |
| Disadvantages | Does not cancel existing Apache-2.0 grants; may reduce contribution and Android ecosystem compatibility; some “source-available” licenses are incompatible with Apache-2.0 combination rules. **LEGAL REVIEW REQUIRED** for any specific text. |
| Ownership requirements | Owner control of files placed under the new license. |
| Contributor implications | Apache-2.0 §5 must not silently apply to those files; inbound terms must be explicit. |
| Compatibility with Apache history | New license applies only to new versions of owner-controlled files. Historical tags remain Apache-2.0. |
| Commercial implications | Can support paid production use. Community growth may decline. |
| Distribution implications | Custom license must ship with every copy. |
| Fork implications | Fork rights depend on the chosen text; existing Apache-2.0 tags remain forkable. |
| Competitive implications | Stronger than Apache-2.0 for *new* code only. |
| Future re-licensing implications | Possible if ownership is unified. |
| Technical architecture implications | Public development workflow must not mix Apache-2.0 contributions into source-available files without a grant. |
| Legal review requirements | High. No source-available text is selected. |

---

## Option 6 — Separate commercial modules

Keep the public runtime public. Distribute premium functionality as separately built, separately licensed packages (AAR, plugin, on-device module, or side-loaded pack).

| Dimension | Assessment |
|---|---|
| Advantages | Matches the planned plugin lifecycle in [`../architecture/plugin-architecture.md`](../architecture/plugin-architecture.md); reduces accidental publication of premium source; supports a marketplace later. |
| Disadvantages | Plugin manager, signing, and isolation are currently **MISSING** (feature matrix). Implementation cost is real. A plugin that is a Derivative Work of the core still carries Apache-2.0 obligations. |
| Ownership requirements | Entity-owned plugin source and signing keys. |
| Contributor implications | Plugin contributions need inbound terms to the entity. |
| Compatibility with Apache history | High for the core. |
| Commercial implications | Supports add-on purchase and Enterprise packs. |
| Distribution implications | Separate artifacts, separate notices, signed manifests. |
| Fork implications | Core forks cannot lawfully include proprietary plugin binaries they did not receive rights to. |
| Competitive implications | Stronger isolation than a single mixed tree. |
| Future re-licensing implications | Plugins can be licensed independently. |
| Technical architecture implications | Requires plugin package format, trust roots, and entitlement IDs. Not implemented. |
| Legal review requirements | Plugin license, signature policy, and Apache-2.0 combination. |

---

## Option 7 — Commercial cloud / service layer

Keep on-device software under its current or future license. Charge for hosted inference routing, sync, catalog extras, support, or organization controls.

| Dimension | Assessment |
|---|---|
| Advantages | Service terms are contracts, not a relicense of the app; compatible with Apache-2.0 clients; privacy-first design can keep prompts local unless the user opts into cloud. |
| Disadvantages | Conflicts with current local-only invariants if cloud is implicit. [`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md) currently forbids outbound user intelligence. Cloud is **MISSING** in the feature matrix. |
| Ownership requirements | Service operator entity; customer contracts; provider API terms. |
| Contributor implications | Low for the public client. |
| Compatibility with Apache history | Full. |
| Commercial implications | Subscription and usage billing without locking the local core. |
| Distribution implications | Client remains an Apache-2.0 (or mixed) distribution; service has separate terms. |
| Fork implications | Forks can omit or replace the service. |
| Competitive implications | Differentiation is operations, models, and reliability. |
| Future re-licensing implications | Independent of the client license. |
| Technical architecture implications | See [`../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](../architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md). Cloud transmission requires an explicit permission boundary. |
| Legal review requirements | Privacy, provider terms, data-processing terms, export. |

---

## Option 8 — Hybrid licensing

Combine a public Apache-2.0 core, separately licensed premium modules or plugins, optional source-available enterprise components, and contractual cloud/support services.

| Dimension | Assessment |
|---|---|
| Advantages | Maximum commercial flexibility; does not rewrite history; supports Free / Pro / Enterprise / Cloud sketches; compatible with AI-agent public development if proprietary trees stay out of the public repository. |
| Disadvantages | Highest documentation and process burden; easy to mislabel a module; requires ownership and inbound-license hygiene. |
| Ownership requirements | Named entity plus contributor policy. |
| Contributor implications | Multiple inbound paths; agents must read license docs before adding files. |
| Compatibility with Apache history | Compatible if historical tags are left unchanged. |
| Commercial implications | Matches the intended commercial objective. |
| Distribution implications | Per-artifact license set, NOTICE, SBOM, entitlement metadata. |
| Fork implications | Core remains forkable; premium and service layers do not automatically follow. |
| Competitive implications | Control concentrates in new work, brand, services, and models. |
| Future re-licensing implications | Each layer can be decided separately later. |
| Technical architecture implications | Entitlement, module boundaries, public-development policy, and secret policy are all required as documentation before any implementation. |
| Legal review requirements | High, per layer. |

---

## Comparison summary

| Option | Relicense existing Apache-2.0 history? | Exclusive control of published core? | Supports paid tier? | Fits public GitHub + agents? | Ownership prerequisite |
|---|---|---|---|---|---|
| Continue Apache-2.0 | No | No | Yes (weak exclusive enforcement) | Yes | Low |
| Proprietary future modules | No | No (core); possible (new modules) | Yes | Yes, if premium stays out of public Git | Medium–high |
| Dual licensing | No for past copies | No for Apache copy | Yes | Medium | High |
| Open-core | No | No (core) | Yes | Yes | Medium |
| Source-available | No for past copies | Only for new owner-controlled files | Yes | Reduced | High |
| Separate commercial modules | No | No (core) | Yes | Yes | Medium |
| Cloud / service layer | No | No | Yes | Yes | Medium (contracts) |
| Hybrid | No | No (core) | Yes | Yes, with strict boundaries | High |

## What is not permitted in documentation or implementation

- Claiming existing Apache-2.0 commits are proprietary.
- Removing Apache-2.0 from historical tags as a way to revoke grants.
- Treating a directory name (`premium/`, `enterprise/`) as a license.
- Treating Git authorship, bot identities, or public visibility as copyright assignment.
- Implementing DRM, license servers, payment, or CLA enforcement in this phase.

## Decision

**DECISION REQUIRED**

**LEGAL REVIEW REQUIRED**

No license text is selected. Apache License 2.0 remains the current project license. Related placeholders: [`../decisions/ADR-0101-future-commercial-licensing-strategy.md`](../decisions/ADR-0101-future-commercial-licensing-strategy.md).
