# ADR 0006: Opt-in one-shot model tool proposals

- Status: Accepted; build verified, device verification pending
- Date: 2026-08-16

## Context

LAI already had typed manual Accessibility and Shizuku tools plus an `AgentPolicy` confirmation bit, but local model output was treated only as chat text. Connecting arbitrary model JSON directly to those authorities would allow malformed calls, schema ambiguity, model-authored confirmation, shell-argument abuse, accidental multi-step execution, and prompt-injection content to cross into Android control.

The first model-driven automation increment must provide real product value without introducing an autonomous agent loop before replay protection, persistent audit, foreground binding, and adversarial device evidence exist.

## Decision

Implement model tools as an explicit, disabled-by-default, one-shot proposal flow:

1. The user enables **Local action proposals** in Settings.
2. LAI adds a trusted system instruction describing the fixed built-in registry.
3. A proposal is considered only if the model's entire response is one JSON object with exactly `id`, `name`, and `arguments`.
4. `ToolCallParser` enforces a 16 KiB envelope, IDs, known tool names, exact keys, JSON types, string/path/count limits, selectors, enum/package rules, and operation-specific shell schemas.
5. `screen.type` uses a nested selector so entered text cannot also be interpreted as selector text. Model-authored sensitive-input flags are forbidden.
6. `AgentRuntime` invokes the same validator again before dispatch, including calls from future plugin/manual paths.
7. Every model proposal—including read-only observation—opens a trusted Compose dialog. **Approve once** supplies confirmation separately; **Do not run** invokes no Android authority.
8. At most one call executes. Tool output is not automatically returned to the model, and no subsequent call is generated.
9. LAI keeps at most 50 in-memory content-free audit records: tool name, risk, approval/result state, and timestamp. Arguments and output are excluded from diagnostics.
10. Trusted Kotlin system messages are merged into one native system message and preserved during context trimming.

## Consequences

Positive:

- model output cannot directly authorize itself;
- JSON ambiguity and unknown fields fail closed;
- shell operations still pass through the argv-only allowlist;
- password entry remains blocked;
- the user sees a human-readable action and risk before authority is invoked;
- no autonomous cascade can result from one approval;
- ordinary chat remains unchanged while the feature is disabled.

Costs:

- strict whole-response JSON may reject otherwise understandable fenced or mixed model output;
- the tool system instruction consumes context and may increase prefill time while enabled;
- tool results cannot yet support a multi-step task;
- the in-memory audit disappears on process death and is not replay protection;
- exact model compliance and Android behavior require physical testing.

## Alternatives considered

- **Parse JSON from prose or Markdown:** rejected because extraction is ambiguous and can turn quoted/untrusted content into authority.
- **Allow the model to set `confirmed`:** rejected because model output is never trusted UI state.
- **Automatically execute read-only calls:** rejected for the first model-driven phase; visible user review is simpler and safer.
- **Immediately build an autonomous loop:** rejected until persistent audit, foreground binding, loop budgets, result provenance, cancellation, and adversarial tests exist.
- **Reuse a flat `text` field for both selector and entered text:** rejected as ambiguous; typing uses a nested selector.
- **Persist full calls/results for debugging:** rejected because they may contain typed text, screen content, package data, or shell output.

## Current limitation

This is a one-action proposal mechanism, not an autonomous agent. GitHub run 31953199936 passed parser tests, coverage, Kotlin/C++, lint and APK assembly, but it still needs physical validation with the selected Qwen model, Accessibility, Shizuku, rotation/backgrounding, denial, replay attempts, prompt injection, and diagnostics inspection.

## Future migration path

Before any multi-step agent loop:

1. add replay-resistant app-private append-only audit metadata;
2. bind proposals to a fresh foreground package/screen observation where applicable;
3. define content provenance for user text, screen text, model output, and tool results;
4. add bounded step/time/token budgets and an always-available stop control;
5. feed only typed, size-limited tool results back to the model;
6. require new confirmation for every consequential step;
7. add parser/property fuzzing and physical prompt-injection tests;
8. keep all execution local and preserve the same canonical validator/authority boundaries.
