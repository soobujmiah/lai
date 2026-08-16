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

## Confirmation rule

Confirmation is trusted UI state supplied separately to `AgentRuntime.execute`. A model cannot set `"confirmed": true` in arguments. Read-only snapshot/OCR and scrolling do not require confirmation; clicks, text, global actions, app launches, and mutations do.

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
{"viewId":"package:id/input","text":"বাংলা লিখুন","allowSensitiveInput":false}
```

Password targets fail unless a separate trusted product flow authorizes sensitive input. Model-authored `allowSensitiveInput` must never be accepted without a stronger user confirmation design.

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

Before autonomous multi-step execution, add an app-private append-only audit record with call ID, tool, redacted arguments, result code, foreground package, confirmation source, and timestamps. Never log entered text or screenshot content by default.
