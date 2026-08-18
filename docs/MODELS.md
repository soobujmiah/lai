# Model Management

LAI shall manage models **verified, bounded, offline-first, and `install -r` safe**.

## Current

*   **Catalog:** Signed `catalog/models-v1.json` rev3 (`catalog-public-key.pem`), `catalog-public-key.pem` ECDSA, `models-v1.json` `1,131` bytes + detached `models-v1.sig`, `RemoteModelCatalogRepository` (ONLY `platform:download` has `INTERNET`, `OkHttp 5.4.0`, WorkManager `2.10.1`, `Range` resume, `206` ETag, `atomically staged` `part` + `registry.json`).
*   **Trust:** `validate_model_catalog.py` (schema, `compatibleBackendIds`/`preferredBackendId`/`fallbackBackendIds`, size/sha, `sequence` windows, `canonical` bytes, `SHA256withRSA` `3072–4096` bit, rollback policy) — like NpuHub `CATALOG.md` simplified.
*   **Storage:** `ModelRepository` (`noBackupFilesDir/models`, `registry.json` atomic `tmp→rename`, `SHA-256` streaming `8192`, `GGUF` magic `ModelFormatDetector`, `4 GiB` file cap, `64` files cap) + `WorkspaceDiscovery` (`storage/LAI/models` SAF `depth 4`, `256` cap, `8 GB` hard cap, `SHA` streaming, `WorkspacePolicy` `REVIEWED/LOCAL_UNREVIEWED/REJECTED`) + **auto-import** `storage/LAI/models/*.gguf` on launch (`17ad75b`) — survives `install -r`.
*   **Lifecycle:** `list` (≤100 sessions, ≤512 msgs for chat, but models: `64` files), `download` (explicit tap, cancellable, `Range`, `206` only, `size/digest` validated), `import` (file picker + SAF, bounded), `Keep copy` (verified `DocumentFile` export to `Downloads`, survives uninstall), `Delete` (active-guarded, `install -r` keeps `LAI/`), `load/unload` (scheduler preflight `1.93 GB` peak vs `4.0 GB` free, `570 ms` load).
*   **Verification:** Exact `displayName`, `bytes`, `sha256`, `active` in `DiagnosticsReportV1` + `ModelDiagnostics`; `1,117,320,736` bytes for `Qwen 1.5B`.
*   **APK weight:** `model/` gitignored (`*.gguf` forbidden), `validate_repo.sh` keeps repo `~1.25 MB <128 MB`; `app/build/sbom` lightweight; no `*.gguf` in repo.

## Future

*   **Capability detection:** `BackendDescriptor` (`id`, `computeClass`, `supportedModelFormats`, `supportedQuantizations`, `preference`) + `InferenceScheduler` memory/thermal/battery-aware routing → `3B Q4_K_M ~1.7 GB` needs Vulkan, `5B` needs QNN.
*   **One-run grants:** Like NpuHub `features:models` — exact `size/SHA` check, bounded preview, one-run `InstallGrant` + `atomic staging` (today `registry.json` is permanent, not one-run).
*   **External storage:** `storage/LAI/models` is the **single** user-owned store (already) — `SAF` `GRANTED/REVOKED`, coarse counts in UI, never `MANAGE_EXTERNAL_STORAGE`.
*   **Provenance:** `THIRD_PARTY_NOTICES` + `MODEL_LICENSES` (`Qwen` Apache-2.0), `CycloneDX` SBOM (from `sbom-*.txt`).

## Evidence

`AVAILABLE` (loader `libvulkan.so` on Adreno 825) → `SUPPORTED` (Qwen `llama-cpu` validated) → `ACTIVE` (570 ms load) → `MEASURED` (16–22 tok/s prefill) → `UNKNOWN`. Unmeasured = `N/A`.

## Testing

`ModelFormatDetectorTest` (`GGUF` magic), `WorkspacePolicyTest` (`REVIEWED/LOCAL_UNREVIEWED/REJECTED`), `ModelRepository` `Range` + `part` + `SHA`, `install -r` grant persistence, `WorkManager` kill mid-download, `4 GiB` file cap, `64`-file scan cap.
