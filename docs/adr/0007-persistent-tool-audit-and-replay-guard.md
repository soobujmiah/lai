# ADR 0007: Persistent content-free tool audit and replay guard

- Status: Accepted; build verified, device verification pending
- Date: 2026-08-16
- Refines: [ADR 0006](0006-one-shot-model-tool-proposals.md)

## Context

The first one-shot tool proposal implementation kept only 50 redacted events in ViewModel memory. That was enough to inspect a live session but disappeared on process death, could not detect exact-call replay across restarts, and could not prove that trusted approval was recorded before Android authority was invoked.

Persisting full calls or results would create a sensitive log containing selector text, entered text, screen data, package information, or shell output. The security record therefore needs durable sequencing and integrity without retaining those values.

## Decision

Add a concrete `platform:audit` boundary with these rules:

1. Audit bytes live only in `noBackupFilesDir/security/tool-audit-v1.jsonl`; Android removes them on uninstall and does not include them in Auto Backup.
2. Records contain schema/sequence, random event ID, SHA-256 call fingerprint, canonical tool name/risk, outcome, timestamp, previous-record hash, and record hash.
3. Arguments, selectors, typed text, model output, tool output, package names, OCR, screenshots, and shell output are never fields in the persistent schema.
4. Pure-JVM `ToolAuditLedger` validates every tool through the canonical parser, enforces tool/risk identity, computes the call fingerprint, verifies the complete hash chain, and enforces transitions.
5. `USER_APPROVED` must be fsync-appended before `AgentRuntime` receives trusted confirmation. If this write fails, no Android authority is invoked.
6. An exact call fingerprint can receive only one approval and one completion record. Reusing the same call ID and arguments is rejected as replay, including after process restart.
7. `EXECUTION_SUCCEEDED` or `EXECUTION_FAILED` requires a prior approval and is appended after execution.
8. Denials invoke no authority and are appended best-effort as `USER_DENIED`.
9. Parsing is bounded to 2 MiB, 4,000 events, and 4 KiB per JSONL record. Unknown fields, partial lines, unsupported schemas, sequence gaps, transition errors, and hash mismatches fail verification.
10. Audit verification failure disables model tool proposals. Diagnostics expose only recent content-free event projections and an integrity boolean—not fingerprints or record hashes.

## Consequences

Positive:

- trusted approval is durable before a one-shot action runs;
- exact-call replay is blocked across Activity and process restarts;
- partial writes and accidental/tampered record changes are detected;
- model/user content is absent from the persistent record and diagnostics;
- file I/O, limits, and lifecycle have one Android owner;
- ledger transitions and hashing are testable without Android.

Costs and limits:

- each approved action creates two events;
- verification is linear in the bounded audit size before each append;
- a completion-write failure leaves a durable approval with no completion and disables proposals for the session;
- reaching the 2 MiB/4,000-event limit blocks further proposals until a future reviewed archival/reset design exists;
- an unkeyed SHA-256 chain is tamper-evident for corruption and ordinary app-private access, but it is not secure against a root attacker who can rewrite the file and recompute the entire chain;
- fingerprints may permit guessing of very small known call spaces, so they are never exported.

## Alternatives considered

- **Keep memory-only records:** rejected because approval and replay state vanish on process death.
- **Persist full JSON calls/results:** rejected as an unnecessary sensitive-content log.
- **Store only independent records:** rejected because deletion/reordering would not be detected.
- **Execute first, write later:** rejected because audit failure would leave an unaudited action.
- **Android Keystore HMAC immediately:** deferred; it improves root/tamper resistance but adds key lifecycle, backup, device-lock, invalidation, and recovery behavior requiring a dedicated design and physical tests.
- **Silent truncation/rotation:** rejected because it would erase security history without explicit policy.

## Verification

GitHub Actions run 31956572135 passed pure-JVM ledger tests, Android file persistence/reopen/corruption/replay tests, coverage, Kotlin/C++, lint and APK assembly. Physical process-death, fsync failure, capacity, corruption-test-build and diagnostics inspection remain pending.

## Future migration path

Before autonomous multi-step tools, add a reviewed checkpoint/export/reset UX, foreground-screen binding, step budgets, result provenance, cancellation, and physical adversarial tests. If stronger tamper resistance is required, introduce a versioned Android Keystore MAC key and migration record while retaining content-free events and fail-closed execution ordering.
