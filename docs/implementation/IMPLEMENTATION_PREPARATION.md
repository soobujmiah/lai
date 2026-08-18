# Implementation preparation

**Status:** documentation-driven implementation order  
**Date:** 2026-08-18  
**Code authorization:** none. This file does not authorize implementation.  
**Does not replace:** [`../ROADMAP.md`](../ROADMAP.md) (canonical product Phase 0–14), [`implementation-plan.md`](implementation-plan.md) (2026-08-17 plan), [`current-state.md`](current-state.md)

This document is the **implementation-order overlay**. It tells a future engineering agent what to build next, in what sequence, and what must not be touched without approval. Product vision remains in [`../ROADMAP.md`](../ROADMAP.md) and [`../MASTER_ROADMAP.md`](../MASTER_ROADMAP.md). Legal/IP remains in [`../legal/COMMERCIAL_IP_POLICY.md`](../legal/COMMERCIAL_IP_POLICY.md).

Governing sequence (already in force):

```text
DOCUMENTATION FIRST → ARCHITECTURE REVIEW → LICENSING/IP REVIEW
→ IMPLEMENTATION → TESTING → RELEASE REVIEW
```

---

## What LAI is

LAI is a Bangla-first, privacy-first, local-first Android Personal AI orchestrator and automation runtime. Current source is one application (`dev.lai.runtime`) that runs on-device inference (llama.cpp CPU, Qwen 2.5 1.5B Instruct Q4_K_M as the reviewed catalog artifact), one-shot confirmed tools, Accessibility and Shizuku automation, signed model catalog download, and a SAF workspace. Cloud, multi-step agents, RAG, OCR engines, and commercial entitlement are **not** implemented.

Public GitHub visibility is a development fact. It is not commercial entitlement. Published code is Apache-2.0 (IP-001). Future premium modules require review before code (IP-002).

---

## What already exists (source-verified, 2026-08-18)

Reconciled from the working tree, not solely from [`current-state.md`](current-state.md) (that audit is dated 2026-08-17 and is **stale** in several rows — see Gaps).

| Area | Source state | Evidence |
|---|---|---|
| Gradle graph | **16 modules** | `settings.gradle.kts` includes `platform:history` in addition to the 15 listed in the 2026-08-17 module map |
| Core | IMPLEMENTED | contracts, policy (parser, audit ledger, local-first, shell, settings, context window), scheduler (memory + `ThermalGovernorPolicy`), model catalog |
| Platform | IMPLEMENTED | download (OkHttp, WorkManager, bounded copy), audit, device, accessibility, workspace, shizuku, **history** |
| Runtime | PARTIAL | llama CPU real; Vulkan compile path in-flight; OCR placeholder; orchestrator one-shot |
| App | PARTIAL | Compose Chat / Screen Reader / Automator; `QuickSettingsSheet`; `ToolsDashboard`; `WorkspaceSettingsCoordinator`; large `MainViewModel` |
| Tests | PARTIAL | 30 `*Test.kt` files including app coordinator, history, download bounds, thermal, context window |
| Device | PARTIAL | SM8735 CPU chat measured; Vulkan generate and broad tool-compliance still open |
| Cloud / Gateway / RAG / plugins host / entitlement | MISSING | documented only |

**Free-foundation candidates already in source:** local CPU chat, one-shot confirmed tools, basic Accessibility/Shizuku, catalog download/import, workspace, diagnostics. Exact FREE set remains **DECISION REQUIRED** (IP-006). These capabilities are treated as the evaluation foundation until that decision is recorded.

**Premium / proprietary:** none in source. Candidates listed in [`../legal/PROPRIETARY_BOUNDARIES.md`](../legal/PROPRIETARY_BOUNDARIES.md).

**Third-party:** AndroidX, Kotlin, OkHttp, WorkManager, Shizuku, CI-fetched llama.cpp (MIT). Intake: [`../legal/THIRD_PARTY_INTAKE.md`](../legal/THIRD_PARTY_INTAKE.md).

---

## What must never change without explicit approval

