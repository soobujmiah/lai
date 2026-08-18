# NpuHub vs LAI — What you would miss if you only keep LAI

**Date:** 2026-08-18 — **read-only comparison, no code changed** as requested.  
**Sources:** `soobujmiah/NpuHub` (private, Phase 6.6) and `soobujmiah/lai` (`main` @ `0263d30` + `17ad75b` + `c4732fe` — CPU `20 tok/s` on SM8735).

> **One sentence:** NpuHub is a **vendor-agnostic, offline-first platform** (LiteRT + RAG + pipeline + full agent + OCR + benchmark). LAI is a **Bangla-first, Snapdragon-first on-device LLM + Android automation runtime** (llama.cpp + Accessibility/Shizuku + adaptive cores). They share ~30% (offline, privacy, scheduler, hash-chained audit) but diverge hard after that.

---

## 1. What NpuHub has that LAI is *missing* today

| # | NpuHub module (real, tested) | What it does | LAI status | Gap if you want “all NpuHub in LAI” |
|---|---|---|---|---|
| **T1** | `core/tokenization` — **Verified SentencePiece unigram** | NFKC + reviewed separator/drop sets, dummy prefix, Viterbi segmentation, fairseq id remap — validated vs `sentencepiece` reference on reviewed vocab | **Missing** — LAI relies on llama.cpp’s internal tokenizer, no separate verified tokenizer | Port the entire `core:tokenization` + its `sentencepiece` reference tests if you need deterministic embedding tokenization |
| **R1** | `core/rag` — **Chunking + BM25 lexical retrieval + citation provenance** | Bounded deterministic chunking, BM25-style scoring, citation tracking; `core:rag` 6.6 foundations | **Planned** (`ROADMAP Phase 10`) — not built | You’d need to port `core:rag` wholesale |
| **R2** | `core/rag` + `backend:rag-litert` — **Embedding contracts + LiteRT embedder** | Contracts `RagEmbeddingInputShaping`, `RagEmbeddingOutput`, `RagEmbeddingProvider` seam; **pinned profile `Granite Embedding 107M Multilingual` TFLite** — seq256, **384-dim** (corrected from 768), `INT32[1,256]` → `FLOAT32[1,384]` masked-mean-pooled L2-norm; `LiteRtRagEmbeddingProvider` CPU-only, fail-closed `EMBEDDING_*` codes; retrieval still `LEXICAL_ONLY` until dense index (6.6.3) | **Missing** — no embedding model, no `RagEmbeddingProvider` | This is the biggest RAG gap — 107M model (~400 MB) + LiteRT CPU adapter |
| **R3** | `features/rag` — **Document store + cited-retrieval tab** | App-private document store, staging of verified artifacts, dense index (future) | **Missing** — no RAG UI | Port `features:rag` + storage |
| **P1** | `core/pipeline` — **DAG validation + ordering** | Validates DAG, topological order, cycle detection | **Missing** — LAI has no pipeline abstraction | Needed for any multi-step agent |
| **A1** | `core/agent` — **Full agent core (Phase 7)** | Intent → Planner (deterministic) → Memory (bounded auditable) → Tool registry → **One-run approval grants** → **Fail-closed step executor** (cancellation + emergency stop) → **Independent verification**; `AGENT_CORE.md` + remaining `argument binding / intent matching` future | **LAI: one-shot only** — `ToolInstructionGate` + `AgentRuntime.parseToolProposal` (single JSON) + hash-chained `ToolAuditLedger` + `AGENT`: no planner, no memory, no multi-step loop, no verification | To reach NpuHub you must replace one-shot with the full `Intent→Plan→Approve→Execute→Verify` loop |
| **B1** | `backend/litert` — **Bundled + reviewed-installed LiteRT CPU (real inference)** | LiteRT `Environment` + `CompiledModel` CPU-only, `FLOAT32 [1,8,8,3]` → 192 outputs validated against fixture `x3` contract, measured latency only on `CompiledModel.run` | **Missing** — LAI has **no LiteRT**; only `llama.cpp` CPU + scaffold Vulkan | Port `backend:litert` if you want LiteRT as second runtime (vendor-agnostic path) |
| **B2** | `backend/rag-litert` — **LiteRT embedder CPU** | Runs the 107M embedder CPU-only behind the same seam | **Missing** | See R2 |
| **O1** | `backend/ocr` + `features/ocr` — **Verified Tesseract 5.5.3 Bengali OCR stack** | **Provenance profile:** Tesseract `5.5.3` @ `db0ec62f` + `tessdata_fast/ben.traineddata` @ `874164...` (855,841 bytes, SHA `311630...`), **only `ben`**, `image/png` ≤32 MiB, `GRAY8` 4096×4096, LSTM OEM 1, PSM 3; `ReviewedOcrByteVerifier` (64 KiB, exact size/SHA), `ReviewedOcrRunAuthorizer` (60s evidence, 5-min preview/consent, one-use grant), `PngRasterDecoder` (pure JVM, `Inflater`/`CRC32`, BT.601 luma, white-alpha), EXIF/`pHYs` DPI, `features:ocr` stages artifact app-privately and runs **one reviewed recognition per approval** | **LAI: `PlaceholderBanglaOcrEngine` → `OcrModelRequiredException`** — contracts + `OcrResult` JSON scaffold only | Port the entire Phase 5B.1–5B.2B OCR stack if you want NpuHub’s OCR parity |
| **F1** | `features/textgen` — **Reviewed text-gen: tokenizer import + tokenize preview** | Verified tokenizer import, tokenize preview UI | **LAI: has *real* generation via llama.cpp (NpuHub doesn’t yet!)** — different path | NpuHub’s textgen is still *contract only*; LAI is ahead on *actual* LLM inference |
| **F2** | `features/models` — **Verified inventory + per-run approval + grant preparation** | Exact digest/size check, bounded preview, one-run grant, atomic staging | **LAI: `ModelRepository` + catalog trust but simpler (signed `models-v1.json` + SHA-256, no one-run grant)** | Port the stricter one-run grant model if you want NpuHub’s security level |
| **F3** | `benchmark` — **Deterministic JSON/Markdown/CSV telemetry export** | Exports only `SchedulerDecisionTelemetry` (no prompts, no model bytes) | **LAI: `DiagnosticsReportV1` JSON + lightweight SBOM (`sbom-*.txt`)** — similar but less structured | Port `benchmark` if you want NpuHub’s stable export schema |
| **D1** | `docs/*` — **Deep contracts** | `LOCAL_AI.md`, `RAG_EMBEDDING_*`, `OCR_EXECUTION_BOUNDARY`, `BACKENDS`, `CATALOG`, `TELEMETRY` — each with exact byte/phase/revision | **LAI: `docs/` lighter, 128 MB policy, but no equivalent depth for RAG/tokenization** | You’d need to mirror those docs |

