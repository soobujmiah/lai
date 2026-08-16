# Master implementation plan

Last updated: 2026-08-17

## Governing sequence

`Document → Review → Plan → Implement → Test → Document → Verify`

No roadmap feature is authorized by this document alone. Each phase must pass its documentation gate and any required ADR must be accepted before code. Existing working systems are preserved unless source evidence and an approved migration justify change.

## Phase 0 — documentation audit (current)

**Objective:** establish source-verified current state, architecture maps, feature matrix, roadmap, testing plan, and ADR index.

**Deliverables:** the immediate directive documents under `docs/implementation`, `docs/architecture`, `docs/product`, and `docs/decisions`; links to useful legacy docs; explicit conflicts and unknowns.

**Verification:** source gate, link review, status vocabulary review, clean diff, and CI on the documentation commit. No roadmap code in this phase.

**Exit:** another engineer can identify actual modules/interfaces, supported backends/devices, permissions/data flow, implemented/planned capability, risks, tests, and rollback gaps.

## Phase 1 — architecture and ADRs

**Objective:** resolve P0 architecture before implementation.

1. Define AI Gateway contracts and migration boundary from direct ViewModel/runtime composition.
2. Define managed localhost server lifecycle, API, authentication, process/resource/port policy, loopback/LAN rules, client visibility, logs, and kill switch.
3. Define multi-step task, checkpoint, provenance, diff, verification, rollback, and durable recovery contracts.
4. Define project trust and project/filesystem/secret boundaries needed before workstation work.
5. Update threat model, privacy policy, licensing review, test strategy, and migration for each.

**Required ADRs:** gateway, localhost security/process model, task/checkpoint model, project trust/diff storage. Reject or defer options explicitly.

**No-go:** do not expose a server port, add remote provider SDKs, implement unrestricted shell, or start workstation UI during this phase.

## Phase 2 — Universal AI Gateway

**Objective:** provider-independent AI entry point while preserving the current embedded CPU path.

**Increment:** pure provider/model/request/stream/router/context/capability/error contracts → embedded adapter → app migration → tests. Keep tool permission and audit outside provider SDKs. Usage tracking is local/content-free.

**Acceptance:** current Qwen CPU install/load/chat still works; UI has no concrete provider dependency; unavailable providers are not shown as working; cancellation/errors preserve provenance; no new network egress.

**Rollback:** feature branch/module can be removed and app returned to direct `InferenceEngine` composition without model-registry migration.

## Phase 3 — managed localhost LLM server

**Objective:** first-class OpenAI-compatible loopback provider where practical.

**Increment:** approved API spec → loopback-only authenticated server → health/models → streaming chat → lifecycle/resource controls/log redaction → provider adapter → explicit LAN opt-in last.

**Acceptance:** loopback default, safe port allocation, auth required, start/stop/restart/kill, client visibility, bounded requests/streams, process-death recovery, no content logs, no silent LAN/cloud exposure.

**Rollback:** stop/disable server; embedded provider remains usable.

## Phase 4 — inference backends

**Objective:** stabilize multi-backend selection and harden current model I/O before adding acceleration.

**First:** complete stream hardening (model download/import byte ceilings are implemented in source; SAF discovery deadline/cancellation bounds remain), recoverable registry/catalog/settings writes, device tests, and context/cancellation/thermal reliability. Then qualify Vulkan. QNN follows only with licensed tooling, converted artifacts, isolated adapter, fallback, and device evidence.

**Acceptance:** capability self-tests are truthful; scheduler evidence and fallback are visible; no theoretical hardware claims; CPU correctness baseline remains unchanged.

## Phase 5 — multi-step agent runtime

**Objective:** evolve one-shot tools into bounded, inspectable tasks without weakening consent.

**Increment:** goal/plan/step contracts, maximum steps/time/resources, typed observations, per-step permission, foreground/provenance binding, cancellation/retry, verification. No model-authored confirmation.

**Acceptance:** adversarial parser/tool tests, restart behavior, service death, loop limits, and complete privacy-safe task trace. One-shot mode remains fallback.

