# RAG / Knowledge Management

LAI shall provide **local, privacy-safe RAG** — lexical first, then dense, with citations.

## Current

**Missing** — only `OcrEngine` scaffold. No `core:rag`, no embedding, no vector store.

## Target — Port from NpuHub 6.6 (additive)

*   **`core/tokenization` (SentencePiece unigram):** NFKC + reviewed separator/drop sets, dummy prefix, Viterbi segmentation, fairseq id remap — validated vs `sentencepiece` reference on reviewed vocab. Needed for `Granite` tokenization.

*   **`core/rag`:** Deterministic bounded chunking (like NpuHub `core:rag`), `BM25`-style lexical retrieval with citation provenance. `DiscoveryLimits` (`64` files, `4 GiB` file, `gguf` only today) + `ModelCandidate`/`DiscoveredModel` pattern reused.

*   **`RagEmbedding` (NpuHub 6.6 pinned):** **Granite Embedding 107M Multilingual TFLite** — `multilingual incl. Bengali`, `seq256`, **384-dim** (corrected from 768), `INT32[1,256]` → `FLOAT32[1,384]` masked-mean-pooled L2-norm. Contracts `RagEmbeddingInputShaping`, `RagEmbeddingOutput`, `RagEmbeddingPinnedProfiles.GRANITE_SEQ256`. `backend:rag-litert` `LiteRtRagEmbeddingProvider` CPU-only, fail-closed `EMBEDDING_*` codes. **Model bytes never bundled** — downloaded `storage/LAI/models` like LLM.

*   **Contracts:** `RagEmbeddingProvider` seam (like `InferenceEngine`), `LEXICAL_ONLY` until `features:rag` stages verified artifacts and builds dense index (`6.6.3` in NpuHub). No `MEASURED` / retrieval-accuracy claim without `MEASURED` value.

*   **Flow:** `ingestion` (explicit tap, `ACTION_OPEN_DOCUMENT_TREE`) → `parsing` (PDF/DOCX/TXT/MD/CSV/JSON — `PngRasterDecoder`-style pure JVM) → `chunking` (bounded) → `metadata` (path/sha/size/format) → `BM25` → `embeddings` (107M) → `vector search` (app-private `SQLCipher` + `AndroidKeystore` as in NpuHub `Enc Vector DB`) → `hybrid retrieval` → `reranking` → `citations` + `provenance` + `incremental updates` + `deletion`.

*   **Store:** App-private `SQLCipher` + `Keystore`-wrapped key material, versioned `embedding/model` metadata, bounded migrations, explicit `index deletion/export` controls (as NpuHub `Enc Vector DB`).

## UI

`RAG` tab (cited retrieval) + `Files` tab (document store). Auto-import `storage/LAI/models` pattern reused for documents: `storage/LAI/documents`.

## Testing

`gen_rag_*` fixtures, `LEXICAL_ONLY` vs `dense` A/B, `384-dim` shape check, `INT32[1,256]` bounds, `EMBEDDING_*` fail-closed codes, `SQLCipher` migration, `install -r` grant persistence.

## Device Test Mandatory

On-device embedding latency + retrieval quality on a **versioned Bengali/English corpus** — no claim without `MEASURED`.
