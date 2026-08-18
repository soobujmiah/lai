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
*   **LOGGING.md** — centralized `LaiLog` (logcat + file + export), debug `DEBUG` vs signed-release `INFO`, redaction contract, R8 `mapping.txt` use
*   **NETWORK.md** — only `platform:download` has `INTERNET` (signed catalog + reviewed GGUF), allowlist, `WorkManager` `Range`
*   **CLOUD.md** — `Local` vs `Cloud` vs `Remote` badges, `cost` + `privacy` + `quota` controls

## Future

*   **FUTURE.md** — additive `Vulkan → QNN → tokenization → RAG → agent → workstation → OCR → graph → hardening`

**Writing style:** `LAI provides/shall/is` — not `I recommend/You should`. No ownership mention, no AI author, no conversational commentary. **No unnecessary code changes, no feature implementation in this phase.**

---

## Product, architecture, legal, and governance navigation

This section is the entry map for a future engineering agent. Existing topical documents above remain the product and implementation reference. Legal and commercial documents are an added layer; they do not replace [`ROADMAP.md`](ROADMAP.md), [`product/feature-matrix.md`](product/feature-matrix.md), or [`LEGAL_AND_LICENSING.md`](LEGAL_AND_LICENSING.md).

### PRODUCT

*   **Vision / principles** — [`DESIGN_PHILOSOPHY.md`](DESIGN_PHILOSOPHY.md)
*   **Roadmap** — [`ROADMAP.md`](ROADMAP.md) (canonical Phase 0–14); pointer [`product/roadmap.md`](product/roadmap.md)
*   **Feature matrix** — [`product/feature-matrix.md`](product/feature-matrix.md)
*   **Commercial feature matrix** — [`product/COMMERCIAL_FEATURE_MATRIX.md`](product/COMMERCIAL_FEATURE_MATRIX.md)
*   **Product structure options** — [`product/COMMERCIAL_PRODUCT_STRUCTURE.md`](product/COMMERCIAL_PRODUCT_STRUCTURE.md)
*   **Product boundaries** — [`legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md`](legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md), [`PRIVACY_INVARIANTS.md`](PRIVACY_INVARIANTS.md)
*   **Current implementation state** — [`implementation/current-state.md`](implementation/current-state.md), [`STATUS.md`](STATUS.md), [`PROJECT_STATE.md`](../PROJECT_STATE.md)
*   **Implementation preparation / next milestone** — [`implementation/IMPLEMENTATION_PREPARATION.md`](implementation/IMPLEMENTATION_PREPARATION.md)

### ARCHITECTURE

*   **System** — [`architecture/overview.md`](architecture/overview.md), [`architecture/system-architecture.md`](architecture/system-architecture.md), [`ARCHITECTURE.md`](ARCHITECTURE.md)
*   **Modules** — [`architecture/module-map.md`](architecture/module-map.md), [`MODULES.md`](MODULES.md)
*   **Agent** — [`architecture/agent-architecture.md`](architecture/agent-architecture.md), [`AGENT.md`](AGENT.md)
*   **Tool** — [`TOOL_CATALOG.md`](TOOL_CATALOG.md), [`AUTOMATION_TOOLS.md`](AUTOMATION_TOOLS.md)
*   **Backend** — [`architecture/ai-architecture.md`](architecture/ai-architecture.md), [`VENDOR_BACKEND_STRATEGY.md`](VENDOR_BACKEND_STRATEGY.md), [`AI_RUNTIME.md`](AI_RUNTIME.md)
*   **RAG / OCR / knowledge** — [`RAG.md`](RAG.md), [`OCR_VISION.md`](OCR_VISION.md), [`KNOWLEDGE_GRAPH.md`](KNOWLEDGE_GRAPH.md)
*   **UI/UX** — [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md), [`UI_ARCHITECTURE.md`](UI_ARCHITECTURE.md)
*   **Hybrid / providers** — [`architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md`](architecture/HYBRID_AND_PROVIDER_ARCHITECTURE.md), [`AI_PROVIDERS.md`](AI_PROVIDERS.md), [`CLOUD.md`](CLOUD.md)
*   **Commercial module boundaries** — [`architecture/COMMERCIAL_MODULE_BOUNDARIES.md`](architecture/COMMERCIAL_MODULE_BOUNDARIES.md)
*   **Entitlement architecture** — [`architecture/ENTITLEMENT_ARCHITECTURE.md`](architecture/ENTITLEMENT_ARCHITECTURE.md)
*   **Plugins** — [`architecture/plugin-architecture.md`](architecture/plugin-architecture.md)

