# ADR-0104 — Free versus paid capability model

## Status

Proposed. **Not final.**

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED**

## Legal review status

Not required for the product principle; required before gating a capability that third-party licenses restrict.

## Technical review status

Overlay drafted. Not implemented.

## Context

The commercial objective is a meaningful free foundation plus paid professional capabilities. Exact pricing and the free set are not finalized. See [`../product/COMMERCIAL_FEATURE_MATRIX.md`](../product/COMMERCIAL_FEATURE_MATRIX.md).

## Problem

Hard-coding a tier list into the application would freeze an unapproved business decision and could violate the “no artificial frustration” principle.

## Options

Document a flexible capability-ID model; or implement feature locks now.

## Decision

Document only. Capability IDs may be designed; tiers are mappings that can change. Implementation of locks is forbidden in this phase.

## Consequences

Current binaries remain ungated. Public source that implements a capability remains Apache-2.0.

## Security implications

None.

## Privacy implications

Paid cloud capabilities, if any, require consent.

## Licensing implications

Entitlement does not revoke Apache-2.0.

## Migration

None.

## Alternatives rejected

Everything-locked-behind-payment. Implementing DRM in this phase.
