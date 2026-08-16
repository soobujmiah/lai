# LAI master roadmap and implementation backlog

Audit date: 2026-08-17

This is the canonical repository roadmap. It follows the mandated Phase 0–14 order and does not authorize code or replace source evidence. The useful pre-directive engineering backlog is retained in Appendix A; conflicts are resolved in favor of current source, accepted ADRs, and the audited phase order.

## Global phase gate

Every phase requires architecture/interfaces/security/privacy/tests/limitations/user/developer documentation and accepted ADRs where needed. Acceptance means relevant tests actually ran. Rollback/migration must be defined before stateful or authority-bearing changes.

## Phase 0 — documentation audit

- **Objective:** source-verified project record.
- **Current state:** PARTIAL before this audit; useful but differently organized legacy docs existed.
- **Target:** current state, architecture maps, feature matrix, master roadmap, implementation/testing plans, ADR index.
- **Dependencies:** source/tests/build/native/manifests/CI and existing docs.
- **Deliverables:** immediate directive documents; conflict list; links rather than duplicate deletion.
- **Architecture:** document 15-module graph and boundaries.
- **Security/privacy:** record permissions, egress, authority, secrets, known risks.
- **Tests:** source gate; link/status review; CI on resulting commit.
- **Documentation:** this phase is documentation-only.
- **Acceptance:** another engineer can answer current/planned/safe/tested/extend/revert questions.
- **Rollback/migration:** documentation-only diff; revert inaccurate files without source migration.

## Phase 1 — architecture and ADRs

- **Objective:** approve P0 architecture before feature code.
- **Current:** PARTIAL; embedded inference and one-shot tools are designed, Gateway/server/task/project architecture is not.
- **Target:** accepted ADRs/specs for Gateway, localhost, task/checkpoint/diff, and project trust.
- **Dependencies:** Phase 0 and threat/privacy/license review.
- **Deliverables:** contracts diagrams, API/lifecycle/failure/recovery designs, migration plans.
- **Architecture:** preserve current modules; propose new boundaries only with dependency review.
- **Security/privacy:** provider trust, loopback/LAN, secrets, autonomy, project authority.
- **Tests:** testability review and planned failure matrices.
- **Documentation:** ADRs plus AI/agent/project/security specs.
- **Acceptance:** all blocking decisions accepted; no unresolved source contradiction.
- **Rollback/migration:** design-only; rejected options retained in ADRs.

## Phase 2 — AI Gateway

- **Objective:** provider-independent AI access.
- **Current:** MISSING; app directly uses one `InferenceEngine`.
- **Target:** registry/router/context/model/tool/permission/usage/audit gateway with embedded provider.
- **Dependencies:** Gateway ADR; stable inference contracts.
- **Deliverables:** pure contracts, embedded adapter, app migration, capability/error/provenance UX.
- **Architecture:** UI never imports provider SDKs.
- **Security/privacy:** no provider bypass of permissions; no new egress; content-free usage records.
- **Tests:** routing, streaming, cancellation, fallback prohibition, lifecycle, regression CPU chat.
- **Documentation:** AI Gateway developer/user docs.
- **Acceptance:** existing CPU path passes; unavailable providers remain unavailable.
- **Rollback/migration:** direct embedded adapter fallback; no model storage format break.

## Phase 3 — localhost LLM server

- **Objective:** managed OpenAI-compatible loopback service where practical.
- **Current:** MISSING.
- **Target:** health/models/streaming chat, lifecycle, auth, limits, logs, safe ports, clients, kill switch; LAN explicit only.
- **Dependencies:** Gateway, server ADR/threat model.
- **Deliverables:** server runtime/provider, API specification, management UI.
- **Architecture:** isolated service/process decision documented; model backend remains replaceable.
- **Security/privacy:** loopback default, authentication, rate/resource bounds, content-log prohibition, LAN warning/revocation.
- **Tests:** bind/auth/API/stream/client/port/process death/reboot/kill/LAN opt-in.
- **Documentation:** `docs/ai/localhost-server.md` approved before code; API/user troubleshooting.
- **Acceptance:** no unauthenticated or silent non-loopback exposure.
- **Rollback/migration:** stop/disable server; embedded provider remains functional.

