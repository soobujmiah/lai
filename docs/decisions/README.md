# Architectural decision records

## Purpose

ADRs record significant, durable choices before implementation. New architectural changes use:

`docs/decisions/ADR-XXXX-short-title.md`

Do not create an ADR for routine implementation that follows an accepted design. Create one for new trust boundaries, authority, provider/runtime architecture, storage formats with migration cost, network exposure, plugin execution, project trust, or a deliberate change to phase order.

## Required format

```markdown
# ADR-XXXX — Decision title
## Status
Proposed / Accepted / Superseded / Rejected
## Context
## Problem
## Options
### Option A
### Option B
## Decision
## Consequences
## Security implications
## Privacy implications
## Licensing implications
## Migration
## Alternatives rejected
```

## Existing accepted decision history

The repository already contains useful ADRs under the legacy `docs/adr/` path. They remain authoritative and are not duplicated or moved merely for cosmetics.

| ADR | Decision | Status/source |
|---|---|---|
| 0002 | Modular local-first backbone | [`../adr/0002-modular-local-first-backbone.md`](../adr/0002-modular-local-first-backbone.md) |
| 0003 | Dual connectivity editions | [`../adr/0003-dual-connectivity-editions.md`](../adr/0003-dual-connectivity-editions.md); historical context, later superseded in product direction |
| 0004 | Single local-first application | [`../adr/0004-single-local-first-app.md`](../adr/0004-single-local-first-app.md) |
| 0005 | Snapdragon-first, vendor-neutral backends | [`../adr/0005-snapdragon-first-vendor-neutral-backends.md`](../adr/0005-snapdragon-first-vendor-neutral-backends.md) |
| 0006 | One-shot model tool proposals | [`../adr/0006-one-shot-model-tool-proposals.md`](../adr/0006-one-shot-model-tool-proposals.md) |
| 0007 | Persistent audit and replay guard | [`../adr/0007-persistent-tool-audit-and-replay-guard.md`](../adr/0007-persistent-tool-audit-and-replay-guard.md) |

Before relying on ADR 0003, read ADR 0004 and current source; the single local-first application is the active direction.

## Next decisions required before implementation

These are ADR candidates, not accepted designs:

1. AI Gateway provider contract and migration from direct app-to-`InferenceEngine` composition.
2. Managed localhost server process/lifecycle, loopback authentication, API compatibility, and LAN exposure policy.
3. Multi-step agent/task/checkpoint model and durable recovery semantics.
4. Diff/rollback storage and project trust model.
5. Project domain, SAF/private/Linux filesystem boundaries, and secret references.
6. Plugin package, signing trust roots, permissions, isolation, and lifecycle.
7. Remote provider data policy and explicit cloud/LAN consent.

ADR numbers must be allocated by inspecting both `docs/adr/` and `docs/decisions/`; do not reuse an existing number.

## Commercial, licensing, and ownership series (0100)

These records live in `docs/decisions/` so they do not collide with accepted engineering ADRs 0002–0007. None of them changes [`../../LICENSE`](../../LICENSE). Proposed items are not implementation authorization.

| ADR | Title | Status |
|---|---|---|
| 0100 | Current Apache-2.0 state | Accepted as fact record — [`ADR-0100-current-apache-2.0-state.md`](ADR-0100-current-apache-2.0-state.md) |
| 0101 | Future commercial licensing strategy | Proposed — [`ADR-0101-future-commercial-licensing-strategy.md`](ADR-0101-future-commercial-licensing-strategy.md) |
| 0102 | Copyright ownership model | Proposed — [`ADR-0102-copyright-ownership-model.md`](ADR-0102-copyright-ownership-model.md) |
| 0103 | Contributor agreement strategy | Proposed — [`ADR-0103-contributor-agreement-strategy.md`](ADR-0103-contributor-agreement-strategy.md) |
| 0104 | Free vs paid capability model | Proposed — [`ADR-0104-free-vs-paid-capability-model.md`](ADR-0104-free-vs-paid-capability-model.md) |
| 0105 | Entitlement architecture | Proposed — [`ADR-0105-entitlement-architecture.md`](ADR-0105-entitlement-architecture.md) |
| 0106 | Public repository strategy | Proposed — [`ADR-0106-public-repository-strategy.md`](ADR-0106-public-repository-strategy.md) |
| 0107 | Commercial module boundaries | Proposed — [`ADR-0107-commercial-module-boundaries.md`](ADR-0107-commercial-module-boundaries.md) |
| 0108 | Third-party license governance | Proposed process — [`ADR-0108-third-party-license-governance.md`](ADR-0108-third-party-license-governance.md) |
| 0109 | AI-assisted development governance | Accepted as agent operating policy — [`ADR-0109-ai-assisted-development-governance.md`](ADR-0109-ai-assisted-development-governance.md) |

Map index: [`../legal/README.md`](../legal/README.md).

## Review rule

An ADR is not accepted merely because a file exists. Acceptance requires review of source compatibility, security, privacy, licensing, migration, testability, and rollback. Proposed ADRs cannot be used to describe unimplemented functionality as current capability.
