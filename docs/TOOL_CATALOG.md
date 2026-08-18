# Tool Catalog

LAI shall expose tools as **modular, typed, policy-gated** operations — never raw shell. Every tool declares `id`, `purpose`, `input/output schema`, `permission`, `risk`, `destructive`, `network`, `local/cloud`, `cancellation`, `timeout`, `audit`, `verification`, `fallback`, `dependencies`. Status is `AVAILABLE / PLANNED / EXPERIMENTAL / FUTURE / PLATFORM-LIMITED / DEVICE-DEPENDENT`. No fake claims.

## Implemented — Build-verified or Device-validated

| Tool | Purpose | Risk | Status | Notes |
|---|---|---|---|---|
| `screen.snapshot` | Read accessibility tree | `READ_ONLY` | **Device validated** | ≤400 nodes, depth 24, password omitted, `flagIncludeNotImportantViews` |
| `screen.click` | Click by `viewId/text/contentDescription/path` | `INTERACTION` | Build verified | Requires `ACCESSIBILITY_REQUIRED`, `CONFIRMATION_REQUIRED` if `requiresConfirmation` |
| `screen.type` | Enter text into field | `SENSITIVE` | Build verified | `isPassword` check, `allowSensitiveInput` false by default |
| `screen.scroll` | Scroll container/forward/back | `INTERACTION` | Build verified | Auto-finds scrollable |
| `system.global_action` | `back/home/recents/notifications` | `INTERACTION` | Build verified | `performGlobalAction` |
| `app.launch` | Launch by package | `INTERACTION` | Build verified | Regex `^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$` |
| `ocr.current_screen` | Capture + local OCR | `READ_ONLY` | Scaffold | `Placeholder` fails `OcrModelRequiredException` |
| `shell.operation` | One allowlisted Shizuku op | `ELEVATED` | Device validated `UID 2000` | `argv` allowlist (`settings.get`, `device.info`, …), no raw shell, bounds |
| `device.info` | `SOC/manufacturer/model/SDK/cores/memory/battery/thermal` | `READ_ONLY` | Device validated | Via `AndroidRuntimeEnvironmentProvider` |
| `settings.get/put` | Typed per-tool settings | `READ_ONLY`/`SENSITIVE` | Build verified | `SettingsPolicy` NaN/unknown-field safe, `settings.json` non-secret |
| `package.list_user/install_existing/force_stop` | Packages | `INTERACTION` | Build verified | Via `shell.operation` allowlist |
| `input.keyevent` | Key events | `INTERACTION` | Build verified | Via `shell.operation` |

Invariants: **one proposal per response**, validated twice, **approval-before-authority**, replay blocked, model text can never self-approve, `ToolInstructionGate` recall-biased (`hi` = zero tool tokens).

## Researched — PLANNED / FUTURE (representative, not exhaustive)

**AI/Inference:** `model.management` (list/load/unload/delete/keep copy), `tokenizer`, `inference` (generate/countTokens), `embedding` (Granite 107M `384-dim` via `backend:rag-litert`), `reranking`, `vision` (image Q/A), `audio`/`speech` (Whisper STT streaming, parallel TTS + barge-in VAD).

**Files:** `file.search/copy/move/rename/delete/archive/compress/extract/metadata/hash/duplicate/convert` — all over SAF `storage/LAI/` (no `MANAGE_EXTERNAL_STORAGE`), bounded traversal.

**Documents:** `pdf/docx/xlsx/pptx/md/txt/csv/json/xml/yaml/html` — `read/search/extract/summarize/compare/transform/generate/cite` via `core:rag` + `core:tokenization`.

**Developer:** `repo.inspect/code.search/generate/edit/refactor/debug/test.generate/build/logs/staticAnalysis/dependencyAnalysis` — `Inspect→Understand→Plan→Modify→Build→Test→Verify→Document` over `storage/LAI/` projects.

**Git/GitHub:** `clone/branch/status/diff/commit/push/pull/issue/pr/release/workflow/repoAnalysis` — requires explicit remote, never auto-push.

**Linux/Terminal:** `shell/process/package/compile/script/env/network/diagnostics/monitor` — PRoot/QEMU `linux` runtime, no root assumed, capability detection + fallback.

**Android (see above):** `notification.interaction`, `system.settings` where permitted.

**Automation:** `task.schedule/chain/conditional/retry/cancel/trigger/workflow`, `pipeline` DAG (`core:pipeline`).

**Research/Web:** `search/page.retrieve/extract/citation/comparison/summarize` — explicit `HYBRID` cloud opt-in, no silent upload.

**Knowledge:** `memory/document.indexing/rag/graph` — `MEMORY.md` + `RAG.md` + `KNOWLEDGE_GRAPH.md`.

**System:** `cpu/ram/storage/thermal/battery/network/backend/model` — `AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN`.

All future tools shall be `PLANNED` until `AVAILABLE` on SM8735, with per-tool tests, `ToolAuditLedger` entry, and graceful fallback.
