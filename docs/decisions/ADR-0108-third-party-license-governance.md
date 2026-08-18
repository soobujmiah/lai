# ADR-0108 — Third-party license governance

## Status

Proposed process. Root registers already exist and remain canonical for short tables.

## Date

2026-08-18

## Decision owner

Unassigned.

## Legal review status

**LEGAL REVIEW REQUIRED** for Shizuku extra terms, QNN, GPL-family tools, and any UNKNOWN component before ship.

## Technical review status

Fielded registry created: [`../legal/THIRD_PARTY_COMPLIANCE.md`](../legal/THIRD_PARTY_COMPLIANCE.md). SBOM still MISSING/PARTIAL.

## Context

Feature matrix already marks legal tracking PARTIAL. Version drift exists between registers and `gradle/libs.versions.toml`.

## Problem

A component must not be marked licensed merely because a similar version is commonly known to use that license.

## Options

Continue short registers only; add a fielded registry; fully automate SBOM now.

## Decision

Add a fielded registry as documentation. Do not implement SBOM automation in this phase. Unknown licenses fail closed for production.

## Consequences

New dependencies require a registry row in the same change. Existing stale versions are documented, not silently “fixed” in Gradle.

## Security implications

Supply-chain review remains a release gate.

## Privacy implications

Analytics SDKs remain forbidden by architecture checks.

## Licensing implications

No change to LAI’s Apache-2.0 grant.

## Migration

Future SBOM generators should consume the same fields.

## Alternatives rejected

Claiming current registers are complete. Changing packaging excludes in this phase.
