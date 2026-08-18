# AI Providers

LAI shall be **provider-agnostic** — never locked to one model, backend, or vendor.

## Current

**Local only:** `llama.cpp` `Qwen 1.5B` via `NativeInferenceEngine` direct. No provider abstraction — `MainViewModel` calls `InferenceEngine` directly. `RemoteModelCatalogRepository` fetches signed `models-v1.json` from allowlist `raw.githubusercontent.com`/`huggingface.co` (SHA-256 + size exact), but no LLM provider call.

## Target — Hybrid

**Local:** `CPU` (`llama-cpu`), `GPU` (`vulkan`, Adreno 825), `NPU` (`qnn`, HTP) — `BackendId` opaque, `Scheduler` evidence `AVAILABLE/SUPPORTED/ACTIVE/MEASURED`.

**Cloud (OpenAI-compatible):** `OpenAI`, `Anthropic`, `Gemini`, `OpenRouter` + `other future` — each as `CloudProvider` adapter behind `AI Gateway` (`registry` → `router` → `context` → `model` → `tool` → `permission` → `usage` → `audit`). `CloudProvider` holds `baseUrl`, `apiKey` (via `AndroidKeystore` + `EncryptedSharedPreferences`, never in `registry.json` or `DiagnosticsReportV1`), `model` (`gpt-4o`, `claude-3.5`, `gemini-2.0`), `cost` per 1k tokens, `quota`, `privacy` flag (`localOnlyUntilUserExport`).

**Remote:** `Ollama` (`http://localhost:11434` or `http://<lan-ip>:11434`), `local network AI servers`, `desktop/PC` (`llama.cpp server`, `vLLM`), `custom OpenAI-compatible endpoints` — same `CloudProvider` interface, `host` is `127.0.0.1` or `192.168.x.x`, `LAN` explicit opt-in with `authentication` (bearer token) + `warning` (no silent LAN), `revocation` (delete endpoint).

## Routing (per §4)

`AI Gateway` selects `Local` vs `Cloud` vs `Remote` by `capability` (from `BackendDescriptor` + `ModelSpec` + `CatalogSource`), `privacy` (local-first if `tool` is `SENSITIVE`/`ELEVATED`), `latency` (`16 tok/s` CPU vs `cloud` `TTFT`), `cost` (`provider.quota`), `model availability` (`installedModels` vs `cloud model list`), `task complexity` (`ToolInstructionGate` + `Agent` `Intent`), `device resources` (`availableMemoryBytes` vs `estimatedPeakBytes`), `network` (`AVAILABLE` vs `UNKNOWN`), `user preference` (`SettingsDocumentV1` `providerPreference`), `battery` (`64%` + `charging`), `thermal` (`NOMINAL` vs `SEVERE`). Fallback truthful: if `cloud` `UNAVAILABLE` → `local`.

## Auth / Cost / Privacy

`AndroidKeystore` + `MasterKey` for `apiKey`, `cost tracking` (per `GenerationMetrics` `promptTokens`/`generatedTokens` × `provider.cost`), `quotas` (daily/monthly, `WorkManager` reset), `privacy controls` (per-request `localOnly` flag, `Documents` never auto-upload, `DiagnosticsPrivacy` excludes `prompts`), `failure handling` (`CANCELLED`/`FAILED` terminal, `retry` bounded, `fallback` to next provider).

## Testing

`routing` (all 9 signals), `fallback` (cloud `401` → local `16 tok/s`), `auth` (bad key → `401` + no leak in log), `LAN` opt-in warning, `install -r` grant persistence, `offline` (no network → local only).
