# Implementation status

Last reviewed: 2026-08-16

Latest verified remote build: commit `d097b42`, [Android run 31944742526](https://github.com/soobujmiah/lai/actions/runs/31944742526). Boundaries, catalog v2, coverage, retained-model SAF export/reopen/SHA path, diagnostics, lint, Kotlin/JNI/native runtime and APK assembly passed. The APK artifact archive is approximately 13.85 MB. Signed catalog revision 2 was independently verified.

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
| LLM JNI/session interface | Device validated metrics | 207-token prompt, 182-token output and native TTFT/prefill/decode/total counters observed; cancellation pending |
| llama.cpp CPU inference | Device validated basic | Qwen loaded in 578 ms; coherent Bangla output, 45.76 prefill and 20.35 decode tok/s measured; broad quality/thermal testing pending |
| v0.7 runtime reliability | Partially device validated | four-turn prompt growth and native metrics passed; Stop/recovery, New chat, trimming and memory-pressure handling pending |
| Diagnostics JSON v1 | Device validated | v0.7.1 JSON exported with four samples and expected privacy exclusions; no user content observed |
| Retained model copy | Build verified | SAF export, destination reopen/SHA verification and post-reinstall import UX passed CI; device uninstall/reimport pending |
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
