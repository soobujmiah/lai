# System architecture blueprint

## 1. Scope

LAI separates product UX, agent decisions, Android control, model storage, recognition, and native inference so each can be replaced or tested without granting another component more authority. Phase labels are part of the design: unavailable adapters fail closed and are never presented as accelerated.

## 2. Compile-time module layers

The enforced graph is documented in [MODULES.md](MODULES.md). Core contracts, policy and scheduling are pure JVM; network, Accessibility and Shizuku each have one platform owner; JNI/C++ lives in runtime adapters; `app` is the composition root. See [the NpuHub comparison](ARCHITECTURE_COMPARISON_NPUHUB.md) and [ADR 0002](adr/0002-modular-local-first-backbone.md).

## 3. Runtime layers

```mermaid
flowchart TB
  subgraph Presentation[Presentation process]
    UI[Jetpack Compose]
    VM[MainViewModel]
    UI --> VM
  end

  subgraph Orchestration[Policy and orchestration]
    AR[AgentRuntime]
    TC[ToolDefinition registry]
    CG[Confirmation gate]
    AR --> TC
    TC --> CG
  end

  subgraph Android[Android integration]
    AS[AccessibilityAutomationService]
    SS[Screen screenshot]
    SH[ElevatedShell]
    SZ[Shizuku binder]
    AS --> SS
    SH --> SZ
  end

  subgraph Intelligence[Intelligence plugins]
    MR[ModelRepository]
    OE[OcrEngine]
    IE[InferenceEngine]
    JNI[NativeBindings / JNI]
    BR[C++ Backend registry]
    CPU[llama.cpp CPU]
    VK[llama.cpp Vulkan]
    QNN[QAIRT/QNN HTP]
    IE --> JNI --> BR
    BR -. phase 2 .-> CPU
    BR -. phase 2 .-> VK
    BR -. phase 3 .-> QNN
  end

  VM --> AR
  VM --> MR
  VM --> IE
  CG --> AS
  CG --> SH
  SS --> OE
```

### Presentation

`LaiApp` shows only three primary modes. Runtime details and model URL controls are behind Developer Mode. `MainViewModel` performs lifecycle-bound coroutine work and exposes immutable state.

### Agent/tool policy

The model-facing format is `ToolCall(id, name, arguments)`. `AgentRuntime` resolves only registered tools, checks confirmation, validates arguments, and returns `ToolResult`. The model never receives Java/Kotlin object references, Binder objects, accessibility nodes, or a shell.

### Accessibility

`AccessibilityAutomationService` is Android-bound and main-thread confined. `AccessibilityGateway` holds only a weak service reference. Every operation obtains a fresh active root. Snapshots are flattened to immutable, serializable nodes and bounded to 400 nodes / depth 24. Password text and descriptions are omitted.

Selectors are deterministic in this order:

1. fully qualified view resource ID;
2. hierarchy path;
3. exact visible text;
4. exact content description.

### Elevated operations

`ShizukuController` observes binder/permission state. `ShellCommandPolicy` accepts named structured operations and emits an argv list. A Shizuku `UserService` executes `ProcessBuilder(argv)` under shell/root identity; the removed/private `newProcess` API is not used. No command string is passed through `sh -c`; package names, namespaces, keys, values, and key codes are validated. Mutations require confirmation. Output is limited to 64 KiB and execution to 10 seconds by default.

### Model storage

`ModelRepository` lives in the only network-owning module and streams directly to app-private no-backup storage. It supports HTTP Range resume, mandatory SHA-256, explicit-user-action and reviewed-host policy, redirect revalidation, and GGUF magic validation. Registry replacement is write-then-rename. The repository contains no weights. Installed models are loaded only after an explicit user tap, preventing multi-gigabyte startup allocations.

### Native inference

Kotlin owns one `InferenceEngine`; JNI maps opaque integer handles to shared C++ `BackendSession` instances. C++ validates file existence, GGUF magic, context range, and backend availability. The Phase 2 CPU candidate uses pinned llama.cpp, clears context memory per request, applies the model chat template, evaluates bounded prompt batches, samples tokens, and streams only complete UTF-8 code-point sequences through a cancellable JNI callback. Vulkan and QNN remain unavailable adapters, so capability reporting stays truthful.

