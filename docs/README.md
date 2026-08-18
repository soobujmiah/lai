# LAI Documentation — Index

**Directive:** *Documentation first, implementation later.* This `docs/` is the **permanent product, architecture, engineering, UX, security, tooling, and roadmap reference** — a future agent shall start from here without history.

**Source of truth:** Current repository (`main` @ `17ad75b` + `c4732fe` + `0263d30`), `PROJECT_STATE.md`, and SM8735 device evidence (0.1.139 `16–22 tok/s`).

## Core

*   **MASTER_ROADMAP.md** — `0–20` phases `CPU → GPU → NPU → Unified → Model → AI → Tool → Agent → Developer → Linux → Files → OCR → Memory → RAG → Automation → Hybrid → Security → Multimodal → Graph → Hardening`
*   **DEVELOPMENT_STATE.md** — `CURRENT PHASE/TASK/STATUS`, `LAST VERIFIED STATE`, `NEXT TASK`, `BLOCKERS`, `FILES/MODULES TO INSPECT`, `REQUIRED TESTS`, `DEVICE TEST REQUIREMENTS`
*   **CURRENT_STATUS.md** — honest `Implemented / Scaffold / Planned` with evidence states
*   **ARCHITECTURE.md** — `16`-module graph, trust boundaries, data flow, native JNI/C++ boundary (`batch 32`, `little 7 → big 0-3`)
*   **NPUHUB_VS_LAI_DIFF.md** — private `NpuHub` vs `lai` gap (`9` subsystems to port vs `5` LAI moats)

## Product & Design

*   **DESIGN_PHILOSOPHY.md** — offline/privacy/provider/backend-agnostic, fail-closed, modular, `One system` feel
*   **DESIGN_SYSTEM.md** — `Material3`, `18 dp` bubbles, `14–16 dp` gaps, `light/dark/system`, `48 dp` targets
*   **UI_ARCHITECTURE.md** — `Chat/Screen Reader/Automator` + `Settings`, `progressive disclosure`, `command palette`, not 13 tabs
*   **CHAT_UI.md** — streaming `id`-keyed, `ToolConfirmationDialog`, `Thinking locally…`, `Stop` `45s`
*   **AGENT_UI.md** — `TASK → PLAN → STEP → VERIFY → RESULT`, per-step `Approve/Deny/Pause/Cancel/Stop`
*   **TOOL_UX.md** — `ToolsDashboard` cards, `risk` badge, one-shot confirmation

## AI & Tools

*   **AI_RUNTIME.md** — `InferenceEngine` + `Backend` + `BackendSession`, hybrid routing `capability/privacy/latency/cost/battery/thermal`
*   **AI_PROVIDERS.md** — `CPU/GPU/NPU` + `OpenAI/Anthropic/Gemini/OpenRouter` + `Ollama/LAN` provider-agnostic
*   **TOOL_CATALOG.md** — `15` implemented + `~45` researched tools (`AVAILABLE/PLANNED/FUTURE`), schemas, risk, audit
*   **AGENT.md** — `Intent→Context→Plan→Memory→Tool→Permission→Execution→Observation→Verification`
*   **PIPELINE.md** — `core:pipeline` DAG validation, parallel/conditional/retry/timeout
*   **MODELS.md** — signed `models-v1.json` rev3, `ModelRepository` SHA-256, `storage/LAI/models` auto-import, `Keep copy`, `4 GiB` cap
*   **DEVELOPER_AI.md** — `Inspect→Understand→Plan→Modify→Build→Test→Verify→Document` over `storage/LAI/`
*   **LINUX.md** — `PRoot/QEMU` `shell/filesystem/process/package/compiler/build` — no root, capability detection
*   **FILES_AND_DOCUMENTS.md** — `PDF/DOCX/XLSX/...` bounded ingestion, `BM25` → `384-dim` dense, citations
*   **OCR_VISION.md** — `Placeholder` today, Tesseract `5.5.3` `ben` provenance `db0ec62f` + `ben.traineddata` `311630...` tomorrow, `PngRasterDecoder`
*   **AUDIO_SPEECH.md** — `Whisper large-v3 Q4` STT + parallel `TTS + barge-in VAD` future

## Knowledge & Automation

*   **MEMORY.md** — `conversation` (`keepLastTurns`) + `task` + `project` + `preferences`, bounded, auditable
*   **RAG.md** — `core/tokenization` SentencePiece unigram + `Granite 107M 384-dim` LiteRT embedder + `features:rag` dense index
*   **KNOWLEDGE_GRAPH.md** — `documents/concepts/code/functions` → `references/related-to` + `zoom/pan` (future, modular)
*   **ANDROID.md** — `Accessibility` 400 nodes + `Shizuku UID 2000` argv allowlist + Xiaomi `Lock + No restrictions + Autostart`, `install -r`
*   **AUTOMATION.md** — (in `AGENT.md` + `PIPELINE.md` — scheduled/conditional/retries/cancellation)
*   **LINUX.md** — see above

## System

*   **SECURITY.md** — fail-closed, `AVAILABLE/SUPPORTED/ACTIVE/MEASURED`, `ToolAuditLedger` hash chain, `validate_repo.sh` 128 MB
*   **PERMISSIONS.md** — `OBSERVE/SUGGEST/PREPARE/EXECUTE/ADVANCED`, one-run grants, `READ_ONLY…CRITICAL`
*   **AUDIT.md** — `tool/timestamp/authorization/digest/result/verification`, content-free, `install -r` keeps
*   **PERFORMANCE.md** — SM8735 `16–22 tok/s` CPU, `40–60 tok/s` Vulkan next, `QNN` later, governor `little 7 → big 0-3`
*   **BENCHMARKING.md** — `MEASURED` only with value, `JSON/Markdown/CSV` export, `sbom-*.txt` → `CycloneDX`
*   **TESTING.md** — `static/unit/integration/CI/Android/UI/device/GPU/NPU/performance/thermal/security/regression`, `DEVICE TEST REQUIRED` never fabricated
*   **NETWORK.md** — only `platform:download` has `INTERNET` (signed catalog + reviewed GGUF), allowlist, `WorkManager` `Range`
*   **CLOUD.md** — `Local` vs `Cloud` vs `Remote` badges, `cost` + `privacy` + `quota` controls

## Future

*   **FUTURE.md** — additive `Vulkan → QNN → tokenization → RAG → agent → workstation → OCR → graph → hardening`

**Writing style:** `LAI provides/shall/is` — not `I recommend/You should`. No ownership mention, no AI author, no conversational commentary. **No unnecessary code changes, no feature implementation in this phase.**
