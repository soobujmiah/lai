# LAI current license audit

**Classification:** informational fact record only. Not a relicensing proposal, not a `LICENSE` change, and not a release artifact.  
**Status:** documentation and audit only  
**Decision:** **DECISION REQUIRED**  
**Audit date:** 2026-08-18  
**Audited tree:** public repository `https://github.com/soobujmiah/lai`, default branch `main`, commit `94d88aa` (`docs: complete documentation-first directive — LAI permanent reference`)  
**Created:** 2026-08-16 (`f0f1b81`)  
**GitHub license API field:** `Apache-2.0` (`spdx_id: Apache-2.0`)  
**Repository visibility (GitHub API):** public, not a fork  

This file records what the repository currently contains and what the existing license texts say. It does **not** choose a replacement license, remove Apache-2.0, relicense anything, or give legal advice. Statements about Apache-2.0 quote or paraphrase the license text already present in [`LICENSE`](../LICENSE). Unresolved facts are marked **UNVERIFIED** or **UNKNOWN**.

Existing canonical registers remain unchanged:

- [`LICENSE`](../LICENSE)
- [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) (last audited 2026-08-17)
- [`THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md) (last audited 2026-08-17)
- [`MODEL_LICENSES.md`](../MODEL_LICENSES.md) (last audited 2026-08-17)
- [`docs/legal/licensing.md`](legal/licensing.md)

---

## 0. Method and limits

Inspected: root legal files; `CONTRIBUTING.md`; `SECURITY.md`; Gradle version catalog and every `build.gradle.kts`; `catalog/models-v1.json`; native CMake/CI fetch; Android resources; Git history authors; GitHub metadata; documentation that names licenses; absence of CLA/DCO files.

Not performed in this audit:

- resolving the full Maven/Gradle transitive graph or generating an SBOM;
- downloading model weights or Qualcomm SDKs;
- checking every published GitHub Release APK for shipped notices;
- obtaining a copyright-assignment opinion for bot/human author identities;
- reviewing private `NpuHub` source (docs state it was compared, not copied).

The project's own registers already state that production distribution still requires exact resolved notices, SBOM, provenance, and an unknown-license policy. [`docs/product/feature-matrix.md`](product/feature-matrix.md) lists **Legal/license tracking** as **PARTIAL**.

---

## 1. Exact location of the current Apache-2.0 license

| Location | What it is |
|---|---|
| [`LICENSE`](../LICENSE) (repository root) | Full Apache License Version 2.0, January 2004, text, followed by the standard application notice. SHA-256 `c136452851ff631b190f483946165cd480892ce9195e70a0a7d043ac785df92f`. 190 lines. Present since the first commit `f0f1b81` (2026-08-16). |
| GitHub repository license metadata | Detected/declared as Apache License 2.0 (`Apache-2.0`). |

There is **no** root `NOTICE` file. There are **no** `SPDX-License-Identifier` headers in Kotlin, C++, Python, or shell sources.

The application block at the end of [`LICENSE`](../LICENSE) is:

```text
Copyright 2026 LAI contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

A short pointer also exists at [`docs/legal/licensing.md`](legal/licensing.md) (“Project license: `../../LICENSE`”). That file is a map, not a second license grant.

---

## 2. Copyright notices currently present

### 2.1 Project copyright

| Notice | Where |
|---|---|
| `Copyright 2026 LAI contributors` | Only in [`LICENSE`](../LICENSE) application notice |

No per-file copyright headers were found in application source. No named individual, company, or legal entity appears in a project copyright line. Package namespace is `dev.lai.runtime`. Application id is `dev.lai.runtime`.

### 2.2 Git author identities on `main` (104 commits)

These are commit metadata, **not** copyright assignments:

| Commits | Author string |
|---:|---|
| 39 | `soobujmiah <soobujmiah@users.noreply.github.com>` |
| 39 | `LAI Architecture Bot <lai-architecture@users.noreply.github.com>` |
| 12 | `LAI Agent <agent@arena.ai>` |
| 7 | `LAI Engineering <dev@lai.runtime>` |
| 5 | `dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>` |
| 1 | `miahsobuj <miahsobuj@github.com>` |
| 1 | `Sobuj <56004845+soobujmiah@users.noreply.github.com>` |

Whether those identities are one person, employees, or independent contributors is **UNKNOWN** from repository files alone.

### 2.3 Third-party copyright referenced but not vendored

| Work | Notice documented in this audit |
|---|---|
| llama.cpp (pinned commit, not stored in Git) | Upstream `LICENSE` at commit `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`: `Copyright (c) 2023-2026 The ggml authors` |
| Qwen 2.5 1.5B Instruct GGUF (not stored in Git) | Upstream Hugging Face `LICENSE` for `Qwen/Qwen2.5-1.5B-Instruct-GGUF` is Apache License 2.0. Exact copyright line in that upstream file was not fully extracted in this pass; Alibaba Cloud / Qwen Team attribution is typical for this family and must be re-read from the upstream file before redistribution. |

---

## 3. All files that explicitly reference Apache-2.0

Exact string search for `Apache-2.0`, `Apache License 2.0`, `Apache License, Version 2.0`, or `Apache License` (excluding incidental words such as “COMPLETED”):

| File | Role of the reference |
|---|---|
| [`LICENSE`](../LICENSE) | Full license text and application notice |
| [`README.md`](../README.md) | “Apache License 2.0. Third-party model weights, Qualcomm SDK components, Shizuku, and future inference engines retain their own licenses…” |
| [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) | “LAI is licensed under Apache License 2.0”; AndroidX/Kotlin/OkHttp/Shizuku listed as Apache-2.0 |
| [`THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md) | Same families listed as Apache-2.0 |
| [`MODEL_LICENSES.md`](../MODEL_LICENSES.md) | Catalog field `Apache-2.0` for the reviewed Qwen artifact |
| [`catalog/models-v1.json`](../catalog/models-v1.json) | `"license": "Apache-2.0"` |
| [`core/model/src/main/kotlin/dev/lai/runtime/model/ReviewedModelCatalog.kt`](../core/model/src/main/kotlin/dev/lai/runtime/model/ReviewedModelCatalog.kt) | Embedded fallback catalog `license = "Apache-2.0"` |
| [`docs/MODELS.md`](MODELS.md) | Provenance note: Qwen Apache-2.0 |
| [`docs/MODELS_AND_BACKENDS.md`](MODELS_AND_BACKENDS.md) | Reviewed Qwen artifact described as Apache-2.0 |
| [`docs/OCR_VISION.md`](OCR_VISION.md) | Planned Tesseract + `ben.traineddata` labeled Apache-2.0 (not bundled) |
| [`docs/product/feature-matrix.md`](product/feature-matrix.md) | Legal tracking row: “Apache-2.0 and model/backend cautions” |
| [`docs/legal/licensing.md`](legal/licensing.md) | Points at root `LICENSE` (no SPDX string) |

No source file other than `ReviewedModelCatalog.kt` contains an Apache-2.0 identifier. That identifier describes the **model artifact**, not the Kotlin file’s own license.

---

## 4. Third-party dependencies and their licenses

Declared coordinates come from [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) and module `build.gradle.kts` files. License identifiers below are either (a) copied from the project’s 2026-08-17 registers, or (b) the commonly published upstream SPDX for that project. **Resolved POM/NOTICE texts were not re-downloaded in this audit.** Transitive artifacts are **UNKNOWN** until an SBOM is generated.

### 4.1 Direct runtime libraries (declared)

| Coordinate / family | Declared version (catalog) | License as recorded / commonly published | In 2026-08-17 register? |
|---|---|---|---|
| AndroidX Core KTX | 1.16.0 | Apache-2.0 | Yes (family) |
| AndroidX Activity Compose | 1.10.1 | Apache-2.0 | Yes (family) |
| AndroidX Lifecycle (runtime-ktx, runtime-compose, viewmodel-compose) | 2.9.1 | Apache-2.0 | Yes (family) |
| AndroidX DataStore Preferences | 1.1.7 | Apache-2.0 | Yes (family) |
| AndroidX Compose BOM + ui / foundation / material3 / tooling-preview | BOM 2025.05.01 | Apache-2.0 | Yes (family) |
| Compose UI Tooling | same BOM, `debugImplementation` | Apache-2.0 | Yes (family) |
| Kotlin Gradle plugins / stdlib (via Kotlin 2.4.10) | 2.4.10 | Apache-2.0 | Yes (family; **version stale in register**) |
| kotlinx-coroutines-core / android | 1.11.0 | Apache-2.0 | Yes (family; **version stale**) |
| kotlinx-serialization-json | 1.11.0 | Apache-2.0 | Yes (family; **version stale**) |
| OkHttp | 5.4.0 | Apache-2.0 (Okio typically Apache-2.0) | Yes (**register still says 4.12.0**) |
| AndroidX Work Runtime KTX | 2.10.1 | commonly Apache-2.0 | **No — missing from both registers** |
| Shizuku API | 13.1.5 | Apache-2.0 (see §4.4) | Yes |
| Shizuku Provider | 13.1.5 | Apache-2.0 (see §4.4) | Yes |

### 4.2 Direct test / coverage / build tools

| Tool | Version in tree / CI | License commonly published | Distribution note in repo |
|---|---|---|---|
| JUnit 4 | 4.13.2 | EPL-1.0 (register) | Test only; “verify distribution scope” |
| JaCoCo | 0.8.13 (`build.gradle.kts`) | commonly EPL-2.0 | **Not listed** in third-party registers; used for JVM coverage, not app runtime |
| Android Gradle Plugin | 9.3.1 | Google / Apache-mix per artifact | Build tool |
| Gradle | 9.5.0 (CI `android_build.yml`) | Apache-2.0 | Not committed; obtained on CI. [`docs/BUILD_AND_RELEASE.md`](BUILD_AND_RELEASE.md) still says Gradle 8.13 |
| Android SDK / Build Tools / NDK 27.0.12077973 / CMake 3.22.1 | CI env | vendor/SDK terms | “do not redistribute from source repository” |
| JDK 17 Temurin | CI | GPLv2 + Classpath Exception (typical for Temurin) | Build image only |
| GitHub Actions: `actions/checkout@v7`, `actions/setup-java@v5`, `android-actions/setup-android@v4`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7` | pinned major tags | typically MIT (UNVERIFIED per tag) | CI only |

### 4.3 Native / system build inputs fetched on CI, not stored in Git

| Component | Pin | License observed | How it enters a build |
|---|---|---|---|
| llama.cpp | `ad1de39e0708e3ced9c71bb3c82d93a2c046a73f` from `https://github.com/ggml-org/llama.cpp.git` | MIT; `Copyright (c) 2023-2026 The ggml authors` (upstream `LICENSE` at that commit) | [`scripts/ci/fetch_llama_cpp.sh`](../scripts/ci/fetch_llama_cpp.sh) + `LAI_ENABLE_LLAMA_CPP=ON` |
| SPIRV-Headers, SPIRV-Tools, glslang, libvulkan-dev | Ubuntu packages on the runner | various (Apache-2.0 / BSD / Khronos — **UNVERIFIED per package**) | Vulkan compile of llama.cpp; not committed |

A compiled `liblai_runtime.so` that links llama.cpp would be a **distribution** of MIT-licensed code and must retain the MIT copyright and permission notice. The project registers already require this. Whether current GitHub Release APKs include that notice is **UNVERIFIED**.

### 4.4 Shizuku extra terms (documented by upstream, not copied here as a new grant)

Project registers: Apache-2.0. Upstream Shizuku README additionally forbids using Shizuku launcher artwork and using the Shizuku name / certain permission names as if the app were Shizuku. LAI depends on `dev.rikka.shizuku:api` / `provider` 13.1.5 and treats the Shizuku **app** as separate software. Exact Maven POM text was not re-fetched.

### 4.5 Register vs catalog version drift (documented fact)

[`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) still lists Kotlin 2.1.21, coroutines 1.10.2, serialization 1.8.1, OkHttp 4.12.0. [`docs/implementation/current-state.md`](implementation/current-state.md) still lists Kotlin 2.1.21 and AGP 8.11.0. The live catalog is Kotlin 2.4.10, AGP 9.3.1, coroutines/serialization 1.11.0, OkHttp 5.4.0, compile/target SDK 36.

This is a **documentation inconsistency**, not a second project license.

---

## 5. Bundled third-party source code

**None found in Git.**

Facts supporting that statement:

- `model/` contains only `.gitkeep` and a README; `*.gguf` is forbidden by `scripts/validate_repo.sh`.
- llama.cpp is fetched into `$RUNNER_TEMP` on CI; CMake fails if `LAI_ENABLE_LLAMA_CPP=ON` and the checkout is missing.
- No `third_party/`, `vendor/`, or `external/` source trees.
- No `.so`, `.aar`, `.jar`, `.apk`, `.onnx`, `.tflite` files in the tree.
- [`CHANGELOG.md`](../CHANGELOG.md) states NpuHub was compared “without copying private source.” [`docs/ARCHITECTURE_COMPARISON_NPUHUB.md`](ARCHITECTURE_COMPARISON_NPUHUB.md) repeats that no private source was copied.

LAI-authored native files (not upstream copies):

- `runtime/llama/src/main/cpp/native_inference.cpp`
- `runtime/llama/src/main/cpp/backend_registry.cpp`
- `runtime/llama/src/main/cpp/vulkan_backend.cpp`
- `runtime/llama/src/main/cpp/llama_cpu_backend.cpp` (includes `llama.h` only when llama.cpp is present)
- `runtime/llama/src/main/cpp/include/lai/backend.h`
- `runtime/llama/src/main/cpp/CMakeLists.txt`

Those files have **no** license or copyright header.

[`app/build.gradle.kts`](../app/build.gradle.kts) packaging excludes `META-INF/AL2.0`, `META-INF/LGPL2.1`, `META-INF/LICENSE.md`, and `META-INF/NOTICE.md` from the APK. That is a merge-conflict workaround common on Android. Apache-2.0 §4 still requires giving recipients a copy of the License and retaining notices. Whether the shipped APK satisfies that by other means is **UNVERIFIED**. The exclude list mentioning `LGPL2.1` does **not** mean LAI source is LGPL; it means some dependency may ship that filename.

---

## 6. Model files, model references, and their licenses

### 6.1 In the Git repository

| Item | Present? |
|---|---|
| Model weight files (`.gguf`, `.onnx`, `.tflite`, `.safetensors`, …) | **No** |
| OCR traineddata | **No** |
| Qualcomm QNN / QAIRT binaries | **No** |

[`MODEL_LICENSES.md`](../MODEL_LICENSES.md): “LAI stores no model weights in Git. Model acquisition is an explicit user action, and artifact integrity/provenance metadata does not by itself grant redistribution rights.”

### 6.2 Reviewed catalog artifact (referenced, not bundled)

| Field | Value in catalog + `ReviewedModelCatalog` + `MODEL_LICENSES.md` |
|---|---|
| Catalog ID | `qwen2.5-1.5b-instruct-q4-k-m` |
| Upstream | `Qwen/Qwen2.5-1.5B-Instruct-GGUF` |
| File | `qwen2.5-1.5b-instruct-q4_k_m.gguf` |
| URL | `https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf` |
| SHA-256 | `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e` |
| Bytes | 1,117,320,736 |
| Catalog license field | `Apache-2.0` |
| Upstream LICENSE file (fetched 2026-08-18) | Apache License Version 2.0 text |

The register still requires re-verification at acquisition and before any redistribution.

### 6.3 Local unreviewed models

User-imported or workspace-discovered unknown GGUF files are classified `LOCAL_UNREVIEWED`. LAI does not claim their license. That is documented in [`MODEL_LICENSES.md`](../MODEL_LICENSES.md).

### 6.4 Planned / documented but not present

| Planned component | License note in docs | In repo? |
|---|---|---|
| Tesseract 5.5.3 @ `db0ec62f…` + `tessdata_fast/ben.traineddata` | docs say Apache-2.0 | No |
| Future QAIRT/QNN | UNKNOWN until licensed acquisition | No |
| Whisper large-v3 Q4 (speech docs) | not registered in `MODEL_LICENSES.md` | No |
| Granite Embedding 107M (RAG docs) | not registered | No |
| Bangla handwriting OCR dataset | “dataset/licence decision” still open | No |

---

## 7. Generated code and licensing implications

| Kind | In Git? | Note |
|---|---|---|
| Gradle wrapper JAR | No (forbidden) | Obtained on CI |
| `BuildConfig` | No | `buildConfig = true`; generated at compile time from LAI sources |
| Compose compiler / kotlinx.serialization generated classes | No | Generated at compile time |
| CMake / Ninja / object files / `liblai_runtime.so` | No | CI artifacts |
| APK / AAB | No | Published only as GitHub Release / Actions artifacts |
| `catalog/models-v1.json` `generatedAt` | Yes | Hand-maintained JSON; “generatedAt” is a metadata field, not codegen |
| Embedded catalog Kotlin object | Yes | Source, not generated |
| `catalog/catalog-public-key.pem` | Yes | LAI catalog verification key; not a third-party library |
| OpenAPI / protobuf / SQLDelight / Wire sources | None found | — |

Apache-2.0 defines “Object” form to include compiled code and generated documentation. Shipping an APK is therefore a distribution of the Work (and of any linked third-party Object form) and is subject to Apache-2.0 §4 plus each third-party license’s notice rules. The repo’s own registers already say exact notices are **MISSING/PARTIAL** for production.

No committed file is marked `DO NOT EDIT` or appears to be copied codegen from another project.

---

## 8. Fonts, icons, images, datasets, and other assets

| Asset class | What exists | License evidence |
|---|---|---|
| Fonts | `app/src/main/res/values/styles.xml` sets `android:fontFamily` to `sans` (platform default) | No bundled font files |
| Material Icons Extended | Explicitly **not** used; [`docs/DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) says text glyphs replace that ~9 MB dependency | N/A |
| App icon | [`app/src/main/res/drawable/ic_lai.xml`](../app/src/main/res/drawable/ic_lai.xml) — original vector (“L” + “I” on a blue rounded square) | No third-party notice; treated as LAI-authored unless proven otherwise |
| Raster images / screenshots in repo | None | — |
| Audio / video | None | — |
| Datasets | None in Git. Bangla OCR quality set is planned and blocked on a license decision | UNKNOWN |
| Localization strings | `values/strings.xml` and `values-bn/strings.xml` (app + accessibility) | Project text; no third-party corpus notice |
| Catalog signing public key | `catalog/catalog-public-key.pem` | Project key material, not a creative-commons asset |

---

## 9. `CONTRIBUTING.md` and contributor licensing terms

[`CONTRIBUTING.md`](../CONTRIBUTING.md) contains:

- documentation-update requirements;
- source-only / no-binary rules;
- engineering and catalog rules;
- conventional-commit style;
- PR review gates, including **“license review for a new dependency/model/runtime.”**

It does **not** contain:

- a Contributor License Agreement;
- a Developer Certificate of Origin;
- a `Signed-off-by` requirement;
- an inbound license statement;
- a copyright assignment to a company;
- a relicense consent.

The only inbound-license text that currently applies by default is Apache-2.0 **§5** in [`LICENSE`](../LICENSE) (quoted in §13).

---

## 10. Whether a CLA or DCO exists

| Mechanism | Present? |
|---|---|
| `CLA`, `CLA.md`, `CONTRIBUTOR_LICENSE_AGREEMENT` | **No** |
| `DCO`, `DCO.md` | **No** |
| GitHub CLA bot / required status check | **No** files under `.github/` implement one |
| PR template sign-off | [`.github/pull_request_template.md`](../.github/pull_request_template.md) has no DCO/CLA checkbox |
| Separate license agreement referenced | **No** |

Apache-2.0 §5 says a separate license agreement would control if one existed. None is in this repository.

---

## 11. Conflicting or inconsistent license declarations

**No second project license** (no GPL, MIT, proprietary EULA, or dual-license banner for LAI itself) was found.

Observed inconsistencies that are **not** a second license, but matter for compliance hygiene:

1. **Stale third-party version numbers** versus `gradle/libs.versions.toml` (see §4.5).
2. **WorkManager and JaCoCo omitted** from the 2026-08-17 registers.
3. **No `NOTICE` file** while Apache-2.0 §4(d) would apply if one existed or if dependencies require NOTICE aggregation.
4. **No SPDX / per-file headers**, so file-level license is inferred from the root `LICENSE` only.
5. **APK packaging excludes** `META-INF/AL2.0`, `META-INF/LGPL2.1`, `META-INF/LICENSE.md`, `META-INF/NOTICE.md` without a documented replacement notice package inside the APK.
6. **llama.cpp MIT** vs **LAI Apache-2.0**: different licenses on different works. MIT is generally compatible with Apache-2.0 distribution **if** the MIT notice is preserved. That is a combination, not a conflict of LAI’s own grant.
7. **JUnit EPL-1.0** is test-scoped; EPL copyleft would matter only if those classes were shipped. Not currently described as shipped.
8. **Future components** documented as targets (QNN proprietary SDK, PRoot/QEMU, SQLCipher, Whisper, Granite, Tesseract, NpuHub ports) are **not** licensed in-tree. Several of those families are historically **not** Apache-2.0. They are not current conflicts; they are future review gates.
9. **Copyright line is collective** (`LAI contributors`) while Git authors use several identities, including bots. That is an ownership-clarity gap, not two licenses.
10. GitHub Releases already published APKs (documented through at least `v0.9.7` tags). Those artifacts were published while the tree declared Apache-2.0.

---

## 12. Compatibility with a future commercial LAI product

This section describes **what the existing texts allow or withhold**. It is not a business or legal recommendation.

Facts that support offering a **paid** product that uses this code:

- Apache-2.0 §2 grants the right to reproduce, prepare derivative works, publicly display/perform, sublicense, and distribute, including in Object form, without a royalty to contributors.
- Apache-2.0 §3 grants a patent license from each contributor for claims necessarily infringed by their contribution.
- Apache-2.0 §9 expressly allows charging for warranty/support.
- The license does not forbid commercial use.

Facts that **prevent exclusive lock-down of what is already published**:

- The grant is **non-exclusive**, **perpetual**, **irrevocable**, and **worldwide** (Apache-2.0 §2).
- The repository is **public** and GitHub already labels it Apache-2.0.
- Anyone who received a copy (including via Git clone or a Release APK) already has those rights for **that** Work, subject to §4 conditions.
- Relicensing **past** commits to a proprietary-only grant cannot retract the Apache-2.0 grant already made for those commits.
- Third-party works (AndroidX, Kotlin, OkHttp, Shizuku, llama.cpp, Qwen weights if redistributed) keep **their** licenses regardless of any future LAI product license.
- Qualcomm / other vendor SDKs, if later used, will add **separate** contractual limits (already flagged UNKNOWN).

Facts that determine whether **future** LAI versions could be proprietary, dual-licensed, or open-core:

- Relicensing **new** original work requires control of that new work’s copyright.
- Relicensing **old** original work as a whole requires agreement of **all** copyright holders of that work, or a rewrite of portions the product owner does not control.
- There is **no CLA/DCO** and no named assignee. Copyright is noticed only as “LAI contributors.”
- Apache-2.0 §5 already places inbound contributions under Apache-2.0 unless a separate agreement exists.

**Conclusion that stays inside documented facts:** Apache-2.0 is compatible with selling a commercial product **and** incompatible with pretending the already-published public tree is exclusively owned closed source. A future commercial structure is possible only for rights the product owner actually holds. Which strategy to pick is **DECISION REQUIRED**.

---

## 13. Rights Apache-2.0 currently grants to third parties

Quoted from [`LICENSE`](../LICENSE). “You” is any recipient exercising the license.

**Copyright (section 2).** Each Contributor grants a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and Derivative Works in Source or Object form.

**Patent (section 3).** Each Contributor grants a perpetual, worldwide, non-exclusive, no-charge, royalty-free patent license (irrevocable except as stated) to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work, limited to patent claims necessarily infringed by that Contributor’s Contribution(s) alone or combined with the Work. Filing patent litigation alleging the Work or a Contribution infringes terminates that patent license.

**Redistribution (section 4).** Copies and Derivative Works may be distributed in any medium, with or without modification, in Source or Object form, if the distributor:

- gives recipients a copy of this License;
- causes modified files to carry prominent change notices;
- retains copyright, patent, trademark, and attribution notices;
- reproduces NOTICE file attributions if a NOTICE file is part of the Work.

The same section allows adding a copyright statement on modifications and offering **additional or different** terms for modifications or for a Derivative Work as a whole, **provided** use/reproduction/distribution of the original Work still complies with Apache-2.0.

**Contributions (section 5).** Intentional submissions are under Apache-2.0 unless explicitly stated otherwise, without superseding a separate signed agreement.

**Trademarks (section 6).** No trademark license except reasonable use in describing origin and reproducing NOTICE content.

**Warranty / liability (sections 7–8).** AS IS; no title, non-infringement, merchantability, or fitness warranty; damages disclaimed except as required by law or written agreement.

**Support (section 9).** Recipients may sell support/warranty on their own behalf, with indemnity of Contributors.

---

## 14. Limitations Apache-2.0 creates for exclusive commercial control

Direct consequences of the text in §13 plus the public publication facts in §12:

| Exclusive-control goal | What Apache-2.0 / this repo does |
|---|---|
| Stop third parties from using the published source | Not possible under §2 for copies already licensed |
| Stop forks, competing apps, or paid redistribution of the published Work | Not possible, if they keep §4 conditions |
| Charge a royalty for the published Work itself | Grant is royalty-free |
| Revoke the license later | Grant is irrevocable (patent grant excepts litigation termination only) |
| Claim the license is exclusive to one company | Grant is non-exclusive |
| Hide attribution / drop Apache-2.0 when shipping binaries of this Work | §4(a)–(d) still apply to the original Work |
| Use “LAI” as a trademark monopoly via this license | §6 grants no trademark rights; also creates no automatic trademark |
| Relicense the entire history to proprietary-only | Requires all copyright holders, which are not identified beyond “LAI contributors” |
| Make inbound PRs automatically assign copyright | Not present; §5 only applies Apache-2.0 to the contribution |
| Treat llama.cpp / AndroidX / Qwen weights as LAI-owned | False; they remain separately licensed |
| Ship a closed APK of LAI-only code without any notice | Still a distribution of an Apache-2.0 Work; §4 applies |
| Patent-sue recipients for claims covered by §3 | Risk of losing the patent license under the litigation clause |

Apache-2.0 does **not** by itself forbid:

- selling the app;
- keeping some **new**, separately authored modules under a different license if those modules are not Derivative Works that fail §4’s “as a whole” conditions (whether a given module qualifies is fact-specific and **not decided here**);
- offering paid hosted services, support, or hardware bundles.

---

## 15. Future licensing strategies (options only — none selected)

No replacement license is chosen. The following are commonly discussed structures and how they interact with **this** repository’s current facts.

### 15.1 Proprietary (closed source going forward)

- Possible only for code the owner exclusively controls.
- Already-published Apache-2.0 commits remain Apache-2.0 for recipients.
- Requires a named copyright owner, and usually a CLA or original-author identity cleanup, before later versions can drop Apache-2.0.
- Third-party Apache-2.0/MIT components can still be used if their notices travel with binaries.
- Copyleft or proprietary vendor SDKs (future QNN, possible GPL tools) need a separate review.

### 15.2 Source-available (viewable source, restricted use)

- A new license on **new** versions could restrict production use, SaaS, or competition.
- It cannot cancel Apache-2.0 rights in already released trees.
- Community/contribution process would need inbound terms that are not Apache-2.0 §5 default, or contributions will remain Apache-2.0.

### 15.3 Dual licensing

- Typical pattern: open copy under Apache-2.0 (or copyleft) **and** a paid proprietary grant of the **same** owner-controlled code.
- Requires unified copyright (single owner or CLA). This repo does not currently document that.
- Dual licensing does not relicense llama.cpp, AndroidX, Shizuku, or Qwen.

### 15.4 Open-core

- Keep a public Apache-2.0 core; sell proprietary modules (cloud sync, extra backends, enterprise policy, signed catalogs, support).
- Fits the current grant for the core, because Apache-2.0 already allows proprietary derivatives **of the owner’s modifications** if §4 is met for the original Work.
- Plugin/runtime boundaries already exist (`plugins/api`, `runtime/*`), which is an engineering fit, not a legal decision.
- Catalog/model/vendor pieces must stay on their own licenses.

### 15.5 Continued Apache-2.0

- Least change: keep [`LICENSE`](../LICENSE), add missing NOTICE/SBOM/version pins, optionally add SPDX headers and a CLA/DCO only if inbound control is desired later.
- Commercial products remain allowed; exclusive control of the public core remains unavailable.
- Matches current GitHub metadata, README, and catalog fields.

---

## 16. Gap list (facts only; no remediation in this change)

These are documentation/process gaps observed during the audit. This commit does **not** fix them.

1. No root `NOTICE` and no per-file SPDX.
2. Third-party registers last audited 2026-08-17 and do not match the live version catalog.
3. WorkManager and JaCoCo missing from those registers.
4. No generated SBOM or collected upstream license texts in Git (CI writes a lightweight `dependencies` dump as an artifact only).
5. APK packaging drops several `META-INF` license/notice names.
6. No CLA/DCO; copyright owner is the collective phrase “LAI contributors.”
7. `CONTRIBUTING.md` has no inbound license paragraph beyond what Apache-2.0 §5 already says.
8. Planned models/datasets/SDKs are not all listed in `MODEL_LICENSES.md`.
9. llama.cpp MIT notice obligation applies to native binaries that CI actually links; presence in current Release APKs is UNVERIFIED.
10. Feature matrix already marks legal tracking **PARTIAL**.

---

## Decision

**DECISION REQUIRED**

No replacement license is selected. Apache-2.0 remains the current project license in [`LICENSE`](../LICENSE). No license file, copyright line, or source file was modified to produce this audit.

## Related planning documents

The following documents extend this audit. They do not change the current grant:

- [`legal/COMMERCIAL_IP_POLICY.md`](legal/COMMERCIAL_IP_POLICY.md)
- [`legal/OWNERSHIP_DECISIONS.md`](legal/OWNERSHIP_DECISIONS.md)
- [`legal/README.md`](legal/README.md)
- [`legal/LICENSING_STRATEGY.md`](legal/LICENSING_STRATEGY.md)
- [`legal/OWNERSHIP_MODEL.md`](legal/OWNERSHIP_MODEL.md)
- [`legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md`](legal/PUBLIC_VS_COMMERCIAL_BOUNDARY.md)
- [`decisions/ADR-0100-current-apache-2.0-state.md`](decisions/ADR-0100-current-apache-2.0-state.md)
