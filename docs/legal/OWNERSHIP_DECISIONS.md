# Ownership and IP decision register

**Status:** canonical register  
**Date:** 2026-08-18  
**Policy:** [`COMMERCIAL_IP_POLICY.md`](COMMERCIAL_IP_POLICY.md)

Each row is a recorded decision or an explicit non-decision. Unmade choices are **DECISION REQUIRED**. This register does not create a legal entity or assign copyright.

## Record format

| Field | Meaning |
|---|---|
| Decision ID | `IP-NNN` |
| Date | ISO date |
| Subject | Short title |
| Decision | What was decided, or that a decision is required |
| Rationale | Why |
| Affected modules | Scope |
| License | Applicable license or “n/a” |
| Ownership status | Established / placeholder / third-party / unknown |
| Follow-up | Required next action |
| Unresolved | Legal questions still open |

## Register

### IP-001 — Existing Apache-2.0 history

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | Existing Apache-2.0 history |
| Decision | Preserve existing obligations. Do not relicense, hide, or revoke published Apache-2.0 copies. |
| Rationale | Those copies already carry a perpetual, non-exclusive grant. See [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md) and ADR-0100. |
| Affected modules | Entire published tree and historical tags/releases |
| License | Apache-2.0 |
| Ownership status | Collective notice `Copyright 2026 LAI contributors`; exclusive owner not established |
| Follow-up | None for preservation. Entity/assignment is a separate decision. |
| Unresolved | None for the preservation rule. Identity of contributors remains open (IP-003). |
| Status | **ACCEPTED** |

### IP-002 — Future proprietary modules

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | Future proprietary modules |
| Decision | Require explicit ownership and licensing review before implementation of any module intended to be proprietary, premium, or separately licensed. |
| Rationale | Directory separation is not a license. Derivative-work status is fact-specific. **LEGAL REVIEW REQUIRED** per module. |
| Affected modules | All future premium/enterprise/cloud/plugin packs |
| License | Not selected. Must be recorded before implementation. |
| Ownership status | No proprietary module exists |
| Follow-up | Complete [`PROPRIETARY_BOUNDARIES.md`](PROPRIETARY_BOUNDARIES.md) review for each candidate before code. |
| Unresolved | Which candidates leave the public tree (**DECISION REQUIRED**). |
| Status | **ACCEPTED** (process) |

### IP-003 — Identity of the LAI project owner / rights holder

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | Named rights holder |
| Decision | **DECISION REQUIRED.** Use the placeholder “LAI project owner / rights holder” until an instrument names a person or entity. |
| Rationale | No company, assignment, or individual copyright line exists beyond `LAI contributors`. Inventing an owner would be false. |
| Affected modules | All future inbound and outbound licensing |
| License | n/a |
| Ownership status | Placeholder |
| Follow-up | Legal formation or recorded individual ownership, then update this row. |
| Unresolved | Natural-person identity behind hosting account; relationship among Git identities. **LEGAL REVIEW REQUIRED.** |
| Status | **DECISION REQUIRED** |

### IP-004 — Inbound contributor agreement

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | CLA / DCO / assignment |
| Decision | **DECISION REQUIRED.** None implemented. Comparison in [`CONTRIBUTOR_RIGHTS.md`](CONTRIBUTOR_RIGHTS.md). |
| Rationale | For commercially controlled *new* work that accepts outside patches, a CLA or assignment is a candidate. A DCO alone does not grant relicensing rights. |
| Affected modules | Future public-core and premium-module contributions |
| License | Apache-2.0 §5 inbound default today |
| Ownership status | Uncontrolled inbound |
| Follow-up | Owner decision; legal text; then optional automation. Do not activate in this phase. |
| Unresolved | Which instrument, if any. **LEGAL REVIEW REQUIRED.** |
| Status | **DECISION REQUIRED** |

### IP-005 — License of new LAI-authored work

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | License for work created after IP-003 is resolved |
| Decision | **DECISION REQUIRED.** Strategy options remain in [`LICENSING_STRATEGY.md`](LICENSING_STRATEGY.md). Recommended technical direction only: public Apache-2.0 core plus separately reviewed premium modules/services. |
| Rationale | Selecting a license without a rights holder and without legal review would over-claim. |
| Affected modules | Future original work |
| License | Unselected for new unpublished work; published tree remains Apache-2.0 |
| Ownership status | Depends on IP-003 |
| Follow-up | Decide after IP-003. |
| Unresolved | Open-core vs dual license vs service-only. |
| Status | **DECISION REQUIRED** |

### IP-006 — Exact free versus paid set

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | FREE / PRO / ENTERPRISE / CLOUD mapping |
| Decision | **DECISION REQUIRED.** Architectural sketch only: [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md). |
| Rationale | Pricing and SKU names are not finalized. Implementation of locks is forbidden until this decision and an entitlement ADR are accepted. |
| Affected modules | Product surface |
| License | n/a |
| Ownership status | n/a |
| Follow-up | Approve capability IDs, then optionally implement `EntitlementPort`. |
| Unresolved | Exact FREE set; subscription vs one-time; whether official binaries gate public-source capabilities. |
| Status | **DECISION REQUIRED** |

### IP-007 — Trademark registration

| Field | Content |
|---|---|
| Date | 2026-08-18 |
| Subject | LAI name and logo |
| Decision | **DECISION REQUIRED.** No registration is verified. Optional future legal action. |
| Rationale | Copyright license ≠ trademark. Claiming an unregistered mark would be false. |
| Affected modules | Store listings, domains, UI name |
| License | n/a |
| Ownership status | Unverified |
| Follow-up | Clearance search if commercialization proceeds. |
| Unresolved | Registrability of “LAI”; collisions. **LEGAL REVIEW REQUIRED.** |
| Status | **DECISION REQUIRED** |

## How to add a row

A new `IP-NNN` is added in the same change that records an owner-approved decision. Agents must not mark a row **ACCEPTED** unless the decision is already documented as fact (as IP-001) or as an adopted process (as IP-002).
