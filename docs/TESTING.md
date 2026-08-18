# Testing

LAI shall be **evidence-based** — never invent results from tests that could not run.

## Strategy (per `MASTER_ROADMAP` gates)

| Level | What | Where | When |
|---|---|---|---|
| **Static analysis** | `ktlint` + `detekt` (as NpuHub `config/detekt`), `check_architecture_boundaries.py`, `check_docs_links.py`, `validate_repo.sh` (128 MB, no `*.apk/*.so/*.gguf/*.jks`, token scan) | AI Arena (`2–4` cores) | Every `push` (`source-policy` job) |
| **Unit test** | `169` JVM tests (`core:contracts/policy/scheduler/model`, `plugins:api`, `platform:history/workspace/download/audit`, `app` `WorkspaceSettingsCoordinatorTest`) + `ThermalGovernorPolicyTest` (9 tests, hysteresis) + `WorkspacePolicy` classification + `ModelFormatDetector` `GGUF` magic | AI Arena | `coverageCheck` + `JaCoCo` ratchets (`.15/.55/.70/.50/.50`) |
| **Integration test** | `ModelRepository` (SHA-256, resume `Range`, `part` staging, `verifyAndActivate`), `WorkspaceDiscovery` (depth 4, 256 cap, 8 GB, SHA streaming), `ToolAuditRepository` (hash chain, replay guard), `ChatHistoryRepository` (atomic) | AI Arena (JVM) | `testDebugUnitTest` |
| **CI test** | `coverageCheck` + `app:lintDebug` + `assembleDebug` (native `liblai_runtime.so` arm64) | GitHub Actions (`JDK 17 + API 36 + NDK 27 + CMake 3.22.1`, `gradle/actions` current) | `build` job (`LAI_ENABLE_LLAMA_CPP=ON`, `LAI_LLAMA_CPP_DIR` pinned `ad1de39`) |
| **Android build test** | `assembleDebug`/`assembleRelease` (V1–V4 `lai-release` RSA-4096) | Actions (remote toolchain) | Every `push`/`tag` |
| **UI test** | `Compose` `Chat/Screen Reader/Automator` ( `imeAnimationTarget` vs `imeVisible` as in `16bf06b`), `ToolsDashboard`, Xiaomi lock guide | AI Arena (robo) + device | Manual |
| **Device test** | **SM8735 Redmi Turbo 4 Pro** (`arm64-v8a`, 8 cores, Adreno 825) — `count_tokens` 334→93, `generate` prefill `93@16 tok/s`, `11@20 tok/s` reuse, `thermal: decode threads`, `core: pinned to big 0-3`, `vulkan: loader`, `storage/LAI/models` auto-import, `install -r` grant, Xiaomi lock, `Shizuku UID 2000` | **Physical device** | `DEVICE TEST REQUIRED` for `MEASURED` claim |
| **GPU test** | Vulkan `40–60 tok/s` prefill on Adreno 825, `n_gpu_layers=99`, CPU fallback | Device (Adreno 825) | After `0263d30` (`SPIRV-Headers`) + `generate()` wire |
| **NPU test** | QNN `GGUF→DLC` INT4, `HTP` context, `QAIRT` licensed CI | Device + licensed CI | Future |
| **Performance** | `GenerationMetrics` (`promptEvaluationMs`, `timeToFirstTokenMs`, `decodeMs`, `totalMs`, `promptTokensPerSecond`, `decodeTokensPerSecond`) — `benchmark` `JSON/Markdown/CSV` (like NpuHub) | Device | `MEASURED` only with value |
| **Thermal/battery** | `PowerManager` callback → `ThermalGovernorPolicy` hysteresis, `pin_to_little` idle, `battery 39%→97%`, `NOMINAL→MODERATE` flapping, `Reduced CPU threads…` notice | Device | Warm-load |
| **Security** | `ToolAuditLedger` full-chain verify, `argv` allowlist injection (`package.force_stop` `;id`), `core` import `Android` fail, `validate_model_catalog.py` `models-v1.json` rev3 | CI + device | Every push |
| **Regression** | `never trySend` for tokens, `never imePadding()` twice, `never gate bar on current inset`, `Stop` watchdog `45s` must not unload, `kv_tokens_` only after successful `llama_decode`, thread change only between decodes, `CRITICAL` never `<2` on SM8735 | CI | Every push |

**AI Arena constraints:** `~128 MB`, `2–4` cores, not final device — use for `inspect/docs/code/static`, not for `download 1.1 GB`, `install SDKs`, or `large builds`. Real-device/NPU/Vulkan/thermal tests are explicitly `DEVICE TEST REQUIRED` and must not be fabricated.

**Reports:** `docs/device-results/` (`run126` stall, `run139` `16 tok/s` validated) + `DiagnosticsReportV1` + `sbom-*.txt` (lightweight) → `CycloneDX` future.
