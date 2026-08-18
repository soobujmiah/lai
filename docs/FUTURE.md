# Future

LAI shall evolve **additively** — `CPU (16–22 tok/s) ✅ → GPU Vulkan (Adreno 825, 40–60 tok/s) → NPU QNN/HTP (licensed) → Unified inference → Model capability detection → Hybrid local/cloud/remote → Tool ecosystem (60 tools) → Multi-step agent → Developer AI → Linux/Terminal → Files/Documents → OCR/Vision → Memory → RAG → Knowledge graph → Security hardening → Multimodal → Product hardening** — as `MASTER_ROADMAP` `0–20`.

## Near-term (next 3 PRs, until device test mandatory)

1. **Vulkan real `generate()`** (`n_gpu_layers=99` on Adreno 825) — `0263d30` proper `SPIRV-Headers` on CI already, `available true` next, then `generate()` wire → **3B `Q4_K_M` (~1.7 GB)** feasible on SM8735 `4.0 GB` free.
2. **QNN/HTP isolated `runtime:qnn`** (licensed `QAIRT` via `secrets.QAIRT_URL`, `GGUF→DLC` + INT4) → **5B**.
3. **`core:tokenization` (SentencePiece unigram, NFKC, Viterbi)** → `core:rag` + `backend:rag-litert` (Granite 107M `384-dim`) + `features:rag` (dense index) — port from NpuHub `6.6`.

## Mid-term (after RAG)

`core:pipeline` DAG → `core:agent` full loop (`Intent→Plan→Memory→Approve→Execute→Verify`) → `features:agent` `TASK→PLAN→STEP→VERIFY` UI → `benchmark` `CycloneDX` + `SLSA` + reproducible builds.

## Long-term (after agent)

`Developer workstation` (`Inspect→Understand→Plan→Modify→Build→Test→Verify→Document` over `storage/LAI/`), `Linux` `PRoot/QEMU`, `Files/Documents` intelligence (`PDF/DOCX/XLSX`), `OCR` `Tesseract 5.5.3` `ben`, `Knowledge graph` `Obsidian`-style, `Multimodal` `Whisper STT` + `parallel TTS + barge-in`.

## Never

Claim `Vulkan 40 tok/s`, `QNN 5B`, `OCR ben` `MEASURED` without SM8735 log; bundle `1.1 GB` `*.gguf` in repo (`128 MB` cap); add `MANAGE_EXTERNAL_STORAGE` or raw `sh -c`; bypass `ToolAuditLedger` approval-before-authority.

## Product Feel

One system: `Home/Chat/Agent/Tools/Files/Documents/Projects/Developer/Terminal/Knowledge/Models/Automation/Device/Settings` with **progressive disclosure** + **command palette** + **search**, not 13 top-level tabs. `Chat` stays streaming `primaryContainer`/`surfaceVariant` `18 dp` `id`-keyed; `Agent` shows `TASK→VERIFY`; `RAG` shows citations; `Knowledge` shows graph.

## Directive

**Document first, understand current state, follow documentation, implement one phase at a time, test with real evidence, update documentation, continue from documented state.** This `docs/` system is the persistent memory.
