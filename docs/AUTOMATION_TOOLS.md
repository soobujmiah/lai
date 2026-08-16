# Android automation tool contract

## Envelope

```json
{
  "id": "unique-call-id",
  "name": "screen.click",
  "arguments": {"viewId": "com.example:id/continue"}
}
```

Results always correlate by `callId` and contain `success`, bounded `output`, and optional structured `error`.

## Model-proposal boundary

Local action proposals are disabled by default. When enabled, LAI instructs the model that an explicit Android operation request **must** produce one bare JSON proposal when a single schema can perform the next action; otherwise it should answer normally or ask for missing details. A proposal is recognized only when the model's entire response is one JSON object with exactly `id`, `name`, and `arguments`. The envelope is limited to 16 KiB; tool IDs/names, object keys, types, selector depth, strings, package names, enums, and shell operations are bounded. Markdown-wrapped, mixed prose, malformed, unknown-field, unknown-tool, and wrong-type proposals fail closed.

The same validator runs again in `AgentRuntime` before authority dispatch. Natural-language answers and unrelated JSON remain assistant text. Developer Mode and user-exported diagnostics expose session-only content-free counters for examined, accepted, rejected and ordinary responses plus coarse rejection codes; no model response or argument text is retained.

## Confirmation rule

Every model-authored proposal—including read-only tools—opens a trusted Compose review dialog and runs at most once after **Approve once**. Newlines, control characters, and bidirectional-format controls are escaped/replaced in the human-readable preview without changing the validated execution value. Denial invokes no Android authority. A model cannot set `"confirmed": true`; unknown envelope fields are rejected before a `ToolCall` exists. Runtime policy separately requires confirmation for clicks, typing, global actions, app launches, and elevated operations, providing defense in depth.

There is no autonomous multi-step loop and tool output is not automatically fed back to the model. Before execution, LAI fsync-appends a content-free approval to an app-private hash-chained JSONL audit; failure blocks the action. Exact call ID+arguments replay is rejected across process restarts. The UI/diagnostics expose at most 50 recent projections containing tool name, risk, approval/result state, and timestamp—never arguments, fingerprints, hashes, or output.

## Tools

### `screen.snapshot`

Read-only. No arguments. Returns package, title, timestamp, truncation, and up to 400 flattened nodes. Password node text/content descriptions are null.

### `screen.click`

Confirmation required. Selector accepts one or more:

```json
{"viewId":"package:id/button","text":"Continue","contentDescription":"Continue","path":[0,2,1]}
```

A non-clickable target walks at most six parents to find a clickable ancestor.

### `screen.type`

Confirmation required.

```json
{
  "selector":{"viewId":"package:id/input"},
  "text":"বাংলা লিখুন"
}
```

The nested selector prevents typed content from being ambiguously reused as selector text. `allowSensitiveInput` is not in the model schema and is rejected as an unknown field. Password targets fail; a future sensitive-input flow would require a separate trusted design and cannot be authorized by model JSON.

### `screen.scroll`

```json
{"selector":{"viewId":"package:id/list"},"forward":true}
```

Selector is optional; the first scrollable node is used otherwise.

### `system.global_action`

Confirmation required. `action`: `back`, `home`, `recents`, or `notifications`.

### `app.launch`

Confirmation required. Accepts a validated package name. No arbitrary intent URI or extras are accepted.

### `ocr.current_screen`

Read-only from the tool-policy perspective, though Android accessibility screenshot permission is required. Returns `OcrResult` schema v1 when an OCR plugin is installed.

### `shell.operation`

Shizuku required. No raw command exists.

```json
{
  "operation":"settings.get",
  "arguments":{"namespace":"system","key":"screen_brightness"}
}
```

Operations:

| Operation | Mutation | Notes |
|---|---:|---|
| `device.info` | No | `getprop`, output bounded |
| `package.list_user` | No | third-party packages only |
| `package.force_stop` | Yes | validated package |
| `package.install_existing` | Yes | validated package |
| `settings.get` | No | system/secure/global, validated key |
| `settings.put` | Yes | small explicit setting-key allowlist |
| `input.keyevent` | Yes | small explicit key-code allowlist |

## Prompt-injection defense

Text read from another app is untrusted content. It may say “ignore previous instructions,” request secrets, or present fake confirmation. The agent must not treat screen text as user authority. Consequential calls still pass through policy and trusted confirmation UI.

## Audit evolution

The current one-shot flow stores a bounded app-private, no-backup, hash-chained audit and exports only a privacy-filtered projection after user request. It blocks execution unless approval is durably recorded first and rejects an exact previously approved call. Before autonomous multi-step execution, add reviewed archival/reset policy, foreground-package/screen binding, step budgets, result provenance, and cancellation. Never persist entered text, selector text, package names, shell output, screenshots, OCR content, model output, fingerprints, or record hashes in diagnostics.
