# Security architecture

Last audited: 2026-08-17

## Purpose

Define security ownership and trust boundaries for the current repository. [`../SECURITY_AND_SAFETY.md`](../SECURITY_AND_SAFETY.md) remains the detailed control inventory; this document maps controls to architecture and future gates.

## Assets

User prompts/generations, visible screen content, screenshots, model artifacts, workspace documents, Accessibility/Shizuku authority, audit integrity, native-process integrity, plugin inputs, signing keys, SDK credentials, and future project secrets.

## Responsibilities

- `LocalFirstPolicy`: purpose/data/direction/host/digest network decisions.
- `BuiltInToolCatalog`, `ToolCallParser`, `AgentPolicy`: model boundary and consent.
- `ShellCommandPolicy`: named operation to validated argv.
- `ToolAuditLedger`/`ToolAuditRepository`: transition/replay checks and durable chain.
- Model/catalog repositories: signatures, hashes, formats, private activation.
- Workspace policy/repository: explicit SAF grant, schema/size/format/hash classification.
- Architecture/source scripts: dependency, authority, secret-pattern, and binary gates.
- CI: isolated toolchain and optional secret-scoped release signing.

## Interfaces and dependencies

Security decisions are pure core code where possible; Android persistence/authority is platform-owned. Runtime and app call policy rather than embedding alternate permission rules. CI secrets enter only environment variables and temporary runner files. No AI-facing interface exposes credentials or unrestricted processes.

## Trust boundaries and data flow

| Boundary | Input | Current control | Failure result |
|---|---|---|---|
| Model output | text/JSON | 16 KiB whole-response parser, exact schemas, policy, trusted review | reject/no authority |
| Screen content | nodes/bitmap | bounds, password omission, no authority by content | typed failure/data only |
| Download | public bytes | HTTPS, allowlist, signature/digest/size/magic | no activation |
| SAF workspace | provider metadata/bytes | explicit grant, bounded schema/discovery, hash classification | defaults/rejected metadata |
| Accessibility | system service | user enablement, fresh roots, typed commands | unavailable |
| Shizuku | binder/elevated process | user permission, UID visibility, argv allowlist, timeout/output cap | denied/failure |
| Native runtime | JNI/C++ | opaque handles, bounded config, explicit close | inference failure |
| Audit file | private JSONL | bounds, exact decode, hash chain, transition policy, fsync | proposals disabled |
| CI secrets | environment | GitHub secrets, temporary keystore | unsigned/debug test artifact |

## Security invariants

1. User-derived content has no outbound network path.
2. Only `platform:download` may own network transport/permission.
3. No model, plugin, or screen content grants confirmation authority.
4. No arbitrary shell command API exists.
5. Model activation requires digest and format validation.
6. Unavailable acceleration is not advertised.
7. Consequential actions are per-action reviewed.
8. Production secrets never enter source, logs, model context, or agent tools.

## Known risks and contradictions

- Model download/import streams now have a pre-write ceiling, but SAF workspace discovery is not yet bounded during every read; post-read classification remains insufficient against provider-driven I/O exhaustion.
- SAF settings replacement deletes the old generation before rename completion.
- Audit hashes are unkeyed and do not resist a root attacker rewriting all private bytes.
- Screenshot redaction beyond password-node omission is missing.
- Native adapters lack memory-safety fuzzing.
- CI action references are mutable tags and release permission is broader than the tag-only comment suggests.
- No formal project trust, secret manager, localhost threat model, plugin sandbox, SBOM, provenance, or incident-response procedure exists.

## Lifecycle and recovery

Security state initializes with proposal mode off. Corrupt/missing audit state fails closed. Accessibility and Shizuku are revocable by the user. Models can be unloaded/deleted; retained copies are user-controlled. The current system lacks audit archive/reset, robust interrupted-write recovery across all stores, encrypted backup, and process-death task recovery.

## Testing strategy

Use unit/property/fuzz tests for parsers, policies, canonicalization, URL/redirect handling, argument compilation, schema migrations, and bounds. Use fake SAF/content providers for partial writes and deceptive metadata. Use instrumentation/physical tests for service death, permission revocation, screenshots, Shizuku identity, storage full, cancellation, and reboot. Add dependency/SBOM/provenance checks before production signing.

## Extension strategy

Before AI Gateway/localhost/remote providers: create provider trust and network-policy ADRs, loopback authentication, LAN opt-in, secret references, client visibility, and kill switch. Before project/workstation/plugin execution: introduce project trust, deny-by-default permission manifests, process/filesystem boundaries, diff/rollback, secret redaction, and incident response. Security reviews are phase gates, not post-implementation tasks.
