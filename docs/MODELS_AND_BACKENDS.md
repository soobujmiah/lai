# Models and native acceleration

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
