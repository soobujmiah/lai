# LAI Capability & Tool Benchmark Foundation

**Status:** Architecture/documentation baseline
**Date:** 2026-08-24
**Scope:** AI runtime, agents, automation, device execution, model infrastructure and external benchmark research

## 1. Purpose

LAI is the **AI intelligence, inference, agent and device-execution platform**. It is not GGEN's creative editor. LAI may serve GGEN through a capability contract, but remains independently useful as a local-first Android AI runtime.

The repository already documents a local-first architecture, tool policy, audit/replay controls, model management, device-aware scheduling and replaceable runtime boundaries. The README explicitly separates UI, agent/tool policy, model repository, accessibility, OCR, elevated shell, device profile/scheduler and isolated inference adapters. fileciteturn29file0L2-L2

## 2. Capability inventory

| Capability family | LAI ownership | Current state | Benchmark set | Initial disposition |
|---|---|---|---|---|
| AI gateway / provider routing | LAI | architecture | LiteLLM, Bifrost, Portkey, cloud gateways | BUILD/ADAPT |
| Local LLM runtime | LAI | CPU validated | llama.cpp, ExecuTorch, MLC, MediaPipe/LiteRT | ADOPT/ADAPT |
| Vulkan inference | LAI | qualification gate | llama.cpp Vulkan, vendor runtimes | BUILD behind adapter |
| Qualcomm NPU | LAI | roadmap/qualification | QNN/QAIRT, LiteRT delegates | PROVIDER/ADAPTER |
| Model registry | LAI | implemented | Ollama, Hugging Face Hub, model catalogs | BUILD |
| Model download/verification | LAI | implemented | HF Hub, OCI/artifact systems | BUILD with strict policy |
| Agent runtime | LAI | implemented foundation | OpenAI Agents SDK, LangGraph, CrewAI, AutoGen | BUILD/ADAPT |
| Tool execution | LAI | implemented | MCP, function calling ecosystems | BUILD around typed policy |
| MCP | LAI | roadmap/integration | MCP ecosystem | ADOPT protocol |
| A2A | LAI | future | A2A ecosystem | ADOPT at agent boundary |
| Android Accessibility | LAI | implemented | Android Accessibility APIs, Tasker ecosystem | BUILD |
| Shizuku/elevated operations | LAI | implemented | Shizuku, ADB | BUILD with consent policy |
| OCR | LAI | interface/placeholder | PaddleOCR, ML Kit, Tesseract, Docling | PROVIDER |
| RAG | LAI | roadmap | SQLite-vec, Qdrant, LanceDB, Chroma | BUILD/ADAPT |
| Memory | LAI | roadmap | Mem0, Letta/MemGPT patterns, vector stores | BUILD/ADAPT |
| Scheduler | LAI | implemented foundation | Android scheduling, inference schedulers | BUILD |
| Thermal/memory/battery awareness | LAI | implemented/qualifying | Android thermal APIs, vendor telemetry | BUILD |
| Audit/replay | LAI | implemented | append-only audit systems, agent observability | BUILD |
| Plugin ecosystem | LAI | contract | MCP, Android plugin models, WASM/plugin systems | ADAPT |
| Developer AI | LAI | roadmap | Claude Code, Codex, Aider, OpenHands, Cline | BUILD/ADAPT |
| Terminal/code execution | LAI | roadmap | local shell/containers/PRoot/QEMU | BUILD with strict sandbox |
| Git/build/test | LAI | roadmap | Git CLI, GitHub APIs, CI runners | PROVIDER/BUILD |
| Remote development | LAI | roadmap | SSH, VS Code Remote, devcontainers | ADAPT |
| Linux/PRoot/QEMU | LAI | direction | Termux, proot-distro, QEMU | ADAPT |
| Multimodal AI | LAI | roadmap | Gemini, OpenAI, local VLMs | PROVIDER |

## 3. AI gateway decision

LAI should own the **capability routing layer**, but it should not be forced into a single gateway implementation prematurely.

Current ecosystem research shows a meaningful tradeoff: LiteLLM emphasizes broad provider coverage and a common API, while Bifrost emphasizes high-throughput Go-based gateway performance and enterprise routing features. Community reports are useful evidence but are not sufficient alone for production selection. citeturn0reddit38turn0reddit39

**Decision direction:** define an LAI-native typed capability contract first. Provider adapters can expose OpenAI-compatible APIs, native vendor APIs, local llama.cpp, remote LAI, or another gateway. The contract must survive provider replacement.

