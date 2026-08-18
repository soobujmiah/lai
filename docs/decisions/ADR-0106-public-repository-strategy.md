# ADR-0106 — Public repository strategy

## Status

Proposed as policy documentation. Visibility is unchanged.

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED** for a second private repository or GitHub protection settings.

## Legal review status

Not required to keep the repository public. Required before claiming that visibility equals ownership.

## Technical review status

Current visibility: public. Workflow depends on public GitHub and AI-agent development.

## Context

The repository is public because the development workflow depends on public GitHub access and AI-agent-based development. The public tree is not the commercial entitlement. See [`../governance/PUBLIC_DEVELOPMENT_POLICY.md`](../governance/PUBLIC_DEVELOPMENT_POLICY.md).

## Problem

Public development can leak premium source or secrets if boundaries are undefined.

## Options

Remain public; go private; public core plus private premium tree.

## Decision

**DECISION REQUIRED** for a private tree. This ADR records that visibility is not changed in this phase and that Apache-2.0 history remains public.

## Consequences

Agents continue to work on the public tree. Premium source, if created later, must not be committed here until a hosting decision exists.

## Security implications

Secret policy remains in force.

## Privacy implications

No customer data in Git.

## Licensing implications

Public copies remain Apache-2.0.

## Migration

None.

## Alternatives rejected

Changing visibility in this phase. Treating clone access as a paid SKU.
