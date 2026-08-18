# Commercial module boundaries

**Status:** engineering candidate map  
**Date:** 2026-08-18  
**Canonical IP policy:** [`../legal/PROPRIETARY_BOUNDARIES.md`](../legal/PROPRIETARY_BOUNDARIES.md)  
**Related ADR:** [`../decisions/ADR-0107-commercial-module-boundaries.md`](../decisions/ADR-0107-commercial-module-boundaries.md)  
**Existing module map:** [`module-map.md`](module-map.md), [`../MODULES.md`](../MODULES.md)

Legal classification and the free/premium split are defined in the canonical policy. This file records technical seams only.

Technical modularity does not automatically create legal separation. A separate Gradle module that modifies or is based on the public Work may still be a Derivative Work under Apache-2.0. **LEGAL REVIEW REQUIRED** before any proprietary label.

## How to read the table

| Column | Meaning |
|---|---|
| Public/private candidate | Planning hint only |
| License dependency | Known third-party or LAI-core coupling |
| Proprietary implementation technically possible | Whether an API seam exists or can be added |
| Legal review | Required before treating the module as separately licensable |

## Candidates

| Module candidate | Current source | Public/private candidate | License dependency | Dependency direction | API boundary | Data boundary | Security boundary | Entitlement boundary | Proprietary technically possible? | Legal review |
|---|---|---|---|---|---|---|---|---|---|---|
| Core runtime (`core/contracts`, `core/policy`, `core/scheduler`, `core/model`) | IMPLEMENTED | Public core | LAI Apache-2.0 + Kotlin/kotlinx | Downward only | Pure JVM contracts | No user content stored | Policy is the gate | Free capabilities live here as policy, not DRM | Low: this *is* the Work | Yes, if split is proposed |
| App shell / Compose UI | IMPLEMENTED | Public core | AndroidX Compose Apache-2.0 | Depends on all feature ports | UI must not own authority | Local UI state | No network | UI shows entitlement state only | Low for existing screens | Yes |
| llama.cpp adapter (`runtime/llama`) | IMPLEMENTED | Public core | MIT llama.cpp at CI pin | Isolated JNI | `InferenceEngine` | Prompts stay in process | Native memory safety | Free local inference | New backends can be separate modules | Yes for new adapters |
| Vulkan path | PARTIAL | Public or premium-if-packaged-with-extra-binaries | llama.cpp MIT + SPIR-V build deps | Same native module today | Backend ID `llama-vulkan` | Local | Driver trust | Prefer capability ID, not a second APK | Possible as optional native flavor | Yes |
| QNN / QAIRT adapter | PLANNED; empty adapters forbidden | Private/vendor pack likely | UNKNOWN vendor agreement | Dedicated runtime module (ADR 0005) | Opaque backend descriptor | Local | SDK must not leak into `core` | Premium backend pack possible | Yes, as a separately acquired SDK adapter | **Required** |
| Agent engine (multi-step) | MISSING; one-shot exists | Public one-shot; PRO multi-step candidate | LAI core | Orchestrator depends on contracts | Planner API | Task state local | Approval-before-authority | `cap.agent.multistep` | Possible as later module | Yes |
| Automation engine (Accessibility / Shizuku) | PARTIAL | Public basic | Android + Shizuku Apache-2.0 | Authority modules isolated | Typed tools only | Screen/shell local | Highest device authority | Recipe packs, not authority bypass | Recipe packs possible; authority stays public | Yes |
| RAG | MISSING | PRO candidate | Future embedder / SQLCipher UNKNOWN | New `core`/`features` modules | Retriever port | Documents local by default | Malicious docs | `cap.rag.local` | Yes as new module | **Required** (models + SQLCipher) |
| OCR | Placeholder IMPLEMENTED | PRO candidate | Tesseract/tessdata planned Apache-2.0 (UNVERIFIED at pin) | `runtime/ocr` seam exists | `OcrEngine` | Screenshots local | Image parsers | `cap.ocr.printed_bn` | Yes behind existing seam | **Required** |
| Developer tools | MISSING | PRO candidate | Git/LSP/Linux stack mixed (QEMU GPL risk) | Must be feature modules | Project ports | Source local | Process isolation | `cap.dev.workstation` | Yes | **Required** (GPL/toolchains) |
| Cloud connectors | MISSING | CLOUD / BYO | Provider terms | Future gateway; not `platform:download` for prompts | Provider port | Egress only with consent | Keys in Keystore | `cap.cloud.provider` | Yes as separate adapters | **Required** |
| Enterprise controls | MISSING | ENTERPRISE / private | LAI + future MDM | Policy extensions | Org policy port | Metadata only | Must not weaken local-first | `cap.org.policy` | Yes | Yes |
| Premium backends | PLANNED | Private pack | Vendor SDKs | Dedicated runtime | Backend descriptor | Local | No vendor types in core | Backend capability IDs | Yes | **Required** |
| Premium integrations | MISSING | Plugin artifacts | Plugin + third-party | `plugins/api` is local-only today | Manifest + host | Capability-scoped | Untrusted packages | Add-on SKUs | Yes, after plugin manager | **Required** |
| Advanced knowledge graph | MISSING | PRO after RAG | RAG + UI | `features:knowledge` (planned) | Graph port | Derived from local RAG | Parser risk | Capability ID | Yes | Yes |
| Advanced visualization | MISSING | PRO / studio | Rendering stacks UNKNOWN | Independent studio modules (roadmap) | Documented later | Local assets | Parser/GPU | Add-on | Unknown until designed | **Required** |
| Professional workflow packs | MISSING | Add-on | Depends on pack | Plugins or feature modules | Pack manifest | Pack-scoped | Same gates as built-ins | Add-on SKUs | Yes | Yes |
| Entitlement adapter | MISSING | Official-binaries only candidate | New; keys are secrets | Depends on policy, not authorities | `EntitlementPort` | SKU metadata only | Key protection | The adapter itself | Yes, replaceable in public builds | Yes |

## Plugin path

[`plugin-architecture.md`](plugin-architecture.md) is PARTIAL: contracts exist, manager does not. A future plugin package is the cleanest *technical* place for commercial modules that must not enter the public Git tree. Isolation, signing, and license review are prerequisites. None are implemented.

## NpuHub ports

Several roadmap items say “port from NpuHub.” NpuHub is documented as a private repository. Pattern adoption is not source copying. Any actual port requires recorded authorization and a license review ([`../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md`](../legal/AI_ASSISTED_DEVELOPMENT_POLICY.md)).

## Decision

**DECISION REQUIRED** for which candidates leave the public tree.

**LEGAL REVIEW REQUIRED** per module before a proprietary or source-available label is applied.
