# LAI Master Roadmap

**Status:** Documentation-only directive — 2026-08-18  
**Source of truth:** Current repository (`soobujmiah/lai`, `main` @ `17ad75b` + `c4732fe` + `0263d30`), `docs/ROADMAP.md` Phases 0–14, and device evidence on Xiaomi Redmi Turbo 4 Pro (SM8735, Adreno 825).

## 0. Documentation / Foundation — CURRENT

**Objective:** Source-verified product record so a future agent can start without history.  
**Current:** `PROJECT_STATE.md` (2026-08-18), `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/DEVICE_VALIDATION.md`, 45+ docs, `scripts/validate_repo.sh` (928 KB <128 MB, boundaries OK, catalog rev3).  
**Deliverables of this directive:** `MASTER_ROADMAP.md`, `DEVELOPMENT_STATE.md`, full `docs/` system (Design, AI Runtime, Providers, Tool Catalog, Agent, Pipeline, etc.) — no code changes.  
**Acceptance:** Another engineer can answer *what LAI is / what is built / what remains / what is next* from docs alone.

## 1. CPU Inference — COMPLETE (device-validated)

**Current:** `runtime:llama` JNI/C++ `llama.cpp` CPU, mmap GGUF, chat template, cancellable streaming (`trySendBlocking` + `buffer(256)`), metrics. Qwen 2.5 1.5B Q4_K_M `1,117,320,736 bytes` SHA-256 exact. KV-prefix reuse `25×` (17s → 0.6s), `16–22 tok/s` prefill / `8–12 tok/s` decode on SM8735 (0.1.139: 93 tok in 5.8s, 11 tok reuse in 0.54s). Thermal governor closed-loop (`PowerManager` → `ThermalGovernorPolicy` hysteresis → JNI atomic between `llama_decode`), adaptive cores `little 7 idle → big 0-3 burst`, batch `32`, prompt shortened 1075→180 chars. `storage/LAI/models` auto-import on `install -r` (signed `lai-release` key, no Release needed).  
**Remaining device walkthrough:** warm-load thermal notice, `windowedConversationTurns >0` slider test, Bangla quality judgement.  
**Gate:** Measured on SM8735, CPU fallback truthful.

## 2. GPU / Vulkan — SCAFFOLD → REAL (proper way)

**Current:** `backend_registry` reports `vulkan` via `dlopen libvulkan.so` probe — `loader found, Adreno 825` on SM8735 (log 17:39:59). `CMakeLists` now `GGML_VULKAN=ON` with CI `spirv-headers/glslang` apt step (`0263d30`, proper way). `available() true` after that build.

**Next:** `open()` with `n_gpu_layers=99` + `generate()` wiring → measure `40–60 tok/s` prefill on same 93 tok, vs CPU `16 tok/s`. Keeps CPU fallback. **Device test mandatory** for first 93 tok prefill on Adreno 825. **Enables 3B Q4_K_M (~1.7 GB) without QNN.**

## 3. NPU — QNN/HTP — PLANNED (licensed)

**Current:** Boundary documented only, no code.  
**Next:** Licensed QAIRT SDK via `secrets.QAIRT_URL` (never in repo), model conversion `GGUF → DLC` + INT4 calibration, dedicated isolated `runtime:qnn` outside `runtime:llama`, `HTP context manifest/cache`, scheduler routing `Adreno/HTP/CPU` under thermal/memory. **Device test mandatory** for HTP. **Enables 5B and half power.**

## 4. Unified Inference Orchestration

**Current:** `InferenceScheduler` + `BackendId`/`BackendDescriptor` + `DeviceProfile` + evidence/thermal/battery/memory preflight, but UI calls `InferenceEngine` directly (no Gateway).  
**Target:** `AI Gateway` (registry/router/context/model/tool/permission/usage/audit) with embedded provider, then `unified backend/provider orchestration` (CPU/Vulkan/QNN + cloud/remote) — capability/latency/cost/privacy-aware routing, fallback truthful. **Planned after GPU/NPU are measured.**

## 5. Model System

**Current:** Signed `catalog/models-v1.json` rev3 + `RemoteModelCatalogRepository` (ONLY network owner), `ModelRepository` (SHA-256, resume, GGUF validate, `noBackupFilesDir/models`), `WorkspaceDiscovery` (`storage/LAI/models` SAF bounded traversal depth 4/256 files/8 GB cap, SHA streaming, `WorkspacePolicy` classification), `Keep copy` export, Delete guard, `128 MB` source cap (no GGUF in repo).  
**Next:** Capability detection + intelligent selection (3B/5B memory-aware), one-run grant tightening (NpuHub-style), provenance/license display.

