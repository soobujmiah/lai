# Public development policy

**Status:** operating policy (documentation)  
**Date:** 2026-08-18  
**Related ADR:** [`../decisions/ADR-0106-public-repository-strategy.md`](../decisions/ADR-0106-public-repository-strategy.md)  
**Contribution engineering rules:** [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md)  
**Development policy:** [`../development/development-policy.md`](../development/development-policy.md)

LAI can remain a public development repository while a commercial product evolves. This file states how. GitHub settings are **not** changed in this phase. Canonical IP policy: [`../legal/COMMERCIAL_IP_POLICY.md`](../legal/COMMERCIAL_IP_POLICY.md).

Public visibility does not mean all future functionality must be open source, that future proprietary modules must be published, that trademarks are freely available, that secrets may be committed, that third-party code may be copied freely, or that commercial rights are automatically granted to unpublished work. Anything actually published under Apache-2.0 receives the rights granted by that license. This is not an “anti-theft” promise for already published copies.

## Rules

1. The public repository is the development and reference repository.
2. Existing Apache-licensed code remains Apache-licensed. Historical tags are not rewritten to revoke grants.
3. Proprietary assets must not be committed. See [`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md).
4. Secrets are forbidden.
5. Commercial-only code must have a documented boundary before it exists ([`../architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](../architecture/COMMERCIAL_MODULE_BOUNDARIES.md)).
6. AI agents inspect licensing documentation before adding dependencies ([`../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md`](../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md)).
7. Pull requests must pass license review when they add a dependency, model, dataset, SDK, or asset. Automation for that review is not yet implemented.
8. CODEOWNERS should eventually be considered. **Not configured.**
9. Protected branches should eventually be considered. **Not configured in this change.**
10. Security scanning should be enabled. GitHub Security Advisories are already the documented reporting path ([`../../SECURITY.md`](../../SECURITY.md)).
11. Dependency scanning should be enabled. Dependabot already watches Gradle and GitHub Actions ([`.github/dependabot.yml`](../../.github/dependabot.yml)).
12. Release artifacts require the compliance gates in [`../release/COMMERCIAL_RELEASE_COMPLIANCE.md`](../release/COMMERCIAL_RELEASE_COMPLIANCE.md).

## Compatibility with AI-agent workflows

Public visibility is an acknowledged development constraint. Agents operate on the public tree. Therefore:

- premium source, if any, lives outside this repository until a different hosting decision is accepted;
- designs for premium modules may be public without including their source;
- agents must not treat a planned premium module as present in source.

## What public development does not imply

- Every future capability must be released under Apache-2.0.
- Store listing, trademarks, or support are included with a clone.
- Bot commit identities own copyright.

## Decision

**DECISION REQUIRED** for CODEOWNERS, branch protection, and whether a second private repository will exist.

No GitHub setting is changed in this phase.
