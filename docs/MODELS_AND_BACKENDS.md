# Models, product configuration, Model Center and native acceleration

This existing document is the canonical product specification for model acquisition and per-tool configuration; the repository does not maintain a separate `PRD.md`, so these requirements are merged here rather than duplicated in a new file.

## Model lifecycle

1. User enters a Hugging Face HTTPS file URL in Developer Mode.
2. `ModelRepository` validates explicit consent, reviewed host, ID, and mandatory SHA-256 through `LocalFirstPolicy`.
3. Download streams to `<id>.gguf.part` in app-private no-backup storage.
4. A partial file is resumed with an HTTP Range request when supported.
5. LAI calculates SHA-256 and verifies the first four bytes are `GGUF`.
6. The file is renamed to `<id>.gguf`; metadata is written atomically to `registry.json`.
7. A selected backend opens it through an opaque JNI session handle.
8. **Keep copy** writes a user-selected document, reopens it, and verifies SHA-256 before reporting success.

The app requests neither all-files access nor media storage permission. Android removes the optimized private model on uninstall, but a user-owned Keep copy in Documents/Downloads remains. After reinstall, Import file verifies and restores the private runtime copy without redownloading.

## Reviewed built-in catalog

`core:model` ships immutable metadata only—never weights. The current recommended CPU baseline is the official Qwen 2.5 1.5B Instruct Q4_K_M artifact (1,117,320,736 bytes, Apache-2.0, SHA-256 `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e`). Metadata, build compatibility and Snapdragon CPU execution are reviewed. One coherent Bangla sample exists, but broad Bangla quality remains explicitly unvalidated.

The app fetches a detached-signature-verified supported-model catalog on explicit refresh, caches verified bytes for offline browsing, and keeps an embedded fallback. Each model has one-tap explicit download. Android file import accepts only bytes matching reviewed SHA-256, exact size and GGUF signature. Manual URL entry remains Developer Mode only.

Catalog revision 3 describes compatibility per artifact: format, quantization, context size, compatible backend IDs, preferred/fallback order, estimated peak memory, required ABIs, and validation evidence. A logical model may later have separate GGUF and converted accelerator artifacts; LAI will not claim they are interchangeable. Schema changes remain signed and backward compatible, and the app refuses a signed catalog older than its embedded revision.

## Bangla model requirements

Candidate chat models should be evaluated, not merely advertised as multilingual. Acceptance includes:

- native-script Bangla instruction following;
- Bangla/English code switching;
- Unicode normalization and punctuation;
- safety responses that preserve Bangla meaning;
- tool-call JSON validity after Bangla prompts;
- perplexity/quality and human preference on a versioned Bangla set;
- license allowing mobile distribution or user-initiated download.

Tokenizer changes require model retraining or compatible vocabulary; LAI must not silently bolt a new tokenizer onto an incompatible GGUF.

## Product configuration and contextual quick settings (target)

LAI uses typed, versioned per-tool configuration rather than one unvalidated map. Configuration has three layers, highest precedence first:

1. **one-request override** from the Chat quick-settings sheet;
2. **conversation/tool-session override**, cleared with New chat or tool-session reset;
3. **saved defaults** from `config/settings.json`, with embedded safe defaults when no workspace is granted.

The Chat top bar shows a **⚙** icon when a model or attached tool has tunable parameters. It opens a bottom sheet for only the current selection. The sheet provides **Apply to this request**, **Save as default**, and **Reset**; changing a slider must not reload a model until the user applies it. Values are validated before they reach Kotlin/native contracts, and unsupported controls are disabled with the backend/model reason.

Initial typed controls:

