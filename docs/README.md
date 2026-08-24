# LAI Documentation — Index

**Directive:** *Documentation first, implementation later.* This `docs/` is the **permanent product, architecture, engineering, UX, security, tooling, and roadmap reference** — a future agent shall start from here without history.

**Source of truth:** Current repository, `PROJECT_STATE.md`, and physical-device evidence. Architecture and accepted ADRs constrain implementation.

## Core

* **MASTER_ROADMAP.md** — phases `CPU → GPU → NPU → Unified → Model → AI → Tool → Agent → Developer → Linux → Files → OCR → Memory → RAG → Automation → Hybrid → Security → Multimodal → Graph → Hardening`
* **DEVELOPMENT_STATE.md** — current task/status, last verified state, next task, blockers and device requirements
* **CURRENT_STATUS.md** — honest `Implemented / Scaffold / Planned` state with evidence
* **ARCHITECTURE.md** — module graph, trust boundaries and JNI/native boundary
* **PROJECT_STATE.md** — detailed implementation/build/device snapshot

## GGEN integration

* **architecture/GGEN_INTEGRATION_BOUNDARY.md** — ownership and cross-repository boundary
* **architecture/GGEN_CAPABILITY_CONTRACT.md** — LAI-side semantic mirror of the GGEN capability protocol
* **architecture/GGEN_TOOL_CAPABILITY_MAP.md** — capability-to-runtime/tool mapping and first-adapter sequence

GGEN remains an independent user-facing Creative & Document Studio. LAI is an optional provider/runtime. No GGEN source dependency on LAI internals is permitted.

## Product & Design

* **DESIGN_PHILOSOPHY.md** — offline/privacy/provider/backend-agnostic, fail-closed, modular
* **DESIGN_SYSTEM.md** — Material3 and accessibility rules
* **UI_ARCHITECTURE.md** — Chat/Screen Reader/Automator + Settings
* **AGENT_UI.md** — `TASK → PLAN → STEP → VERIFY → RESULT`
* **TOOL_UX.md** — risk-aware tool presentation and confirmation

## AI & Tools

* **AI_RUNTIME.md** — inference engine, backend sessions and routing constraints
* **AI_PROVIDERS.md** — provider-neutral local/cloud/remote strategy
* **TOOL_CATALOG.md** — typed tool inventory, risk, permissions, audit and status
* **AGENT.md** — intent through execution and verification
* **PIPELINE.md** — validated DAG execution
* **MODELS.md** — signed model catalog and lifecycle
* **DEVELOPER_AI.md** — developer-workstation capability roadmap
* **LINUX.md** — PRoot/QEMU development runtime
* **FILES_AND_DOCUMENTS.md** — bounded document ingestion and retrieval
* **OCR_VISION.md** — OCR/vision boundary

## Knowledge & Automation

* **MEMORY.md**, **RAG.md**, **KNOWLEDGE_GRAPH.md** — bounded memory, retrieval and future graph
* **ANDROID.md**, **AUTOMATION_TOOLS.md** — Android authority and tool contracts

## System

* **SECURITY.md** — fail-closed policy, evidence vocabulary and audit
* **PERMISSIONS.md** — authority levels and consent
* **AUDIT.md** — content-free hash-chained audit records
* **PERFORMANCE.md** / **BENCHMARKING.md** — measured runtime performance and evidence rules
* **TESTING.md** — static/unit/integration/CI/device/GPU/NPU/security/regression strategy
* **LOGGING.md** / **DIAGNOSTICS_EXPORT.md** — redacted diagnostics
* **NETWORK.md** / **CLOUD.md** — network ownership and cloud/remote semantics

## Legal & governance

See `legal/`, `governance/`, `decisions/`, and the project license/model/vendor policies before introducing dependencies, models, datasets or commercial modules.

## Operating rule for future agents

1. Read this index.
2. Read the relevant architecture document and current implementation state.
3. Read accepted ADRs and any Proposed ADR that constrains the task.
4. Determine the current phase/task.
5. Identify the next approved implementation task. Documentation is not implementation authorization.
6. Check licensing and commercial constraints before adding dependencies, models, datasets, SDKs or commercial modules.
7. Only then modify code.

Required sequence: **documented plan → approved decision → implementation → test → documentation update → audit**.

**Writing style:** `LAI provides/shall/is`. No conversational author voice. No feature implementation merely because it appears in roadmap documentation.
