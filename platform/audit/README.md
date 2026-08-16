# `platform:audit`

Owns persistent security metadata for model-proposed tool execution.

- Storage: `noBackupFilesDir/security/tool-audit-v1.jsonl`; app-private, excluded from Auto Backup, removed on uninstall.
- Content: sequence/event ID, canonical call fingerprint, tool name/risk, outcome, timestamp and hash-chain links only.
- Forbidden content: arguments, selectors, typed text, package names, model/tool output, screenshots, OCR, documents and shell output.
- Ordering: a verified `USER_APPROVED` event is fsynced before Android authority is invoked; success/failure follows execution.
- Limits: 2 MiB, 4,000 events, 4 KiB per record. Integrity or capacity failure disables model tool proposals.
- Tests: pure hashing, transition, canonicalization, replay and tamper behavior live in `core:policy`; Android file lifecycle still requires physical testing.

See [ADR 0007](../../docs/adr/0007-persistent-tool-audit-and-replay-guard.md).
