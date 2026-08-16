# Documentation-first development policy

This policy operationalizes the master directive for contributors and engineering agents.

## Required workflow

`Inspect source and tests → document current state → design architecture → write ADR when required → plan → implement incrementally → test → update docs → verify`

Never use “idea → immediate code” for a significant feature. Never refactor a working subsystem merely for aesthetic consistency.

## Source-of-truth order

1. Current source code
2. Current tests
3. Accepted ADRs
4. Architecture documentation
5. Feature specifications
6. Canonical roadmap
7. README
8. Assumptions

Conflicts must be recorded and resolved explicitly before continuing.

## Change-management checklist

Before modifying a subsystem: read its docs; inspect implementation and tests; identify dependencies/data/authority; document the proposed change; decide whether an ADR is required; implement; run relevant tests; update current-state/feature/roadmap/user/developer docs; record limitations and rollback.

## Quality priority

Correctness > Security > Privacy > Recoverability > Maintainability > Performance > Feature count.

## Engineering-agent prohibitions

Never expose or commit secrets; disable security checks to make tests pass; remove permission gates without documented authorization; add unrestricted shell execution; weaken model/catalog verification; silently add network access, LAN exposure, cloud fallback, telemetry, or uploads; bypass approval; claim a test/hardware capability that lacks evidence; or expose signing keys to AI/model/tool context.

## No feature theater

Do not add fake buttons, canned success, production-labeled mock backends, fake hardware support, fabricated benchmarks, or documentation that turns planned behavior into implemented behavior. Use the audited statuses IMPLEMENTED, PARTIAL, EXPERIMENTAL, PLANNED, MISSING, DEPRECATED, or UNKNOWN.

## Documentation gate

Every significant change updates the appropriate architecture, current state, feature matrix, roadmap, tests, security/privacy, licensing, user, and developer documentation in the same change. Missing required documentation blocks the next phase.

## Testing rule

“Tested” is permitted only after the named test ran. Record test, environment, command/procedure, result, failure/artifacts, evidence level, limitation, date, and reviewer. See [`../implementation/testing-plan.md`](../implementation/testing-plan.md).

## Secret handling

Use GitHub Actions secrets, temporary runner files, credential managers, or GitHub CLI/device authentication. Never place tokens in URLs, source, logs, prompts, diagnostics, examples, remote configuration, or commits. Revoke any credential disclosed in chat/logs.

## Review and rollback

Large file/project changes are diff-first. Show proposal, permission, approval, apply, test, checkpoint, and rollback. For actions that cannot be rolled back, show that fact before approval and define compensation/recovery where possible.