| Item | Why |
|---|---|
| [`../../LICENSE`](../../LICENSE) | IP-001 |
| Historical Apache-2.0 tags / published grants | Cannot be revoked by documentation |
| Local-first egress invariants | [`../PRIVACY_INVARIANTS.md`](../PRIVACY_INVARIANTS.md) |
| Network only in `platform:download` | Architecture gate |
| Accessibility / Shizuku / JNI isolation | Authority boundary |
| No raw shell / no model-authored confirmation | Safety |
| No unknown-license dependency | Fail closed |
| No vendor types in `core` | ADR 0005 |
| No empty vendor adapters | Engineering rule |
| No entitlement/DRM/payment until IP-006 + ADR-0105 accepted | Commercial policy |
| No NpuHub private-source copy | Provenance |
| Single application ID `dev.lai.runtime` | ADR 0004 |

---

## Documentation vs implementation gaps

| Topic | Documentation says | Source now | Action |
|---|---|---|---|
| Module count | 15 modules (`current-state`, `module-map`) | 16 (`platform:history`) | Refresh those audits before Phase 1 code |
| Toolchain | Kotlin 2.1.21, AGP 8.11, Gradle 8.13, API 35 | Kotlin 2.4.10, AGP 9.3.1, Gradle 9.5.0, API 36 | Refresh current-state / BUILD_AND_RELEASE |
| Settings UI | “not implemented” in current-state | `QuickSettingsSheet`, coordinator + tests | Mark PARTIAL/IMPLEMENTED honestly |
| Tools Dashboard | PLANNED in current-state | `ToolsDashboard.kt` exists | Refresh status |
| Context window | Planned in some STATUS rows | `ContextWindowPolicy` + tests | Refresh status |
| Thermal governor | Planned closed-loop | `ThermalGovernorPolicy` + tests | Refresh status |
| WorkManager downloads | Planned process recovery | `ModelDownloadWorker` present | Refresh; process-death still PARTIAL |
| App tests | “no app test source” | `WorkspaceSettingsCoordinatorTest` | Refresh |
| Download tests | module-map “no direct test” | `BoundedStreamCopyTest` | Refresh |
| Vulkan | Scaffold vs in-flight `GGML_VULKAN=ON` | CMake option ON when CI fetches llama | Device-qualify; do not claim MEASURED |
| Two Phase 0–14 maps | `ROADMAP.md` vs `implementation-plan.md` vs this overlay | Three different phase lists | This file is **implementation order**; `ROADMAP.md` stays product-canonical |
| Legal tracking | PARTIAL | Registers stale vs catalog | Not a code blocker for free-core hardening |

These contradictions are **documentation debt**. They block honest Phase 0 exit, not the existence of the CPU path.

---

## Feature dependencies (implementation order)

```text
Repo / docs baseline (Phase 0)
        ↓
Existing core honesty + reliability (Phase 1)
        ↓
Inference abstraction kept honest (Phase 2) ── existing InferenceEngine
        ↓
Model management hardening (Phase 3)
        ↓
Agent runtime multi-step (Phase 4) ── requires Phase 5 tools + Phase 6 consent unchanged
        ↓
Tool-calling expansion (Phase 5)
        ↓
Permission/consent remains the gate (Phase 6) ── already IMPLEMENTED for one-shot
        ↓
Android automation recipes (Phase 7) ── after consent + audit
        ↓
Knowledge / RAG (Phase 8) ── after model + project/workspace honesty; model licenses
        ↓
Hybrid local/cloud (Phase 9) ── ADR + privacy exception; not before Phase 2 gateway
        ↓
Developer / code-agent (Phase 10) ── Project domain first (missing)
        ↓
Premium boundaries (Phase 11) ── IP-003/005/006; no code before review
        ↓
Advanced modules (Phase 12) ── OCR engine, graph, plugins host
        ↓
Production hardening (Phase 13)
        ↓
Release / commercial readiness (Phase 14)
```

Later phases must not be started because they appear in product vision docs.

---

## Recommended first implementation milestone

**M1 — Free-core reliability and source-of-truth alignment**

This is the smallest safe next step. It does not add dependencies, models, cloud, entitlement, QNN, RAG, or a second license.

