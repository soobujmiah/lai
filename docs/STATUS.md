# Implementation status

Last reviewed: 2026-08-19

> The directive-aligned, source-audited status is maintained in [`implementation/current-state.md`](implementation/current-state.md). The canonical full roadmap is [`ROADMAP.md`](ROADMAP.md). The authoritative handoff snapshot is [`PROJECT_STATE.md`](../PROJECT_STATE.md). This legacy evidence table is retained for detailed build/device history and uses its original evidence vocabulary.

Current `main` head: **`35d90ea`** · CI **#173 green** (signed release APK + R8 mapping; debug skipped by the signed-only rule) · **187 unit tests** · 16 modules · catalog revision 4.

Since the 2026-08-18 repair session: workspace auto-import of `storage/LAI/models/*.gguf` (nullable-hash compile fix), full Vulkan toolchain (`SPIRV-Headers` CMake config, `glslc`, `vulkan.hpp`, `spirv.hpp`), a real Vulkan adapter with full layer offload, Adreno driver workarounds (`GGML_VK_DISABLE_COOPMAT/_2`, `GGML_VK_DISABLE_MMVQ`), in-app capture of the failing shader name, auto CPU fallback, centralized `LaiLog` diagnostics (debug `DEBUG` / release `INFO`, redactor, crash handler, log + JSON export), and a **deterministic test keystore** so every non-tag release installs over the previous build. Latest production release: [`v0.9.7`](https://github.com/soobujmiah/lai/releases/tag/v0.9.7).

Legend: **Ready** = implemented and intended to work; **Build verified** = compiles and packages remotely but awaits the named physical-device gate; **Scaffold** = compiling contract with honest unavailable behavior; **Planned** = not implemented.

| Capability | Status | Evidence / boundary |
|---|---|---|
| Source-only repository policy | Ready | `scripts/validate_repo.sh` enforces size, binary, docs, and token rules |
| Layered module backbone | Ready | sixteen-module graph including dedicated persistent-audit ownership passed run 31956572135 |
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
| **Vulkan GPU adapter** | **Unsupported on this device — driver bug (addr2line-verified); CPU default** | Real `VulkanBackend::open()` with full layer offload (`LLAMA_LOAD_MODE_NONE`, IGPU probe); MMVQ compile failure fixed via LAI patch (`ggml-vulkan-skip-mmvq.patch`, CI #175) but the **Adreno 825 driver crashes natively during Vulkan compute** (0.1.175, process restart on send — uncatchable in Kotlin). Per the no-claim-without-evidence rule, `validated_accelerators` default is **empty = CPU-only**; `llama-vulkan` is opt-in for qualified devices. Coopmat/MMVQ disables, `GGML_VK_DISABLE_INTEGER_DOT_PRODUCT/_F16/_ASYNC` (experimental), `std::cerr`→`LAI-llama`, in-app shader capture, native crash handler (backtrace into the log file), and Kotlin-level CPU fallback remain. |
| **Centralized diagnostics logging** | Ready | `LaiLog` (logcat + app-private 512 KiB-rotated file + in-memory tail in the export), per-build verbosity (debug `DEBUG`, signed release `INFO`), `LaiLogRedactor` (tokens/api keys/passwords/PEM/AWS redacted; 11 unit tests), uncaught-crash handler, in-app "Export diagnostic log" + `logs[]` in the JSON export; `std::cerr` redirect so ggml errors name the shader. See [LOGGING.md](LOGGING.md). |
| **Release signing / install-over-previous** | Ready | `v*` tags → production keystore from `ANDROID_KEYSTORE_*` secrets (`PRODUCTION_SIGNED=true`); otherwise a **deterministic test keystore** (`scripts/ci/generate_test_keystore.py`, committed seed, `openssl pkcs12 -legacy`) so every non-tag release shares one certificate (SHA-256 `D3:A6:6C:E0:…`) and installs over the previous build. Working rule: default CI builds ONLY the signed release APK. |
| Runtime reliability | Partially device validated | v0.8.0 prompt growth 382→510 and four generations passed; Stop/recovery, New chat, forced trimming and memory-pressure handling pending |
| Diagnostics JSON v1 | Device validated | v0.7.1 and v0.8.0 exports contained expected runtime/performance fields and privacy exclusions with no user content observed |
| Retained model copy | Device validated | v0.8.0 Keep copy survived uninstall; offline Import file restored exact 1,117,320,736-byte reviewed SHA-256 and loaded successfully |
| Dual Dashboard + Chat tool UX | Planned | Standalone OCR/Voice/Image Generation/Vector Search and Chat **+ Attach Tools** must share contracts/adapters; no duplicate tool engines |
| Contextual per-tool settings | Build verified (contracts/policy) | typed `SettingsDocumentV1` ranges/defaults and `SettingsPolicy` validation/migration are pure core and unit-tested (NaN/infinity, unknown fields, context-dependent max-token ceiling, deterministic v1 seam); Chat ⚙ sheet, versioned non-secret `settings.json` persistence and SAF store remain pending |
| User-owned `/sdcard/LAI/` workspace | Build verified (device test pending) | `platform:workspace` owns the SAF tree grant (`ACTION_OPEN_DOCUMENT_TREE` + persistable permission, no `MANAGE_EXTERNAL_STORAGE`, no raw paths), canonical child resolution, a bounded discovery traversal (depth/count/size caps, streamed SHA-256, reviewed-catalog match via the pure classifier) and a temp-write-then-replace settings store backed by the pure codec; pure decisions and the GGUF magic detector are unit-tested; the Chat ⚙ quick-settings sheet, the Settings workspace card (grant/revoke + coarse-count scan) and the port-driven `WorkspaceSettingsCoordinator` are implemented and unit-tested against fakes; physical SAF grant/discovery on device remains pending |
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
