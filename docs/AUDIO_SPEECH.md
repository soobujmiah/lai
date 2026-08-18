# Audio / Speech

LAI shall provide **local, privacy-safe audio and speech** — no cloud by default.

## Current

**Scaffold only** — `OcrEngine` contracts exist, but **no STT/TTS** implementation. `Tool` `ocr.current_screen` is the only media interface.

## Target (future, modular, after RAG)

*   **STT:** `Whisper-class` `large-v3` `Q4` streaming `STT` `backend:stt-litert` (like `backend:rag-litert` LiteRT CPU) — `STT` `backend` `STT` workload, `AVAILABLE/SUPPORTED/ACTIVE/MEASURED`, `streaming` `Flow<String>` (partial), `cancellation` (`isCancelled`), `bounded` `32`-token chunk, `language` `bn-BD` + `en`, `model` `whisper-large-v3-q4_0` `~1.5 GB` in `storage/LAI/models` (same `install -r` rule).
*   **TTS:** Parallel streaming `TTS` + `barge-in` VAD — `sentence/phrase` chunks synthesize while `LLM` streams, `Interrupt VAD` stops queued `PCM` + active `TTS` + `generation` with bounded `~200 ms` latency, `echo-suppression` policy, `PCM` `16kHz` `16-bit` `WAV`, `AndroidKeystore` not needed, `plugins/api` versioned contract (`LaiPlugin`).
*   **Contracts:** `SttRequest` (`opaque LocalInputReference` + `bn-BD` + bounded `audio` `size` + `SHA-256` + `sampleRate`) → `SttResult` (`blocks` with `confidence`, `language`, `polarity`) + `TtsRequest` (`text`, `voice`, `language`, `rate`) → `TtsResult` (`pcm` `ByteArray`, `duration`, `phoneme`).
*   **UI:** `Voice` tab (like `Chat` + `Screen Reader` + `Automator`), `Hold to speak` (`STT` streaming), `Speak` (`TTS` `play`/`pause`/`barge-in`), `Tool` `stt.current_audio` / `tts.speak`.

## Architecture

`features:voice` (UI) → `backend:stt-litert`/`backend:tts` (LiteRT CPU, like `backend:rag-litert`) → `core:ai` contracts (`LocalSttRequest`/`LocalTtsRequest`), never `platform` direct. `validate_repo.sh` still `128 MB` (no `*.wav`/`*.tflite` in repo).

## Testing

`STT` `large-v3` `Q4` on-device `WER` vs `NpuHub` `gen_textgen_smoke_corpus.py`-style fixtures, `TTS` `barge-in` `200 ms` latency, `PCM` queue `bounded`, `install -r` `LAI/models` persistence.

## Status

`FUTURE` — scaffold only, no `AVAILABLE`, no `MEASURED`.
