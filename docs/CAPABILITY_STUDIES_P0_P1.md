# LAI Capability Studies — P0/P1

**Status:** Architecture baseline / implementation guidance
**Date:** 2026-08-24
**Scope:** LAI runtime, AI Gateway, providers, inference backends, agent/tool execution, memory/RAG, Android automation, security/audit, developer workstation, and observability.

> This document is a planning authority. It describes what LAI should own, what it should expose as contracts, and what must be proven before a capability is considered production-ready. It does not authorize implementation by itself.

## 1. Product boundary

LAI is the **AI intelligence, inference, agent, automation, and execution platform**. It is not GGEN's creative/document editor and must not absorb GGEN's artifact model, canvas state, document history, or UI-specific editing semantics.

LAI owns:

- model/provider discovery and selection
- local CPU/GPU/NPU inference
- cloud and remote AI providers
- routing, fallback, quotas, cost and privacy policy
- multimodal inference services
- agent runtime and tool execution
- Android Accessibility/Shizuku automation
- RAG and memory services
- device/resource/thermal-aware scheduling
- developer AI and development-workstation execution
- security, permissions, audit and recovery
- observability and benchmark evidence

GGEN consumes LAI through capability contracts. GGEN must remain usable without LAI, without AI, and without network access.

## 2. Evidence levels

Every capability must distinguish:

- **DESIGNED** — contract/architecture documented.
- **IMPLEMENTED** — code exists and tests cover the behavior.
- **AVAILABLE** — runtime can discover the capability on the current device.
- **SUPPORTED** — backend/model/provider is intentionally supported.
- **ACTIVE** — selected for an actual request.
- **MEASURED** — performance or behavior was measured on a named device/configuration.
- **PRODUCTION** — security, failure, persistence, cancellation and regression requirements are satisfied.

Never report a higher state from a lower state. In particular, `available != measured` and `compiled != usable`.

## 3. P0 capabilities

### P0.1 AI Gateway / Capability Router

**Purpose:** one policy-controlled front door for local, cloud and remote AI.

Required responsibilities:

- capability registry
- provider/backend discovery
- normalized request/response envelopes
- capability matching
- routing policy
- retry and bounded fallback
- cancellation/deadline propagation
- privacy and permission checks
- usage/cost accounting
- audit events
- health and telemetry

Recommended pipeline:

`request → auth/permission → capability match → policy/context → candidate ranking → execution → bounded retry/fallback → normalized response → usage/audit`

Provider adapters must be isolated from routing policy. Current and external gateway research consistently favors explicit provider adapters, capability-aware routing, bounded retries/fallback, and a single normalized internal request model rather than provider-specific branching throughout the application. citeturn0search0turn0search8turn0search9

Acceptance criteria:

- adding a provider does not modify GGEN-facing contracts
- local-first privacy policy can veto cloud/remote routing
- a failed provider cannot cause an unbounded retry loop
- cancellation reaches the active backend
- fallback is truthful and observable
- no prompt/content is placed in privacy-filtered diagnostics

### P0.2 Provider abstraction

Provider classes:

1. `LocalBackendProvider` — CPU/GPU/NPU.
2. `CloudProvider` — OpenAI, Anthropic, Gemini, OpenRouter and future vendors.
3. `RemoteProvider` — Ollama, LAN server, desktop/server endpoint.
4. `CustomOpenAICompatibleProvider` — explicit user-configured endpoint.

Provider contract should expose capability metadata, health, auth requirements, model catalog, invocation, cancellation and usage metadata. Secrets remain outside normal registry/config exports.

Required provider metadata:

- provider ID/version
- endpoint class
- supported capabilities
- supported modalities
- model IDs
- context/output limits
- streaming support
- tool-calling support
- embedding support
- image generation/edit support where applicable
- cost/quota metadata when known
- privacy/data-retention declaration
- health state

### P0.3 Local inference runtime

Target backend ladder:

`CPU/llama.cpp → Vulkan/Adreno → QNN/HTP`