## Phase 4 — inference backends

- **Objective:** reliable model lifecycle and measured backend diversity.
- **Current:** PARTIAL; CPU real, scheduler/contracts present, Vulkan/QNN absent.
- **Target:** hardened streams/state, qualified Vulkan, later isolated licensed QNN, truthful fallback.
- **Dependencies:** current CPU baseline, DeviceProfile, model metadata, licensing.
- **Deliverables:** byte/deadline bounds, recoverable state, backend probes/adapters, benchmark evidence.
- **Architecture:** no vendor types in generic core; second real provider before manager generalization.
- **Security/privacy:** artifact integrity/native hardening; local compute.
- **Tests:** fuzz/JNI/cancel/memory/thermal/storage/device matrices.
- **Documentation:** model lifecycle, compatibility, license, hardware, benchmarks.
- **Acceptance:** measured named-device evidence and CPU fallback; no theoretical claims.
- **Rollback/migration:** disable/remove new adapter without invalidating models or CPU.

## Phase 5 — agent runtime

- **Objective:** bounded multi-step tasks over typed tools.
- **Current:** PARTIAL one-shot runtime.
- **Target:** goal/plan/context/step/policy/permission/execute/observe/verify loop with limits.
- **Dependencies:** Gateway, task ADR, foreground provenance.
- **Deliverables:** task contracts/runtime, retries/timeouts/budgets/cancel, verification, history.
- **Architecture:** Kotlin policy retains authority; native/model code cannot confirm.
- **Security/privacy:** per-step consent, injection resistance, no arbitrary shell, content-minimized history.
- **Tests:** adversarial loops, budgets, cancellation, service death, restart, replay.
- **Documentation:** task lifecycle/safety/permissions.
- **Acceptance:** complete inspectable trace and hard global stop.
- **Rollback/migration:** one-shot mode preserved; incomplete tasks safely abandoned/resumed per policy.

## Phase 6 — task center, diff, rollback

- **Objective:** transparent and reversible project changes.
- **Current:** MISSING.
- **Target:** task UI, diffs, approval, apply, test, checkpoint, rollback/compensation.
- **Dependencies:** Phase 5 and initial Project/checkpoint contracts.
- **Deliverables:** task center, diff engine, checkpoint store, audit references.
- **Architecture:** mutation separate from proposal and approval.
- **Security/privacy:** explicit file/command display; secrets redacted; irreversible actions labeled.
- **Tests:** diff/property tests, partial apply, storage full, crash, rollback integrity.
- **Documentation:** rollback and task-center user/developer docs.
- **Acceptance:** no silent large overwrite; restore verified.
- **Rollback/migration:** checkpoint format versioned; export/recovery path.

## Phase 7 — project system

- **Objective:** first-class Project domain and trust states.
- **Current:** MISSING; generic SAF workspace is not Project.
- **Target:** project files/runtime/toolchain/Git/policy/models/permissions/tasks/secret references/docs/backups.
- **Dependencies:** trust/diff/storage ADRs.
- **Deliverables:** project metadata, trusted/restricted/untrusted policy, import/open/close/recovery.
- **Architecture:** SAF/private/Linux providers behind project filesystem contracts.
- **Security/privacy:** path confinement, capability isolation, secret references, explicit ingestion/network.
- **Tests:** traversal, provider deception, trust transition, concurrent/crash/migration.
- **Documentation:** project/trust/storage/recovery docs.
- **Acceptance:** two projects cannot leak authority/data; damaged metadata recovers safely.
- **Rollback/migration:** current workspace migration is non-destructive and reversible.

## Phase 8 — development workstation