| Item | Type | Why first |
|---|---|---|
| Refresh `current-state.md` and `architecture/module-map.md` to 16 modules, live toolchain, and the classes that already exist | Documentation | Removes false “MISSING/PLANNED” that would cause an agent to reimplement working code |
| Atomic SAF settings write (known last-valid-generation loss on rename failure) | Code, existing module | Data-loss bug in free foundation; no new architecture |
| Keep download/history/thermal/context tests green; add only tests that lock current contracts | Test | Prevents regression while `MainViewModel` stays large |
| No Vulkan claim, no QNN, no Gateway, no cloud adapter | Constraint | Those need ADRs / licenses / device evidence |

**Exit for M1:** docs match the 16-module tree; settings persist without silent loss; CI `validate_repo.sh` + `coverageCheck` + lint still pass; no new Maven/native dependency; no privacy invariant change.

**M2 (after M1, still not started now):** device-qualified Vulkan `generate()` behind existing backend IDs, fail closed if `libvulkan.so` / evidence is missing. Requires named-device test. Does not require IP-003–IP-006.

`DEVELOPMENT_STATE.md` still lists an immediate **device walkthrough** (auto-import + `hi` + logcat). That is validation, not a new implementation phase.

---

## Phase 0 — Repository / documentation baseline

- **Objective:** A future agent can answer what exists, what is planned, what is free/premium/third-party, and what must not change, without asking those questions again.
- **Prerequisites:** None. Largely complete after the legal/IP documentation set.
- **Components:** `docs/` tree, root registers, `LICENSE`, this file.
- **Dependencies:** None.
- **Interfaces/contracts:** Documentation indexes in [`../README.md`](../README.md) and [`../legal/README.md`](../legal/README.md).
- **Security:** Secret policy remains; no keys in Git.
- **Privacy:** Invariants documented; no new egress.
- **Licensing/IP:** IP-001/IP-002 accepted; IP-003–007 **DECISION REQUIRED**. Audit is informational.
- **Free/premium:** Classification sketch only; no locks.
- **Testing:** `scripts/validate_documentation.py`, `validate_repo.sh`.
- **Completion criteria:** Canonical legal set present; implementation order recorded; stale current-state **identified** (full rewrite of current-state may complete in M1).
- **Next-phase dependency:** Phase 1 must not start new product surfaces until M1 doc refresh is done.

**Status:** PARTIAL (legal set + this overlay exist; M1 documentation alignment of current-state/module-map is done in the M1 change).

---

## Phase 1 — Core foundation

- **Objective:** Keep the existing 16-module local-first foundation correct, bounded, and honestly documented.
- **Prerequisites:** Phase 0 enough to avoid reimplementing existing types.
- **Components:** `core:*`, `platform:*` except new authority, `app` composition, `runtime:llama` CPU path.
- **Dependencies:** No new libraries.
- **Interfaces:** Existing `InferenceEngine`, `LocalFirstPolicy`, `Workspace*`, `ChatHistoryRepository`.
- **Security:** Do not widen permissions; do not add INTERNET outside download.
- **Privacy:** Chat history stays no-backup, local, content-bearing but non-exported.
- **Licensing/IP:** Work stays Apache-2.0 public core.
- **Free/premium:** Free-foundation hardening.
- **Testing:** Existing coverage ratchets; add tests only for repaired contracts.
- **Completion criteria:** M1 exit; CPU path still device-capable; `MainViewModel` not required to be split yet (split is optional later, not a gate).
- **Next-phase dependency:** Phase 2 builds on current `InferenceEngine`, not a rewrite.

**Status:** MOSTLY IMPLEMENTED; M1 remaining.

---

## Phase 2 — AI inference abstraction

