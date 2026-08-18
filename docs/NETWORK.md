# Network

LAI shall use **network only for signed catalog + reviewed GGUF**, never for user content, and fail-closed on unknown.

## Current

*   **Owner:** Only `platform:download` has `INTERNET` (`OkHttp 5.4.0`, `WorkManager 2.10.1`, `Range` resume, `206` ETag, `SHA-256` + size exact, `4 GiB` cap, bounded traversal). `check_architecture_boundaries.py` enforces `core`/`platform`/`runtime` never import `java.net`/`okhttp`.
*   **Allowlist:** `raw.githubusercontent.com`/`huggingface.co` for `models-v1.json` + `*.gguf` (verified `models-v1.sig` ECDSA, `models-v1.json` `1,131` bytes, `sequence` windows, `canonical` bytes) — no user `prompts/screenshots` upload, no analytics.
*   **Transport:** `ModelDownloadWorker` (WorkManager, survives `install -r`, `Range` resume, `part` staging, `verifyAndActivate` before `registry.json` commit), `RemoteModelCatalogRepository` (signed `v1` envelope, `SHA256withRSA` `3072–4096` bit, `atomic` durable `highest-sequence`).

## Future — Hybrid

Cloud `OpenAI/Anthropic/Gemini/OpenRouter` + Remote `Ollama/LAN` as `CloudProvider` adapters behind `AI Gateway` — same `OkHttp` allowlist + `AndroidKeystore` `apiKey`, `cost`/`quota`, `privacy` per-request `localOnly` flag, `LAN` explicit opt-in + `bearer` auth + `warning` + `revocation`, no silent `LAN` fallback, `offline` → `local` only.

## Security

`validate_repo.sh` scans `ghp_*/github_pat_*`, `check_architecture_boundaries` fails on `core` network import, `DiagnosticsPrivacy` excludes `network_identifiers`, `ToolAuditLedger` is content-free, `install -r` keeps `noBackupFilesDir` but not `SAF` grant without persistable permission.

## Testing

`Range` `206` vs `200` resume, `4 GiB` cap, `256`-file scan cap, `WorkManager` kill mid-download, `offline` → `local` fallback, `LAN` without opt-in blocked, `install -r` grant persistence.
