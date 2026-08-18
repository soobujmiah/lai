# Cloud / Hybrid Experience

LAI shall **clearly communicate** `local vs cloud`, `data leaving device`, `cost`, and `privacy`.

## Current

**Local-only** — no cloud/remote provider, no `AI Gateway`. `DiagnosticsReportV1` privacy `localOnlyUntilUserExport true`, `excludedData` (`prompts/generated_text/screenshots/...`).

## Target

Every generation shows **which intelligence** is used:

*   **Local:** `LLAMA-CPU` / `VULKAN` / `QNN` (`16–22 tok/s` you validated, `40–60 tok/s` Vulkan next) — badge `Local` + `model` (`Qwen 1.5B`) + `backend` + `thermal` + `battery`.
*   **Cloud:** `OpenAI gpt-4o` / `Anthropic claude-3.5` / `Gemini 2.0` / `OpenRouter` — badge `Cloud` + `provider` + `model` + `estimated cost` (`promptTokens` + `generatedTokens` × `provider.cost`) + `privacy` warning (`data leaves device`) + `network dependency` + `token usage`.
*   **Remote:** `Ollama` / `LAN 192.168.1.10:11434` — badge `Remote` + `host` + `latency` + `LAN explicit opt-in` warning.

**Controls:** `Settings → AI → Provider` (multiple, ordered), per-request `Local only` toggle (forces `local` even if `cloud` faster), `Cost tracking` (daily/monthly `quota`, `WorkManager` reset), `Quotas` (block cloud when exceeded, fallback `local`), `Privacy` (per-tool `SENSITIVE` forces `local`, `Documents` never auto-upload), `Authentication` (`AndroidKeystore` `apiKey`, never in `registry.json` or diagnostics).

**Failure:** `cloud 401/429/5xx` → `fallback` to `local` with `notice` (“Cloud unavailable, used local Qwen 1.5B”), `retry` bounded, no silent `LAN` fallback.

## Testing

`provider` `401` → `local` fallback, `quota` exceeded → `local`, `LAN` without opt-in → blocked, `offline` → `local` only, `install -r` grant persistence, `Cost` calculation vs `GenerationMetrics`.