| Tool | Parameter | Product range / values | Initial default and notes |
|---|---|---|---|
| Image Generation | Steps | integer `1..50` | `20`; backend may publish a lower tested maximum |
| Image Generation | CFG scale | decimal `1.0..20.0` | `7.0`; finite values only |
| Image Generation | Aspect ratio | reviewed presets such as `1:1`, `4:3`, `3:4`, `16:9`, `9:16` | `1:1`; adapter maps preset to supported dimensions and pixel budget |
| Image Generation | Seed | signed 64-bit integer or `Random` | `Random`; resolved seed is shown with the result for local reproducibility |
| LLM | Temperature | decimal `0.0..2.0` | `0.7`; `0` selects deterministic/greedy behavior where supported |
| LLM | Top-P | decimal `0.05..1.0` | `0.9` |
| LLM | Max new tokens | integer `1..4096`, additionally bounded by remaining context | current 1.5B test flow stays at `256` until changed by a reviewed release |
| Voice/TTS | Speed | decimal `0.5x..2.0x` | `1.0x` |
| Voice/TTS | Pitch | decimal `0.5x..2.0x` or adapter-declared equivalent | `1.0x`; the UI displays the adapter's unit |
| Voice/STT | Noise filter | `Off`, `Low`, `Medium`, `High` | `Medium`; quality/latency trade-off must be stated |
| Vector Search | Chunk size | integer `128..2048` tokenizer tokens | `512`; actual tokenizer/model ID is stored with the index |

`settings.json` stores schema version, tool/model-scoped defaults, and migration metadata only. It must not store credentials, prompts, generated media/text, document chunks, Accessibility content, shell output, or external-provider secrets. Writes use temp-document + replace semantics where the provider supports them; otherwise LAI writes a new version, verifies it, then switches the active document reference. Invalid, non-finite, unknown-security-sensitive, or out-of-range values fail closed to defaults and produce a local migration warning.

## Categorized Model Center (target)

The current signed one-model list remains active while the product evolves into a categorized **Model Center**. Categories are Chat/LLM, OCR & Vision, Speech-to-Text, Text-to-Speech, Embeddings & Rerankers, and Image Generation. Each card shows artifact size, format/quantization, license/source, language/quality evidence, compatible backends/hardware, estimated memory/storage, installed/download state, and whether the artifact is reviewed, local-unreviewed, or device-validated.

Model Center acquisition paths:

