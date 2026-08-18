# Development State

**Snapshot:** 2026-08-18T17:45 UTC — `main` @ `0263d30` + `17ad75b` + `c4732fe` + `712ad6f` → `17ad75b` auto-import  
**Build:** `Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10 + OkHttp 5.4.0 + API 36` — Actions majors current, `validate_repo.sh` PASS (~1.25 MB <128 MB), boundaries/catalog/docs PASS.  
**Last green:** `push` `#140` (scaffold), `workflow_dispatch` `#137`/`#133` (release) — **#142 failed** on `SPIRV-Headers` (now fixed via revert + proper CI apt step in `0263d30`). Head `17ad75b` (workspace auto-import) is green; `0263d30` proper Vulkan `GGML_VULKAN=ON` is in-flight.

## Current Phase

**Documentation-first directive — Phase 0** (per `LAI — MASTER DOCUMENTATION & PRODUCT SPECIFICATION DIRECTIVE`).  
**No code changes** until this docs system is complete. This file is the primary continuity.

## Current Task

**Implementation preparation complete.** Next authorized *documentation* step inside milestone M1 is to refresh stale `implementation/current-state.md` / `architecture/module-map.md` to the 16-module tree. Next authorized *code* step is M1 only (SAF settings atomic write + contract tests). See [`implementation/IMPLEMENTATION_PREPARATION.md`](implementation/IMPLEMENTATION_PREPARATION.md). No other roadmap phase is authorized.

## Current Status (honest, evidence-based)