**Count:** NpuHub has **~9 major subsystems** LAI doesn’t yet (T1, R1, R2, R3, P1, A1, B1/B2, O1, F1/F3 at NpuHub depth).

---

## 2. What LAI has that NpuHub *doesn’t* (or is stronger)

| LAI strength | Detail | NpuHub equivalent | Why it matters for you |
|---|---|---|---|
| **Snapdragon 8s Gen 4 specialization** | SM8735 `1×X4 +3×A720 +4×A520`, Adreno **825** (you corrected 750), adaptive cores `little 7 idle → big 0-3 burst`, thermal governor `1→2` threads + batch `32`, `20 tok/s` prefill you just validated `0.1.139` | NpuHub is **vendor-agnostic** — `backend:cpu` is synthetic probe, `backend:litert` is CPU-only, no SM8735/Adreno/QNN tuning | For **3B–5B clever Bangla models**, your 8s Gen 4 tuning is the moat |
| **Bangla-first** | System prompt `~180 chars` (short, no literal translation), `values-bn`, `ToolInstructionGate` with inflection-tolerant Bangla stems | NpuHub is multilingual (Granite 107M 384-dim does incl. Bengali) but **not Bangla-first** | Your “no literal translation, admit ignorance” quality pass |
| **Android automation** | `AccessibilityAutomationService` + `AccessibilityGateway` (400 nodes, `flagIncludeNotImportantViews`, `canTakeScreenshot`), `Shizuku` `UID 2000` + `argv` allowlist, **15 tools** (`screen.snapshot/click/type/scroll`, `system.global_action`, `app.launch`, `shell.operation`, `ocr.current_screen`, …) + hash-chained no-backup `ToolAuditLedger` + `ToolInstructionGate` | NpuHub `core:agent` is **generic** (no Accessibility/Shizuku, no `screen.*` tools) — it’s planning/memory/verification without Android authority | You have **real device control**, NpuHub has **real planning** — complementary |
| **Workspace `storage/LAI/models` auto-import** | SAF `ACTION_OPEN_DOCUMENT_TREE`, `WorkspaceDiscovery` bounded traversal (depth 4, 256 files, 8 GB cap, SHA streaming), `install -r` keeps `LAI/` + auto-import on launch (`17ad75b`) | NpuHub `features:models` is stricter (one-run grant, atomic staging) but **not** SAF workspace-centric | Your rule: *model lives in `storage/LAI/models`, APK stays <128 MB* — NpuHub would need that |
| **Real LLM inference today** | **llama.cpp CPU + KV reuse `25×` (17s→0.6s) + streaming `trySendBlocking`** → you generate `9–25 tokens` @ `12 tok/s` decode **right now** | NpuHub **text generation still “contract foundation only”** — no `TEXT_GENERATION` execution; its real inference is the **tiny `1×8×8×3` x3 LiteRT fixture** (192 outputs) | For your “work and text further with clever strong bigger model” — LAI is ahead |
| **Xiaomi lock guide** | `AutomatorScreen` card `Lock 🔒 + No restrictions + Autostart` — solves HyperOS killing Accessibility | No equivalent | UX moat for your Redmi Turbo 4 Pro |
| **Vulkan scaffold for Adreno 825** | `dlopen libvulkan.so` probe, `LOG_GGML_VULKAN` ready (proper CI `SPIRV-Headers` fix in `0263d30`) | NpuHub has no Vulkan — its GPU path is `backend:litert` CPU | Your 3B GPU path |

