# Ownership model

**Status:** planning document  
**Date:** 2026-08-18  
**Copyright notice in force:** `Copyright 2026 LAI contributors` in [`../../LICENSE`](../../LICENSE)  
**Ownership decision:** **OWNER DECISION REQUIRED**  
**Legal interpretation:** **LEGAL REVIEW REQUIRED**

This document records observable repository facts and candidate future ownership structures. It does not assign copyright, create a company, or name a legal owner. Until a formal instrument exists, documentation uses the placeholder **LAI project owner / rights holder**. That phrase is not a legal entity. Canonical policy: [`COMMERCIAL_IP_POLICY.md`](COMMERCIAL_IP_POLICY.md). Register: [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md).

## Non-claims

The following statements are intentionally **not** made:

- Git commit authorship is copyright ownership.
- Public GitHub visibility transfers copyright.
- AI agents or bots own copyright.
- A package name (`dev.lai.runtime`) is a legal entity.
- A future company automatically owns existing commits.
- This document itself transfers rights.

## Current copyright notice

The only project copyright line in the tree is:

```text
Copyright 2026 LAI contributors
```

It appears solely in the application notice of [`../../LICENSE`](../../LICENSE). No per-file copyright headers exist. No SPDX identity exists on source files. No `NOTICE` file exists.

“LAI contributors” is a collective phrase. It is not, by itself, evidence of a corporation, partnership, or assignment.

## Repository and maintainer identity

Observable GitHub / Git facts at audit time:

| Fact | Value |
|---|---|
| Host | `https://github.com/soobujmiah/lai` |
| Visibility | Public |
| Fork flag | False |
| Created | 2026-08-16 |
| Default branch | `main` |
| GitHub license field | Apache-2.0 |
| Application id | `dev.lai.runtime` |

The GitHub user `soobujmiah` is the hosting account. Hosting an account is not documented here as copyright assignment. **LEGAL REVIEW REQUIRED** to determine the legal person behind that account and any employment or contractor relationship.

## Commit identities

`git log` author strings on `main` (104 commits at audit `94d88aa`):

| Commits | Author string |
|---:|---|
| 39 | `soobujmiah <soobujmiah@users.noreply.github.com>` |
| 39 | `LAI Architecture Bot <lai-architecture@users.noreply.github.com>` |
| 12 | `LAI Agent <agent@arena.ai>` |
| 7 | `LAI Engineering <dev@lai.runtime>` |
| 5 | `dependabot[bot]` |
| 1 | `miahsobuj <miahsobuj@github.com>` |
| 1 | `Sobuj <56004845+soobujmiah@users.noreply.github.com>` |

These strings are commit metadata.

Human-looking identities (`soobujmiah`, `Sobuj`, `miahsobuj`) may or may not be the same natural person. That question is **UNKNOWN** from repository files.

## AI-agent and bot identities

The following are treated as **development tooling / commit identities**, not copyright owners:

- LAI Architecture Bot
- LAI Agent
- AI Arena / Arena.ai agent commit identities
- dependabot[bot]
- other automated tooling

Copyright, if any, in works produced with those tools vests according to applicable law and any contract with the human who directed the work. This documentation does not determine that vesting. **LEGAL REVIEW REQUIRED.**

AI agents must not be documented, listed, or registered as copyright owners.

## Corporate / entity ownership

No company name, registered office, or assignment agreement is present in the repository.

A future legal entity is a candidate owner of:

- new original work created for that entity,
- trademarks and store listings,
- signing keys and catalog private keys (as trade secrets / key material),
- customer contracts and service terms.

Transfer of *existing* copyright into that entity requires an instrument that this repository does not contain. **OWNER DECISION REQUIRED.** **LEGAL REVIEW REQUIRED.**

## Candidate future structures

| Structure | What it would establish | Prerequisite | Status |
|---|---|---|---|
| Status quo (“LAI contributors”) | Collective Apache-2.0 notice only | None | Current |
| Named natural person as owner of specified files | Individual ownership record | Identity verification; file inventory | Not adopted |
| Company owns new work by default (employment / contractor) | Entity control of future files | Entity formation; contracts | Not adopted |
| Copyright assignment of existing work | Entity control of historical files | Written assignments from all holders | Not adopted |
| Exclusive license from contributors to an entity | Entity commercialization rights without full assignment | CLA or exclusive license agreements | Not adopted |
| CLA for inbound future contributions | Predictable inbound grant | Legal text; process | Not adopted; see [`CONTRIBUTOR_LICENSING_POLICY.md`](CONTRIBUTOR_LICENSING_POLICY.md) |
| DCO only | Provenance attestation, not assignment | Process | Not adopted |

## Inbound license currently in force

Apache-2.0 section 5 states that unless a contributor explicitly states otherwise, a Contribution intentionally submitted for inclusion is under the terms of Apache-2.0, without additional terms, and without superseding a separate license agreement if one exists.

No separate inbound agreement exists in this repository.

## Exclusive licensing rights

Exclusive commercialization of the *entire historical tree* is not established. Exclusive commercialization of *future entity-owned modules* is a candidate once ownership instruments exist.

See [`LICENSING_STRATEGY.md`](LICENSING_STRATEGY.md) and [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md) sections 12–14.

## Decision

**OWNER DECISION REQUIRED** for:

- whether a legal entity will be formed or designated,
- whether historical copyright will be assigned,
- how human authors related to the `soobujmiah` / `Sobuj` / `miahsobuj` identities will be recorded,
- whether a CLA, DCO, or assignment will be adopted.

**LEGAL REVIEW REQUIRED** before any public statement that a named person or company owns LAI.

Related ADR: [`../decisions/ADR-0102-copyright-ownership-model.md`](../decisions/ADR-0102-copyright-ownership-model.md).
