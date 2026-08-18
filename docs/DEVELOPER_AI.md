# Developer AI

LAI shall provide a **mobile developer workstation** over `storage/LAI/` projects — not a code generator alone.

## Current

**Missing** — no file manager, editor, terminal, Git, or Linux runtime. `AGENT.md` one-shot is the only automation.

## Target Workflow

```text
INSPECT → UNDERSTAND → PLAN → MODIFY → BUILD → TEST → VERIFY → DOCUMENT
```

All over a **user-owned SAF Project** (`storage/LAI/<project>/`): `fileManager` (SAF `ACTION_OPEN_DOCUMENT_TREE`, coarse counts, no `MANAGE_EXTERNAL_STORAGE`), `editor` (LSP, search/diff, autosave), `terminal` (see `LINUX.md`), `Git` (`clone/branch/status/diff/commit/push/pull`), `GitHub` (`issue/pr/release/workflow/repoAnalysis`), `build` (Gradle/NDK/CMake `install -r`), `logs/diagnostics`, `project architecture analysis`.

## Capabilities

*   **Repository inspection:** List files, `ls/R` bounded, `.git` aware, respects `validate_repo.sh` `128 MB` + `*.gguf/*.so` forbidden.
*   **Code search/generation/editing/refactoring:** `code.search` (ripgrep-like, bounded), `code.generate` (LLM, streaming, cancellable), `code.edit` (diff + `ToolAuditLedger` per-step approval, no silent overwrite), `refactor` (diff + test gate).
*   **Debugging:** `logs` (`logcat -s LAI-llama`), `diagnostics` (`DiagnosticsReportV1`), `staticAnalysis` (`detekt/ktlint` as in NpuHub `config/`).
*   **Test generation/build assistance:** `test.generate` (bounded, JVM-only), `build` (delegates to `gradle` with `validate_repo.sh` gate, not local heavy SDK).
*   **Documentation generation:** `docs/` auto-update (this directive).

## Architecture

`features:developer` (UI) → `core:agent` (plan) → `core:pipeline` (DAG) → `platform:workspace` (SAF) + `platform:shizuku` (where available) + `backend:litert`/`runtime:llama` (for code LLM). **No feature calls vendor runtime directly.**

## Security

Untrusted code runs in project-scoped `linux` container (future), no `MANAGE_EXTERNAL_STORAGE`, `tool` `EXECUTE`+ requires approval, `diff` shown before `apply`.

## Testing

Large files (256-file scan cap), `process kill` mid-build, `merge conflict`, `install -r` grant persistence, `WorkManager` download during build.
