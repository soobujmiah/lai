# Implementation status

Last reviewed: 2026-08-16

Latest verified remote build: commit `9ac684e`, [GitHub Actions run 31917533925](https://github.com/soobujmiah/lai/actions/runs/31917533925). Source policy, Kotlin/C++, unit tests, lint, APK assembly, and artifact upload passed. The resulting `lai-debug-9` artifact is approximately 10.9 MB.

Legend: **Ready** = implemented and intended to work; **Scaffold** = compiling contract with honest unavailable behavior; **Planned** = not implemented.

| Capability | Status | Evidence / boundary |
|---|---|---|
| Source-only repository policy | Ready | `scripts/validate_repo.sh` enforces size, binary, docs, and token rules |
| GitHub Android toolchain | Ready | workflow installs API 35, Build Tools, NDK, CMake, Gradle remotely |
| arm64 native `.so` build | Ready | app externalNativeBuild compiles C++20 JNI library |
| Compose three-mode UX | Ready | chat, screen reader, automator; developer controls hidden |
| Bangla resources and UTF-8 tool protocol | Ready | `values-bn`; JSON unit test |
| Accessibility connection/snapshot | Ready | bounded flattened tree; password text omitted |
| Click/type/scroll/global/launch actions | Ready | typed commands; confirmation enforced at agent boundary |
| Android 11+ accessibility screenshot | Ready | hardware buffer copied to ARGB and closed |
| Shizuku binder and permission | Ready | state flow, UID, request listener |
| Elevated operation allowlist | Ready | Shizuku UserService + argv-only policy; no raw shell; tests for injection |
| Hugging Face GGUF download | Ready | resume, private storage, SHA-256, GGUF validation |
| LLM JNI/session interface | Scaffold | stable interface; zero available backends reported |
| llama.cpp CPU inference | Planned (Phase 2) | no upstream source or binary committed |
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
