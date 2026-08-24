# LAI Master Roadmap

**Status:** Documentation-only directive — 2026-08-24  
**Source of truth:** current repository source/tests, `docs/ROADMAP.md`, accepted ADRs, and device evidence. This roadmap describes target work; it does not override source evidence.

## Documentation truth rule

Every capability uses one of: `IMPLEMENTED / VALIDATED / EXPERIMENTAL / PLANNED / BLOCKED / NOT_STARTED`.

Source presence is not validation. Hardware-dependent claims require named-device evidence. When this roadmap conflicts with source, tests, accepted ADRs, or device evidence, the evidence wins and this document must be corrected before implementation continues.

## 1. CPU Inference — VALIDATED

Embedded llama.cpp CPU inference, GGUF loading, streaming and device evidence establish the CPU baseline. Remaining work is additional stress/thermal/Bangla-quality evidence where required.

## 2. GPU / Vulkan — EXPERIMENTAL / QUALIFICATION GATE

Vulkan loader/backend probing and build scaffolding exist. This is not equivalent to production GPU inference validation. Next: actual inference execution on the target Adreno device, failure/success evidence, measured performance and retained CPU fallback. Do not claim a throughput target until measured.

## 3. NPU / QNN / HTP — PLANNED

Architecture/boundary only. Licensed SDK availability, model conversion, isolated QNN runtime and named-device HTP validation are prerequisites.

## 4. AI Gateway / Unified Inference Orchestration — PLANNED

Provider-neutral `InferenceEngine` and scheduler/backend contracts exist; application composition still uses the embedded inference path directly. ADR-0011 proposes the Gateway as the next boundary.

Target: registry/router/context/model/tool/permission/usage/audit gateway with the embedded CPU provider first. No cloud/Vulkan/QNN provider becomes implicitly available through the Gateway. The Gateway must never silently substitute an unavailable backend; explicit policy-approved fallback must be observable in provenance.

## 5. Model System — IMPLEMENTED BASELINE / PLANNED EXTENSIONS

Signed catalog, model repository, SHA-256 verification, resumable acquisition, GGUF validation and bounded workspace discovery exist. Next: capability-aware model selection, provenance/license UX and additional lifecycle hardening.

## 6. Chat & Streaming — IMPLEMENTED BASELINE

Chat modes, streaming, cancellation watchdog, history and quick settings exist. Next: multimodal attachments, citations and UX refinement.

## 7. Tool Ecosystem — IMPLEMENTED ONE-SHOT BASELINE / PLANNED EXPANSION

Typed built-in Android tools, schema validation, approval-before-authority and hash-chained audit exist. Execution is one-shot and user-confirmed. Expand the researched catalog only when each tool has a real executor, permission model, tests and evidence. `PLANNED` entries must never be presented as available tools.

## 8. Multi-step Agent — PLANNED

One-shot tool-proposal execution exists. Target: bounded `Intent → Context → Plan → Memory → Tool → Permission → Execute → Observe → Verify → Next step → Result` with task state, budgets, cancellation, retry and independent verification.

## 9. Developer AI — PLANNED

Target: inspect → understand → plan → modify → build → test → verify → document over trusted project workspaces, using the same LAI tool/permission/audit authority.

## 10. Linux / Terminal — NOT_STARTED

No canonical terminal runtime is implemented. Termux is a research benchmark, not a bundled capability claim. Target: managed PRoot/QEMU/runtime abstraction after workstation/project boundaries are stable, with explicit mounts, process/resource policy and fallback behavior.

## 11. Files / Documents Intelligence — PLANNED

Bounded SAF workspace/model discovery exists; general document intelligence is not implemented as the target subsystem. Target: read/search/extract/summarize/compare/transform/generate/cite across justified formats, with provenance and explicit ingestion.

## 12. OCR / Vision — EXPERIMENTAL / PLANNED

OCR contracts and adapter boundary exist; production Bangla OCR remains incomplete. Placeholder behavior must remain explicit. Target: verified Bangla OCR with model provenance, integrity checks, authorization, quality dataset and named-device evidence.

## 13. Memory — PARTIAL

Bounded conversation history/context policy exists. Cross-task/project memory is not implemented. Target: user-controlled conversation/task/project memory with retention, deletion, provenance and privacy controls.

## 14. RAG / Knowledge Management — PLANNED

No production RAG subsystem. Target: parser/chunker/lexical retrieval first, then measured embeddings/vector retrieval and citations. Indexes must be rebuildable and source documents untouched.

## 15. Android Automation — IMPLEMENTED BASELINE / PLANNED HARDENING

Accessibility and privileged/Shizuku-related authority contracts/tools exist with approval and audit controls, subject to device permission state. Target: foreground-bound recipes, per-step confirmation, loop/time limits, global stop, recovery and scoped Shizuku recipes.

## 16. Hybrid Local / Cloud / Remote AI — PLANNED

No production cloud/remote provider system should be inferred from contracts or ADRs. Target: local CPU/GPU/NPU plus explicitly configured cloud/custom/remote providers, capability/privacy/cost/latency-aware routing, truthful fallback, credentials, quotas and egress controls. This follows the AI Gateway and provider-specific evidence gates.

## 17. Security Hardening — IMPLEMENTED BASELINE / ADDITIVE

Hash-chained tool audit, approval-before-authority, replay protection, source boundary checks and bounded storage permissions exist. Target: stronger evidence states, task-scoped grants, emergency stop, tamper-evident history hardening and additional supply-chain/security controls.

## 18. Multimodal Vision / Audio / Speech — PLANNED

Target: versioned interfaces and measured local adapters for image, document, audio, STT, TTS and image-generation capabilities. Each requires separate licensing, performance and device evidence.

## 19. Knowledge Graph / Mind Map — FUTURE

No production implementation. Remains downstream of RAG and knowledge-management foundations.

## 20. Product Hardening & Release — PLANNED / BASELINE EXISTS

Signed releases and CI/build infrastructure exist. Future work includes SBOM, provenance, reproducible builds and additional supply-chain hardening.

## Mandatory device gates

Never claim production readiness without logs for hardware-dependent features, including actual Vulkan inference, QNN/HTP inference, Bangla OCR quality, tool dispatch harnesses, thermal behavior and future multi-step agent/RAG measurements.

## Additive evolution

Preserve working runtime and module boundaries. Extend through isolated contracts/adapters and bounded vertical slices. Do not rewrite working foundations merely to satisfy roadmap shape.
