# Implementation status

Last reviewed: 2026-08-16

Latest release: [`v0.8.2`](https://github.com/soobujmiah/lai/releases/tag/v0.8.2), commit `cbf6ff9`, [release run 31958838061](https://github.com/soobujmiah/lai/actions/runs/31958838061). Privacy-safe proposal counters, strengthened exact-format instruction, persistent-ledger/Android file/replay/corruption tests, strict schemas, trusted review UI, diagnostics privacy, vendor boundaries, coverage, lint, Kotlin/JNI/native runtime and release APK assembly passed. The published temporary/debug-signed `app-release.apk` is 8,371,584 bytes. [Catalog run 31951341094](https://github.com/soobujmiah/lai/actions/runs/31951341094) signed and published revision 3; the exact 1,131-byte catalog asset and detached signature were independently verified.

Legend: **Ready** = implemented and intended to work; **Build verified** = compiles and packages remotely but awaits the named physical-device gate; **Scaffold** = compiling contract with honest unavailable behavior; **Planned** = not implemented.

| Capability | Status | Evidence / boundary |
|---|---|---|
| Source-only repository policy | Ready | `scripts/validate_repo.sh` enforces size, binary, docs, and token rules |
| Layered module backbone | Ready | fourteen-module graph including dedicated persistent-audit ownership passed run 31956572135 |
| Zero-egress architecture policy | Ready | only download module owns transport; outbound user data denied; boundary check passes |
| Signed web model catalog | Build verified | encrypted ECDSA signer, stable `catalog-v1` assets, in-app verification/cache/fallback and supported-list UI passed; device refresh pending |
| Evidence-aware scheduler | Device validated (CPU) | Redmi selected CPU with 3,077 MiB available vs 1,833 MiB estimated peak at nominal thermal state |
| Vendor-neutral backend boundary | Build verified | opaque adapter IDs, generic descriptors, artifact/backend/ABI checks, `DeviceProfile`, isolated llama runtime and CI terminology guard passed run 31951341085 |
| Reviewed model catalog | Device validated baseline; revision 3 published | official Qwen artifact installed with expected size/hash; signed metadata declares GGUF/backend/ABI/context/memory compatibility and prevents downgrade; in-app rev3 refresh pending |
| Android environment provider | Device validated | v0.8.0 reported API 36, QTI SM8735, arm64-v8a, 8 cores, 4,313,878,528 available bytes, 84% battery and thermal=NOMINAL |
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
| llama.cpp CPU inference | Device validated basic | v0.8.0 loaded Qwen in 514 ms; four samples averaged 47.36 prefill and 20.70 decode tok/s; broad quality/thermal testing pending |
| Runtime reliability | Partially device validated | v0.8.0 prompt growth 382→510 and four generations passed; Stop/recovery, New chat, forced trimming and memory-pressure handling pending |
| Diagnostics JSON v1 | Device validated | v0.7.1 and v0.8.0 exports contained expected runtime/performance fields and privacy exclusions with no user content observed |
| Retained model copy | Device validated | v0.8.0 Keep copy survived uninstall; offline Import file restored exact 1,117,320,736-byte reviewed SHA-256 and loaded successfully |
| Dual Dashboard + Chat tool UX | Planned | Standalone OCR/Voice/Image Generation/Vector Search and Chat **+ Attach Tools** must share contracts/adapters; no duplicate tool engines |
| Contextual per-tool settings | Build verified (contracts/policy) | typed `SettingsDocumentV1` ranges/defaults and `SettingsPolicy` validation/migration are pure core and unit-tested (NaN/infinity, unknown fields, context-dependent max-token ceiling, deterministic v1 seam); Chat ⚙ sheet, versioned non-secret `settings.json` persistence and SAF store remain pending |
| User-owned `/sdcard/LAI/` workspace | Planned | must use explicit SAF tree grant, bounded restore/discovery and private verified runtime copies; no broad-storage permission |
| Categorized Model Center/background download | Planned | signed categories, pause/resume/cancel/process recovery and manual GGUF import are specified; existing 1.5B catalog/download remains active |
| Native C++ micro-model task chaining | Planned | bounded DAG for LLM + Embedding/Whisper with aggregate memory admission; current 1.5B runtime remains unchanged |
| Adreno Vulkan offload | Planned (Phase 2) | requires pinned adapter/device qualification |
| QAIRT/QNN HTP offload | Planned (Phase 3) | Snapdragon priority; requires a dedicated isolated adapter, converted artifact, licensed SDK/runtime and physical-device evidence |
| Bangla printed OCR model | Scaffold | contract and JSON ready; engine returns model-required error |
| Bangla handwriting OCR | Planned | dataset/model/license selection required |
| One-shot LLM tool proposals | Build verified; model-compliance retest required | v0.8.0 produced no valid proposal; stronger exact-format instruction and privacy-safe accepted/rejected/not-tool counters passed run 31958557120 |
| Persistent tool audit/replay guard | Build verified v0.8.1 | no-backup JSONL, approval-before-authority fsync, bounded full-chain verification, Android file tests, content-free records and exact-call replay rejection passed run 31956572135; device validation pending |
| Autonomous multi-step tool loop | Planned | requires foreground binding, typed result provenance/feedback, loop limits, cancellation and adversarial device evidence |
| Rolling Context Window | Planned | explicit recent-turn/summary token budgeting must replace simple oldest-turn omission only after correctness/device evidence |
| Dynamic thermal/battery throttling | Planned | current scheduler performs coarse admission; closed-loop thread/batch/backend throttling and hysteresis remain unimplemented |
| Streaming TTS + Barge-In | Planned | requires bounded PCM queues, parallel synthesis, Interrupt VAD, echo policy and cancellation propagation |
| Encrypted Vector DB | Planned | SQLCipher/Keystore key lifecycle, embedding metadata, migrations and deletion/export UX require design and implementation |
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
