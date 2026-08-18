# ADR-0107 — Commercial module boundaries

## Status

Proposed. **Not final.**

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED**

## Legal review status

**LEGAL REVIEW REQUIRED** per module before a proprietary label.

## Technical review status

Candidate table in [`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md). No module graph change.

## Context

Existing isolation (core / platform / runtime / plugins) is a technical seam, not a license boundary.

## Problem

Assuming every future directory can be proprietary would misstate Apache-2.0.

## Options

Keep all future work public Apache-2.0; split new modules; use plugins; use services.

## Decision

**DECISION REQUIRED.** No module is declared proprietary. Directory separation is not a license.

## Consequences

Architecture remains as in ADR 0002 / ADR 0004 / ADR 0005.

## Security implications

Authority modules stay isolated regardless of SKU.

## Privacy implications

Cloud connectors remain forbidden until a consent path exists.

## Licensing implications

None until a split is accepted.

## Migration

None.

## Alternatives rejected

Implementing a `premium/` tree in this phase. Copying private NpuHub source.