- **Objective:** flagship mobile development experience.
- **Current:** MISSING.
- **Target:** file manager, editor/LSP/search/diff, terminal, Git, build center, artifacts, adaptive UX.
- **Dependencies:** Project, trust, task/diff, toolchain/process boundaries.
- **Deliverables:** staged vertical slices from read-only browsing to build workflows.
- **Architecture:** shared project services; UI features do not own alternate engines.
- **Security/privacy:** untrusted builds/processes, credentials, hooks, signing-key exclusion, explicit remotes.
- **Tests:** large files/projects, process kill, conflicts, interrupted builds, accessibility/device forms.
- **Documentation:** workstation and user guides per surface.
- **Acceptance:** real operations only; no fake buttons/success states.
- **Rollback/migration:** autosave/checkpoints; per-feature disable; artifact cleanup.

## Phase 9 — Linux runtime

- **Objective:** managed PRoot/QEMU/future runtime abstraction.
- **Current:** MISSING.
- **Target:** distro/rootfs, packages, mounts, processes, profiles, display integration, diagnostics.
- **Dependencies:** Project, terminal, storage/resource manager, license policy.
- **Deliverables:** runtime contract and one qualified implementation before generalization.
- **Architecture:** runtime isolation and mounts explicit.
- **Security/privacy:** rootfs integrity, process boundaries, project mount consent, no hidden network.
- **Tests:** install/start/stop/kill/reboot/storage pressure/package failure.
- **Documentation:** compatibility and non-guarantees.
- **Acceptance:** named workflows work; unsupported desktop apps not promised.
- **Rollback/migration:** removable runtime images; project data survives runtime reset.

## Phase 10 — RAG and multimodal

- **Objective:** local knowledge and separate media interfaces.
- **Current:** MISSING/PARTIAL OCR contracts.
- **Target:** explicit RAG pipeline plus text/image/document/audio interfaces and measured adapters.
- **Dependencies:** Gateway, Project trust, storage, model/tool lifecycle.
- **Deliverables:** parser/chunker/embedding/index/retriever/context; OCR/STT/TTS/image adapters incrementally.
- **Architecture:** large media dependencies outside core.
- **Security/privacy:** explicit ingestion/deletion/export; malicious media; local default.
- **Tests:** quality sets, parser fuzz, index migration, memory/storage, cancellation.
- **Documentation:** model/data licenses, provenance, limitations.
- **Acceptance:** measured quality and no automatic whole-project ingestion.
- **Rollback/migration:** index is disposable/rebuildable; source documents untouched.

## Phase 11 — reliability and performance

- **Objective:** recovery, storage-pressure, benchmark and thermal systems.
- **Current:** PARTIAL metrics/preflight/retained model copy.
- **Target:** snapshots, restore/integrity/encryption option, process recovery, quotas/eviction, measured center.
- **Dependencies:** all stateful prior phases.
- **Deliverables:** recovery manager, backup format, resource policy, benchmark protocol/UI.
- **Architecture:** content-free telemetry remains local.
- **Security/privacy:** encrypted backup option, key lifecycle, safe deletion, no fabricated/uploaded metrics.
- **Tests:** app kill/reboot/interruption/storage full/thermal/battery/corruption.
- **Documentation:** recovery/performance evidence and runbooks.
- **Acceptance:** verified restore and graceful resource degradation.
- **Rollback/migration:** versioned backups and fallback readers.

## Phase 12 — Plugin SDK and manager

- **Objective:** safe extensibility.
- **Current:** PARTIAL contract only.
- **Target:** signed packages, permissions, lifecycle, commands/UI/tools, compatibility, isolation, audit, rollback.
- **Dependencies:** Project trust, Gateway/tools, task center, security/supply chain.
- **Deliverables:** SDK docs/tools, package format, manager, one reference plugin.
- **Architecture:** plugins use capabilities, never raw authority.
- **Security/privacy:** deny default, trust roots, integrity, sandbox, data scopes.
- **Tests:** malicious package/schema/crash/update/rollback/compatibility.
- **Documentation:** full SDK/manifest/permission/lifecycle guides.
- **Acceptance:** plugin cannot bypass network/tool/project policy.
- **Rollback/migration:** disable/quarantine and prior-version restore.

## Phase 13 — creative studios

