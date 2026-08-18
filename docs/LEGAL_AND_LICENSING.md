# Legal and Licensing Audit

**Scope:** Documentation and audit only — no license file was modified, deleted, replaced, or rewritten; no source was changed; no legal claim beyond documented facts is made. **Final licensing decision: DECISION REQUIRED.**

**Audit date:** 2026-08-18 — `main` @ `94d88aa` (`docs: complete documentation-first`), `validate_repo.sh` PASS (`1035925 bytes <128 MB`), `16` modules, `169` tests. **This audit is factual, not legal advice.**

## 1. Exact location of the current Apache-2.0 license

*   **File:** `LICENSE` at repository root (`/LICENSE`, `11,021` bytes, `Apache License Version 2.0, January 2004`, `http://www.apache.org/licenses/`). Full text present, including `Appendix` with `Copyright [yyyy] [name of copyright owner]` placeholder.
*   No additional `LICENSE` or `COPYING` files in subdirectories (`find . -name LICENSE*` returns only root).

## 2. Copyright notices currently present

*   **No `Copyright (c) …` line is committed in `LICENSE` or in source headers** — the `LICENSE` appendix retains the template `Copyright [yyyy] [name of copyright owner]` without substitution. No `Copyright` string was found in `*.md`/`*.kt`/`*.kts` via `grep -r "Copyright"` (0 hits) on `main`.
*   Git history (`git log`) shows author `soobujmiah` (`soobujmiah@users.noreply.github.com`), but no explicit `Copyright` assignment file (e.g., `NOTICE`, `AUTHORS`) is present. The effective licensor is the repository owner as understood, but the file itself does not name them.

## 3. All files that explicitly reference Apache-2.0

*   `LICENSE` (full text)
*   `README.md` — `Apache License 2.0. Third-party model weights, Qualcomm SDK components, Shizuku, and future inference engines retain their own licenses…`
*   `THIRD_PARTY_NOTICES.md` — 4 rows with `Apache-2.0` (`AndroidX Core/Activity/Lifecycle/DataStore/Compose`, `Kotlin/coroutines/serialization`, `OkHttp 4.12.0`, `Shizuku 13.1.5`)
*   `THIRD_PARTY_LICENSES.md` — same 4 families + `Apache-2.0` identifier
*   `MODEL_LICENSES.md` — `qwen2.5-1.5b-instruct-q4-k-m` `Apache-2.0` (upstream `Qwen/Qwen2.5-1.5B-Instruct-GGUF`)
*   `core/model/src/main/kotlin/dev/lai/runtime/model/ReviewedModelCatalog.kt` — `license = "Apache-2.0"` for the recommended CPU baseline
*   `docs/MODELS.md`, `docs/MODELS_AND_BACKENDS.md`, `docs/product/feature-matrix.md` (legal/license tracking row)

No other `*.kt`/`*.kts`/`*.toml` declares `Apache-2.0` as its own.

## 4. All third-party dependencies and their licenses (resolved, to be re-verified per release)

Per `THIRD_PARTY_NOTICES.md` / `THIRD_PARTY_LICENSES.md` (last audited 2026-08-17) and `gradle/libs.versions.toml` (`Gradle 9.5.0 + AGP 9.3.1 + Kotlin 2.4.10`):

