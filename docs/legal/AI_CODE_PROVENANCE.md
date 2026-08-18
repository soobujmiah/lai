# AI code provenance

**Status:** canonical AI-assisted IP policy  
**Date:** 2026-08-18  
**Pointer from:** [`AI_ASSISTED_DEVELOPMENT_POLICY.md`](AI_ASSISTED_DEVELOPMENT_POLICY.md)  
**Related ADR:** [`../decisions/ADR-0109-ai-assisted-development-governance.md`](../decisions/ADR-0109-ai-assisted-development-governance.md)

LAI is developed with substantial use of AI coding agents. This file is the canonical provenance and IP rule for that workflow.

## Non-ownership

An AI agent is never the copyright owner. An agent identity (`LAI Agent`, `LAI Architecture Bot`, Arena.ai agents, Dependabot, other bots) is a tooling / commit string. It is not a human contributor and not a legal rights holder. See [`OWNERSHIP_MODEL.md`](OWNERSHIP_MODEL.md).

Generated text becomes project work only after project review (human or recorded project review process). Review does not by itself create a company owner.

## Rules

1. AI-generated code is reviewed before acceptance.
2. The project must not knowingly import copyrighted source from incompatible projects.
3. Agents must not copy code from private or proprietary repositories, including private `NpuHub`, unless a recorded authorization and license review exist. Comparison documents already state that NpuHub was reviewed without copying source.
4. Agents must not reproduce large third-party source files into this tree.
5. Third-party code is identified and licensed before incorporation ([`THIRD_PARTY_INTAKE.md`](THIRD_PARTY_INTAKE.md)).
6. Agents must not change [`../../LICENSE`](../../LICENSE), add a second project license, or invent a copyright holder.
7. Agents must not add dependencies, models, datasets, SDKs, or fonts without a license review in the same change.
8. Agents must not commit secrets ([`../security/COMMERCIAL_SECRET_POLICY.md`](../security/COMMERCIAL_SECRET_POLICY.md)).
9. Agents must not implement entitlement, payment, CLA, or DRM unless that implementation phase is explicitly approved.
10. Planned behavior must not be documented as implemented.

Unknown license fails closed.

## Practical provenance process

For a change that adds or substantially rewrites a component:

| Step | Record |
|---|---|
| 1. Intent | Purpose, module, free/premium/third-party class ([`COMMERCIAL_IP_POLICY.md`](COMMERCIAL_IP_POLICY.md)) |
| 2. Sources consulted | Public docs, this tree, named upstream URLs. No private trees unless authorized |
| 3. Third-party material | Coordinate/commit, license text location, NOTICE needs |
| 4. Review | Tests run, docs updated, architecture boundaries still valid |
| 5. Retention when practical | PR description, commit message, and — for important components — a short provenance note in the module doc or ADR. Agent logs and prompts are retained when the environment allows; they are not committed if they contain secrets |
| 6. Author string | May remain a bot identity; that does not make the bot a rights holder |

A routine typo fix does not need a provenance note. A new runtime, plugin host, vendor adapter, or copied algorithm does.

## Classification

| Class | Handling |
|---|---|
| AI-generated / AI-assisted | Review + same license gates as any patch |
| Human-reviewed AI code | Eligible to merge if gates pass |
| Third-party / copied | Intake required; unknown blocks |
| Generated docs / tests / config | Must not invent status, licenses, or passing tests |
| Generated assets | Original or licensed; no copied proprietary UI chrome |

## Decision

Policy is in force as documentation. Copyright vesting of AI-assisted works under applicable law is **LEGAL REVIEW REQUIRED**. DCO/CLA application to agent patches is **DECISION REQUIRED** (IP-004).
