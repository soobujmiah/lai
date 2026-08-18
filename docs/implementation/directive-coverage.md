# Master directive documentation coverage

Last reviewed: 2026-08-17

The master directive says to adapt its recommended documentation tree and **not create meaningless placeholders**. This matrix records which requested subjects have substantive documents, which are consolidated into an existing canonical document, and which must be created before their future implementation phase.

## Immediate Task 1 deliverables

| Required path | Coverage |
|---|---|
| `docs/implementation/current-state.md` | Complete source audit |
| `docs/architecture/overview.md` | Complete overview |
| `docs/architecture/module-map.md` | Complete 16-module map/dependency graph |
| `docs/architecture/ai-architecture.md` | Current AI architecture and explicit missing targets |
| `docs/architecture/agent-architecture.md` | Current one-shot architecture and multi-step boundary |
| `docs/architecture/security-architecture.md` | Current trust/security architecture and gaps |
| `docs/product/feature-matrix.md` | Complete target/current capability matrix |
| `docs/product/roadmap.md` | Compatibility path to canonical `docs/ROADMAP.md` |
| `docs/implementation/implementation-plan.md` | Phase 0–14 implementation sequence |
| `docs/implementation/testing-plan.md` | Cross-phase testing/evidence plan |
| `docs/decisions/README.md` | ADR policy, existing decisions, next required ADRs |

## Architecture tree coverage

| Recommended subject | Current canonical coverage | Action |
|---|---|---|
| architecture overview/system/runtime | `architecture/overview.md`, `ARCHITECTURE.md` | Maintain with source |
| module/dependency map | `architecture/module-map.md`, `MODULES.md` | Maintain with Gradle graph |
| AI architecture | `architecture/ai-architecture.md`, `MODELS_AND_BACKENDS.md` | Add Gateway/server specs before code |
| agent/automation | `architecture/agent-architecture.md`, `AUTOMATION_TOOLS.md` | Add task/rollback specs before Phase 5/6 |
| storage architecture | `ARCHITECTURE.md`, current-state workspace section | Create dedicated document before Project/storage redesign |
| networking architecture | `security-architecture.md`, `PRIVACY_INVARIANTS.md` | Create provider/localhost network spec in Phase 1 |
| security architecture | `architecture/security-architecture.md`, `SECURITY_AND_SAFETY.md` | Current |
| plugin architecture | `architecture/plugin-architecture.md` | Expand before Plugin Manager |

## Product and implementation coverage

| Recommended subject | Current canonical coverage | Action |
|---|---|---|
| vision | root `README.md` product principles and master roadmap | Dedicated vision only when reviewed product requirements add value |
| roadmap | `ROADMAP.md` | Canonical Phase 0–14 plus retained backlog |
| feature/capability matrix | `product/feature-matrix.md`, `STATUS.md` | Current |
| definition of done | master directive checklist reproduced as implementation phase gate | Create dedicated checklist when release workflow consumes it |
| current state | `implementation/current-state.md` | Current |
| implementation/phase plan | `implementation/implementation-plan.md`, `ROADMAP.md` | Current |
| testing plan | `implementation/testing-plan.md`, `DEVICE_TESTING.md` | Current |
| migration plan | phase-specific sections in roadmap/ADRs | Dedicated plans required for stateful migrations, not empty global file |

## AI, agent, workstation, and studio coverage

| Area | Current state | Documentation action |
|---|---|---|
| AI Gateway | MISSING | ADR and `docs/ai/ai-gateway.md` required before Phase 2 code |
| localhost server | MISSING | threat model and `docs/ai/localhost-server.md` required before Phase 3 code |
| remote providers | MISSING | provider/data-flow docs required before Phase 14 |
| RAG/multimodal | MISSING/PARTIAL OCR | dedicated docs required before Phase 10 |
| multi-step agent/task center | MISSING | task lifecycle/permissions/safety/rollback docs required before Phase 5/6 |
| Project/workstation/editor/terminal/Git/build/Linux | MISSING | dedicated workstation docs required during Phase 7–9 architecture, before code |
| Vector/Paint/3D Studio | MISSING | dedicated studio docs required before Phase 13, not placeholders now |

## Security, platform, plugins, legal, development, and user coverage

| Area | Existing coverage | Missing document gate |
|---|---|---|
| threat/security/network/audit/secrets | `security-architecture.md`, `SECURITY_AND_SAFETY.md`, `PRIVACY_INVARIANTS.md`, ADR 0007 | project trust, localhost, secrets, incident response before affected phases |
| device/performance/thermal/storage/recovery | `DEVICE_TESTING.md`, device evidence, `ARCHITECTURE.md`, current-state/roadmap | dedicated recovery/performance docs before Phase 11 |
| plugin SDK/manifest/permissions/lifecycle | `plugin-architecture.md`, source API | full SDK/lifecycle docs before Phase 12 |
| licensing/third-party/models/distribution | `THIRD_PARTY_NOTICES.md`, `THIRD_PARTY_LICENSES.md`, `MODEL_LICENSES.md`, `legal/licensing.md` | generate exact resolved notices/SBOM before production |
| setup/coding/testing/CI/release | `CONTRIBUTING.md`, `BUILD_AND_RELEASE.md`, testing plan, workflows | split only when content grows; no empty duplicates |
| getting started/AI/models/automation/privacy/security/troubleshooting | root README and subsystem docs are developer-heavy | user guides must be written alongside the corresponding production-ready surface |

## Roadmap feature coverage

All 26 target capabilities from the directive are represented in `product/feature-matrix.md` and assigned to the canonical Phase 0–14 sequence in `ROADMAP.md`, including:

- AI Gateway, localhost server, multi-backend inference;
- agent runtime, task center, diff and rollback;
- Project, editor, terminal, Git, build center, Linux;
- RAG, multimodal, Project Trust, backup/recovery, storage pressure, performance;
- Vector/Paint/3D Studio, Plugin SDK/Manager;
- remote development, remote AI management, and local API/integrations.

## Rule for adding the remaining recommended files

A missing recommended path is not forgotten merely because it has no placeholder. It is a documentation gate attached to its phase above. Before implementation begins, the path must contain reviewed purpose, responsibilities, interfaces, dependencies, lifecycle, data flow, security boundaries, failure behavior, testing strategy, extension strategy, and any required ADR.