### LEGAL

*   **Index** — [`legal/README.md`](legal/README.md)
*   **Commercial IP policy** — [`legal/COMMERCIAL_IP_POLICY.md`](legal/COMMERCIAL_IP_POLICY.md)
*   **Ownership decisions** — [`legal/OWNERSHIP_DECISIONS.md`](legal/OWNERSHIP_DECISIONS.md)
*   **Licensing audit** — [`LEGAL_AND_LICENSING.md`](LEGAL_AND_LICENSING.md)
*   **Licensing strategy** — [`legal/LICENSING_STRATEGY.md`](legal/LICENSING_STRATEGY.md)
*   **Ownership model** — [`legal/OWNERSHIP_MODEL.md`](legal/OWNERSHIP_MODEL.md)
*   **Contributor rights** — [`legal/CONTRIBUTOR_RIGHTS.md`](legal/CONTRIBUTOR_RIGHTS.md)
*   **AI code provenance** — [`legal/AI_CODE_PROVENANCE.md`](legal/AI_CODE_PROVENANCE.md)
*   **Third-party intake / inventory** — [`legal/THIRD_PARTY_INTAKE.md`](legal/THIRD_PARTY_INTAKE.md), [`legal/THIRD_PARTY_COMPLIANCE.md`](legal/THIRD_PARTY_COMPLIANCE.md)
*   **Model IP** — [`legal/MODEL_IP_POLICY.md`](legal/MODEL_IP_POLICY.md), [`../MODEL_LICENSES.md`](../MODEL_LICENSES.md)
*   **Vendor licenses** — [`legal/VENDOR_LICENSE_POLICY.md`](legal/VENDOR_LICENSE_POLICY.md)
*   **Proprietary boundaries** — [`legal/PROPRIETARY_BOUNDARIES.md`](legal/PROPRIETARY_BOUNDARIES.md)
*   **Trademark** — [`legal/TRADEMARK_POLICY.md`](legal/TRADEMARK_POLICY.md)
*   **SBOM and provenance** — [`legal/SBOM_AND_PROVENANCE.md`](legal/SBOM_AND_PROVENANCE.md)
*   **Release compliance** — [`legal/RELEASE_COMPLIANCE.md`](legal/RELEASE_COMPLIANCE.md)
*   **Project license text** — [`../LICENSE`](../LICENSE) (Apache License 2.0; unchanged)

### GOVERNANCE

*   **Public development policy** — [`governance/PUBLIC_DEVELOPMENT_POLICY.md`](governance/PUBLIC_DEVELOPMENT_POLICY.md)
*   **Contribution policy** — [`../CONTRIBUTING.md`](../CONTRIBUTING.md), [`development/development-policy.md`](development/development-policy.md)
*   **Security policy** — [`../SECURITY.md`](../SECURITY.md), [`SECURITY_AND_SAFETY.md`](SECURITY_AND_SAFETY.md), [`security/COMMERCIAL_SECRET_POLICY.md`](security/COMMERCIAL_SECRET_POLICY.md)
*   **Decision records** — [`decisions/README.md`](decisions/README.md); commercial/legal series ADR-0100–ADR-0109; accepted engineering ADRs remain in [`adr/`](adr/)

### Operating rule for future agents

1. Read this index.
2. Read the relevant architecture document and current implementation state.
3. Read accepted ADRs and any Proposed ADR that constrains the task.
4. Determine the current phase from [`DEVELOPMENT_STATE.md`](DEVELOPMENT_STATE.md) and [`implementation/current-state.md`](implementation/current-state.md).
5. Identify the next approved implementation task. Documentation of a future item is not authorization to implement it.
6. Check licensing and commercial constraints in `docs/legal/` before adding dependencies, models, datasets, SDKs, or commercial modules.
7. Only then modify code.

Required sequence for implementation: documented plan → approved decision → implementation → test → documentation update → audit.
