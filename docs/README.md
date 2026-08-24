# LAI Documentation — Index

**Directive:** *Documentation first, implementation later.* This `docs/` is the permanent product, architecture, engineering, UX, security, tooling, and roadmap reference — a future agent shall start from here without history.

## GGEN interoperability planning

* **GGEN_INTEGRATION_CONTRACT.md** — proposed external capability boundary for GGEN; defines provider role, capabilities, request/response semantics, privacy, authorization, streaming, artifacts, versioning and implementation gate.
* **GGEN_PROVIDER_CONFORMANCE.md** — conformance requirements and test matrix for a future LAI GGEN provider adapter.

These documents are planning artifacts only. They do not authorize implementation. The shared protocol, transport, schemas, discovery, authentication, artifact transfer and compatibility decisions must be accepted first.

## Existing documentation map

The existing canonical LAI documentation remains authoritative for all other areas.

### Core

* `MASTER_ROADMAP.md` — canonical roadmap and retained implementation backlog
* `DEVELOPMENT_STATE.md` — current phase/task/status and validation requirements
* `CURRENT_STATUS.md` — evidence-backed implementation state
* `ARCHITECTURE.md` — system graph, trust boundaries and data flow

### Product & Design

* `DESIGN_PHILOSOPHY.md`
* `DESIGN_SYSTEM.md`
* `UI_ARCHITECTURE.md`
* `CHAT_UI.md`
* `AGENT_UI.md`
* `TOOL_UX.md`

### AI & Tools

* `AI_RUNTIME.md`
* `AI_PROVIDERS.md`
* `TOOL_CATALOG.md`
* `AGENT.md`
* `PIPELINE.md`
* `MODELS.md`
* `DEVELOPER_AI.md`
* `LINUX.md`
* `FILES_AND_DOCUMENTS.md`
* `OCR_VISION.md`
* `AUDIO_SPEECH.md`

### Knowledge & Automation

* `MEMORY.md`
* `RAG.md`
* `KNOWLEDGE_GRAPH.md`
* `ANDROID.md`
* `AUTOMATION.md`

### System

* `SECURITY.md`
* `PERMISSIONS.md`
* `AUDIT.md`
* `PERFORMANCE.md`
* `BENCHMARKING.md`
* `TESTING.md`
* `LOGGING.md`
* `NETWORK.md`
* `CLOUD.md`

### Future

* `FUTURE.md`

### Operating rule

1. Read this index.
2. Read the relevant architecture document and current implementation state.
3. Read accepted ADRs and any Proposed ADR that constrains the task.
4. Determine the current phase from `DEVELOPMENT_STATE.md` and `implementation/current-state.md`.
5. Identify the next approved implementation task. Documentation of a future item is not authorization to implement it.
6. Check licensing and commercial constraints before adding dependencies, models, datasets, SDKs or commercial modules.
7. Only then modify code.

Required sequence: **documented plan → approved decision → implementation → test → documentation update → audit**.