Current LAI documentation establishes CPU as the truthful fallback, Vulkan as the next measured target, and QNN/HTP as a later licensed/device-specific target. fileciteturn156file0L2-L2

Contract boundaries:

- `InferenceEngine`
- `Backend`
- `BackendSession`
- `GenerationConfig`
- `RuntimeCapabilities`
- `BackendDescriptor`
- `ModelSpec`

Requirements:

- streaming token events
- token counting
- bounded context/output configuration
- thread/decode controls
- deterministic seed where supported
- cancellation
- resource admission before load
- unload/close lifecycle
- truthful metrics
- CPU fallback on accelerator open failure

### P0.4 Model registry/catalog

Separate **model metadata** from **model bytes**.

Registry responsibilities:

- stable model ID/version
- format and quantization
- supported backend IDs
- preferred/fallback backend IDs
- memory estimates
- context limits
- tokenizer/chat-template metadata
- integrity hash and size
- source and license metadata
- installation state

Downloads must be verified before activation. Existing LAI catalog documentation already requires allowlisted sources, SHA-256 and exact-size verification. fileciteturn157file0L2-L2

### P0.5 Agent runtime

Agent runtime must be a controlled orchestration layer, not an unrestricted model loop.

Required:

- session lifecycle
- intent/task state
- capability/tool discovery
- permission checks
- explicit step boundaries
- cancellation
- step/time/tool budgets
- result provenance
- failure classification
- durable audit for consequential operations

Phase 1 should remain **one-shot / bounded action execution**. Autonomous multi-step loops require a separate security design.

### P0.6 Tool execution / Android automation

Tool invocation must use a strict typed envelope and fail closed. Current LAI already defines an envelope with call correlation, bounded output, schema validation, trusted confirmation, hash-chained approval audit, and explicit tools such as screen snapshot/click/type/scroll, global actions, app launch, OCR and restricted Shizuku operations. fileciteturn158file0L2-L2

Production requirements:

- schema validation before authority dispatch
- trusted UI confirmation for model-authored consequential operations
- no model-controlled `confirmed=true`
- no raw shell command surface
- explicit allowlists
- sensitive-input separation
- prompt-injection defense
- durable approval before execution
- replay prevention
- bounded tool output
- cancellation and timeout

### P0.7 Privacy / security / audit

Security is cross-cutting and must not be an optional plugin.

Required controls:

- Android Keystore-backed secrets
- per-request privacy mode
- local-only policy
- explicit LAN opt-in
- authentication for remote endpoints
- permission gate before tool execution
- content-free diagnostics by default
- hash-chained audit for consequential actions
- recovery after interrupted execution
- export only after user request

### P0.8 Scheduler / device-aware execution

Routing and scheduling must consider:

`capability + privacy + latency + cost + model availability + task complexity + memory + network + user preference + battery + thermal`

The decision and its evidence should be inspectable without storing sensitive request content. Current LAI documentation already defines this direction through `SchedulerDecisionTelemetry`. fileciteturn156file0L2-L2

Hard rule: resource estimates are admission evidence, not proof of actual success. Runtime failures must remain observable.

## 4. P1 capabilities

### P1.1 Multimodal gateway

Normalize text, image, audio and document inputs without forcing GGEN to know backend-specific formats.

Contract must define:

- modality descriptors
- MIME/type/size limits
- lifecycle of temporary media
- preprocessing ownership
- model capability matching
- privacy classification
- streaming where supported

### P1.2 OCR service

Expose OCR as a capability, not as a UI implementation.

Minimum contract:

`image/document → OCR request → structured text + blocks + confidence + language/script metadata`

Bangla OCR remains a first-class LAI capability. Do not require GGEN to know which OCR engine produced the result.

### P1.3 Embeddings / RAG

Separate:

- embedding provider
- vector store
- retrieval policy
- reranking
- chunking/index lifecycle
- source provenance

