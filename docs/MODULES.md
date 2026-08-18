# Module ownership and dependency rules

| Module | Owns | May depend on |
|---|---|---|
| `core:contracts` | serializable tool, automation, inference, model, OCR, settings, workspace and shell contracts; workspace grant/settings-store/discovery ports; opaque backend IDs/descriptors | Kotlin/coroutines/serialization only |
| `core:policy` | agent consent, strict built-in tool schemas/parser, local-first data-flow policy, shell argv allowlist, typed settings validation/migration, settings session semantics (saved defaults vs one-request override), workspace discovery classification and bounded settings codec | contracts |
| `core:scheduler` | vendor-neutral device profile, compatibility evidence, thermal/battery/memory routing | contracts |
| `core:model` | immutable reviewed artifact catalog and trust metadata | contracts |
| `plugins:api` | versioned local-only plugin manifest and constrained context | contracts, policy |
| `platform:download` | the only network transport, artifact streaming/hash/activation | core |
| `platform:audit` | app-private no-backup JSONL tool audit, fsync append, bounded parsing and hash-chain enforcement | contracts, policy |
| `platform:device` | Android manufacturer/SoC/API/ABI/CPU facts plus memory, battery, charging and thermal observations | scheduler |
| `platform:accessibility` | Accessibility service, node capture, screenshots and UI actions | contracts |
| `platform:workspace` | user-granted SAF tree, persistable permission, canonical child resolution, bounded model discovery and non-secret settings store; implements the pure workspace ports | contracts, policy |
| `platform:history` | app-private no-backup chat session persistence; the only content-bearing store | contracts |
| `platform:shizuku` | Shizuku binder/UserService and privileged process boundary | contracts, policy |
| `runtime:llama` | JNI/C++ llama.cpp adapter | contracts |
| `runtime:ocr` | OCR Android bitmap adapter seam | contracts |
| `runtime:orchestrator` | tool dispatch across policy and authority adapters | core + required platform/runtime adapters |
| `app` | application composition, lifecycle and Compose UX | any reviewed public module API |

## Rules

- Core never imports Android or implementation modules.
- Platform modules never depend on app or inference runtime modules.
- Runtime adapters expose core contracts and do not own product UI.
- Generic inference/scheduler core contains no hardware-vendor or vendor-SDK identifiers; adapters own stable namespaced backend IDs.
- `runtime:llama` owns llama CPU/Vulkan only. A future real QNN implementation gets its own adapter; no placeholder vendor modules are created.
- Only app composes concrete implementations.
- Only the download platform owns network transport.
- Only `platform:audit` owns persistent model-tool audit bytes; arguments and outputs never enter its contract.
- A future SAF workspace owner belongs under `platform`, while settings schemas/discovery decisions remain pure core contracts/policy. (Realized: `platform:workspace` owns SAF; `core` owns the contracts/classifier/codec.)
- A future C++ `TaskGraph` belongs under runtime adapters and schedules compute only; it cannot acquire Android URI, Accessibility, Shizuku, confirmation, or audit authority.
- Every new module declares its privacy, authority and test boundary in documentation.
- Feature modules will be introduced for Chat, Dashboard Tools, Reader, Automator and Settings when their state/navigation surfaces are separated from `MainViewModel`; they depend on shared contracts, never concrete vendor APIs or duplicate tool engines.
