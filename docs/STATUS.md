# Implementation status

Last reviewed: 2026-08-16

Latest verified remote build: commit `6e29bfe`, [Android run 31942344604](https://github.com/soobujmiah/lai/actions/runs/31942344604). Boundaries, catalog validation, coverage, diagnostics privacy schema/tests, lint, Kotlin/JNI/native runtime and APK assembly passed. The APK artifact archive is approximately 13.85 MB. Signed catalog publication remains verified by runs 31939187858/31939187860.

Legend: **Ready** = implemented and intended to work; **Build verified** = compiles and packages remotely but awaits the named physical-device gate; **Scaffold** = compiling contract with honest unavailable behavior; **Planned** = not implemented.

| Capability | Status | Evidence / boundary |
|---|---|---|
| Source-only repository policy | Ready | `scripts/validate_repo.sh` enforces size, binary, docs, and token rules |
| Layered module backbone | Ready | thirteen-module Gradle/manifest graph compiled and packaged remotely |
| Zero-egress architecture policy | Ready | only download module owns transport; outbound user data denied; boundary check passes |
| Signed web model catalog | Build verified | encrypted ECDSA signer, stable `catalog-v1` assets, in-app verification/cache/fallback and supported-list UI passed; device refresh pending |
| Evidence-aware scheduler | Device validated (CPU) | Redmi selected CPU with 3,077 MiB available vs 1,833 MiB estimated peak at nominal thermal state |
| Reviewed model catalog | Device validated baseline | official Qwen artifact installed with expected size/hash prefix; signed web refresh remains pending |
| Android environment provider | Device validated | Redmi reported memory, 79% battery, charging=false and thermal=NOMINAL |
| GitHub Android toolchain | Ready | workflow installs API 35, Build Tools, NDK, CMake, Gradle remotely |
| arm64 native `.so` build | Ready | isolated `runtime:llama` externalNativeBuild compiles C++20 JNI library |
| Compose three-mode UX | Ready | chat, screen reader, automator; developer controls hidden |
| Bangla resources and UTF-8 tool protocol | Ready | `values-bn`; JSON unit test |
| Accessibility connection/snapshot | Ready | bounded flattened tree; password text omitted |
| Click/type/scroll/global/launch actions | Ready | typed commands; confirmation enforced at agent boundary |
| Android 11+ accessibility screenshot | Ready | hardware buffer copied to ARGB and closed |
| Shizuku binder and permission | Ready | state flow, UID, request listener |
| Elevated operation allowlist | Ready | Shizuku UserService + argv-only policy; no raw shell; tests for injection |
| Reviewed GGUF download | Device validated baseline | Qwen 1,117,320,736-byte artifact activated with reviewed digest; interruption/resume still pending |
| LLM JNI/session interface | Device validated basic | physical model load and 15-token local generation passed; cancellation/metrics pending |
| llama.cpp CPU inference | Device validated basic | Qwen CPU session loaded and generated locally without reported crash; response/Bangla quality and sustained thermals pending |
| v0.7 runtime reliability | Build verified | multi-turn templates, token trimming, state machine, Stop, native metrics, storage/memory handling compiled; device validation pending |
| Diagnostics JSON v1 | Build verified | explicit SAF export, 20-sample local history and content-exclusion schema/tests passed; device export inspection pending |
| Adreno Vulkan offload | Planned (Phase 2) | requires pinned adapter/device qualification |
| QAIRT/QNN HTP offload | Planned (Phase 3) | requires licensed SDK/runtime and physical device |
| Bangla printed OCR model | Scaffold | contract and JSON ready; engine returns model-required error |
| Bangla handwriting OCR | Planned | dataset/model/license selection required |
| LLM-driven tool loop | Planned (Phase 2) | manual safe tools exist; model proposal parser to follow |
| RAG / STT / TTS plugins | Planned | interfaces to be designed after runtime stabilization |

## Definition of "full featured"

LAI becomes production-featured only when all of the following have physical-device evidence:

1. a pinned, reproducible LLM backend generates UTF-8 Bangla correctly;
2. CPU/Vulkan/QNN fallback and capability probes are truthful;
3. Bangla OCR quality is measured on a versioned printed/handwritten test set;
4. tool proposals cannot bypass confirmation or the shell allowlist;
5. thermal, memory pressure, cancellation, and service death recover cleanly;
6. release APK is reproducibly signed and has an SBOM/provenance record.

Phase 1 intentionally does not substitute demos or canned output for these acceptance criteria.