- **Objective:** Vector, Paint, 3D and later media tools as independent modules.
- **Current:** MISSING.
- **Target:** project-integrated creative workflows without core destabilization.
- **Dependencies:** stable Project/plugin/storage/GPU/recovery architecture.
- **Deliverables:** one studio at a time with real file/render/edit operations.
- **Architecture:** independent feature modules/adapters.
- **Security/privacy:** untrusted file formats/assets; local default.
- **Tests:** parser fuzz, autosave/restore, memory/GPU, accessibility.
- **Documentation:** formats/licenses/workflows/limits.
- **Acceptance:** production behavior, no placeholders presented as capability.
- **Rollback/migration:** versioned documents and autosave recovery.

## Phase 14 — remote and ecosystem

- **Objective:** remote development, remote AI management, remote providers, local API/integrations.
- **Current:** MISSING.
- **Target:** explicit authenticated/encrypted endpoint profiles and revocable integrations.
- **Dependencies:** Gateway, Project trust, secrets, localhost/API security, incident response.
- **Deliverables:** remote connection model, manager, audit, user controls.
- **Architecture:** local/loopback remains default; remote adapters isolated.
- **Security/privacy:** TLS/auth/secret references/endpoint trust/egress disclosure/revocation.
- **Tests:** MITM/auth failure, egress assertions, reconnect, revocation, rate limits, kill switch.
- **Documentation:** exact data flows and troubleshooting.
- **Acceptance:** no silent cloud/LAN fallback or hidden upload.
- **Rollback/migration:** delete/revoke endpoint credentials and return to fully local operation.


---

# Appendix A — Existing implementation backlog retained from the pre-directive roadmap

This appendix preserves the repository’s useful implementation-level backlog. Its old phase labels are subordinate to the Phase 0–14 development order above. Completion boxes remain historical/source-planning evidence and do not override the audited feature statuses.

## Legacy backlog: platform foundation

