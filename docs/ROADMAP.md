# Roadmap

## Phase 1 — platform foundation (current)

- [x] GitHub-only Android/NDK/CMake build
- [x] source-only 128 MB policy
- [x] Compose three-mode UX and Bangla resources
- [x] accessibility tree/actions/screenshot
- [x] Shizuku state and structured elevated operations
- [x] Hugging Face GGUF store
- [x] C++ backend/session ABI
- [x] Bangla OCR interface/schema and honest placeholder
- [x] architecture, safety, build, and test documentation
- [x] first GitHub Actions green build and resulting fixes ([run 31917533925](https://github.com/soobujmiah/lai/actions/runs/31917533925))
- [x] physical Snapdragon 8s Gen 4 smoke evidence ([Redmi Turbo 4 Pro result](device-results/2026-08-16-redmi-turbo-4-pro.md))

## Backbone hardening — completed

- [x] fourteen-module core/platform/runtime/plugin architecture including private audit ownership
- [x] zero-egress network ownership and mandatory artifact digest
- [x] architecture boundary source gate
- [x] pure-JVM coverage ratchets
- [x] evidence/thermal/battery/memory scheduler contract
- [x] opaque adapter-owned backend IDs and vendor-neutral compatibility policy
- [x] Android/SoC `DeviceProfile` with adapter-reported capabilities
- [x] CI boundary preventing vendor terminology in generic inference/scheduler source
- [x] versioned local-only plugin API
- [x] NpuHub comparison and ADR
- [x] one local-first application and upgrade path
- [x] reviewed local GGUF import through Android file picker
- [x] uninstall-safe user-owned GGUF export with destination hash verification
- [x] signed web supported-model catalog with verified offline fallback
- [x] artifact/backend/ABI compatibility metadata and signed-catalog downgrade prevention

## Phase 2 — usable local intelligence

- [x] pin/fetch llama.cpp in CI
- [x] CPU model load and local generation on Snapdragon device
- [ ] Stop/recovery and UTF-safe token streaming device validation
- [ ] multi-turn history validated basically; context-trimming stress test pending
- [x] native TTFT/prefill/decode metrics device validation
- [ ] GGML Vulkan build and Adreno capability self-test
- [x] model selection and explicit load/unload
- [x] conservative pre-load memory estimator and low-memory refusal
- [x] one-tap immutable reviewed baseline catalog
- [x] scheduler wiring for memory/battery/thermal-aware model load
- [ ] Bangla system prompt/templates and evaluation pack
- [x] parse complete LLM-proposed JSON tool calls with strict per-tool schemas and dispatch revalidation
- [x] opt-in one-shot trusted confirmation dialog (build verified; v0.8.0 model emitted no valid proposal)
- [x] bounded app-private hash-chained audit, approval-before-execution and exact-call replay guard
- [x] privacy-safe proposal outcome/rejection counters (source candidate; remote build pending)
- [ ] Qwen exact-format compliance retest
- [ ] foreground/screen binding, result provenance and tool-result feedback before any autonomous multi-step loop
- [ ] select and integrate printed Bangla OCR baseline
- [ ] benchmark profile hidden in Developer Mode (implementation candidate added)

## Phase 3 — Snapdragon specialization

- [ ] license-compliant QAIRT SDK CI acquisition
- [ ] QNN model conversion/calibration workflow
- [ ] HTP context manifest/cache
- [ ] dedicated QNN runtime adapter and explicit fallback (outside `runtime:llama`)
- [ ] Adreno/HTP/CPU routing under thermal/memory policy
- [ ] printed and handwritten Bangla OCR QNN acceleration
- [ ] Snapdragon 8s Gen 4 performance/accuracy report

## Phase 4 — plugin ecosystem

- [ ] signed capability-based plugin manifest
- [ ] local RAG vector store abstraction
- [ ] Bangla/English STT
- [ ] offline TTS
- [ ] scoped user-created automation recipes
- [ ] import/export with permission review

## Later — progressive vendor expansion

- [ ] introduce a backend manager only when a second real runtime provider exists
- [ ] evaluate the next vendor against measured product demand and available SDK/licensing
- [ ] add one isolated, physically validated adapter at a time
- [ ] compare correctness/performance with the Snapdragon and CPU baselines

No phase advances based solely on successful compilation. Device correctness, privacy, safety, and documentation are release gates. Empty vendor adapters are not roadmap progress.
