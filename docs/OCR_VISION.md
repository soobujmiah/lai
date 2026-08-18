# OCR / Vision

LAI shall provide **on-device, privacy-safe OCR and vision** — no cloud.

## Current

**Scaffold:** `OcrEngine` contracts (`OcrRequest` bilingual `bn-BD`, `OcrResult` `blocks/language/confidence/polygon/handwriting` JSON), `BanglaOcrService` dispatch, `PlaceholderBanglaOcrEngine` fails `OcrModelRequiredException`. Screenshot `AccessibilityService.takeScreenshot` (Android 11+, ARGB, `buffer.close()`), `NodeSnapshotter` (400 nodes). **No model bundled.**

## Target — Keep NpuHub-grade Provenance (Phase 5B)

**Provenance profile (execution-enabled revision 2):**
*   Engine `Tesseract 5.5.3` @ `db0ec62f81b0737fbbe184d8fea40af5738f8eef` + `tessdata_fast/ben.traineddata` @ `87416418657359cb625c412a48b6e1d6d41c29bd` (`855,841 bytes`, SHA `31163084c279aaebd376216f0c3d5c17ad4b5fee8db49dae79c20000b5de5964`), `Apache-2.0`, CPU only.
*   Fixed: `bn`/`bn-BD` only, `image/png` ≤32 MiB, raster `4096×4096`/`16.7M` pixels, `GRAY8` `rowBytes==width`, DPI `70..1200` or `null`, Tesseract `ben`, `OEM 1` (LSTM), `PSM 3`, no extra variables.

**Contracts (pure JVM, already in NpuHub, port to LAI):**
*   `ReviewedOcrByteVerifier` (64 KiB, exact size/SHA, cancellation-aware, bounded `INPUT_LIMIT_EXCEEDED` etc.)
*   `ReviewedOcrRunAuthorizer` (60s evidence freshness, 5-min preview/consent, one-use `OcrRunPreparationGrant`, `authorizesExecution=false` until resolver)
*   `PngRasterDecoder` (pure JVM `Inflater`/`CRC32`, `GRAY8`, `BT.601` luma, white-alpha, `tRNS` palette, `pHYs` DPI)
*   EXIF orientation/`pHYs` DPI derivation, `LocalOcrRasterMetadata` (dimensions/stride/format/orientation/DPI, no bytes).

**Execution (future, app-private, one per approval):**
*   `features:ocr` stages artifact app-privately, `backend:ocr` (reviewed native adapter) repeats boundary before `executeOcr`, emits `OcrResult` JSON (`blocks` with `polygon`, `confidence`, `handwriting` flag) for `AgentRuntime` (`ocr.current_screen`).

**Beyond Bengali:**
*   Multilingual OCR (same contract, additional `traineddata`), screenshot OCR (current path), document OCR (via `FILES_AND_DOCUMENTS`), visual Q/A / image understanding / screen understanding — each as `core:ai` `LocalOcrRequest` (opaque `LocalInputReference` + `bn-BD` + bounded) + `Backend` `OCR` workload, `AVAILABLE/SUPPORTED/ACTIVE/MEASURED`.

## Testing

`gen_ocr_smoke_corpus.py` fixtures, `ModelCandidate` magic `GGUF` + `PNG` header, `32 MiB` / `16.7M` pixel bounds, `CRC` mismatch, `decode`/`verify` cancellation, **handwritten vs printed Bangla quality set** (`CER/WER`, `UNEVALUATED` until measured).

## Device Test Mandatory

Printed + handwritten Bangla test set, `Adreno 825` not needed — CPU only, `install -r` grant persistence.
