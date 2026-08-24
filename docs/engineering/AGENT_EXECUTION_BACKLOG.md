# LAI Agent Execution Backlog

**Status:** Ready for agent execution
**Date:** 2026-08-24
**Scope:** runtime/tool/provider engineering after research and ownership gates

## Execution contract

Before every task: read SKB ownership matrix + Android research protocol + LAI runtime gap spec, inspect live implementation/tests/evidence, and verify the gap still exists. Never duplicate GGEN creative/document infrastructure.

Completion requires tests/device evidence as applicable, documentation synchronization, commit, push, remote verification and recorded SHA.

## P0 — Runtime contract and safety

### LAI-P0-01 — Capability contract freeze
- Reconcile capability IDs, normalized request/response, errors, streaming and cancellation with GGEN.
- Acceptance: contract tests cover supported/unsupported/denied/timeout/cancelled outcomes.

### LAI-P0-02 — Runtime/provider boundary audit
- Audit provider adapters, routing, secrets, retries/failover and custom endpoints.
- Remove or quarantine duplicate/obsolete paths.
- Acceptance: one canonical runtime path; secrets never enter GGEN documents/logs.

### LAI-P0-03 — Tool authority audit
- Audit tool registry, schemas, risk classification, permissions, confirmations and evidence.
- Acceptance: unknown/invalid/denied operations fail closed.

## P1 — Reliable runtime core

### LAI-P1-01 — Local model lifecycle
- Define model identity, checksum, format/quantization metadata, compatibility, cache, eviction, resumable acquisition and recovery.
- Acceptance: deterministic lifecycle tests and storage accounting.

### LAI-P1-02 — CPU inference qualification
- Preserve validated CPU path; formalize model compatibility and streaming/cancellation evidence.
- Acceptance: repeatable device test report.

### LAI-P1-03 — GPU/NPU qualification matrix
- Run evidence-gated backend qualification on supported devices.
- Keep Adreno/Vulkan instability explicitly blocked/experimental where evidence requires.
- Acceptance: no performance claim without device measurement.

### LAI-P1-04 — Streaming/cancellation semantics
- Define event ordering, partial results, cancellation acknowledgement, timeout and disconnect recovery.
- Acceptance: deterministic failure-path tests.

### LAI-P1-05 — Provider adapters/routing
- Implement normalized adapters, health, bounded retry, failover and circuit breaking only after P0 contract freeze.
- Acceptance: provider-neutral tests and safe `local_only` behavior.

## P2 — Tools and knowledge runtime

### LAI-P2-01 — Termux terminal capability benchmark
- Benchmark practical Termux capabilities/plugins relevant to AI workflows.
- Define least-privilege terminal tool contract; do not equate tool execution with unrestricted shell authority.
- Acceptance: documented capability matrix + permission/confirmation tests.

### LAI-P2-02 — Accessibility/Shizuku hardening
- Audit existing tools, permission gates, confirmation, failure recovery and execution evidence.
- Acceptance: denied/expired/invalid cases fail closed with evidence.

### LAI-P2-03 — OCR execution contract
- Validate OCR runtime path, confidence/provenance and language/script metadata.
- Acceptance: device evidence; no unsupported Bangla OCR claim.

### LAI-P2-04 — Embeddings/RAG foundation
- Benchmark candidate embedding backends and storage approaches before selecting a durable implementation.
- Define chunking, metadata, retrieval, update/delete and provenance contracts.
- Acceptance: benchmark report + minimal reproducible retrieval test.

### LAI-P2-05 — Runtime memory
- Specify explicit scopes, retention, deletion, provenance and consent.
- Keep runtime memory separate from GGEN document state.
- Acceptance: inspect/delete tests and policy review.

## P3 — Agent runtime

### LAI-P3-01 — Agent execution model
- Implement bounded task intake → context → plan → capability discovery → permission → execution → observation → recovery → evidence → completion/abort.
- Acceptance: planner cannot bypass tool registry or permission policy.

### LAI-P3-02 — Agent evidence/audit integration
- Connect agent actions to runtime evidence and audit without treating model output as authority.
- Acceptance: every privileged action has attributable evidence.

## Definition of Done

A task is complete only when:
- the gap was verified against live code;
- benchmark evidence is recorded;
- maturity state is honest;
- ownership is correct;
- security/permission gates pass where relevant;
- tests/device evidence pass;
- docs are synchronized;
- Git commit exists;
- remote branch is updated;
- remote state is verified;
- SHA is recorded.

## Ordering

P0 must precede runtime expansion. P1 establishes reliable execution. P2 expands tools/knowledge only after authority boundaries are sound. P3 agent runtime follows tool and evidence foundations.