| Family | Version / Source | Declared upstream license | Verification source |
|---|---|---|---|
| AndroidX Core/Activity/Lifecycle/DataStore/Compose (`androidx.core:core-ktx 1.16.0`, `activity-compose 1.10.1`, `lifecycle 2.9.1`, `datastore 1.1.7`, `compose-bom 2025.05.01`) | Maven Central | **Apache-2.0** | Resolved POM/artifact `LICENSE` + `NOTICE` metadata |
| Kotlin + kotlinx (`kotlin 2.4.10`, `coroutines 1.11.0`, `serialization 1.11.0`) | JetBrains | **Apache-2.0** | Resolved artifact metadata |
| OkHttp / Okio (`okhttp 5.4.0`) | Square | **Apache-2.0** | Resolved POM + Okio transitive |
| Shizuku `api`/`provider` (`13.1.5`) | Rikka | **Apache-2.0** | Pinned upstream source |
| `llama.cpp` (`ad1de39e0708…`) | `ggerganov/llama.cpp` | **MIT** | Pinned commit root `LICENSE` (MIT copyright/license text required in APK notices) |
| JUnit 4 (`junit 4.13.2`) | Eclipse | **EPL-1.0** | Test artifact metadata — build/test use only |
| Android/Gradle/CMake toolchains (`JDK 17`, `API 36`, `NDK 27`, `CMake 3.22.1`, `gradle/actions`) | Google/Gradle | **Tool-specific** | Installed SDK/NDK/Gradle distributions — not redistributed from source repo |
| Future `QAIRT/QNN` (`qnn-sdk/`, `models/*.dlc`) | Qualcomm | **UNKNOWN until licensed acquisition** | Vendor agreement/SDK package — **no source/binary distribution before legal review** |
| Future plugin/model deps | Various | **UNKNOWN until proposed** | Signed `plugins/api` manifest + `catalog/models-v1.json` upstream terms |

**Release gate (today MISSING/PARTIAL, per `THIRD_PARTY_LICENSES.md`):** Production CI must generate resolved dependency graph, collect exact `LICENSE`/`NOTICE` texts from resolved artifacts + native sources, compare with this register, include required notices in APK/release bundle, and fail on unknown/incompatible licensing. `app/build/sbom/sbom-*.txt` is lightweight (`dependencies` output) → `CycloneDX` future.

## 5. Any bundled third-party source code

*   **No vendored third-party source is committed.** `ls third_party/`, `native/` → not found. `llama.cpp` is **not** vendored — fetched at CI via `scripts/ci/fetch_llama_cpp.sh "$LLAMA_CPP_COMMIT" "$RUNNER_TEMP/llama.cpp"` and built as `EXCLUDE_FROM_ALL` (`CMakeLists` `GGML_VULKAN=ON` with `SPIRV-Headers` on CI). `validate_repo.sh` forbids `*.so`/`*.a`/`*.jar` (`gradle-wrapper.jar`) and would fail if a native SDK were committed.
*   The repository contains only **glue** (`runtime/llama/src/main/cpp/native_inference.cpp`, `llama_cpu_backend.cpp`, `vulkan_backend.cpp`, `backend_registry.cpp`, `include/lai/backend.h`) + `app/proguard-rules.pro` keeps for `NativeBindings`.

## 6. Any model files or model references and their licenses

*   **No model weights are committed.** `find . -name "*.gguf"` (excluding `.git`) → 0; `find . -path "./.git" -prune -o -name "*.gguf"` is in `validate_repo.sh` forbidden list. `model/README.md` documents `storage/LAI/models` (`/sdcard/LAI/models`) as the **single** user-owned SAF store; `model/.gitkeep` is tracked, `*.gguf` is `* .gitignore` (`models/`, `*.gguf`, `*.onnx`, etc.) and `937 KB` repo size confirms no blob.
*   **References:** `catalog/models-v1.json` rev3 (`qwen2.5-1.5b-instruct-q4-k-m` → `Qwen/Qwen2.5-1.5B-Instruct-GGUF` `qwen2.5-1.5b-instruct-q4_k_m.gguf`, `Apache-2.0`, `1,117,320,736` bytes, SHA `6a1a2e…9407e`, `compatibleBackendIds: ["llama-cpu"]`, `preferredBackendId: "llama-cpu"`), `ReviewedModelCatalog.kt` same `license = "Apache-2.0"`, `MODEL_LICENSES.md` documents the same **Apache-2.0** upstream (not bundled) and `LOCAL_UNREVIEWED` for workspace-discovered unknown GGUF (no provenance/safety claim, no auto-load, no redistribution right).