| Area | State | Evidence |
|---|---|---|
| **CPU inference** | **Device validated** (0.1.139) | SM8735 Redri Turbo 4 Pro: `Qwen 1.5B Q4_K_M` 570 ms load, **16–22 tok/s prefill** (93 tok in 5.8s, 11 reuse in 0.54s), `8–12 tok/s` decode, `storage/LAI/models` auto-import, adaptive `little 7 → big 0-3`, batch 32, prompt 180 chars, `20 tok/s` reuse validated |
| **GPU Vulkan** | **Adapter implemented; device qualification in progress** | Real `VulkanBackend::open()` with full layer offload via `llama_session` (build-verified CI #158+); device probe now accepts `GGML_BACKEND_DEVICE_TYPE_IGPU` (Adreno 825 is an integrated GPU — the previous GPU-only check skipped it, which is why `compiledBackends` showed only `llama-cpu`); scheduler enabled by default via `-Plai.validatedAccelerators=llama-vulkan` |
| **NPU QNN/HTP** | **Planned — no code** | Boundary documented only, licensed QAIRT required |
| **Model system** | **Verified** | Signed `catalog/models-v1.json` rev3, `ModelRepository` (SHA-256, resume, GGUF validate, `noBackupFilesDir/models`), `WorkspaceDiscovery` (SAF bounded, SHA streaming), `Keep copy` export, `storage/LAI/models` rule |
| **Tool system** | **One-shot, user-confirmed** | 15 tools, `AgentRuntime.parseToolProposal` (JSON schema, `ToolInstructionGate`), hash-chained `ToolAuditLedger`, `ToolsDashboard` (Vision/Interaction/Elevated) — no multi-step loop |
| **Agent** | **One-shot** | No planner/memory/verification yet |
| **Android automation** | **Device validated** | `AccessibilityAutomationService` (400 nodes, `canTakeScreenshot`), `Shizuku UID 2000` argv allowlist, Xiaomi lock guide (`Lock + No restrictions + Autostart`), `install -r` keeps grant |
| **Diagnostics/logging** | **Implemented** | Centralized `LaiLog` (logcat + app-private file + in-app export; debug `DEBUG` / signed-release `INFO`), `LaiLogRedactor` (token/api-key/password redaction), uncaught-crash handler, R8 `SourceFile/LineNumberTable` + `mapping.txt` artifact — see [LOGGING.md](LOGGING.md) |
| **Linux/terminal** | **Missing** | No PRoot/QEMU — spec only |
| **OCR** | **Scaffold** | `PlaceholderBanglaOcrEngine` fails `OcrModelRequiredException`, `OcrResult` JSON ready |
| **RAG/Memory** | **Missing** | No `core:rag`/`core:tokenization` — planned |
| **Cloud/Remote** | **Missing** | Local-only |

## Last Verified State

*   **Device:** Xiaomi `25053RT47C` SM8735 8 cores, `arm64-v8a`, `Android 36`, `4.0 GB` free, `NOMINAL`, `87–97%` battery, `accessibility true`, `shizuku READY_UID_2000` — `logcat -s LAI-llama` prefill tracing validated.
*   **Stall fixed:** `128/334 in 179 sec @0.7 tok/s` (run 28671, `AWAITING_FIRST_TOKEN` watchdog) → `f7a0db0` (min 2 threads, batch 64) → `08827dd` (batch 32, prompt 334→93, force 4 threads) → `c4732fe`/`16bf06b` (adaptive `7→0-3`) → `0.1.139 @20 tok/s`.
*   **Accessibility reset fixed:** HyperOS lock guide added `16bf06b`.
*   **Signed crash fixed:** `WorkDatabase_Impl` R8 keep in `a6f2ab0`.

## Next Task

**Device test mandatory (immediate, ~5 min on phone):**
1. Update to `#144` (`17ad75b` auto-import) or `#143` (`0263d30` proper Vulkan) when green → `install -r` → grant `storage/LAI` if needed → `LAI/models/*.gguf` auto-shows **Installed** without Import → **Load** → `hi` → `logcat -s LAI-llama | grep -E "core:.*big|prefill done"` → expect `big 0-3` + `~20 tok/s`.
2. If `vulkan: available true` after `0263d30`, wire `VulkanBackend::open n_gpu_layers=99` + `generate()` — device test for 93 tok on Adreno 825.

**Next code after M1 (not authorized yet):** Vulkan `generate()` device qualification is milestone M2. QNN, tokenization, and RAG remain later phases and require license intake. Do not start them because they appear in older “next” lists.

## Blockers

*   **Printed Bangla OCR:** Owner dataset/licence decision (`docs/BANGLA_OCR.md`).
*   **Vulkan real:** Needs `SPIRV-Headers` on CI (now added in `0263d30`) + Adreno 825 qualification.
*   **QNN:** Licensed QAIRT SDK (`secrets.QAIRT_URL`), not in repo.
*   **Dependabot #4:** `androidx 2026.08.00/1.19.0` needs `API 37` not on runners — deferred, stays `2025.05.01/1.16.0`.

## Known Issues

*   Vulkan `0.7 tok/s` before `c4732fe` core swap (4-7 were little) — now `0-3` big, but real Vulkan still pending.
*   `410` on `storage` revert: `install` without `-r` wipes workspace grant — user rule is `install -r`.

## Files to Inspect

`PROJECT_STATE.md`, `docs/MASTER_ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/NPUHUB_VS_LAI_DIFF.md`, `runtime/llama/src/main/cpp/llama_cpu_backend.cpp` (adaptive cores, batch 32), `platform/workspace/*`, `app/src/main/java/dev/lai/runtime/ui/MainViewModel.kt` (autoImport), `app/proguard-rules.pro` (WorkManager keep), `model/README.md` (storage rule).

## Modules to Inspect

`app` (composition + `ToolsDashboard` + Xiaomi guide), `core:contracts/policy/scheduler/model`, `platform:download/audit/device/history/accessibility/workspace/shizuku`, `runtime:llama/ocr/orchestrator`, `plugins:api`.

## Required Tests

*   **CI (no device):** `scripts/validate_repo.sh` (size/boundaries/docs/catalog), `coverageCheck` (176 tests, ratchets), `lintDebug`.
*   **Device:** `count_tokens` 334→93, `generate` prefill 93/11 reuse, `thermal: decode threads`, `core: pinned`, `vulkan: loader`, `storage/LAI/models` auto-import, `install -r` grant persistence, Xiaomi lock.

## Device Test Requirements

SM8735 Redmi Turbo 4 Pro, `arm64-v8a`, `API 36`, `4 GB` free, `charging` or `Performance mode`, Shizuku `UID 2000`, Accessibility ON (locked).

## Documentation to Update (this directive)

`MASTER_ROADMAP.md` ✅, `DEVELOPMENT_STATE.md` (this), `ARCHITECTURE.md`, `DESIGN_PHILOSOPHY.md`, `DESIGN_SYSTEM.md`, `TOOL_CATALOG.md`, `AGENT.md`, `PIPELINE.md`, etc. — see `docs/` audit below. **No code until docs are reviewed.**

## Audit Checklist (from §38)

*See `docs/MASTER_ROADMAP.md:20` gates — device test mandatory for Vulkan, QNN, OCR quality, tool harness, thermal, RAG citations. This file satisfies `DEVELOPMENT_STATE.md`.*
