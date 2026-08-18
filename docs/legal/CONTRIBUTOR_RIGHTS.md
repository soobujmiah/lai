# Contributor rights

**Status:** canonical contributor-IP policy  
**Date:** 2026-08-18  
**Implemented enforcement:** none  
**Register:** [`OWNERSHIP_DECISIONS.md`](OWNERSHIP_DECISIONS.md) IP-004  
**Engineering contribution rules:** [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md)

This file is the canonical inbound-rights policy. [`CONTRIBUTOR_LICENSING_POLICY.md`](CONTRIBUTOR_LICENSING_POLICY.md) is a pointer.

## Current rule

Until a separate agreement exists, Apache-2.0 section 5 applies to intentional submissions to the public Work: the contribution is under Apache-2.0 without additional terms.

Effects:

- Public-core patches are expected to be Apache-2.0.
- The project does not currently receive a documented right to relicense those patches under a different license.
- Relicensing contribution-containing files later would need additional grants or a rewrite. **LEGAL REVIEW REQUIRED.**

No CLA, DCO, copyright assignment, or GitHub inbound App is active. None may be activated in this documentation phase.

## Goal of future commercially controlled development

If the LAI project owner / rights holder later accepts outside patches into code that must be dual-licensed or kept proprietary, inbound terms must be explicit **before** merge. Otherwise Apache-2.0 §5 remains the grant.

## Mechanisms

| Mechanism | What it provides | Relicensing / exclusive control of *new* work | Fit for commercially controlled LAI development |
|---|---|---|---|
| No CLA (status quo) | §5 only | Low | Adequate only if premium modules are written solely by the rights holder and never accept uncleared PRs |
| DCO | Provenance attestation | Low | Useful add-on for provenance; not sufficient alone |
| Contributor license grant / individual CLA | Contractual copyright and patent license to the rights holder, often including sublicense | Medium–high, text-dependent | Candidate if the public or premium tree will take external patches that must later be commercially licensed |
| Corporate CLA | Employer grant | Medium–high for those patches | Candidate if companies contribute |
| Copyright assignment | Transfer of copyright in specified works | Highest if valid | Candidate for a single-owner catalog; highest friction |
| PR inbound terms | Click-through grant | Text-dependent | Must not silently conflict with §5 |

**LEGAL REVIEW REQUIRED** before adopting any instrument. No text is selected.

For the stated goal — future commercially controlled development of LAI-owned work — a **contributor license grant (individual CLA), plus DCO for provenance**, is the documented candidate pairing. It is **not** adopted. **DECISION REQUIRED** (IP-004).

Assignment is stronger and heavier. DCO-only is insufficient for relicensing.

## External pull requests

Until IP-004 is decided:

1. Treat a PR to the public tree as an Apache-2.0 contribution under §5 unless the submitter marks it otherwise.
2. Reject a PR that claims extra terms the project cannot accept.
3. Do not merge a PR into a tree intended to be proprietary. No such tree exists yet.
4. Run license review if the PR adds a dependency, copied file, model, or dataset.
5. Do not treat Dependabot version bumps as a human authorship event; they still need license review when the new version changes terms.

## Multiple contributors on one component

Joint authorship of a file under Apache-2.0 does not assign exclusive control to the LAI project owner / rights holder. A later proprietary relicensing of that file needs all holders or a rewrite. Prefer keeping uncleared multi-author work in the public Apache-2.0 core.

## AI-prepared patches

An agent-prepared patch that a human merges is still a contribution. The agent is not a contributor for copyright purposes. Provenance: [`AI_CODE_PROVENANCE.md`](AI_CODE_PROVENANCE.md).

## Future PR requirements (not implemented)

If IP-004 is accepted, documentation should then add — without implementing in this phase:

- a visible inbound-terms link in [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md);
- optional DCO `Signed-off-by`;
- optional CLA signer record stored off-Git;
- a CI reminder, not a silent license change.

## What must not enter a proprietary core without clearance

- External PRs without a grant to the LAI project owner / rights holder
- Third-party source
- Files whose copyright holders are unknown
- Copied NpuHub or other private-project source
- Unknown-license snippets

## Decision

**DECISION REQUIRED** (IP-004). No CLA or DCO is implemented.
