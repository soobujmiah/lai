# Implementation status

Last reviewed: 2026-08-16

Latest verified remote build: commit `97f039f`, [GitHub Actions run 31921021303](https://github.com/soobujmiah/lai/actions/runs/31921021303). Architecture/privacy boundaries, coverage ratchets, pure-JVM tests, eleven-module Android graph, lint, immutable llama.cpp fetch, arm64 llama.cpp/ggml/JNI compilation, APK assembly, and artifact upload passed. The `lai-debug-24` artifact archive is approximately 13.75 MB.

Legend: **Ready** = implemented and intended to work; **Build verified** = compiles and packages remotely but awaits the named physical-device gate; **Scaffold** = compiling contract with honest unavailable behavior; **Planned** = not implemented.

| Capability | Status | Evidence / boundary |
|---|---|---|
| Source-only repository policy | Ready | `scripts/validate_repo.sh` enforces size, binary, docs, and token rules |
| Layered module backbone | Ready | eleven-module Gradle/manifest graph compiled and packaged remotely |
| Zero-egress architecture policy | Ready | only download module owns network/permission; boundary check passed in CI |
| Evidence-aware scheduler | Ready contract | compile/probe/device/benchmark evidence plus memory/thermal/battery tests pass; app routing integration pending |
| GitHub Android toolchain | Ready | workflow installs API 35, Build Tools, NDK, CMake, Gradle remotely |
| arm64 native `.so` build | Ready | isolated `runtime:llama` externalNativeBuild compiles C++20 JNI library |
| Compose three-mode UX | Ready | chat, screen reader, automator; developer controls hidden |
| Bangla resources and UTF-8 tool protocol | Ready | `values-bn`; JSON unit test |
| Accessibility connection/snapshot | Ready | bounded flattened tree; password text omitted |
| Click/type/scroll/global/launch actions | Ready | typed commands; confirmation enforced at agent boundary |
| Android 11+ accessibility screenshot | Ready | hardware buffer copied to ARGB and closed |
| Shizuku binder and permission | Ready | state flow, UID, request listener |
| Elevated operation allowlist | Ready | Shizuku UserService + argv-only policy; no raw shell; tests for injection |
| Reviewed GGUF download | Build verified | isolated network module, explicit tap, mandatory SHA-256, redirect review, resume/private storage/GGUF validation; device regression pending |
| LLM JNI/session interface | Build verified | cancellable per-token callback compiled and packaged; device stream test pending |
| llama.cpp CPU inference | Build verified (Phase 2) | loader/tokenizer/chat template/sampler compiled; physical GGUF and Bangla validation pending |
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
