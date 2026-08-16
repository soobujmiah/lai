# Third-party notices

Last audited: 2026-08-17

LAI is licensed under Apache License 2.0. The repository references third-party libraries, build tools, native source, and a model artifact that retain their own licenses. This register is documentation, not legal advice; exact versions and upstream license files must be re-verified for every release artifact.

## Runtime dependencies

| Component | Version/source | Purpose | Declared/common upstream license | Distribution note |
|---|---|---|---|---|
| AndroidX Core/Activity/Lifecycle/DataStore/Compose | version catalog / Compose BOM | Android UI/lifecycle/storage | Apache-2.0 | preserve notices required by packaged artifacts |
| Kotlin standard/tooling and kotlinx coroutines/serialization | Kotlin 2.1.21; coroutines 1.10.2; serialization 1.8.1 | language/runtime/concurrency/JSON | Apache-2.0 | verify resolved transitive notices |
| OkHttp | 4.12.0 | catalog/model HTTPS | Apache-2.0 | verify Okio/transitives |
| Shizuku API/provider | 13.1.5 | privileged binder integration | Apache-2.0 | Shizuku app/service is separate software |
| llama.cpp | pinned commit `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f` | local GGUF inference | MIT | include upstream MIT notice in distributed native artifact notices |

## Test/build dependencies

JUnit 4.13.2, Android Gradle Plugin, Gradle, Android SDK/NDK/CMake, GitHub Actions, and their transitive tools are used to test/build the project. Build-tool licensing and redistribution differ from runtime packaging; none should be copied into the APK merely because CI uses them.

## Models and vendor components

The repository contains no model weights or Qualcomm SDK/runtime binaries. The reviewed Qwen artifact is acquired separately by explicit user action and is documented in [`MODEL_LICENSES.md`](MODEL_LICENSES.md). Future QAIRT/QNN components require separate licensed acquisition and redistribution review.

## Release gate

Before production distribution, generate the resolved dependency graph, collect exact license/NOTICE texts from resolved artifacts and native sources, compare them with this register, include required notices in the APK/release bundle, and fail release on unknown or incompatible licensing.