## 6. AI Core — Chat & Streaming

**Current:** `UiMode CHAT/SCREEN_READER/AUTOMATOR`, streaming `callbackFlow`, `Stop` watchdog `45s`, `ChatHistoryRepository` (≤100 sessions, ≤512 msgs, no-backup, atomic), `ContextWindowPolicy` `keepLastTurns`, `QuickSettingsSheet` (creativity/focus/length/memory), diagnostics `v1` (privacy-safe).  
**Next:** Multi-modal attachments (files/images/code), citations, `Chat UX` polish per Design System.

## 7. Tool Ecosystem

**Current:** 15 built-in tools (`screen.snapshot/click/type/scroll`, `system.global_action`, `app.launch`, `ocr.current_screen`, `shell.operation`, `device.info`, `settings.*`, `package.*`, `input.keyevent`) — `AgentRuntime.parseToolProposal` (bounded JSON, per-tool schema, `ToolInstructionGate`), hash-chained `ToolAuditLedger` (approval-before-authority, replay guard), `ToolsDashboard` (Vision/Interaction/Elevated cards). One-shot only, user-confirmed.

**Next:** See `docs/TOOL_CATALOG.md` — ~60 researched tools across AI/inference, files, documents, developer, Git, Linux/terminal, Android, automation, research, knowledge, system — each with `AVAILABLE/PLANNED/FUTURE` status. No fake claims.

## 8. Multi-step Agent

**Current:** One-shot.  
**Target:** `Intent → Context → Plan → Memory → Tool selection → Permission/Approval → Execution → Observation → Verification → Next step → Final result` with task state, planning, dependency ordering, cancellation/emergency-stop, retries, independent verification, audit trail. `planning ≠ execution ≠ observation ≠ verification`. `core:agent` + `core:pipeline` (DAG) will be introduced additively — see `docs/AGENT.md` and `docs/PIPELINE.md`.

## 9. Developer AI

**Target:** `Inspect → Understand → Plan → Modify → Build → Test → Verify → Document` over `storage/LAI/` projects: repo browsing, code search/generation/editing/refactoring, debugging, test generation, build assistance, logs/diagnostics, architecture analysis. **Planned after agent loop.**

## 10. Linux / Terminal

**Current:** None. HyperOS restricts; no root assumed.  
**Target:** PRoot/QEMU managed distro/rootfs, mounts, processes, profiles, display integration — `core:workspace` SAF + `platform:shizuku` where available. Detect capabilities, document fallbacks. **Long-term, after workstation.**

## 11. Files / Documents Intelligence

**Current:** SAF workspace `storage/LAI/` (grant `ACTION_OPEN_DOCUMENT_TREE`, coarse counts, no `MANAGE_EXTERNAL_STORAGE`), `WorkspaceDiscovery`, `Keep copy`.  
**Target:** Read/search/extract/summarize/compare/transform/generate/cite for PDF/DOCX/XLSX/PPTX/Markdown/TXT/CSV/JSON/XML/YAML/HTML/images/archives/code — see `docs/FILES_AND_DOCUMENTS.md`. **Planned.**

## 12. OCR / Vision — scaffold → verified Bengali

**Current:** `OcrEngine` contracts (`OcrResult` blocks/polygon/confidence/handwriting), `PlaceholderBanglaOcrEngine` fails `OcrModelRequiredException`. **Tesseract 5.5.3 `ben.traineddata` not bundled.**

**Target:** Keep NpuHub-grade provenance: Tesseract `5.5.3 @ db0ec62f` + `tessdata_fast/ben.traineddata @ 874164` (855,841 bytes, SHA `311630...`), `ReviewedOcrByteVerifier` (64 KiB, exact size/SHA), `PngRasterDecoder` (pure JVM, `Inflater`/`CRC32`, BT.601 luma), `RunAuthorizer` (60s evidence, 5-min preview/consent, one-use grant), `features:ocr` staging. Port `core/tokenization` SentencePiece unigram for embedding tokenization as well. **Device test mandatory** on printed/handwritten Bangla test set. **Planned.**

## 13. Memory

**Current:** Conversation memory via `ContextWindowPolicy` (`keepLastTurns`) + `ChatHistoryRepository`. No cross-task memory.  
**Target:** Conversation / task / project / user-controlled preferences, retrieval/deletion/provenance, bounded storage, auditable, privacy-safe — see `docs/MEMORY.md`. **Planned.**

## 14. RAG / Knowledge Management

