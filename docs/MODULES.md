# Module ownership and dependency rules

| Module | Owns | May depend on |
|---|---|---|
| `core:contracts` | serializable tool, automation, inference, model, OCR and shell contracts | Kotlin/coroutines/serialization only |
| `core:policy` | agent consent, local-first data-flow policy, shell argv allowlist | contracts |
| `core:scheduler` | backend evidence and thermal/battery/memory routing | contracts |
| `plugins:api` | versioned local-only plugin manifest and constrained context | contracts, policy |
| `platform:download` | the only network transport, artifact streaming/hash/activation | core |
| `platform:accessibility` | Accessibility service, node capture, screenshots and UI actions | contracts |
| `platform:shizuku` | Shizuku binder/UserService and privileged process boundary | contracts, policy |
| `runtime:llama` | JNI/C++ llama.cpp adapter | contracts |
| `runtime:ocr` | OCR Android bitmap adapter seam | contracts |
| `runtime:orchestrator` | tool dispatch across policy and authority adapters | core + required platform/runtime adapters |
| `app` | application composition, lifecycle and Compose UX | any reviewed public module API |

## Rules

- Core never imports Android or implementation modules.
- Platform modules never depend on app or inference runtime modules.
- Runtime adapters expose core contracts and do not own product UI.
- Only app composes concrete implementations.
- Only the download platform owns network transport.
- Every new module declares its privacy, authority and test boundary in documentation.
- Feature modules will be introduced for Chat, Reader, Automator and Settings when their state/navigation surfaces are separated from `MainViewModel`; they will depend on contracts, never concrete vendor APIs.
