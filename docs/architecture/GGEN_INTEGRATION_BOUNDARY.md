# LAI ↔ GGEN Integration Boundary

**Status:** Proposed authoritative boundary
**Date:** 2026-08-24

## LAI's role

LAI is an AI intelligence/runtime/automation platform. It owns inference, model lifecycle, backend execution, scheduling, agent/tool authority, device automation, OCR runtime, memory/RAG infrastructure and execution evidence.

GGEN is an independent AI Creative & Document Studio. It owns creative/document semantics and user-facing project operations.

## Integration rule

LAI exposes capabilities; GGEN consumes them through a provider contract. GGEN must never depend on LAI's internal Kotlin modules, native llama.cpp implementation, Vulkan/OpenCL/QNN implementation, or model files.

Initial capability IDs:

- `text.generate`
- `vision.analyze`
- `ocr.extract`
- `image.generate`
- `image.edit`
- `embedding.create`
- `structured.generate`
- `tool.execute`
- `agent.run`
- `workflow.plan`

## Evidence

LAI must return honest execution metadata. In particular, backend availability, backend acceptance, actual delegation, completed execution, device validation and measured performance are distinct states.

GGEN may render this metadata but must not upgrade it into a stronger claim.

## Security

GGEN is an untrusted consumer from LAI's authority perspective. A provider connection does not automatically grant arbitrary Android, shell, Accessibility or Shizuku authority. Tool calls remain subject to LAI policy, risk classification and user confirmation rules.

## Failure

LAI unavailable → GGEN can fall back to another permitted provider or manual operation. LAI must not require GGEN to install or bundle local models simply to establish the connection.

## Development order

1. Freeze this boundary.
2. Define the versioned capability envelope.
3. Add contract tests.
4. Expose minimal LAI capability discovery and text/OCR operations.
5. Validate local-device transport and evidence.
6. Expand multimodal and agent capabilities.