## 7. Any generated code and its possible licensing implications

*   **Build-generated:** `**/build/` (`.gradle/`, `.cxx/`, `app/build/sbom/sbom-*.txt`, `lint-*.html`) is `* .gitignore` and `validate_repo.sh` ` -path '*/build' -prune` — never committed.
*   **Serialization-generated:** `kotlinx.serialization` (`@Serializable` on `ModelCandidate`/`DiscoveredModel`/`DiagnosticsReportV1` etc.) generates `$Companion`/`serializer()` at compile time. `app/proguard-rules.pro` keeps `serializer(...)` via `keepclasseswithmembers` — no additional licensing, but generated code inherits `Apache-2.0` of the project and `Apache-2.0` of `kotlinx.serialization` (per `THIRD_PARTY_LICENSES`).
*   **No other code generator** (e.g., `protoc`, `flatbuffers`) is vendored.

## 8. Any fonts, icons, images, datasets, or other assets and their licenses

*   **Icons:** `app/src/main/res/drawable/ic_lai.xml` — **custom vector**, no third-party icon font. No `*.ttf`/`*.otf` in `app/src/main/res` (only `ic_lai.xml`, `values/strings.xml`, `values-bn/strings.xml`, `values/styles.xml`). No `assets/` fonts.
*   **Images:** No `*.png`/`*.jpg`/`*.webp` bundled except `docs/` screenshots (if any, not in this snapshot). `runtime/llama` `PngRasterDecoder` is for *user* `image/png` input, not a bundled dataset.
*   **Datasets:** No `*.csv`/`*.jsonl` training/test sets committed. Smoke corpora would be generated via `scripts/gen_*_smoke_corpus.py` (as in NpuHub) but are not present in `lai`.
*   **Licenses:** Custom `ic_lai.xml` is `Apache-2.0` as part of the work; no `SIL OFL` or `CC-BY` asset to attribute.

## 9. CONTRIBUTING.md and whether it contains contributor licensing terms

*   **File:** `CONTRIBUTING.md` (`~180` lines, last at `docs: complete documentation-first`).
*   **Contains:** *Documentation is part of implementation*, *Source-only rule* (`validate_repo.sh`), *Engineering rules* (15), *Supported-model catalog changes*, *Commit style* (conventional), *Pull-request review gates* (`CI green`, `docs updated`, `no size/binary/secret violation`, `explicit real/scaffold/planned`, `safety review`, `license review`, `device evidence`).
*   **Contributor licensing terms:** **No explicit CLA, no explicit DCO, no “by contributing you agree to license under Apache-2.0” clause.** The file requires `license review for a new dependency/model/runtime` but does not state inbound=outbound licensing. This is a **gap** for future commercial/open-core decisions.

## 10. Whether a CLA or DCO exists

*   **CLA:** **Not found** — no `CLA.md`, `CONTRIBUTOR_LICENSE_AGREEMENT.md`, or `.github/CLA*`.
*   **DCO:** **Not found** — no `DCO` text, no `Signed-off-by` requirement in `.github/pull_request_template.md` (template only asks `Documentation was updated`, `validate_repo.sh`, `CI green`, `Security/confirmation`, `Device evidence`). No `git` `signoff` enforcement in `validate_repo.sh` or `check_architecture_boundaries.py`.
*   **Implication:** Contributions are received under GitHub’s default **Terms of Service** + the repository’s `LICENSE` (Apache-2.0), but without an explicit `DCO` there is no `Signed-off-by` provenance trail.

## 11. Whether the repository contains any conflicting license declarations

