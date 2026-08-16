# Plugin architecture

Last audited: 2026-08-17

## Purpose

Describe the existing plugin contract without overstating it as a complete SDK or plugin manager.

## Current status

The plugin system is **PARTIAL**. `:plugins:api` defines API version 1 contracts and validates a local-only manifest. There is no plugin discovery, package format, installer, signature verification, class loading, process isolation, permission dashboard, lifecycle manager, compatibility migration, UI contribution host, rollback, or plugin store.

## Responsibilities and interfaces

- `PluginManifest`: ID, semantic version, API version, description, capabilities, risk, local-only data policy, input/output schemas, optional signing-key ID.
- `LaiPlugin`: manifest, input validation, and suspend execution.
- `PluginExecutionContext`: invoke an already approved typed tool and report progress.
- `PluginResult`: success/output/error.

Capabilities currently enumerate chat tool, OCR post-processor, RAG source, speech input, and speech output. Risk values are low, interaction, sensitive, and elevated. `PluginDataPolicy` only allows `LOCAL_ONLY`.

## Dependencies

`plugins:api` exposes `core:contracts` and `core:policy`. It has no Android dependency and no authority implementation. No app or runtime code currently discovers or executes a `LaiPlugin` implementation.

## Intended lifecycle (not implemented)

A future lifecycle requires: package acquisition → integrity/signature/license review → manifest/schema validation → compatibility check → permission review → disabled installation → explicit enable → bounded execution → audit/health → update/rollback/uninstall. None of these lifecycle stages should be inferred from the interface alone.

## Data flow and security boundaries

The current contract intends JSON input/output and approved typed-tool invocation. A future host must treat plugin packages, schemas, outputs, progress text, and signing metadata as untrusted. Permissions default to deny. Plugins must not receive raw `Context`, Binder, SAF grants, Accessibility nodes, shell, secrets, model paths, or network sockets unless a separately documented capability and policy explicitly permits it.

## Failure behavior

Today manifest validation returns findings and plugin code is otherwise not hosted. A future manager must quarantine invalid/incompatible packages, bound time/memory/output, propagate cancellation, revoke permissions, record failures without user content, and recover from crashes without repeatedly restarting a bad plugin.

## Testing strategy

The existing unit test covers manifest validation. Required future tests include schema fuzzing, compatibility, signature/integrity failures, permission denial, cancellation/timeouts, crash isolation, update rollback, malicious output, storage pressure, and migration across API versions.

## Extension strategy

Do not add capabilities ad hoc. First document the package/manifest format, permission vocabulary, isolation model, lifecycle, compatibility guarantees, audit behavior, signing trust roots, licensing, and rollback. Then create an ADR before implementing the host/manager. UI and AI-tool contribution points must use stable contracts and cannot bypass the same policy gates as built-ins.

## Related source

- `plugins/api/src/main/kotlin/dev/lai/runtime/plugins/LaiPlugin.kt`
- `plugins/api/src/test/kotlin/dev/lai/runtime/plugins/PluginManifestTest.kt`