- **Objective:** Keep provider-neutral contracts honest. Optional later Gateway is a **new ADR**, not a rewrite of working CPU chat.
- **Prerequisites:** Phase 1 M1. Gateway ADR still **not accepted** ([`../decisions/README.md`](../decisions/README.md) candidate list).
- **Components:** `core:contracts` inference types, `runtime:llama`, future adapters only.
- **Dependencies:** llama.cpp pin already in CI; Vulkan packages on CI only.
- **Interfaces:** `InferenceEngine`, opaque `BackendId`, scheduler evidence `AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN`.
- **Security:** Native memory/cancel; no provider SDK in UI.
- **Privacy:** Prompts stay on device.
- **Licensing/IP:** llama.cpp MIT notice when `.so` ships; QNN blocked until vendor review.
- **Free/premium:** Local CPU remains foundation; vendor NPU pack is a premium *candidate*.
- **Testing:** Native CI; named-device metrics; no fabricated tok/s.
- **Completion criteria:** CPU path unchanged; any new backend has truthful probes.
- **Next-phase dependency:** Model management uses the same engine.

**Status:** PARTIAL (CPU IMPLEMENTED; Gateway MISSING; Vulkan PARTIAL).

**No-go:** Do not implement Gateway or localhost server in M1. Those remain design-first (existing implementation-plan Phases 2–3).

---

## Phase 3 — Model management

- **Objective:** Recoverable catalog, download, import, export, discovery, deletion.
- **Prerequisites:** Phase 1 storage honesty.
- **Components:** `core:model`, `platform:download`, `platform:workspace`, catalog JSON + public key.
- **Dependencies:** OkHttp, WorkManager (already declared).
- **Interfaces:** `ReviewedModel`, `ModelRepository`, `RemoteModelCatalogRepository`, `WorkspaceDiscovery`.
- **Security:** SHA-256 before activate; reviewed hosts only.
- **Privacy:** No prompt/model-path leak in diagnostics.
- **Licensing/IP:** [`../legal/MODEL_IP_POLICY.md`](../legal/MODEL_IP_POLICY.md). Do not bundle weights. Unknown GGUF stays `LOCAL_UNREVIEWED`.
- **Free/premium:** User-owned models are not a LAI SKU.
- **Testing:** Range resume, bound copy (exists), process death, install `-r`.
- **Completion criteria:** Pause/resume/cancel survive process death; registry writes are recoverable; no 8 GiB/policy bypass.
- **Next-phase dependency:** Agents load models only through this path.

**Status:** PARTIAL (download/import/Keep copy IMPLEMENTED; process-death PARTIAL).

---

## Phase 4 — Agent runtime

- **Objective:** Bounded multi-step plan/execute/verify **after** one-shot remains the safe default.
- **Prerequisites:** Task/checkpoint ADR (not accepted). Phase 5 parser and Phase 6 consent must not weaken.
- **Components:** `runtime:orchestrator`, `core:policy`, future task store.
- **Dependencies:** None new for design; no cloud required.
- **Interfaces:** New task contracts; existing `AgentRuntime` one-shot preserved as fallback.
- **Security:** No model-authored confirmation; hard stop; loop/time/resource limits.
- **Privacy:** Content-minimized task history; audit stays content-free.
- **Licensing/IP:** Public core unless a later premium “advanced agent” split is reviewed (IP-002).
- **Free/premium:** One-shot = foundation candidate; multi-step = PRO candidate (IP-006).
- **Testing:** Adversarial loops, restart, service death, replay.
- **Completion criteria:** Inspectable trace; one-shot still works.
- **Next-phase dependency:** Tools and permissions are inputs, not afterthoughts.

**Status:** MISSING (one-shot only).

---

## Phase 5 — Tool-calling system

- **Objective:** Keep strict JSON tools; expand only with schema + confirmation + audit.
- **Prerequisites:** Existing `ToolCallParser`, `BuiltInToolCatalog`, `ToolInstructionGate`.
- **Components:** `core:policy`, `runtime:orchestrator`, UI review dialog.
- **Dependencies:** None.
- **Interfaces:** Exact envelope; per-tool schemas; second dispatch validation.
- **Security:** Injection, unknown fields, shell argv mismatch rejected.
- **Privacy:** Arguments not persisted in audit.
- **Licensing/IP:** Public core.
- **Free/premium:** Built-in one-shot tools = foundation; recipe packs = later premium candidate.
- **Testing:** Parser fuzz already partial; Qwen compliance retest on device.
- **Completion criteria:** No bypass; device shows valid or honest non-proposal.
- **Next-phase dependency:** Phase 4 multi-step consumes the same tools.

