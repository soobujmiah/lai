# ADR-0109 — AI-assisted development governance

## Status

Accepted as operating documentation for agents. Not a copyright-vesting decision.

## Date

2026-08-18

## Decision owner

Unassigned. **OWNER DECISION REQUIRED** if a DCO/CLA is applied to agent patches.

## Legal review status

**LEGAL REVIEW REQUIRED** for copyright vesting of AI-assisted works. This ADR does not decide vesting.

## Technical review status

Policy: [`../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md`](../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md).

## Context

Substantial development uses AI coding agents. Commit identities include bots. Private NpuHub comparison docs forbid source copying.

## Problem

Agents can introduce unknown-license code, invent ownership, or change licenses without approval.

## Options

No policy; documentation policy; automated license CI.

## Decision

Documentation policy is in force for agents. AI agents are not copyright owners. Unknown-license material fails closed. License file changes require owner approval. Automated enforcement is not added in this phase.

## Consequences

Agents must read the documentation index and legal docs before adding dependencies or implementing commercial features.

## Security implications

Secrets remain forbidden in prompts, logs, and commits.

## Privacy implications

Customer data must not enter the public tree.

## Licensing implications

Apache-2.0 §5 remains inbound default until a CLA/DCO is adopted.

## Migration

None.

## Alternatives rejected

Treating agent author strings as legal authors. Implementing CLA bots in this phase.