- **Reviewed catalog download:** explicit user action; signed metadata; exact size/SHA-256/format checks before atomic activation.
- **Background download:** WorkManager-backed, promoted to an Android foreground notification for long multi-gigabyte transfers; visible progress; user pause/resume/cancel; HTTP Range/ETag-aware `.part` continuation; network/storage constraints; process-death recovery; no activation until final rehash. Pausing closes transport and preserves only bounded partial bytes plus public download metadata.
- **Manual local `.gguf` import:** Android SAF picker or user-selected LAI workspace; no broad-storage permission. Known hashes inherit signed catalog compatibility. Unknown GGUF files receive structural/size/hash checks and `LOCAL_UNREVIEWED` status, require explicit load approval/Developer Mode according to policy, and never gain quality/license/backend claims automatically.
- **Auto-discovered workspace artifact:** registration only after the bounded startup pipeline in [ARCHITECTURE.md](ARCHITECTURE.md#startup-restore-and-model-auto-discovery-target); loading still requires user selection and a verified private runtime copy.

The downloader may contact only approved public artifact endpoints under existing `LocalFirstPolicy`. Model Center must not upload model inventory, prompts, usage, device documents, or failure content. Download/pause state is product metadata; inference continues offline with already installed artifacts.

## Backend routing design

Generic core does not enumerate vendors. Concrete adapters publish opaque IDs and compatibility facts. The current ID is `llama-cpu`; planned IDs include `llama-vulkan` and, only after a real adapter exists, an implementation-owned Qualcomm QNN/HTP ID.

```text
Model artifact requirements + DeviceProfile
  |- require compiled and runtime-probed adapter
  |- require declared format compatibility
  |- require memory/battery/thermal policy
  |- require physical-device validation for acceleration
  |- prefer real measured performance, then adapter/model preference
  `- choose a compatible fallback or report all rejection reasons
```

A backend is shown as available only after compile-time linkage and runtime probing. Manufacturer/SoC strings provide diagnostic context but never establish backend availability. The scheduler contains no Qualcomm-specific branch; see [VENDOR_BACKEND_STRATEGY.md](VENDOR_BACKEND_STRATEGY.md).

### CPU / llama.cpp (Phase 2)

The current adapter pins upstream commit `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f` (release `b10448`). CI verifies the full SHA before compilation; no upstream source is committed.

Implemented:

- arm64 Android NDK static linkage into `liblai_runtime.so`;
- mmap GGUF loading with CPU-only layer policy;
- explicit 4,096-token context and bounded 512-token prompt batches;
- 2–8 thread routing based on hardware concurrency;
- model-native chat-template application with a Bangla-first system message;
- top-p/temperature/distribution sampling and greedy mode;
- per-token JNI callbacks, UTF-8/UTF-16-safe conversion, and cooperative cancellation;
- explicit load/unload controls in Developer Mode.

Still required before calling it production-ready:

- broaden measured Bangla response-quality evidence beyond the coherent samples already observed;
- physical Stop/recovery, New chat reset, and forced context-trimming validation;
- deterministic golden-prompt smoke artifact suitable for CI;
- 10-minute memory and thermal evidence;
- explicit long-conversation summarization policy beyond oldest-turn omission.

### Adreno Vulkan (Phase 2)

- use llama.cpp/ggml Vulkan only at a tested pinned commit;
- probe extensions and memory budget;
- maintain operator fallback rather than failing the session;
- measure prefill/decode separately;
- prevent UI jank by isolating queues and controlling thermal load.

### QAIRT/QNN Hexagon HTP (Phase 3)

This is the current hardware-acceleration priority and is intentionally Qualcomm-specific. GGUF and QNN context binaries are not interchangeable. QNN deployment normally requires model export/quantization, supported operator partitioning, QAIRT runtime libraries, and device-specific validation. Claims of arbitrary 1B–3B GGUF direct NPU offload are therefore not made by this repository.

The Qualcomm boundary will be a dedicated runtime adapter, not part of `core` or `runtime:llama`. It may own QAIRT/QNN headers and libraries, HTP identifiers, JNI/C++, graph/context formats, conversion recipes, RPC/shared-buffer handling, and Snapdragon probes. It must project only generic descriptors, capabilities, events, and metrics to LAI.

The production adapter should support:

- signed/hashed converted-artifact manifest;
- HTP architecture, firmware, and runtime compatibility probe;
- context-binary cache invalidation;
- bounded RPC/shared-buffer lifecycle;
- graph partition telemetry behind Developer Mode;
- explicit fallback and correctness comparison with the CPU reference;
- licensing review for every packaged runtime library.

Another vendor can implement the same generic inference and capability contracts in its own module; none of the QNN types or cache rules become requirements of that implementation.

## Native ABI

`NativeBindings` currently exposes:

```kotlin
runtimeInfo(): String
createSession(modelPath, backend, contextSize): Long
countTokens(session, roles[], contents[]): Int
generate(session, roles[], contents[], sampling, callback): LongArray?
destroySession(session)
lastError(): String
```

`generate` streams UTF-safe pieces through the callback and returns local timing counters. Conversation history is supplied on each request so context state is deterministic; JNI converts Java UTF-16 to standard UTF-8 for llama.cpp. The llama adapter maps its public namespaced IDs (`llama-cpu`, later `llama-vulkan`) to private C++ registry names. Model files stay in native-accessible app-private storage.

## Memory planning

Before loading, estimate:

```text
weights + KV cache + compute graph + backend staging + safety margin
```

Refuse a load that would cross a configurable fraction of available memory. On Android trim-memory callbacks, stop generation and release optional buffers before risking LMKD termination.