- [x] GitHub-only Android/NDK/CMake build
- [x] source-only 128 MB policy
- [x] Compose three-mode UX and Bangla resources
- [x] accessibility tree/actions/screenshot
- [x] Shizuku state and structured elevated operations
- [x] Hugging Face GGUF store
- [x] C++ backend/session ABI
- [x] Bangla OCR interface/schema and honest placeholder
- [x] architecture, safety, build, and test documentation
- [x] first GitHub Actions green build and resulting fixes ([run 31917533925](https://github.com/soobujmiah/lai/actions/runs/31917533925))
- [x] physical Snapdragon 8s Gen 4 smoke evidence ([Redmi Turbo 4 Pro result](device-results/2026-08-16-redmi-turbo-4-pro.md))

## Backbone hardening — completed

- [x] fourteen-module core/platform/runtime/plugin architecture including private audit ownership
- [x] zero-egress network ownership and mandatory artifact digest
- [x] architecture boundary source gate
- [x] pure-JVM coverage ratchets
- [x] evidence/thermal/battery/memory scheduler contract
- [x] opaque adapter-owned backend IDs and vendor-neutral compatibility policy
- [x] Android/SoC `DeviceProfile` with adapter-reported capabilities
- [x] CI boundary preventing vendor terminology in generic inference/scheduler source
- [x] versioned local-only plugin API
- [x] NpuHub comparison and ADR
- [x] one local-first application and upgrade path
- [x] reviewed local GGUF import through Android file picker
- [x] uninstall-safe user-owned GGUF export with destination hash verification
- [x] signed web supported-model catalog with verified offline fallback
- [x] artifact/backend/ABI compatibility metadata and signed-catalog downgrade prevention

## Legacy backlog: usable local intelligence

- [x] pin/fetch llama.cpp in CI
- [x] CPU model load and local generation on Snapdragon device
- [ ] Stop/recovery and UTF-safe token streaming device validation
- [ ] multi-turn history validated basically; context-trimming stress test pending
- [ ] Rolling Context Window v1: preserve system/tool constraints, recent turns and explicit summary checkpoints under a measured token budget; expose trim/summarization events and never silently claim full recall
- [x] native TTFT/prefill/decode metrics device validation
- [ ] GGML Vulkan build and Adreno capability self-test
- [x] model selection and explicit load/unload
- [x] conservative pre-load memory estimator and low-memory refusal
- [x] one-tap immutable reviewed baseline catalog
- [x] scheduler wiring for memory/battery/thermal-aware model load
- [ ] dual UX shell: Standalone Tools Dashboard plus Unified Chat **+ Attach Tools** menu over shared contracts
- [ ] typed per-tool settings schema, user-owned `config/settings.json`, and contextual Chat ⚙ quick-settings bottom sheet
- [ ] categorized Model Center with foreground-capable background download, pause/resume/cancel, process recovery, and manual `.gguf` import
- [ ] opt-in `/sdcard/LAI/` SAF workspace, bounded settings restore, and SHA-256 `.gguf` auto-discovery without broad storage permission
- [ ] Bangla system prompt/templates and evaluation pack
- [x] parse complete LLM-proposed JSON tool calls with strict per-tool schemas and dispatch revalidation
- [x] opt-in one-shot trusted confirmation dialog (build verified; v0.8.0 model emitted no valid proposal)
- [x] bounded app-private hash-chained audit, approval-before-execution and exact-call replay guard
- [x] privacy-safe proposal outcome/rejection counters (run 31958557120)
- [ ] Qwen exact-format compliance retest
- [ ] foreground/screen binding, result provenance and tool-result feedback before any autonomous multi-step loop
- [ ] select and integrate printed Bangla OCR baseline
- [ ] benchmark profile hidden in Developer Mode (implementation candidate added)

## Legacy backlog: Snapdragon specialization

- [ ] license-compliant QAIRT SDK CI acquisition
- [ ] QNN model conversion/calibration workflow
- [ ] HTP context manifest/cache
- [ ] dedicated QNN runtime adapter and explicit fallback (outside `runtime:llama`)
- [ ] Adreno/HTP/CPU routing under thermal/memory policy
- [ ] closed-loop Thermal/Battery Safety Throttling: dynamic thread/batch/backend admission, Android thermal callbacks, low-battery/charging policy, cooldown/hysteresis, and visible fallback reason
- [ ] native C++ bounded `TaskGraph` for memory-admitted LLM + Embedding/Whisper micro-model chaining; parallelize only when aggregate weights/KV/workspaces/buffers fit
- [ ] qualify 3B–5B LLM task chains without changing or removing the current Qwen 1.5B correctness pipeline
- [ ] printed and handwritten Bangla OCR QNN acceleration
- [ ] Snapdragon 8s Gen 4 performance/accuracy report

## Legacy backlog: advanced local tools and plugin ecosystem

- [ ] signed capability-based plugin manifest
- [ ] local RAG vector store abstraction
- [ ] Encrypted Vector DB using SQLCipher, Android Keystore-wrapped key material, versioned embedding/model metadata, bounded migrations, and explicit index deletion/export controls
- [ ] Bangla/English STT with streaming Whisper-class micro-model adapter
- [ ] Parallel Streaming TTS with Barge-In: sentence/phrase chunks synthesize while LLM output continues; Interrupt VAD stops queued PCM, active TTS, and generation with bounded latency and echo-suppression policy
- [ ] Shizuku System Automation v1: scoped user recipes over the existing argv allowlist, per-step confirmation/foreground binding, service-death recovery, persistent audit, loop/time limits, and global stop
- [ ] scoped user-created automation recipe import/export with capability and permission review
- [ ] Standalone OCR, Voice, Image Generation, and Vector Search tool surfaces reach feature parity with their Chat attachments

## Later — progressive vendor expansion

- [ ] introduce a backend manager only when a second real runtime provider exists
- [ ] evaluate the next vendor against measured product demand and available SDK/licensing
- [ ] add one isolated, physically validated adapter at a time
- [ ] compare correctness/performance with the Snapdragon and CPU baselines

No phase advances based solely on successful compilation. Device correctness, privacy, safety, and documentation are release gates. Empty vendor adapters are not roadmap progress.
