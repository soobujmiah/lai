# Models and native acceleration

## Model lifecycle

1. User enters a Hugging Face HTTPS file URL in Developer Mode.
2. `ModelRepository` validates host, ID, and optional SHA-256.
3. Download streams to `<id>.gguf.part` in app-private no-backup storage.
4. A partial file is resumed with an HTTP Range request when supported.
5. LAI calculates SHA-256 and verifies the first four bytes are `GGUF`.
6. The file is renamed to `<id>.gguf`; metadata is written atomically to `registry.json`.
7. A selected backend opens it through an opaque JNI session handle.

The app requests neither all-files access nor media storage permission. Uninstalling the app removes its models.

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

```text
AUTO
 ├─ QNN/HTP if exact model context is compatible and runtime probe passes
 ├─ Vulkan if device and required operators pass a startup self-test
 └─ CPU as correctness fallback
```

A backend is shown as available only after compile-time linkage and runtime probing. Phase 1 returns an empty backend list.

### CPU / llama.cpp (Phase 2)

- pin upstream by immutable commit;
- fetch only in GitHub Actions;
- compile arm64 with Android NDK;
- expose cancellation and streaming callbacks;
- bound context, batch, thread count, and memory;
- run a deterministic prompt/golden UTF-8 smoke test.

### Adreno Vulkan (Phase 2)

- use llama.cpp/ggml Vulkan only at a tested pinned commit;
- probe extensions and memory budget;
- maintain operator fallback rather than failing the session;
- measure prefill/decode separately;
- prevent UI jank by isolating queues and controlling thermal load.

### QAIRT/QNN Hexagon HTP (Phase 3)

GGUF and QNN context binaries are not interchangeable. QNN deployment normally requires model export/quantization, supported operator partitioning, QAIRT runtime libraries, and device-specific validation. Claims of arbitrary 1B–3B GGUF direct NPU offload are therefore not made by this repository.

The production adapter should support:

- signed/hashed model manifest;
- HTP architecture and runtime compatibility probe;
- context-binary cache invalidation;
- RPC/shared-buffer lifecycle;
- graph partition telemetry behind Developer Mode;
- per-operator fallback and correctness comparison;
- licensing review for every packaged runtime library.

## Native ABI

`NativeBindings` currently exposes:

```kotlin
runtimeInfo(): String
createSession(modelPath, backend, contextSize): Long
generate(session, prompt, configJson): String
destroySession(session)
lastError(): String
```

Phase 2 will replace whole-response `generate` with a callback/cancellable stream while preserving session ownership. JNI inputs are UTF-8 Java strings; model files stay on native-accessible app storage.

## Memory planning

Before loading, estimate:

```text
weights + KV cache + compute graph + backend staging + safety margin
```

Refuse a load that would cross a configurable fraction of available memory. On Android trim-memory callbacks, stop generation and release optional buffers before risking LMKD termination.
