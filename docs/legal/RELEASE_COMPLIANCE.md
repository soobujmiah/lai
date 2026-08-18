# Release compliance

**Status:** canonical commercial release gate  
**Date:** 2026-08-18  
**Pointer from:** [`../release/COMMERCIAL_RELEASE_COMPLIANCE.md`](../release/COMMERCIAL_RELEASE_COMPLIANCE.md)  
**Build mechanics:** [`../BUILD_AND_RELEASE.md`](../BUILD_AND_RELEASE.md)  
**SBOM design:** [`SBOM_AND_PROVENANCE.md`](SBOM_AND_PROVENANCE.md)

A release is not commercially ready until the gates below pass. Unknown required licenses fail closed. This phase does not add CI jobs and does not publish anything.

## Gates

| # | Check | Current | Fail closed |
|---|---|---|---|
| 1 | License compliance | Apache-2.0 project license present | Missing or unexplained second project license |
| 2 | Third-party notices | Registers PARTIAL; APK META-INF excludes | Required notice absent from the shipped package |
| 3 | Model licenses | Qwen referenced, not bundled | Unexpected weights; catalog row incomplete |
| 4 | Dependency licenses | Catalog vs registers stale | UNKNOWN SPDX on a shipped component |
| 5 | SBOM | Text `gradle dependencies` dump only | No SBOM on a production-labeled build |
| 6 | Source provenance | Git SHA / tag | Unsigned mystery artifact |
| 7 | Copyright notices | Collective line only | Contradictory invented owner |
| 8 | NOTICE requirements | No root NOTICE | Unmet Apache-2.0 §4 / upstream NOTICE |
| 9 | Trademark review | Unverified | Infringing branding |
| 10 | Secret scan | `validate_repo.sh` token patterns | Key, keystore, customer data |
| 11 | Proprietary-module boundary | No proprietary module exists | Premium source accidentally in the public tag |
| 12 | Release artifact contents | CI `find` for apk/so | Unexpected `.so`, model, key, permission |
| 13 | Vendor SDK terms | No QNN | Unreviewed vendor binary |
| 14 | Generated / native binary notices | llama.cpp MIT required when linked | MIT notice missing from a llama-linked APK |

## APK META-INF

[`../../app/build.gradle.kts`](../../app/build.gradle.kts) excludes `META-INF/AL2.0`, `META-INF/LGPL2.1`, `META-INF/LICENSE.md`, and `META-INF/NOTICE.md`. Those excludes are **not changed** in this phase. They exist as a duplicate-file merge workaround. They can drop notices that must be replaced by `assets/licenses/`, an in-app licenses screen, and/or a Release `NOTICE.txt`. **DECISION REQUIRED** for the replacement channel. The `LGPL2.1` filename does not make LAI source LGPL.

## Secret scan

Public Git history is permanent exposure. Production secrets never enter the tree. See [`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md). A leaked secret is rotated; history rewrite is not a compliance strategy for already-cloned copies.

## Decision

Notice packaging and SBOM automation remain MISSING/PARTIAL. No release is performed by this documentation phase.