*   **No direct conflict** between declared licenses: `LICENSE` is `Apache-2.0`; `THIRD_PARTY_NOTICES`/`THIRD_PARTY_LICENSES` correctly list `Apache-2.0` for AndroidX/Kotlin/OkHttp/Shizuku and `MIT` for `llama.cpp` + `EPL-1.0` for JUnit — all **permissive and compatible with Apache-2.0** distribution of the `Work`.
*   **No `GPL`/`AGPL`/`CC-BY-NC`** is declared or vendored.
*   **Potential tension to watch:** `llama.cpp` `MIT` is permissive and compatible, but its `MIT` `NOTICE` must be included in the APK’s third-party notices (today `THIRD_PARTY_NOTICES.md` says it must, but no automated `NOTICE` aggregation is implemented — see §4 release gate). No `LICENSE` file in a subdirectory claims a different license for a vendored directory (there is no vendored directory).

## 12. Whether the current licensing structure is compatible with a future commercial LAI product

*   **As documented, it is *compatible* with a commercial product *built on* Apache-2.0** — Apache-2.0 permits commercial use, distribution, and private modification — **but it is not *exclusive* commercial control.** Key facts:
    *   The `LICENSE` at root is `Apache-2.0` without a `NOTICE`/`COPYRIGHT` assignment beyond the template placeholder — anyone who receives the `Work` receives the `Apache-2.0` grant.
    *   No `CLA`/`DCO` centralizes copyright; contributors retain copyright in their contributions (GitHub TOS).
    *   `THIRD_PARTY_LICENSES` `MIT`/`Apache-2.0`/`EPL-1.0` are all commercial-friendly, but `MIT`/`Apache-2.0` require preservation of `LICENSE`/`NOTICE`.
    *   Model `Apache-2.0` (`Qwen`) is commercial-friendly **as an artifact**, but the catalog explicitly says *license metadata does not grant redistribution rights beyond upstream*; the Qwen artifact’s upstream `LICENSE` must be re-verified before any bundling/redistribution.
    *   Future `QAIRT/QNN` is `UNKNOWN until licensed acquisition` — the current `validate_repo.sh` correctly forbids `*.dlc`/`*.so` in repo, but a commercial `QNN` runtime would need a separate **license-compliant CI acquisition** (not committed) and a `THIRD_PARTY_NOTICES` update.
*   **In short:** The structure **allows** a commercial LAI product to be built, shipped, and sold **on top of Apache-2.0**, but it **does not prevent** others from doing the same with the published source.

## 13. What rights Apache-2.0 currently grants to third parties

*Per the `LICENSE` text at root (`Apache License Version 2.0, January 2004`, §§1–9):*

*   **Perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable** (except §3 patent termination) **copyright license** to reproduce, prepare `Derivative Works`, publicly display/perform, sublicense, and distribute `Work` and `Derivative Works` in `Source` or `Object` form.
*   **Perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable patent license** to make/have made/use/offer to sell/sell/import `Work`, where `Licensed Patents` are necessarily infringed by `Your` contributions — **terminates** if `You` institute patent litigation alleging the `Work` infringes.
*   **Grant is to *any* recipient** of the `Work` — not limited to the owner.
*   **Conditions:** Retain `LICENSE` + copyright/attribution notices, state changes, and include `NOTICE` if present; `Derivative Works` may be licensed under different terms, but `Source` form of the `Work` remains `Apache-2.0`.

## 14. What limitations Apache-2.0 creates for exclusive commercial control

*   **No exclusivity:** Apache-2.0 **does not create** a proprietary moat — anyone may fork, rebrand, host, or sell a product based on the published `Work` under `Apache-2.0` (or a compatible license for their `Derivative Works`), provided they preserve `LICENSE`/`NOTICE`.
*   **No “take-back”:** Once published under `Apache-2.0`, that version remains `Apache-2.0` for recipients; a future license change (e.g., to proprietary) **does not retroactively** revoke the `Apache-2.0` grant for code already published at `94d88aa` and prior.
*   **No field-of-use restriction:** Apache-2.0 permits commercial use, including SaaS, on-device, or OEM bundling — you cannot later enforce “non-commercial only” on the Apache-2.0 code.
*   **Trademark not granted:** `LAI` name/logo are **not** licensed by `Apache-2.0` (§6) — exclusive control of brand must be via trademark, not copyright license.
*   **Patent termination is defensive only:** You get a patent grant, but you lose it if you sue over the `Work`.
*   **Contributions remain contributor-copyright:** Without `CLA`, you do not automatically own contributors’ copyright — you only get the `Apache-2.0` license to their contribution.

