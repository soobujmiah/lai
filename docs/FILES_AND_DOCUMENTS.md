# Files & Documents Intelligence

LAI shall provide **SAF-based file and document intelligence** over `storage/LAI/` — not `MANAGE_EXTERNAL_STORAGE`.

## Current

`WorkspaceRepository` (SAF `ACTION_OPEN_DOCUMENT_TREE`, persistable `GRANTED/REVOKED`), `WorkspaceDiscovery` (bounded `depth 4`, `256` files, `8 GB` cap, `SHA-256` streaming, `ModelFormatDetector` `GGUF` magic), `WorkspaceSettingsStore` (`settings.json` atomic), `Keep copy` export (verified, survives uninstall). No `MANAGE_EXTERNAL_STORAGE`. `validate_repo.sh` forbids `*.apk/*.so/*.gguf` in repo (`937 KB <128 MB`).

## Target

**File types:** `PDF/DOCX/XLSX/PPTX/Markdown/TXT/CSV/JSON/XML/YAML/HTML/images/archives/source code` (as in `NpuHub` `core:rag` + `docs/FILES_AND_DOCUMENTS`).

**Capabilities (per type, bounded, streaming):**
*   `read/search/extract/summarize/compare/transform/generate/cite/organize/classify` — all via `core:rag` + `core:tokenization` (Granite 107M `384-dim`, `INT32[1,256]→FLOAT32[1,384]`) + `backend:rag-litert` (LiteRT CPU) + `features:rag` (app-private document store + dense index).
*   **Ingestion:** Explicit user tap (file picker or `LAI/models` auto-import pattern) → `ModelCandidate` (path/size/SHA/format) → `WorkspacePolicy.classify` → `DiscoveredModel` (`REVIEWED/LOCAL_UNREVIEWED/REJECTED`).
*   **Parsing:** Bounded `PDF`/`DOCX` parser (like NpuHub `gen_pdf_fixtures.py`), `PngRasterDecoder`-style pure JVM where possible — no native until reviewed.
*   **Retrieval:** Lexical-only `BM25` first, then dense `384-dim` + hybrid + reranking — `AVAILABLE/SUPPORTED/ACTIVE/MEASURED` never claimed without `MEASURED` value.
*   **Citations:** Provenance (`sha256/size/path`), `LEXICAL_ONLY` until dense index (`6.6.3` in NpuHub).

## UI

`Files` tab (like `Home/Chat/Agent`), `RAG` tab (cited retrieval), `Documents` viewer (code blocks, tables, progress). Auto-import `storage/LAI/models/*.gguf` already does this pattern for models.

## Testing

`gen_ocr_smoke_corpus.py`-style fixtures, parser fuzz, `256`-file scan cap, `WorkspacePolicy` `64` files + `4 GiB` file limit, cancellation between `llama_decode` chunks, `install -r` grant persistence.

## Privacy

`storage/LAI/` grant is **user-owned, coarse counts only** (no file names in diagnostics), `DiagnosticsPrivacy` excludes `documents/tool_arguments/tool_outputs`.
