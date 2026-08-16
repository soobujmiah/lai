# Privacy-filtered diagnostics export

LAI exports diagnostics only after the user chooses a destination through Android's Storage Access Framework. The app writes `application/json`; it does not upload, share automatically, request broad storage access, or remember the destination.

## Schema

Current schema: `DiagnosticsReportV1`, `schemaVersion = 1`.

Included:

- app version/code and production-signing flag;
- operation and signed-catalog status;
- manufacturer/model, optional public SoC manufacturer/model, Android SDK, supported ABIs and reported CPU core count;
- available memory, battery, charging and thermal state;
- compiled namespaced backend IDs, scheduler decision and context size;
- active model ID, installed model IDs/sizes/SHA-256 and load time;
- Accessibility connection boolean and coarse Shizuku state/UID;
- conversation-turn trim count, never conversation text;
- local-action-proposal enabled state, persistent-audit integrity boolean, persistence schema label and up to 50 recent event projections containing only tool name, risk, approval/result state and timestamp;
- up to 20 in-memory generation samples: prompt/output token counts, prefill, TTFT, decode and total duration.

Always excluded:

- prompts and generated text;
- screenshots, OCR text and Accessibility trees;
- foreground package/app names;
- documents, RAG chunks and embeddings;
- tool arguments, selectors, typed automation text, tool results, call fingerprints, record hashes and shell output;
- credentials, IP addresses and network identifiers.

## Example shape

```json
{
  "schemaVersion": 1,
  "generatedAtEpochMs": 0,
  "app": {"versionName": "0.8.0", "operation": "READY"},
  "device": {"androidSdk": 36, "socModel": "example", "thermalState": "NOMINAL"},
  "runtime": {"compiledBackends": ["llama-cpu"], "contextSize": 4096},
  "automation": {"toolProposalsEnabled": true, "auditPersistence": "APP_PRIVATE_HASH_CHAIN_V1", "auditIntegrityValid": true, "records": []},
  "models": [{"id": "model-id", "bytes": 1, "sha256": "...", "active": true}],
  "performance": [{"promptTokens": 20, "generatedTokens": 15, "timeToFirstTokenMs": 100}],
  "privacy": {"localOnlyUntilUserExport": true, "excludedData": ["prompts", "generated_text"]}
}
```

Consumers must ignore unknown fields and select parsers by `schemaVersion`. Diagnostics are support evidence, not telemetry and not proof of model quality.