## 15. What licensing strategies could be considered in the future

*No decision is made — **DECISION REQUIRED** below. All strategies keep the existing published history under `Apache-2.0` for that history.*

*   **Continued Apache-2.0 (permissive open source).** Keep `LICENSE` as `Apache-2.0` for all code. **Pros:** Broadest adoption, simplest, compatible with AndroidX/Kotlin/OkHttp/Shizuku/MIT `llama.cpp`. **Cons:** No exclusivity; competitors may ship identical `Work`.
*   **Open-core.** `LICENSE` stays `Apache-2.0` for `core` (`core/*`, `platform/*`, `runtime:llama` CPU) + `docs` + `plugins:api`; **proprietary** for `enterprise` modules (e.g., `backend/qnn`, advanced `RAG`, `Vulkan` tuning, managed `QNN` + `Tesseract` production adapters, `benchmark` SaaS, `MLOps`). **Pros:** Open moat + commercial upsell. **Cons:** Boundary maintenance (`check_architecture_boundaries.py` must enforce `core` vs `enterprise`), `THIRD_PARTY/NOTICES` split.
*   **Source-available (e.g., `Elastic License 2.0` / `SSPL` / `BSL`).** Source remains public/visible, but commercial production use beyond a threshold requires a commercial license. **Pros:** Deters `SaaS` cloning while keeping source open for audit. **Cons:** Not `OSI` open source, `Apache-2.0` ecosystem friction, `THIRD_PARTY` CI must handle `UNKNOWN` for `QAIRT` already.
*   **Dual licensing (Apache-2.0 + Commercial).** Offer the same `Work` under `Apache-2.0` *and* under a **paid commercial license** that removes `Apache-2.0` `NOTICE`/`attribution` or grants patent indemnity/support. **Pros:** Lets customers avoid `Apache-2.0` obligations. **Cons:** Requires **CLA** to aggregate copyright before you can re-license contributions.
*   **Proprietary (future versions).** Change `LICENSE` for **new** code (after `94d88aa`) to proprietary, keep history `Apache-2.0`. **Pros:** Full exclusivity going forward. **Cons:** Community fork may continue `Apache-2.0` history; contributors must agree; `THIRD_PARTY` `MIT`/`Apache-2.0` deps remain permissive and must still be noticed.
*   **Other `OSI` permissive (e.g., `MIT`).** Not recommended — `Apache-2.0` already grants patent protection that `MIT` does not.

**Prerequisites before any strategy change:** Add **explicit `Copyright (c) 202x soobujmiah`** to `LICENSE`/`NOTICE`, decide **`CLA` vs `DCO`** (NpuHub has neither; LAI currently has neither), run `THIRD_PARTY` **resolved SBOM + `NOTICE` aggregation** (today `sbom-*.txt` lightweight → `CycloneDX` future), and complete **`MODEL_LICENSES` provenance for every reviewed model** (today only `Qwen`).

---

## DECISION REQUIRED

**LAI will remain `Apache-2.0` at `LICENSE` until the owner records a new decision here.** No replacement license has been chosen, no `Apache-2.0` has been removed, and no legal claim beyond the facts above is made. The next step is a **founder decision** among the five strategies above, then:

1. Add `Copyright` line + `NOTICE` if needed,
2. Adopt `CLA` **or** `DCO` (`Signed-off-by`) and document it in `CONTRIBUTING.md`,
3. Generate `CycloneDX` SBOM + `NOTICE` artifact for the chosen `LICENSE`,
4. Update `MODEL_LICENSES`/`THIRD_PARTY_NOTICES` for the chosen commercial `QNN`/`Vulkan` path.

**This file is the audit — it does not change the license.**