---

## 3. If you want “keep ALL NpuHub functionality in LAI” — what you must add

**In priority order (my suggestion, matching your `CPU → GPU → NPU → Agent → Workstation`):**

1. **`core/tokenization`** → `core:tokenization` (SentencePiece unigram) — ~1 file, but needs the `sentencepiece` reference test vocab. **Effort: 1 PR.**
2. **`core/rag` + `backend:rag-litert` + `features/rag`** — the whole RAG vertical (chunking, BM25, embedding contracts, Granite 107M, `features:rag` UI). **Effort: 3 PRs** (largest).
3. **`core/pipeline`** — DAG validation. **Effort: 1 PR**, tiny.
4. **`core/agent` full** — replace one-shot with `Intent → Plan → Memory → Approve → Execute → Verify` + emergency stop. **Effort: 2 PRs**.
5. **`backend/litert` (real)** — if you want vendor-agnostic CPU fallback beside llama.cpp. **Effort: 1 PR** (but you already have llama.cpp, so optional).
6. **`backend/ocr` full Tesseract 5.5.3 stack** — provenance, `ByteVerifier`, `RunAuthorizer`, `PngRasterDecoder`, `features:ocr` staging. **Effort: 2 PRs**, biggest native work.
7. **`benchmark` + stricter `features:models` one-run grants** — for NpuHub-level telemetry/security. **Effort: 1 PR**.

**Total:** **~11 PRs** to reach NpuHub parity, while keeping your SM8735/Adreno/Bangla/Shizuku moats. You would **keep LAI’s `storage/LAI/models` + adaptive cores + Tools Dashboard** — NpuHub doesn’t have those.

**Alternative — keep them separate:**
*   **LAI = your product** (Bangla-first Snapdragon automation + LLM)
*   **NpuHub = your platform/reference** (vendor-agnostic RAG/agent/OCR)
*   Share `core:tokenization` and `core:rag` as **shared modules** (extract to `plugins/api`-like shared artifact) instead of porting everything.

---

## 4. My recommendation — don’t port all at once

You already fixed `CPU @20 tok/s` and `Vulkan scaffold available true` on Adreno 825. For your goal *“work and text further with clever strong bigger model”*:

**Next 3 PRs in LAI (until device test mandatory):**
1. **Vulkan real generate** (`n_gpu_layers=99` on Adreno 825) — unlocks **3B Q4_K_M (~1.7 GB)** without QNN
2. **QNN/HTP isolated runtime** (licensed QAIRT) — unlocks **5B**
3. **Agent multi-step** (the NpuHub `core:agent` loop) — so “clever” isn’t just prompt, it’s planning

Then **cherry-pick from NpuHub** only what you need: `core/tokenization` + `core/rag` (if you want citations), not the whole Tesseract stack unless you need printed Bangla OCR now.

**Decision is yours — I changed nothing, as you asked.** Tell me `port RAG`, `port OCR`, or `keep LAI lean (Vulkan → QNN)` and I’ll queue exactly that.