A production adapter must provide:

- bounded context and allocation plan;
- cancellation and token callback;
- UTF-8-safe streaming;
- backend capability probe;
- thermal/memory events;
- deterministic session destruction;
- CPU fallback when acceleration cannot load.

### OCR

Accessibility screenshot capture creates an ARGB bitmap only in memory. `BanglaOcrService` passes it to an `OcrEngine`; output uses schema version 1 with full text, blocks, BCP-47 language, confidence, polygon, and optional handwritten classification. The current placeholder returns a typed model-required error.

## 4. Control flow

```mermaid
sequenceDiagram
  actor U as User
  participant UI as Compose
  participant L as Local LLM
  participant A as AgentRuntime
  participant X as Accessibility/Shizuku
  U->>UI: Request a task
  UI->>L: Prompt + available tool schemas
  L-->>UI: Proposed ToolCall
  UI->>U: Confirmation for consequential action
  U-->>UI: Approve / deny
  UI->>A: ToolCall + confirmation bit
  A->>A: Validate name, schema, policy
  A->>X: Typed operation
  X-->>A: Bounded result
  A-->>L: ToolResult JSON
  L-->>UI: User-facing response
```

The current UI exposes safe manual demonstrations; feeding tool proposals from a concrete LLM is Phase 2. The confirmation bit must originate in trusted UI state, never in model-authored JSON.

## 5. Threads and ownership

| Work | Dispatcher/thread | Owner |
|---|---|---|
| Compose/state | Main | Activity/ViewModel |
| Accessibility node operations | Main immediate | Accessibility service |
| Model catalog/policy/scheduling | caller / pure JVM | core model, policy and scheduler |
| Android memory/battery/thermal snapshot | short synchronous platform call | platform:device |
| Model network and hashing | IO | platform:download ModelRepository |
| Shizuku process streams | IO coroutines | ElevatedShell |
| OCR preprocessing/inference | Default or plugin-owned | OcrEngine |
| Native token generation | dedicated native worker (Phase 2) | BackendSession |

No accessibility node survives a command. Bitmaps are recycled after OCR. Native session handles are destroyed by `InferenceEngine.close()`.

## 6. Trust boundaries

1. **Untrusted model output:** tool names and arguments require strict parsing.
2. **Untrusted visible UI:** screen text can contain prompt injection; it is data, not authority.
3. **Accessibility authority:** can affect other apps; disabled by default and user-enabled in system settings.
4. **Shizuku authority:** shell/root identity varies; UID is surfaced and operations are allowlisted.
5. **Downloaded model:** size/hash/format are validated; model license and provenance remain user responsibilities.
6. **CI secrets:** signing and future proprietary SDK credentials exist only in GitHub Actions secret scope.

## 7. Plugin seams

- `InferenceEngine`: llama.cpp, ExecuTorch, MNN, or QNN adapter.
- C++ `Backend`: CPU, Vulkan, QNN/HTP.
- `OcrEngine`: TFLite, ONNX Runtime/QNN, or packaged service plugin.
- future `ToolProvider`: RAG, STT/TTS, calendar, files.

Plugins must publish capability and safety metadata rather than relying on type discovery.

## 8. Performance design for Snapdragon 8s Gen 4

- arm64-only initial artifact avoids unused ABI payload.
- streaming file I/O avoids model-sized heap allocations.
- no bitmap crosses JNI in Phase 1.
- C++ uses hidden visibility and section garbage collection.
- future Vulkan path should use persistent buffers, mapped staging pools, and device capability probes.
- future QNN path should cache HTP context binaries by model hash, SoC/firmware ID, QAIRT version, and quantization recipe.
- UI never waits synchronously for network, OCR, shell, or inference work.

Performance claims require device evidence; see [DEVICE_TESTING.md](DEVICE_TESTING.md).