**Current:** `OcrEngine` JSON scaffold only.  
**Target:** `core:rag` (chunking, BM25 lexical, hybrid, reranking), `core:tokenization` (Granite Embedding 107M Multilingual `384-dim`, seq256, `INT32[1,256]` → `FLOAT32[1,384]` L2-norm), `backend:rag-litert` (LiteRT CPU embedder), `features:rag` (app-private document store + dense index), ingestion/parsing/metadata, vector search, citation provenance, incremental updates. Lexical-only first, then dense. **Port from NpuHub 6.6 `core:tokenization` + `core:rag` — 3 PRs. Planned.**

## 15. Android Automation

**Current:** Device-validated `AccessibilityAutomationService` (`canRetrieveWindowContent`, `canTakeScreenshot`, `flagIncludeNotImportantViews`), `NodeSnapshotter` (≤400 nodes, depth 24), `ElevatedShell`/`PrivilegedUserService` (`UID 2000`, argv allowlist, no raw shell), **Tools Dashboard**, Xiaomi lock guide (`Lock 🔒 + No restrictions + Autostart`). `install -r` keeps grant.

**Target:** Foreground-bound, per-step-confirmed recipes with loop/time limits, global stop, persistent audit, service-death recovery — then **Shizuku v1 recipes** (`scoped user recipes`) + `input.keyevent` etc. **Planned** (harness first).

## 16. Hybrid Local / Cloud / Remote AI

**Current:** Local-only; cloud/remote **missing**.  
**Target:** Local **CPU/GPU/NPU** + Cloud **OpenAI/Anthropic/Gemini/OpenRouter** (OpenAI-compatible) + Remote **Ollama / LAN/desktop** — user-configurable multiple providers, routing by capability/privacy/latency/cost/model/task/device/network/preference/battery/thermal, fallback truthful, auth/cost tracking/quotas/privacy controls, OpenAI-compatible loopback server (`localhost` Phase 3) with health/models/streaming chat, auth, LAN explicit+warning. **Planned after unified inference.**

## 17. Security Hardening

**Current:** Hash-chained `ToolAuditLedger` (`APP_PRIVATE_HASH_CHAIN_V1`, fsync, full-chain verification), approval-before-authority, replay guard, `validate_repo.sh` (size/binary/docs/token), architecture boundaries (`network only in platform:download`), no `MANAGE_EXTERNAL_STORAGE`.

**Target:** Evidence states `AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN` (never claim unmeasured as active), permission discovery + one-run/task-scoped grants, fail-closed, emergency stop, tamper-evident history (tool/timestamp/authorization/digest/result/verification, no sensitive content). See `docs/SECURITY.md`. **Additive.**

## 18. Multimodal — Vision/Audio/Speech

**Current:** Screenshot → in-memory ARGB (11+), Bangla OCR scaffold.  
**Target:** Separate text/image/document/audio interfaces + STT (Whisper-class `large-v3` Q4 streaming) + TTS (parallel streaming + barge-in VAD, bounded PCM, echo suppression) + image generation — each as versioned local-only plugin contract (`plugins/api`). **Long-term.**

## 19. Knowledge Graph / Mind Map

**Current:** Missing — spec only.  
**Target:** Modular interactive graph (documents/concepts/code/functions/projects/tasks/memories/entities → `references/related-to/depends-on/derived-from/belongs-to/modifies/created-from`) with zoom/pan/search/filters/expansion, inspired by Obsidian usability, **must not complicate core**. See `docs/KNOWLEDGE_GRAPH.md`. **Future, after RAG.**

## 20. Product Hardening & Release

**Current:** `v0.9.7` production-signed (`lai-release` RSA-4096 V1–V4, SHA-256 `80:03:8D…7E:8E`), `16` modules, `0.93 MB`, `169 tests`, `work:2.10.1`, Actions majors current, **debug signed `0.1.139 @22 tok/s` on SM8735**, `storage/LAI/models` + `install -r` rule.  
**Target:** SBOM (`app/build/sbom` lightweight → CycloneDX), provenance/SLSA, reproducible builds, supply-chain hardening, Play-ready. **Planned.**

---

**Device test mandatory gates (never claim without log):** Vulkan 40–60 tok/s on Adreno 825, QNN DLC on HTP, OCR quality on printed/handwritten Bangla set, tool dispatch harness, thermal `Reduced CPU threads…` notice, multi-step loop, RAG citations.

**Additive evolution:** Keep `16` (→ ~22) modules, boundaries, `storage/LAI/models`, adaptive `little 7 → big 0-3`, Bangla-first — extend via `core:tokenization`, `core:rag`, `core:pipeline`, `core:agent`, `backend:litert/rag-litert`, `features:rag/ocr` as in NpuHub, rather than rewrite.