**Status:** IMPLEMENTED for one-shot; model-format compliance PARTIAL.

---

## Phase 6 — Permission / consent system

- **Objective:** Approval-before-authority remains universal.
- **Prerequisites:** Existing `AgentPolicy`, `ToolAuditRepository`, confirmation UI.
- **Components:** `core:policy`, `platform:audit`, Compose dialog.
- **Dependencies:** None.
- **Interfaces:** `recordDecision` → fsync → `execute(userConfirmed=true)` → `recordCompletion`.
- **Security:** Replay guard; hash chain; no confirmation in model text.
- **Privacy:** Content-free ledger.
- **Licensing/IP:** Public core. Entitlement is **not** this phase (Phase 11).
- **Free/premium:** Consent is not a paid feature.
- **Testing:** Android audit file tests exist; device confirmation still needed.
- **Completion criteria:** Every new consequential tool uses this gate.
- **Next-phase dependency:** Automation (Phase 7) may not add a side door.

**Status:** IMPLEMENTED for current tools.

---

## Phase 7 — Android automation

- **Objective:** Reliable Accessibility + Shizuku recipes without a raw terminal.
- **Prerequisites:** Phase 6. Foreground/provenance binding design.
- **Components:** `platform:accessibility`, `platform:shizuku`, orchestrator.
- **Dependencies:** Shizuku API 13.1.5 (already). Extra Shizuku branding terms: **LEGAL REVIEW REQUIRED** if UI copy changes.
- **Interfaces:** Typed automation commands; argv-only elevated ops.
- **Security:** UID/binder death; selector bounds; password omission.
- **Privacy:** Trees/screenshots local; not in diagnostics.
- **Licensing/IP:** Public core; recipe packs later.
- **Free/premium:** Basic actions = foundation; broad recipe library = candidate PRO.
- **Testing:** Physical device; service death; Xiaomi lock persistence (`install -r`).
- **Completion criteria:** Named recipes with confirmation; no `sh -c`.
- **Next-phase dependency:** None for RAG.

**Status:** PARTIAL (actions exist; recipes/recovery PARTIAL).

---

## Phase 8 — Knowledge / RAG system

- **Objective:** Local parser → chunk → index → retrieve → cite. Knowledge graph is **after** RAG (`features:knowledge`).
- **Prerequisites:** Model IP for embedder; Project/workspace honesty; no automatic whole-tree ingest.
- **Components:** future `core` RAG ports + feature module; not inside `runtime:llama`.
- **Dependencies:** Embedder/SQLCipher UNKNOWN until intake. Granite/Whisper/Tesseract **fail closed** until [`../legal/MODEL_IP_POLICY.md`](../legal/MODEL_IP_POLICY.md) rows exist.
- **Interfaces:** Retriever port; citation provenance.
- **Security:** Malicious documents; bounded parsers.
- **Privacy:** Local default; no upload.
- **Licensing/IP:** IP-002 before any proprietary RAG pack. NpuHub “port” is not a copy license.
- **Free/premium:** Local RAG is a PRO *candidate* (IP-006).
- **Testing:** Fixtures, deletion, index rebuild, measured quality or `UNEVALUATED`.
- **Completion criteria:** Explicit ingest; disposable index; source files untouched.
- **Next-phase dependency:** Graph UI must not start here.

**Status:** MISSING (OCR placeholder only).

---

## Phase 9 — Hybrid local / cloud AI

- **Objective:** Optional user-configured providers behind a Gateway; local remains default.
- **Prerequisites:** Accepted Gateway + remote-provider ADR; privacy exception documented; **LEGAL REVIEW REQUIRED** for each provider ToS.
- **Components:** New provider adapters; **not** `platform:download` for prompts.
- **Dependencies:** No provider SDK in `core`. Keys in Keystore only.
- **Interfaces:** Provider → Model → Capability → Router → Agent → Tool → Permission → Audit.
- **Security:** No silent LAN/cloud fallback; revoke endpoint.
- **Privacy:** Explicit consent; SENSITIVE stays local; documents never auto-upload.
- **Licensing/IP:** Service terms ≠ app relicense. BYO key vs LAI-hosted SKU is IP-006.
- **Free/premium:** Cloud routing is CLOUD / PRO candidate.
- **Testing:** 401 → visible local fallback; offline → local only; no key in logs.
- **Completion criteria:** Honest Local/Cloud/Remote badges; invariants otherwise intact.
- **Next-phase dependency:** Developer tools must not require cloud.

