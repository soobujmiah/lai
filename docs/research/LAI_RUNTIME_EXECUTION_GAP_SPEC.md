# LAI Runtime — Research-to-Engineering Gap Specification

**Status:** Active engineering planning baseline
**Date:** 2026-08-24
**Scope:** local inference, model lifecycle, acceleration, providers, streaming, tools, terminal, OCR, embeddings/RAG, agent runtime

## Rule

LAI is the canonical execution layer. This document converts research into LAI-specific engineering targets; it does not authorize GGEN to reproduce runtime/provider/tool infrastructure.

## 1. Local inference

**Benchmarks:** llama.cpp, ExecuTorch, ONNX Runtime, Qualcomm QNN/AI Engine.

**Target:** reliable offline inference with explicit device qualification.

**Gap checklist:** model loading/unloading, context management, streaming, cancellation, structured output, tool calling, memory limits, model compatibility, observability, failure recovery.

Maturity must distinguish CPU validated from accelerator availability. A backend being present in source is not equivalent to device validation.

## 2. Model lifecycle

**Target:** reproducible model acquisition, integrity, compatibility, caching and lifecycle management.

**Required controls:** model identity/version, checksum, format, quantization metadata, supported backend/device constraints, storage accounting, eviction, resumable acquisition, failure recovery and user consent.

## 3. GPU/NPU qualification

**Target:** evidence-backed acceleration, not marketing claims.

**Qualification states:** API available → backend available → backend accepted → execution completed → device validated → performance measured.

Adreno/Vulkan driver instability must remain explicitly marked BLOCKED/EXPERIMENTAL where evidence requires it. CPU fallback must remain honest and observable.

## 4. Cloud/custom providers

**Target:** provider-neutral capability execution with secure credentials and deterministic policy.

**Gap checklist:** adapter interface, capability discovery, authentication/secret storage, normalized errors, streaming, structured output, tool calling, usage accounting, health, rate limits, retry, failover, circuit breaking, custom endpoints.

Provider selection and failover are LAI-owned. GGEN supplies user intent/constraints through the contract.

## 5. Streaming/cancellation

**Target:** responsive mobile execution with explicit cancellation and terminal states.

Define event ordering, partial output, cancellation acknowledgement, timeout, disconnect recovery and idempotency semantics. Non-idempotent operations must not be blindly retried.

## 6. Android tools and Terminal

**Benchmarks:** Termux ecosystem and Android automation ecosystems.

**Target:** powerful but consent- and policy-gated execution.

Tool model must include stable ID/version, schema, risk class, permissions, confirmation requirement, executor and evidence. Unknown tools, malformed arguments and denied permissions fail closed.

Terminal capabilities must be modeled separately from arbitrary shell authority. Benchmark Termux capabilities/plugins before expanding the tool surface.

## 7. OCR

**Target:** reliable offline/online extraction with confidence/provenance and explicit device/runtime evidence.

Separate OCR execution from GGEN document presentation. OCR results should retain source references, confidence, language/script information where available and error status.

## 8. Embeddings and RAG

**Target:** offline-first retrieval with provenance and controlled resource use.

**Gap checklist:** embedding backend abstraction, model lifecycle, chunking, metadata, vector persistence, indexing, retrieval, reranking where justified, deletion/update semantics, provenance, memory limits and benchmarks.

Avoid locking the architecture to a single vector database before benchmark evidence exists.

## 9. AI memory

**Target:** explicit, inspectable and privacy-aware runtime memory distinct from GGEN document state.

Define scopes, retention, deletion, provenance, retrieval policy and consent. Memory must never silently become document content or vice versa.

## 10. Agent runtime

**Benchmarks:** Android agent/tool systems plus general agent frameworks.

**Target:** bounded planning and execution with evidence.

Required decomposition:

1. task intake;
2. context selection;
3. planning;
4. capability/tool discovery;
5. permission/confirmation;
6. execution;
7. observation;
8. retry/recovery;
9. evidence/audit;
10. completion/abort.

The planner must not bypass tool registry, permissions or runtime policy.

## Cross-cutting acceptance gates

Every LAI capability requires:

1. benchmark research;
2. current implementation audit;
3. explicit maturity state;
4. capability contract/specification;
5. automated tests where practical;
6. device evidence for hardware-dependent behavior;
7. security/permission review for tools;
8. failure-path testing;
9. no GGEN runtime duplication;
10. commit → push → remote verification → SHA.

## Agent instruction

Before implementing a row, inspect the live LAI repository, existing architecture/provider/tool documents, SKB ownership and research records, then verify that the capability is genuinely missing. Update the canonical specification if evidence changes the gap.
