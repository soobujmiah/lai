# LAI AI Assistant Contract

## Authority order

1. `PROJECT_STATE.md` — canonical current-state/handoff snapshot.
2. `docs/ROADMAP.md` — canonical phase roadmap.
3. Accepted ADRs and architecture documentation — architectural authority.
4. Source code, tests, CI results, and physical-device evidence — implementation/verification authority.
5. README and other narrative docs — orientation; never stronger than direct evidence.
6. SKB global standards — engineering baseline; do not override LAI-specific rules.

## Session start

Before changing LAI:

1. Read this file and `PROJECT_STATE.md`.
2. Read the relevant roadmap phase and ADRs.
3. Inspect current `main` HEAD and recent commits.
4. Determine what is implemented, build-verified, device-validated, scaffolded, pending, or blocked.
5. Establish a session contract: target milestone, files expected to change, required tests, device validation, risks, and deferred scope.

## Evidence rules

Use the repository's vocabulary exactly:

- **Implemented** — source exists.
- **Build verified** — CI compiles/tests it.
- **Device validated** — observed on the target physical device.
- **Scaffold** — an honest unavailable boundary exists.
- **Pending/Planned** — not built.

Never upgrade a status without direct evidence. In particular, Vulkan/GPU/NPU/performance claims require device evidence; CPU or build success does not imply accelerator qualification.

## Engineering rules

- Preserve LAI's offline-first/privacy-first boundaries and explicit user authority model.
- Do not weaken consent, tool-policy, audit, zero-egress, secret-redaction, or model-catalog controls merely to make a feature easier.
- Do not commit credentials, private keys, model binaries, proprietary SDKs, or generated secrets.
- Respect the existing 16-module architecture and its documented dependency direction unless an architectural change is justified by an ADR.
- Keep unavailable capabilities honest; use typed seams/placeholders rather than pretending a model/backend exists.
- Preserve licensing/provenance records for llama.cpp, Vulkan/OpenCL/QNN-related dependencies and models.

## Implementation workflow

`read → inspect HEAD → contract → implement smallest coherent change → run relevant CI/tests → collect evidence → update PROJECT_STATE/phase docs → review diff → commit → handoff`

This environment is GitHub-centric. Do not claim local Android/device execution unless actual evidence exists. Builds and CI are authoritative for build verification; the Redmi Turbo 4 Pro is authoritative for physical-device validation.

## Session close — mandatory

Before ending a substantial session:

1. Review `git diff`/GitHub changes and every changed file.
2. Run or trigger the relevant tests/checks and record actual results.
3. Update `PROJECT_STATE.md` and relevant phase/architecture documentation.
4. Record failures, limitations, deferred work, and exact reproduction/verification steps.
5. Commit/push completed work when authorized.
6. Record the resulting commit SHA for substantial milestones.
7. Leave no unexplained dirty changes. If intentionally uncommitted, document why.

## Model switching / recovery

Chat history is not durable memory. Before a risky operation or model switch, create a named snapshot branch from the known-good HEAD. The next AI must reconstruct state from GitHub, this contract, `PROJECT_STATE.md`, roadmap/ADRs, tests/CI and device evidence — never from another model's claims alone.

## Scope discipline

Do not fix unrelated issues during a focused task. Separate implementation, documentation, audit, and research work. If a finding is outside the current milestone, document it as deferred instead of silently expanding scope.
