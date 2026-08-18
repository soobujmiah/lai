# ADR-0101 — Future commercial licensing strategy

## Status

Proposed. **Not final.**

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED**

## Legal review status

**LEGAL REVIEW REQUIRED**

## Technical review status

Engineering recommendation recorded; not accepted.

## Context

LAI is intended to support commercial distribution and a meaningful free tier while remaining a public Apache-2.0 development repository. See [`../legal/LICENSING_STRATEGY.md`](../legal/LICENSING_STRATEGY.md) and [`../product/COMMERCIAL_PRODUCT_STRUCTURE.md`](../product/COMMERCIAL_PRODUCT_STRUCTURE.md).

## Problem

A single future license must not be selected in documentation without owner approval, and existing Apache-2.0 history cannot be declared proprietary.

## Options

Documented in [`../legal/LICENSING_STRATEGY.md`](../legal/LICENSING_STRATEGY.md): continue Apache-2.0; proprietary future modules; dual licensing; open-core; source-available; separate commercial modules; commercial cloud/service layer; hybrid.

## Decision

**DECISION REQUIRED.** No final legal strategy is selected.

Recommended technical direction only: hybrid open-core with separately authored commercial modules and optional commercial services, without relicensing historical commits.

## Consequences

Until accepted, implementation of license changes, DRM, or CLA is forbidden. Agents treat Apache-2.0 as the current project license.

## Security implications

None until a strategy is accepted.

## Privacy implications

A cloud-service strategy would require an explicit consent path.

## Licensing implications

No change to [`../../LICENSE`](../../LICENSE).

## Migration

None.

## Alternatives rejected

Claiming that existing Apache-2.0 code can later be retroactively locked down.
