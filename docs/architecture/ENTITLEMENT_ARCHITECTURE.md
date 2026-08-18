# Entitlement architecture

**Status:** conceptual design  
**Date:** 2026-08-18  
**Implementation:** forbidden in this phase. No DRM, license server, payment, or feature lock is added  
**Related ADR:** [`../decisions/ADR-0105-entitlement-architecture.md`](../decisions/ADR-0105-entitlement-architecture.md)  
**Product overlay:** [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md)

## Purpose

Describe a vendor-neutral entitlement seam that can be implemented later without restructuring the 16-module graph. The seam is documentation only.

## Non-goals for this phase

- License servers
- DRM / obfuscation
- Payment SDKs
- Account systems
- Feature flags that disable current FREE-foundation behavior
- Always-on internet checks

## Placement in the existing graph

Entitlement, if added later, is a policy input—not an authority implementation.

```text
UI / Settings
    → EntitlementPort   (pure query: is capability allowed?)
        → core:policy   (existing consent / local-first / shell policy)
            → runtime/orchestrator and authorities
```

`core:policy` already owns consent, local-first egress denial, and shell compilation. An entitlement check would be an additional typed decision beside those policies. It must not live in `platform:download`, Accessibility, Shizuku, or JNI.

`core` remains Android-free. Entitlement documents and signatures are opaque bytes to core; parsing of signatures happens in a future dedicated adapter.

## Capability model

| Concept | Meaning |
|---|---|
| Capability ID | Stable string (`cap.*`) |
| Grant | A signed or locally cached statement that a set of capability IDs is allowed on a device or account |
| SKU | Marketing bundle that maps to capability IDs |
| Source | `built_in_free`, `local_signed`, `account_online`, `trial`, `addon` |
| Constraint | Expiry, device binding, grace, usage quota (cloud only) |

Official binaries may consult `EntitlementPort`. Recipients who build the public Apache-2.0 tree may replace the adapter. That is a consequence of the current license, not a defect to paper over.

## Required modes

### Online

Used only for:

- subscription status refresh,
- cloud service entitlement,
- optional account linking.

Must be explicit, HTTPS, and isolated to a future entitlement/network adapter. It must not reuse model-download as a hidden phone-home. It must not send prompts, screens, or documents.

### Offline

LAI is privacy-first and offline-first ([`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md), ADR 0004). Paid *on-device* functionality must not require constant internet.

Candidate offline mechanisms (not implemented):

| Mechanism | Role | Security note |
|---|---|---|
| Built-in free set | Compiled default capability list | Public, inspectable |
| Signed local entitlement | File or app-private blob verifying SKU, device, expiry | Key material never in public Git |
| Cached last-known grant | Copy of last successful online check | Stale-grant and clock-skew abuse |
| Grace period | Time-boxed continuation after cache expiry | Documented length; fail closed after grace for *paid* caps only |
| Local-only capabilities | Remain available without any grant beyond the free set | Must include the meaningful free foundation |

Free local inference, one-shot confirmed tools, and basic workspace must remain usable offline without an account.

## Lifecycle operations (future)

| Operation | Requirement |
|---|---|
| Activate | User-visible; no silent grant |
| Refresh | Bounded; offline cache remains valid during refresh failure |
| Trial | Time-boxed capability set; honest UI |
| Lifetime license | Signed grant without subscription refresh, plus optional revocation list fetched only on explicit update |
| Add-on module | Separate artifact + capability IDs |
| Enterprise license | Organization grant; still no content upload |
| Revocation | Only where legally and technically appropriate; must not brick free local capabilities |
| Device transfer | Re-bind with user action; old device loses paid grant after grace |
| Account transfer | Contractual; not designed here |
| Cloud-backed functionality | Separate from on-device grant; provider terms apply |

## Security considerations (documentation only)

- Entitlement keys are production secrets ([`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md)).
- Client-side checks are deterrents, not proof against a rebuilt APK.
- Clock rollback, backup restore, and sideload of a forged grant are expected attack classes.
- Fail closed for *paid* capabilities when a grant is missing or invalid.
- Fail open for *documented free* capabilities when entitlement infrastructure is absent—this is the current source behavior.
- Do not hide entitlement failures inside inference errors.

## Interface sketch (not source)

```text
enum EntitlementState { ALLOWED, DENIED, TRIAL, GRACE, UNKNOWN }

data class EntitlementQuery(val capabilityId: String, val nowEpochMs: Long)

data class EntitlementDecision(
    val state: EntitlementState,
    val source: String,
    val expiresAtEpochMs: Long?,
    val reasonCode: String,
)
```

`UNKNOWN` is for adapter absence. UI must not invent “licensed” from `UNKNOWN`.

## Interaction with privacy

Entitlement traffic, if any, is metadata about SKUs and device/account identifiers—not user intelligence. Diagnostics remain content-free. Cloud inference is a different path and requires the permission boundary in [`HYBRID_AND_PROVIDER_ARCHITECTURE.md`](HYBRID_AND_PROVIDER_ARCHITECTURE.md).

## Decision

**DECISION REQUIRED** for SKU ↔ capability mapping and whether official binaries will gate capabilities that remain in public source.

No implementation in this phase.
