# Third-party compliance registry

**Status:** master planning registry  
**Date:** 2026-08-18  
**Root registers (unchanged):** [`../../THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md), [`../../THIRD_PARTY_LICENSES.md`](../../THIRD_PARTY_LICENSES.md), [`../../MODEL_LICENSES.md`](../../MODEL_LICENSES.md)  
**Audit:** [`../LEGAL_AND_LICENSING.md`](../LEGAL_AND_LICENSING.md)  
**Related ADR:** [`../decisions/ADR-0108-third-party-license-governance.md`](../decisions/ADR-0108-third-party-license-governance.md)

This file extends the root registers with a fielded inventory. It does **not** replace upstream license texts. A component is not “verified licensed” merely because a similar version is commonly published under a known SPDX identifier.

Verification statuses:

| Status | Meaning |
|---|---|
| VERIFIED | Exact version/source license text was read in this documentation cycle |
| REGISTERED | Recorded in the 2026-08-17 root registers; version may be stale |
| DECLARED | Present in LAI Gradle/CI/docs; upstream text not re-read for the live version |
| PLANNED | Not in the repository; future candidate |
| UNKNOWN | License not established; fail closed |

Unknown or incompatible licenses block inclusion and production release.

## Registry field definitions

Each row aims to record: name, version, source URL, repository, license, SPDX, copyright owner, distribution / attribution / NOTICE / modification requirements, patent or trademark notes, bundled vs fetched vs linked, runtime/build/test/model/dataset/SDK/service flags, commercial-use and redistribution limits, verification date, verification source, verification status.

Where a cell is not established, it is left UNKNOWN.

---

## A. Direct runtime libraries (declared in `gradle/libs.versions.toml`)

| Name | Version | Source / module | SPDX (common or registered) | Bundled in APK (typical) | Role | Commercial-use note | Verification |
|---|---|---|---|---|---|---|---|
| AndroidX Core KTX | 1.16.0 | `androidx.core:core-ktx` | Apache-2.0 | Yes (classes) | Runtime | Permissive; preserve notices | DECLARED; REGISTERED as family 2026-08-17 |
| AndroidX Activity Compose | 1.10.1 | `androidx.activity:activity-compose` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| AndroidX Lifecycle runtime-ktx | 2.9.1 | `androidx.lifecycle:lifecycle-runtime-ktx` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| AndroidX Lifecycle runtime-compose | 2.9.1 | `androidx.lifecycle:lifecycle-runtime-compose` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| AndroidX Lifecycle viewmodel-compose | 2.9.1 | `androidx.lifecycle:lifecycle-viewmodel-compose` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| AndroidX DataStore Preferences | 1.1.7 | `androidx.datastore:datastore-preferences` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| AndroidX Compose BOM | 2025.05.01 | `androidx.compose:compose-bom` | Apache-2.0 | Platform BOM | Runtime | Same | DECLARED / REGISTERED family |
| Compose UI / Foundation / Material3 / tooling-preview | via BOM | `androidx.compose.*` | Apache-2.0 | Yes | Runtime | Same | DECLARED / REGISTERED family |
| Compose UI Tooling | via BOM | `androidx.compose.ui:ui-tooling` | Apache-2.0 | Debug only | Debug | Same | DECLARED |
| Kotlin stdlib / tooling | 2.4.10 | JetBrains | Apache-2.0 | Yes (stdlib) | Runtime / build | Register still lists 2.1.21 | DECLARED; register stale |
| kotlinx-coroutines-core | 1.11.0 | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | Apache-2.0 | Yes | Runtime | Register lists 1.10.2 | DECLARED; register stale |
| kotlinx-coroutines-android | 1.11.0 | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Apache-2.0 | Yes | Runtime | Same | DECLARED; register stale |
| kotlinx-serialization-json | 1.11.0 | `org.jetbrains.kotlinx:kotlinx-serialization-json` | Apache-2.0 | Yes | Runtime | Register lists 1.8.1 | DECLARED; register stale |
| OkHttp | 5.4.0 | `com.squareup.okhttp3:okhttp` | Apache-2.0 | Yes | Runtime (`platform:download`) | Register lists 4.12.0; Okio transitive UNKNOWN until SBOM | DECLARED; register stale |
| AndroidX Work Runtime KTX | 2.10.1 | `androidx.work:work-runtime-ktx` | Apache-2.0 (common) | Yes | Runtime | **Absent from 2026-08-17 registers** | DECLARED |
| Shizuku API | 13.1.5 | `dev.rikka.shizuku:api` | Apache-2.0 (registered) | Yes | Runtime | Upstream README states additional name/icon restrictions. **LEGAL REVIEW REQUIRED** | REGISTERED; POM text not re-read |
| Shizuku Provider | 13.1.5 | `dev.rikka.shizuku:provider` | Apache-2.0 (registered) | Yes | Runtime | Shizuku manager app is separate software | REGISTERED |

Transitive Maven artifacts: **UNKNOWN**. Production release requires a resolved SBOM ([`SBOM_AND_PROVENANCE.md`](SBOM_AND_PROVENANCE.md)).

---

## B. Test and coverage

| Name | Version | SPDX (common or registered) | Shipped in APK? | Verification |
|---|---|---|---|---|
| JUnit 4 | 4.13.2 | EPL-1.0 (register) | Intended test-only | REGISTERED; distribution scope still to verify per release |
| JaCoCo | 0.8.13 | EPL-2.0 (common; **not registered**) | Build/test reports | DECLARED in root `build.gradle.kts`; **UNKNOWN** until upstream text is attached to a release review |

---

## C. Build toolchains (not committed; obtained on CI)

| Name | Version in CI / catalog | License posture | Redistribution from this Git tree | Verification |
|---|---|---|---|---|
| Android Gradle Plugin | 9.3.1 | Vendor / per-artifact | No | DECLARED |
| Gradle | 9.5.0 (`android_build.yml`); docs still mention 8.13 | Apache-2.0 (common) | No wrapper JAR in Git | DECLARED |
| Android SDK / Build Tools | API 36 / 36.0.0 | Google SDK terms | Forbidden | DECLARED |
| Android NDK | 27.0.12077973 | LLVM / NDK terms | Forbidden | DECLARED |
| CMake | 3.22.1 | BSD-style (common) | CI only | DECLARED |
| JDK 17 Temurin | CI | GPLv2 + Classpath Exception (typical) | CI only | DECLARED |
| GitHub Actions: checkout@v7, setup-java@v5, setup-android@v4, setup-gradle@v6, upload-artifact@v7 | major tags | typically MIT (per action; tag text UNVERIFIED) | CI only | DECLARED |

---

## D. Native engine fetched at build time (not stored in Git)

| Name | Version / pin | Repository | License | Copyright observed | Bundled source? | Linked into `liblai_runtime.so` when enabled | NOTICE / attribution | Verification |
|---|---|---|---|---|---|---|---|---|
| llama.cpp | `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f` | `https://github.com/ggml-org/llama.cpp` | MIT | `Copyright (c) 2023-2026 The ggml authors` (upstream LICENSE at that commit) | No | Yes, if `LAI_ENABLE_LLAMA_CPP=ON` | MIT notice must accompany distributed native binaries | **VERIFIED** upstream LICENSE at pinned commit, 2026-08-18 |
| llama.cpp third-party internals | as vendored by that commit | various | mixed (upstream acknowledges MIT, public-domain, BSD components for tools LAI disables) | UNKNOWN per file | No | Possibly via ggml | Must be inventoried from the pinned tree before production | UNKNOWN |
| SPIRV-Headers | Ubuntu package on runner | Khronos | commonly Apache-2.0 | UNKNOWN | No | Build-time | Package license UNVERIFIED | DECLARED (CI apt) |
| SPIRV-Tools | Ubuntu package | Khronos | mixed Apache/other (common) | UNKNOWN | No | Build-time | UNVERIFIED | DECLARED |
| glslang | Ubuntu `glslang-tools` | Khronos / Google | BSD-style (common) | UNKNOWN | No | Build-time | UNVERIFIED | DECLARED |
| libvulkan-dev | Ubuntu package | Khronos / vendor | Apache-2.0 MIT mix (common) | UNKNOWN | No | Build-time headers | Device Vulkan driver is not this package | DECLARED |

---

## E. Models, datasets, OCR (not in Git unless noted)

| Name | Version / pin | Source | SPDX / field | Bundled? | Commercial-use | Redistribution | Verification |
|---|---|---|---|---|---|---|---|
| Qwen 2.5 1.5B Instruct Q4_K_M GGUF | catalog id `qwen2.5-1.5b-instruct-q4-k-m`; SHA-256 `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e`; 1,117,320,736 bytes | `https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF` | Catalog field Apache-2.0; upstream LICENSE file is Apache License 2.0 (fetched 2026-08-18) | No | Catalog claims Apache-2.0; re-verify card + LICENSE before LAI redistributes | Not redistributed by LAI Git; user download | VERIFIED license file exists as Apache-2.0; commercial-use restatement **LEGAL REVIEW REQUIRED** if LAI ships weights |
| User-imported GGUF | n/a | User | UNKNOWN | No | Fail closed; `LOCAL_UNREVIEWED` | LAI must not redistribute | Policy only |
| Tesseract 5.5.3 | planned pin `db0ec62f81b0737fbbe184d8fea40af5738f8eef` | tesseract-ocr | docs say Apache-2.0 | No | UNKNOWN until pin is fetched | Not present | PLANNED |
| tessdata_fast `ben.traineddata` | planned pin `87416418657359cb625c412a48b6e1d6d41c29bd`; 855,841 bytes; SHA `31163084c279aaebd376216f0c3d5c17ad4b5fee8db49dae79c20000b5de5964` | tessdata_fast | docs say Apache-2.0 | No | UNKNOWN until pin is fetched | Not present | PLANNED |
| Bangla OCR / handwriting datasets | none | none | UNKNOWN | No | Fail closed | Fail closed | PLANNED / UNKNOWN |
| Granite Embedding 107M | planned | IBM Granite family | UNKNOWN | No | UNKNOWN | UNKNOWN | PLANNED |
| Whisper large-v3 Q4 | planned | OpenAI / community GGUF | UNKNOWN | No | UNKNOWN | UNKNOWN | PLANNED |

See [`MODEL_AND_DATA_LICENSE_POLICY.md`](MODEL_AND_DATA_LICENSE_POLICY.md).

---

## F. Future SDKs and runtimes (not present)

| Name | Status | License | Commercial / redistribution | Gate |
|---|---|---|---|---|
| Qualcomm QAIRT / QNN | PLANNED | UNKNOWN until licensed acquisition | Typically contractual; no source/binary in Git | Block until agreement + redistribution review |
| LiteRT / TensorFlow Lite | PLANNED | UNKNOWN for chosen artifact | UNKNOWN | Block until pin + text |
| ONNX Runtime | PLANNED | commonly MIT; UNVERIFIED | UNKNOWN | Block until pin + text |
| Future vendor NPU SDKs | PLANNED | UNKNOWN | UNKNOWN | Vendor-neutral adapter rule (ADR 0005) |
| SQLCipher | mentioned as future encrypted vector DB | often BSD + optional commercial | UNKNOWN | Block until pin |
| PRoot / QEMU (Linux runtime docs) | PLANNED | QEMU historically GPL | Combination with Apache-2.0 product is **LEGAL REVIEW REQUIRED** | Fail closed until reviewed |
| SentencePiece | planned with tokenization | commonly Apache-2.0; UNVERIFIED | UNKNOWN | Block until pin |

---

## G. Creative assets, fonts, icons, APIs, services

| Name | Present? | License | Notes | Verification |
|---|---|---|---|---|
| `ic_lai.xml` | Yes | Treated as LAI-authored unless proven otherwise | Original vector; no third-party notice | DECLARED |
| Material Icons Extended | No | n/a | Intentionally avoided | n/a |
| Bundled fonts | No | n/a | Platform `sans` only | n/a |
| Raster images / audio / video | No | n/a | — | n/a |
| Cloud AI provider SDKs (OpenAI, Anthropic, Gemini, OpenRouter) | No | Provider terms | Future adapters; user-supplied keys; no silent upload | PLANNED |
| Ollama / local OpenAI-compatible servers | No | Various | User-operated remote; LAN opt-in required | PLANNED |
| Hugging Face artifact hosting | Used as download origin for reviewed GGUF | Hosting ToS + model license | HTTPS allowlist in download policy | DECLARED as transport, not a shipped library |
| GitHub raw catalog hosting | Used for signed catalog | GitHub terms | Catalog bytes accepted only after ECDSA verify | DECLARED |

---

## H. Shizuku trademark / naming notes

Upstream Shizuku documentation states restrictions on using Shizuku artwork and on presenting an app as Shizuku (name / certain permission names). LAI uses the API/provider artifacts and treats the Shizuku manager as separate software. Those extra terms are not restated here as LAI legal conclusions. **LEGAL REVIEW REQUIRED** before any branding that could be confused with Shizuku.

---

## I. APK META-INF packaging

[`app/build.gradle.kts`](../../app/build.gradle.kts) excludes `META-INF/AL2.0`, `META-INF/LGPL2.1`, `META-INF/LICENSE.md`, and `META-INF/NOTICE.md`. This is a common Android duplicate-file workaround. It can drop notices that Apache-2.0 §4 and some third-party licenses expect to travel with Object form.

Which dependency first required the exclude list is **UNKNOWN** from current comments. Replacement notice packaging is documented in [`../release/COMMERCIAL_RELEASE_COMPLIANCE.md`](../release/COMMERCIAL_RELEASE_COMPLIANCE.md). No packaging change is made in this phase.

---

## J. Process

1. Adding a row is required in the same change that adds a dependency, model, dataset, SDK, or asset.
2. Status UNKNOWN fails closed for production.
3. Root registers must be updated when versions change; this file records the fielded inventory.
4. Production CI must eventually emit a resolved SBOM and fail on unknown licenses ([`SBOM_AND_PROVENANCE.md`](SBOM_AND_PROVENANCE.md)). Those controls remain MISSING/PARTIAL.