**Status:** MISSING. **Do not implement in M1–M2.**

---

## Phase 10 — Developer / code-agent capabilities

- **Objective:** Project domain, then files/editor/Git/terminal/build. Shizuku ≠ terminal. CI ≠ on-device build center.
- **Prerequisites:** Project-trust ADR; Phase 4–6; Linux/QEMU **LEGAL REVIEW REQUIRED** (GPL risk).
- **Components:** New feature modules; must not dump authority into `app`.
- **Dependencies:** Unknown until designed; intake required.
- **Interfaces:** Project filesystem/trust ports.
- **Security:** Untrusted projects; no signing keys in the app workspace.
- **Privacy:** Source local by default.
- **Licensing/IP:** Workstation pack is PRO candidate; QEMU combination **LEGAL REVIEW REQUIRED**.
- **Free/premium:** Not foundation.
- **Testing:** Traversal, kill, conflicts, accessibility.
- **Completion criteria:** Real operations only; no fake success.
- **Next-phase dependency:** Premium boundaries may wrap these packs.

**Status:** MISSING.

---

## Phase 11 — Premium / commercial boundaries

- **Objective:** Capability IDs and optional `EntitlementPort` **after** IP-006 and ADR-0105. No DRM in the first slice.
- **Prerequisites:** IP-003 (rights holder) before proprietary modules; IP-002 review per module; IP-006 mapping.
- **Components:** Policy-side port only; not Accessibility/Shizuku/JNI.
- **Dependencies:** None until designed. Signing keys never in public Git.
- **Interfaces:** Sketch in [`../architecture/ENTITLEMENT_ARCHITECTURE.md`](../architecture/ENTITLEMENT_ARCHITECTURE.md).
- **Security:** Fail open for documented free capabilities; fail closed for paid; no phone-home via download.
- **Privacy:** Entitlement metadata only.
- **Licensing/IP:** Does not revoke Apache-2.0. Official binaries may gate; public source remains rebuildable.
- **Free/premium:** This phase *implements* the mapping, it does not invent it.
- **Testing:** Offline cache, clock skew, missing adapter = UNKNOWN not “licensed”.
- **Completion criteria:** Free local chat works with zero account.
- **Next-phase dependency:** Advanced modules may attach capability IDs.

**Status:** DOCUMENTED ONLY. **DECISION REQUIRED.**

---

## Phase 12 — Advanced modules

- **Objective:** Licensed OCR engine, plugin host, knowledge graph, STT/TTS — one module at a time.
- **Prerequisites:** Phase 8 for graph; Tesseract/tessdata pins verified; plugin ADR; no core contamination.
- **Components:** `runtime:ocr` real adapter; `features:knowledge`; plugin manager.
- **Dependencies:** Intake for each artifact. Fail closed if UNKNOWN.
- **Interfaces:** Existing `OcrEngine`; plugin manifest already PARTIAL.
- **Security:** Untrusted plugins; deny-by-default.
- **Privacy:** Screenshots/OCR local.
- **Licensing/IP:** Model/dataset rows required. Graph premium possibility recorded, not decided.
- **Free/premium:** Printed OCR / graph / plugin marketplace are candidates, not FREE by default.
- **Testing:** CER/WER or `UNEVALUATED`; malicious plugin tests before host ships.
- **Completion criteria:** Placeholder never presented as recognition.
- **Next-phase dependency:** Hardening applies to whatever shipped.

**Status:** MISSING / OCR scaffold.

---

## Phase 13 — Production hardening