## 4. Agent interoperability

MCP and A2A solve different layers. MCP is appropriate for tool/data access; A2A is appropriate for independent agents communicating and delegating tasks. Current research also shows that A2A provides richer task/lifecycle semantics while MCP can be lighter but requires application-level lifecycle management. citeturn0academia36

Recent ecosystem movement also places A2A under the Agentic AI Foundation, reinforcing its role as an interoperability standard rather than a vendor-specific feature. citeturn0news37

**LAI decision direction:**

- MCP = tool/resource capability boundary.
- A2A = agent-to-agent delegation boundary.
- LAI policy/audit layer = governance above both.

Protocols do not replace governance. Current research identifies important gaps around membership, deliberation, dissent, human escalation and audit/replay. citeturn0academia43

## 5. Local inference

LAI's local inference boundary must remain replaceable:

```text
Agent / App request
        |
Capability router
        |
Policy + scheduler
        |
Runtime adapter
   ┌────┼────┐
 CPU  GPU   NPU
 llama Vulkan QNN/QAIRT
```

The current repository documents a physically validated arm64 llama.cpp CPU path and explicitly does not claim Vulkan or QNN acceleration until physical qualification. fileciteturn29file0L2-L2

This evidence boundary must remain non-negotiable.

## 6. Device automation

Accessibility and Shizuku are not generic shell tools. They are privileged execution surfaces.

LAI's existing design is therefore correct to require confirmation for consequential actions, prohibit arbitrary shell strings, compile elevated operations from typed validated operations, and maintain a hash-chained audit/replay record. fileciteturn29file0L2-L2

**Decision:** strengthen the typed-operation model rather than replacing it with unrestricted agent execution.

## 7. OCR and document intelligence

OCR is a replaceable capability. PaddleOCR is a strong recognition benchmark; Docling is useful as a structured-document understanding benchmark; OCRmyPDF is a useful searchable-PDF pipeline benchmark.

**Decision:** LAI should expose structured OCR/document-understanding contracts and permit multiple engines. GGEN can consume the result without knowing which engine produced it.

## 8. Developer AI

LAI's Developer AI direction is materially different from GGEN. It includes code understanding, planning, editing, test/build verification, Git, terminal and development-workstation operations.

External agents such as Claude Code, Codex, Aider and OpenHands should be treated as benchmarks for workflow quality—not dependencies to embed blindly.

The key benchmark is the complete loop:

```text
inspect → understand → plan → modify → test → verify → document → report
```

LAI should own orchestration, evidence and policy boundaries; individual coding engines remain replaceable providers/tools.

## 9. Adoption policy

### ADOPT

Use an external standard/protocol directly when interoperability is the primary value and the license/specification is appropriate.

### ADAPT

Use a proven implementation pattern while preserving LAI's typed policy, audit and device-security model.

### BUILD

Implement natively when the capability is core LAI infrastructure or requires deep device/runtime integration.

### PROVIDER

Expose a stable capability contract and keep the implementation external or replaceable. This is preferred for cloud AI, OCR engines, some local runtimes and coding agents.

### REJECT

Reject solutions that require unrestricted shell execution, leak private intelligence, collapse security boundaries, introduce unnecessary vendor lock-in, or duplicate GGEN responsibilities.

## 10. Required research record

For every future LAI dependency or protocol record:

1. Exact project/version/spec revision.
2. License.
3. Android/arm64 viability.
4. Offline viability.
5. CPU/GPU/NPU support.
6. Memory/thermal footprint.
7. API/protocol surface.
8. Security model.
9. Permission requirements.
10. Observability/auditability.
11. Failure and cancellation semantics.
12. Maintenance/community health.
13. Evidence quality.
14. ADOPT/ADAPT/BUILD/PROVIDER/REJECT decision.
15. Exit/replacement strategy.

## 11. Next research passes

1. Complete local-runtime benchmark matrix.
2. Complete Qualcomm QNN/QAIRT and LiteRT boundary research.
3. Complete AI gateway/provider matrix.
4. Complete MCP/A2A/plugin interoperability matrix.
5. Complete agent-runtime benchmark.
6. Complete OCR/RAG/memory benchmark.
7. Complete Android automation/security benchmark.
8. Complete Developer AI workstation benchmark.
9. Map accepted capabilities to LAI contracts/modules.
10. Publish agent-ready implementation specifications.