## Phase 6 — task center, diff, checkpoint, rollback

**Objective:** make every consequential task understandable and reversible where possible.

**Increment:** task history/status UI; file proposal/diff; explicit apply; tests; checkpoint; rollback/compensation; audit reference. Define non-reversible Android actions clearly.

**Acceptance:** users can see goal, plan, current tool, permission, changed files, commands, results, tests, errors, audit reference, and summary; project mutations are never silent.

## Phase 7 — project system

**Objective:** introduce `Project` as a trusted/restricted/untrusted domain over explicit filesystem/runtime/toolchain/Git/policy/model/permission/task/secret-reference/documentation/backup boundaries.

**Acceptance:** path traversal and provider deception tests, trust transitions, per-project permission isolation, no raw secret exposure, snapshot/recovery, migration from the current generic workspace without data loss.

## Phase 8 — development workstation

**Objective:** incremental mobile development surfaces over the Project domain.

Order: project/file manager → read-only viewer/search → editor/diff → diagnostics/LSP → terminal sessions → Git browser/staging/commit/diff/conflict → build center/artifacts. Add tablet/foldable, keyboard/mouse, accessibility, and Bangla/English behavior from the start.

**No-go:** do not equate Shizuku operations with a full terminal or CI with an on-device build center.

## Phase 9 — Linux runtime

**Objective:** abstract PRoot/QEMU/future runtimes with explicit mounts, process/package/environment profiles, diagnostics, lifecycle, and compatibility limits.

**Acceptance:** distribution/license review, image integrity, bounded storage/processes, mount isolation, kill/recovery, terminal integration, and honest app compatibility.

## Phase 10 — RAG and multimodal

**Objective:** local-by-default parser/chunker/embedding/index/retriever/context and separate text/image/OCR/document/audio interfaces.

**Acceptance:** explicit ingestion policy, deletion/export, index/model/version migrations, measured quality, storage bounds, no automatic whole-project ingestion, no large dependencies in core.

## Phase 11 — reliability and performance

**Objective:** backup/recovery, storage-pressure manager, benchmark/performance center, closed-loop thermal/battery policy, process-death/reboot recovery.

**Acceptance:** storage full, app kill, reboot, interrupted model load/download/build, permission loss, and corrupt state tests; only measured metrics are shown.

## Phase 12 — full Plugin SDK and manager

**Objective:** signed packages, deny-by-default permissions, compatibility, isolation, lifecycle, UI/tool contributions, audit, update, rollback, uninstall.

**Acceptance:** malicious package/schema/output/crash tests; trust-root and license policy; no bypass of agent/network/project permissions.

## Phase 13 — creative studios

**Objective:** Vector/Paint/3D and later document/media tools as independent modules over stable project/plugin/runtime APIs.

**Acceptance:** modules cannot destabilize core; file formats, licensing, autosave/recovery, memory pressure, accessibility, and rollback are documented/tested.

## Phase 14 — remote and ecosystem

**Objective:** explicit remote development, remote AI server management, LAN/cloud providers, local API/integrations.

**Acceptance:** explicit opt-in, authentication, encryption, secret references, endpoint trust, revocation, audit, no silent uploads/fallback, and incident response.

## Cross-phase documentation gate

Before every phase transition answer:

- Architecture documented? YES/NO
- Interfaces documented? YES/NO
- Security documented? YES/NO
- Privacy documented? YES/NO
- Tests documented? YES/NO
- Known limitations documented? YES/NO
- User docs updated? YES/NO
- Developer docs updated? YES/NO
- ADR required and accepted? YES/NO

Any required **NO** blocks implementation or phase advancement.

## Immediate next actions after this audit

1. Review these documents against source and legacy docs.
2. Run CI and resolve documentation/source contradictions without weakening checks.
3. Fix current reliability/security debt (stream bounds, SAF recovery, tests, CI least privilege) as documented hardening work.
4. Draft—not implement—the AI Gateway ADR/spec.
5. Draft the localhost server specification and threat model before any server code.