- **Objective:** Recovery, storage pressure, thermal/battery closed loop, native fuzz, supply-chain pins.
- **Prerequisites:** Stateful phases that exist; do not wait for Phase 12 to harden Phase 1.
- **Components:** Existing scheduler/device; future recovery manager.
- **Dependencies:** None required to start *partial* hardening in M1.
- **Interfaces:** Existing thermal policy; extend rather than replace.
- **Security:** Unkeyed audit limitation remains documented; no fake tamper-proofing.
- **Privacy:** Metrics content-free.
- **Licensing/IP:** SBOM/NOTICE work is compliance, not a relicense.
- **Free/premium:** Hardening is not a SKU.
- **Testing:** Kill, reboot, disk full, thermal, corrupt registry.
- **Completion criteria:** Named-device recovery evidence.
- **Next-phase dependency:** Release gates consume this evidence.

**Status:** PARTIAL (thermal policy and preflight exist).

---

## Phase 14 — Release / commercial readiness

- **Objective:** Production-signed artifact, NOTICE/SBOM, secret scan, no UNKNOWN shipped licenses.
- **Prerequisites:** [`../legal/RELEASE_COMPLIANCE.md`](../legal/RELEASE_COMPLIANCE.md) gates. IP-007 optional for store brand.
- **Components:** CI, signing secrets, NOTICE package replacing META-INF excludes.
- **Dependencies:** None new in source tree.
- **Interfaces:** Existing tag workflow.
- **Security:** Debug-signed APKs not labeled production.
- **Privacy:** Release notes must not imply weights are in the APK.
- **Licensing/IP:** Apache-2.0 + third-party notices; llama.cpp MIT if linked.
- **Free/premium:** SKU text must match IP-006 when that exists.
- **Testing:** Artifact inventory; `apkanalyzer` permissions; hash publication.
- **Completion criteria:** All 14 release gates pass or are explicitly waived in the IP register.
- **Next-phase dependency:** None in this overlay.

**Status:** PARTIAL (CI builds APKs; production NOTICE/SBOM MISSING).

---

## Blocking decisions

| ID / topic | Blocks | Status |
|---|---|---|
| IP-003 rights holder | Proprietary modules, assignment | **DECISION REQUIRED** |
| IP-004 CLA/DCO | Relicensing inbound patches | **DECISION REQUIRED** |
| IP-005 new-work license | Non-Apache publication of new files | **DECISION REQUIRED** |
| IP-006 FREE/paid map | Entitlement implementation (Phase 11) | **DECISION REQUIRED** |
| IP-007 trademark | Store enforcement | **DECISION REQUIRED** (optional) |
| Gateway / localhost / task / project-trust ADRs | Phases 2 (gateway), 4, 9, 10 | Proposed only / not written |
| OCR dataset/engine license | Phase 12 OCR | **DECISION REQUIRED** |
| QAIRT agreement | QNN adapter | UNKNOWN / blocked |
| Vulkan device qualification | M2 | Device test, not a legal decision |
| Stale current-state | Honest Phase 0 exit | Documentation fix in M1 |

None of the IP-003–007 items block **M1**.

---

## How a future agent starts implementation

1. Read [`../README.md`](../README.md) and this file.
2. Read [`../legal/OWNERSHIP_DECISIONS.md`](../legal/OWNERSHIP_DECISIONS.md).
3. Diff this overlay against source (`settings.gradle.kts`, module trees) — prefer source if docs are stale.
4. Implement **only M1** unless a later milestone is explicitly approved.
5. Update current-state, feature matrix, and this file in the same change.
6. Do not implement Phase 9–12 because they are written here.

---

## Related documents

| Role | File |
|---|---|
| Product Phase 0–14 | [`../ROADMAP.md`](../ROADMAP.md) |
| Older implementation plan | [`implementation-plan.md`](implementation-plan.md) |
| Stale source audit | [`current-state.md`](current-state.md) |
| Live continuity snapshot | [`../DEVELOPMENT_STATE.md`](../DEVELOPMENT_STATE.md) |
| Feature statuses | [`../product/feature-matrix.md`](../product/feature-matrix.md) |
| Definition of done | [`../product/definition-of-done.md`](../product/definition-of-done.md) |
