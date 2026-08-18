# Third-party intake

**Status:** canonical intake policy  
**Date:** 2026-08-18  
**Inventory:** [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md)  
**Root registers:** [`../../THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md), [`../../THIRD_PARTY_LICENSES.md`](../../THIRD_PARTY_LICENSES.md)  
**Related ADR:** [`../decisions/ADR-0108-third-party-license-governance.md`](../decisions/ADR-0108-third-party-license-governance.md)

No dependency, SDK, model, dataset, native library, font, icon, or copied source is added until this intake is completed in the same change. A component is not licensed merely because a similar version is commonly published under a known SPDX id.

Unknown licenses **block** integration.

## Required record

| Field | Purpose |
|---|---|
| Name | Component identity |
| Version | Exact Maven version, git commit, or package version |
| Exact source | URL |
| Commit / version pin | Immutable when possible |
| License | SPDX plus pointer to the text for *this* version |
| Copyright holder | As stated by upstream |
| Redistribution rights | Whether LAI may ship it |
| Modification rights | Whether LAI may modify |
| Attribution requirements | Names that must appear |
| NOTICE requirements | NOTICE file or equivalent |
| Patent terms | If stated |
| Trademark restrictions | If stated (example: Shizuku name/icons) |
| Commercial-use restrictions | If any |
| Shippable in a commercial APK | yes / no / unknown |
| Linkable into proprietary components | yes / no / unknown — **LEGAL REVIEW REQUIRED** when unknown |
| Source-disclosure required | yes / no / unknown (copyleft) |
| Compatible with intended commercial model | yes / no / unknown |
| Runtime / build / test / model / dataset / SDK / service | Role |
| Bundled / fetched / dynamically linked | How it enters a build |
| Verification date and source | Where the text was read |
| Verification status | VERIFIED / DECLARED / REGISTERED / PLANNED / UNKNOWN |

## Fail-closed rules

- Status UNKNOWN on a component that would ship → block production release.
- Copyleft that would require disclosing LAI proprietary modules → block those modules from linking it until **LEGAL REVIEW REQUIRED** completes.
- Vendor SDK without a redistribution clause review → block ([`VENDOR_LICENSE_POLICY.md`](VENDOR_LICENSE_POLICY.md)).
- Model without [`MODEL_IP_POLICY.md`](MODEL_IP_POLICY.md) fields → block catalog inclusion and bundling.

## Process

```text
Propose coordinate/pin
→ Read the license text for that exact version
→ Fill the inventory row
→ Update root registers if the family is new or the version materially changed
→ Architecture-boundary check
→ Only then add the Gradle/CI/source reference
```

Do not download large SDKs or models into the constrained workspace solely to inspect a license when the upstream LICENSE URL can be read remotely.

## Commercial APK questions

Shipping an APK is Object-form distribution of LAI’s Apache-2.0 Work **and** of linked third-party Object form. Intake must answer whether notices can be satisfied by the release NOTICE strategy in [`RELEASE_COMPLIANCE.md`](RELEASE_COMPLIANCE.md).

Linking a library into a *future* proprietary module is a separate question from linking it into the public Apache-2.0 core. Apache-2.0 and MIT dependencies are commonly usable in both; copyleft and vendor SDKs may not be. **LEGAL REVIEW REQUIRED** per case. No general legal conclusion is stated here.

## Current inventory location

Filled rows live in [`THIRD_PARTY_COMPLIANCE.md`](THIRD_PARTY_COMPLIANCE.md). That file is the registry, not a second policy.