Every retrieved item needs provenance. Memory retrieval must not silently become authority to perform consequential actions.

### P1.4 Memory

Memory classes:

- session state
- user-approved persistent memory
- project/workspace memory
- retrieval index
- operational/audit state

Sensitive data must have explicit retention/deletion policy. Memory must never become an implicit cross-repository channel to GGEN or other systems.

### P1.5 Image generation/editing

Expose provider-neutral operations:

- `image.generate`
- `image.edit`
- `image.variation`
- optional mask/reference-image inputs

GGEN owns the document/artifact integration. LAI owns model/provider execution and returns structured results plus provenance.

### P1.6 Developer AI / development workstation

LAI may provide:

- repository inspection
- code understanding
- planning
- patch generation
- build/test execution
- terminal
- Git
- editor/LSP integration
- Linux/PRoot/QEMU execution
- artifact verification

This is LAI territory, not the definition of GGEN.

Consequential developer operations must use the same permission/audit model as Android automation.

### P1.7 Remote development/runtime

Support explicit remote targets:

- desktop AI server
- LAN workstation
- Ollama
- llama.cpp server
- vLLM/custom OpenAI-compatible endpoint

Remote execution must carry endpoint identity, authentication state, network trust classification and cancellation semantics.

### P1.8 Observability / benchmark center

Record structured metrics such as:

- time-to-first-token
- tokens/sec
- prompt/generated tokens
- memory peak
- backend selected
- model selected
- provider latency
- fallback count
- failure class
- thermal/battery state where policy allows

Never manufacture benchmark values. Existing runtime documentation explicitly requires measured evidence before claiming Vulkan or QNN performance. fileciteturn156file0L2-L2

## 5. Recommended implementation waves

### Wave A — Contract foundation

1. Gateway request/response model
2. Capability registry
3. Provider/backend interfaces
4. Model registry
5. cancellation/deadline contract
6. permission/privacy primitives
7. normalized telemetry

### Wave B — Local runtime

1. CPU production hardening
2. Vulkan measured path
3. resource admission
4. model lifecycle
5. benchmark harness
6. truthful fallback

### Wave C — Hybrid providers

1. OpenAI-compatible provider
2. Anthropic/Gemini adapters
3. remote Ollama/custom endpoint
4. routing policy
5. quota/cost accounting
6. provider health and bounded fallback

### Wave D — Agent/tool plane

1. typed tool registry
2. policy gate
3. confirmation UI
4. durable audit
5. one-shot agent actions
6. cancellation/replay defense
7. only then evaluate bounded multi-step workflows

### Wave E — Intelligence services

1. OCR
2. multimodal
3. embeddings
4. RAG
5. memory
6. image generation/editing

### Wave F — Developer platform

1. repo inspection
2. Git/terminal
3. build/test
4. LSP/editor
5. Linux/PRoot/QEMU
6. remote execution

## 6. GGEN integration rule

GGEN must consume **capabilities**, not LAI implementation details.

Preferred call shape:

`GGEN → AI Capability Contract → LAI Gateway → provider/backend → model`

Forbidden coupling:

- GGEN importing LAI Kotlin classes
- GGEN depending on llama.cpp
- GGEN depending on Vulkan/QNN
- GGEN storing LAI model paths
- GGEN implementing provider-specific auth
- LAI owning GGEN document state

The same GGEN contract should also be satisfiable by cloud/custom providers, allowing GGEN to operate with LAI absent.

## 7. Definition of done

A capability is not complete merely because a UI control exists or a backend compiles.

For production status, require:

- documented contract
- implementation
- automated tests
- negative/failure tests
- cancellation test
- persistence/recovery test where state exists
- privacy/security review
- telemetry evidence
- device/provider measurement where applicable
- documentation updated
- no false capability claims

## 8. Architectural decision

**LAI remains the intelligence/execution platform. GGEN remains the creative/document platform.**

Integration is capability-contract based. Neither repository becomes a submodule or internal implementation dependency of the other.
