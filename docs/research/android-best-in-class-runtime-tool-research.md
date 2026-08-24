# LAI Android Best-in-Class Runtime & Tool Research

**Status:** Research baseline / agent guidance
**Date:** 2026-08-24
**Scope:** Android-first AI runtime, terminal, automation, OCR, agent, memory/RAG, provider and developer-tool ecosystem.

## 1. Purpose

This document gives LAI agents an Android-specific benchmark layer. Existing Android applications are used as evidence for interaction patterns, execution models and integration opportunities. They are not automatically dependencies or architecture authorities.

## 2. Decision vocabulary

- **BUILD** — LAI should own the capability.
- **ADAPT** — adopt a proven pattern behind LAI contracts.
- **INTEGRATE** — consume an external runtime/service/library.
- **PROVIDER** — expose the capability through a provider/backend contract.
- **REJECT** — do not reproduce the benchmark behavior.

## 3. Terminal / developer environment

### Termux — primary benchmark

Termux is the most important Android benchmark for LAI's terminal/developer surface. Study its terminal emulator, shell environment, package ecosystem, SSH/Git/toolchain workflows, Android storage interaction, background limitations, keyboard behavior and plugin ecosystem.

Related Termux ecosystem patterns worth studying include Termux:API, Termux:Widget, Termux:Float, Termux:Tasker and boot/background integration.

### UserLAnd / Andronix

Use these as secondary benchmarks for user-facing Linux distribution/container/PRoot onboarding. Their value is the packaging and UX around bringing Linux userlands to Android, not necessarily the runtime architecture LAI should adopt.

### LAI target

LAI Terminal should combine:

`Termux's mature terminal UX + Android-native permission/authority + LAI AI agent + typed execution policy + audit/evidence`

LAI must never expose model-controlled unrestricted shell authority. Terminal execution is a privileged tool plane and must use the same consent, allowlist, timeout, cancellation and audit rules as other consequential tools.

## 4. Local AI / model runtime

| Capability | Benchmark | LAI lesson |
|---|---|---|
| GGUF local LLM | llama.cpp / Android llama apps | retain direct lightweight native execution |
| Android accelerator abstraction | ExecuTorch | keep backend-neutral contract and replaceable adapters |
| Interoperability | ONNX Runtime | useful for non-GGUF models and QNN/ONNX workloads |
| Qualcomm acceleration | QNN / Qualcomm AI Engine ecosystem | device-specific backend behind LAI contract |
| Local model UX | PocketPal AI / MLC-style Android clients | study model import, download, chat and hardware disclosure UX |

Current LAI truth remains CPU llama.cpp as the qualified baseline; accelerator support must be separately measured and never inferred from compilation.

## 5. AI assistant / agent UX

Android AI clients should be studied for:

- model selection
- conversation/session persistence
- streaming
- attachments
- local vs cloud disclosure
- context limits
- model download/import
- generation controls
- tool/action confirmation

LAI's differentiator is not another chat UI. Its value is controlled execution: typed tools, Android authority, policy, evidence and device-aware runtime.

## 6. Android automation

Benchmark ecosystems include Accessibility-based automation apps, Tasker-style automation and Shizuku-based tools. Study:

- action discovery
- permission onboarding
- foreground/background behavior
- failure reporting
- confirmation UX
- app targeting
- retries and idempotency

LAI remains the canonical authority for its own Accessibility/Shizuku tool surface. External automation apps are research references, not authorities to be silently delegated to.

## 7. OCR / document vision

Benchmark Adobe Scan, Google Drive scanning and other Android OCR/scanner workflows for capture quality, perspective correction, language selection, structured text extraction and document/PDF output.

LAI should expose OCR as a provider-neutral capability:

`image/document → structured blocks + text + confidence + language/script + provenance`

Bangla OCR is a first-class target. GGEN owns document presentation; LAI owns OCR execution.

## 8. RAG / memory / knowledge

Android-local AI applications and note/knowledge tools should be benchmarked for:

- document ingestion
- chunking
- embeddings
- vector storage
- semantic retrieval
- citations/provenance
- persistent memory
- deletion/retention
- offline indexing

LAI should separate session memory, user-approved persistent memory, project/workspace memory, retrieval indexes and audit state. Retrieval must never become implicit authority to execute a consequential action.

## 9. Remote/cloud AI

Benchmark Android clients that support OpenAI-compatible endpoints, Ollama, LAN inference and major cloud APIs. Study endpoint configuration, authentication, model discovery, streaming, cancellation, privacy disclosure and failure handling.

LAI owns provider abstraction and routing. GGEN must consume a provider-neutral capability contract and must not implement provider-specific routing itself.

## 10. Developer tooling

Benchmark Android Git clients, code editors and remote-development clients for:

- repository browsing
- diff review
- commit/push workflow
- terminal integration
- syntax highlighting
- search/navigation
- SSH/remote sessions
- build/test feedback

LAI may eventually provide developer AI/workstation capabilities, but consequential Git/build/terminal operations require explicit policy and audit.

## 11. Tool/agent architecture target

All agent-facing tools must have:

- stable ID/version
- typed request schema
- bounded output schema
- risk classification
- permission requirements
- cancellation/timeout
- confirmation requirement
- audit event
- provenance/evidence
- idempotency or replay defense where relevant
- explicit failure classification

No benchmark app's unrestricted automation model overrides LAI's security boundary.

## 12. Android-specific evaluation criteria

Every benchmark must evaluate:

- phone/tablet UX
- touch/stylus/keyboard/mouse
- foreground/background limits
- Android lifecycle
- SAF
- storage permissions
- battery/thermal impact
- memory pressure
- offline operation
- network trust
- accessibility
- privacy
- crash recovery
- observability

## 13. Current ownership decisions

| Capability | Canonical owner | External benchmark role |
|---|---|---|
| Local LLM execution | LAI | llama.cpp, ExecuTorch, ONNX Runtime |
| Model lifecycle | LAI | PocketPal/MLC-style UX |
| Provider routing | LAI | cloud/remote client patterns |
| Terminal | LAI | Termux primary benchmark |
| Android automation | LAI | Tasker/accessibility/Shizuku ecosystem |
| OCR execution | LAI | Adobe Scan/Google scanning UX |
| RAG/embeddings/memory | LAI | Android local-AI/knowledge apps |
| Agent runtime | LAI | Android AI-agent products |
| Developer AI | LAI | Android code/Git/terminal apps |
| Creative/document artifact editing | GGEN | Canva/Adobe/Android creative apps |

## 14. Agent research protocol

Before implementing a capability, an agent must:

1. identify at least one Android benchmark and one broader professional/open-source benchmark where applicable;
2. record the exact behavior worth copying as a pattern;
3. verify whether LAI already implements it;
4. classify maturity as DESIGNED/IMPLEMENTED/AVAILABLE/SUPPORTED/ACTIVE/MEASURED/PRODUCTION where applicable;
5. choose BUILD/ADAPT/INTEGRATE/PROVIDER/REJECT;
6. define security and permission implications;
7. define device evidence required;
8. update the relevant architecture/roadmap document before implementation.

## 15. Research boundary

This document is a living baseline. Android applications change frequently. Agents must re-check official documentation, current Android availability, licensing and device compatibility before making a production dependency decision.
