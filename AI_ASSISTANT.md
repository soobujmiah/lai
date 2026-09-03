# LAI AI Assistant Contract

## Authority order

1. `PROJECT_STATE.md` — canonical current-state/handoff snapshot.
2. `docs/ROADMAP.md` — canonical phase roadmap.
3. Accepted ADRs and architecture documentation — architectural authority.
4. Source code, tests, CI results, and physical-device evidence — implementation/verification authority.
5. README and other narrative docs — orientation; never stronger than direct evidence.
6. SKB global standards — engineering baseline; do not override LAI-specific rules.

## SKB knowledge continuity

LAI is connected to Sobuj's canonical knowledge base: `soobujmiah/skb`.

Before any non-trivial decision, use progressive discovery: LAI instructions/current state → SKB `AGENT_NAVIGATION.md` → minimum relevant SKB context → live LAI evidence.

A later session does not require the user to repeat “read SKB” merely because the project name was used. If SKB is unavailable, do not invent context; continue only from verified LAI evidence where safe.

At session close, perform a Knowledge Return Review. Identify durable LAI facts, decisions, architecture changes, constraints, capabilities, failures, or verification evidence that may need to be recorded or updated in SKB. Never return secrets, private payloads, temporary noise, or unsupported claims. SKB recommendations never override LAI-specific rules or human authority.

See `SKB_KNOWLEDGE_CONTINUITY.md` for the project-local contract.

Before any GPU/NPU/accelerator work specifically, also read SKB's `devices/redmi-turbo-4-pro/accelerator-access-audit.md` — the canonical, cross-project record of what this device's Vulkan/OpenCL/QNN-Hexagon doors actually allow, including the 2026-09-03 finding that direct FastRPC/QNN NPU access is verified-blocked for third-party apps industry-wide (not LAI- or HyperOS-specific), corroborated in this repo's `docs/HANDOFF-2026-09-03-npu-android-ecosystem-research.md` and `docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md`.

## Session start

Before changing LAI:

1. Read this file and `SKB_KNOWLEDGE_CONTINUITY.md`.
2. Read `PROJECT_STATE.md`.
3. Read the relevant roadmap phase and ADRs.
4. Inspect current `main` HEAD and recent commits.
5. Determine what is implemented, build-verified, device-validated, scaffolded, pending, or blocked.
6. Establish a session contract: target milestone, files expected to change, required tests, device validation, risks, and deferred scope.

## Evidence rules

Use the repository's vocabulary exactly:

- **Implemented** — source exists.
- **Build verified** — CI compiles/tests it.
- **Device validated** — observed on the target physical device.
- **Scaffold** — an honest unavailable boundary exists.
- **Pending/Planned** — not built.

Never upgrade a status without direct evidence. Vulkan/GPU/NPU/performance claims require device evidence; CPU or build success does not imply accelerator qualification.

## Adaptive engineering rule

SKB continuity does not impose a fixed technology template. Select the appropriate language, framework, architecture, module structure, documentation, dependencies, testing strategy, and validation method from LAI's actual requirements and constraints. Preserve LAI-specific architecture where required; justify material deviations through the repository's ADR process.

## Android device testing

ADB-first is the default methodology for any real-device interaction — see `docs/TESTING.md`
§"ADB-first device testing" for the full priority order (app-native intents/interfaces → ADB →
instrumentation → UIAutomator → coordinate taps, last resort) and `scripts/device/lai_adb.sh`
for the reusable helper. Discover → control → observe → verify, not click → wait → screenshot →
click. Never poll with an arbitrary sleep; wait on an observable condition (process state,
activity draw completion, a specific logcat pattern) instead, and read logs through a tag/regex
filter, not a raw dump.

## Engineering rules

- Preserve LAI's offline-first/privacy-first boundaries and explicit user authority model.
- Do not weaken consent, tool-policy, audit, zero-egress, secret-redaction, or model-catalog controls merely to make a feature easier.
- Do not commit credentials, private keys, model binaries, proprietary SDKs, or generated secrets.
- Respect the existing architecture and documented dependency direction unless an architectural change is justified by an ADR.
- Keep unavailable capabilities honest; use typed seams/placeholders rather than pretending a model/backend exists.
- Preserve licensing/provenance records for llama.cpp, Vulkan/OpenCL/QNN-related dependencies and models.

## Implementation workflow

`read → inspect HEAD → contract → implement smallest coherent change → run relevant CI/tests → collect evidence → update PROJECT_STATE/phase docs → review diff → Knowledge Return Review → commit and push immediately → handoff`

This environment is GitHub-centric **for the build/compile step only** — that is a device-health policy choice (avoid CPU/thermal/battery/storage load on Sobuj's phone), not a missing local toolchain: his Termux/PRoot environment has a real local ARM64 native Android toolchain (see `soobujmiah/adt`) and local builds are technically possible when specifically needed. Builds/CI are authoritative for build verification; the Redmi Turbo 4 Pro is authoritative for physical-device validation. Everything other than the build itself — downloading artifacts, installing, launching, using, reading logs, debugging, and fixing source — happens locally on the phone/PRoot setup, which is Sobuj's actual repo workstation, not just a remote-job dispatcher. Do not claim local Android/device execution unless actual evidence exists.

## Session close — mandatory

Before ending a substantial session:

1. Review `git diff`/GitHub changes and every changed file.
2. Run or trigger the relevant tests/checks and record actual results.
3. Update `PROJECT_STATE.md` and relevant phase/architecture documentation.
4. Record failures, limitations, deferred work, and exact reproduction/verification steps.
5. Perform the SKB Knowledge Return Review.
6. Commit/push completed work when authorized — **push validated work first**: as soon as a change is actually validated (tests/CI green, or device-validated where that's the relevant bar), commit and push it before starting further work, rather than letting it sit unpushed on the single phone/PRoot working copy where it can be lost.
7. Record the resulting commit SHA for substantial milestones.
8. Leave no unexplained dirty changes. If intentionally uncommitted, document why.

## Model switching / recovery

Chat history is not durable memory. Before a risky operation or model switch, create a named snapshot branch from the known-good HEAD. The next AI must reconstruct state from GitHub, this contract, `PROJECT_STATE.md`, roadmap/ADRs, tests/CI and relevant SKB context — never from another model's claims alone.

## Scope discipline

Do not fix unrelated issues during a focused task. Separate implementation, documentation, audit, and research work. If a finding is outside the current milestone, document it as deferred instead of silently expanding scope.
