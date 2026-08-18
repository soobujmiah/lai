# ADR-0105 — Entitlement architecture

## Status

Proposed. **Not final.**

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED**

## Legal review status

**LEGAL REVIEW REQUIRED** before device binding, revocation, or account systems.

## Technical review status

Conceptual seam documented in [`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md). Not implemented.

## Context

Offline-first and privacy-first constraints forbid always-on license servers for local capabilities.

## Problem

A future paid tier needs a seam that can be added without restructuring authorities or sending user intelligence off-device.

## Options

No entitlement; online-only checks; offline signed grants plus optional online refresh; full DRM.

## Decision

**DECISION REQUIRED** for SKU mapping. Recommended technical direction: `EntitlementPort` beside `core:policy`, fail open for documented free capabilities, fail closed for paid capabilities, offline cache and grace, no DRM in this phase.

## Consequences

No current user-visible change.

## Security implications

Client-side checks are not proof against rebuilds of public source.

## Privacy implications

Entitlement traffic must not include prompts or documents.

## Licensing implications

Official binaries may gate features; public source remains Apache-2.0.

## Migration

None.

## Alternatives rejected

Always-on internet requirement for local chat. Implementation in this phase.
