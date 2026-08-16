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

Local action proposals are disabled by default. When the user enables them, LAI adds a trusted system instruction describing the built-in schemas. A proposal is recognized only when the model's entire response is one JSON object with exactly `id`, `name`, and `arguments`. The envelope is limited to 16 KiB; tool IDs/names, object keys, types, selector depth, strings, package names, enums, and shell operations are bounded. Markdown-wrapped, mixed prose, malformed, unknown-field, unknown-tool, and wrong-type proposals fail closed.

The same validator runs again in `AgentRuntime` before authority dispatch. Natural-language answers and unrelated JSON remain assistant text.

## Confirmation rule

Every model-authored proposal—including read-only tools—opens a trusted Compose review dialog and runs at most once after **Approve once**. Newlines, control characters, and bidirectional-format controls are escaped/replaced in the human-readable preview without changing the validated execution value. Denial invokes no Android authority. A model cannot set `"confirmed": true`; unknown envelope fields are rejected before a `ToolCall` exists. Runtime policy separately requires confirmation for clicks, typing, global actions, app launches, and elevated operations, providing defense in depth.

There is no autonomous multi-step loop and tool output is not automatically fed back to the model. LAI keeps at most 50 in-memory redacted audit records containing tool name, risk, approval/result state, and timestamp—never arguments or output. Persistent append-only audit storage remains a gate before autonomous execution.

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

The current one-shot flow stores only a 50-record in-memory audit and exports a privacy-filtered subset in user-requested diagnostics. Before any autonomous multi-step execution, add an app-private append-only record with replay protection, tool, redacted argument categories, result code, foreground-package binding, confirmation source, and timestamps. Never persist entered text, selector text, shell output, screenshots, OCR content, or model output by default.
