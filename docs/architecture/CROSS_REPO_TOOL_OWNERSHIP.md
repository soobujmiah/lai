# Cross-Repository Tool Ownership

## LAI canonical

- inference and generation runtime
- CPU/GPU/NPU backends
- model registry and lifecycle
- accelerator scheduling
- OCR model/runtime execution
- agent runtime
- policy-gated tool execution
- Accessibility and Shizuku authority
- RAG/memory runtime
- AI gateway/provider execution
- execution evidence and AI diagnostics
- developer-AI workstation infrastructure

## GGEN canonical

- canvas and editing tools
- vector/raster/painting/font/3D domain semantics
- document/PDF/template tools
- project/asset/export model
- workflow authoring
- AI-assisted creative UX

## Boundary examples

GGEN's OCR feature owns selection, preview, editable placement and document integration. LAI owns OCR model execution and returns normalized OCR data.

GGEN's image-generation feature owns prompt/reference UI, result management and placement. LAI or another provider owns generation execution.

GGEN's workflow editor owns workflow definitions. LAI may execute an explicitly authorized AI/automation node through a versioned contract.

## No direct dependency

Do not import GGEN source into LAI or LAI implementation modules into GGEN. Cross-repository reuse occurs through stable capability contracts, file formats, plugin APIs or explicit worker protocols.
