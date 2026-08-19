# GitHub-only build and release

## Why no Gradle wrapper JAR?

The repository is source-only. `gradle-wrapper.jar`, Android SDK/NDK, CMake, QAIRT/QNN SDKs, APKs, and models are not committed. GitHub Actions obtains Gradle 8.13 with `gradle/actions/setup-gradle` and Android tools with `sdkmanager`.

## Workflow triggers

`.github/workflows/android_build.yml` runs on:

- pushes and pull requests to `main`;
- manual `workflow_dispatch` for debug/release/both;
- tags matching `v*`, which create or update a GitHub Release.

The build performs:

1. source/docs/secret plus module/network/authority boundary validation;
2. JDK 17 setup;
3. API 36, Build Tools 36.0.0, NDK 27.0.12077973, and CMake 3.22.1 install on the runner;
4. immutable-SHA llama.cpp fetch into runner-temporary storage;
5. pure-JVM coverage tests, private-audit Android unit tests, app unit tests, and Android lint;
6. Kotlin/Compose resources and arm64 C++/llama.cpp JNI compilation;
7. **both** APK variants in a single run — `debug` and `release` (release uses the production keystore on tags, else the debug key) — plus the release R8 `mapping.txt` artifact;
8. version-tag release.

A separate `catalog_publish.yml` validates `catalog/models-v1.json`, signs its exact bytes with the encrypted `MODEL_CATALOG_SIGNING_KEY`, verifies the committed public key matches, and publishes stable `catalog-v1` assets.

## Trigger manually

GitHub → **Actions** → **Android build** → **Run workflow**. The default `build_type` is **`both`**, so one run produces the installable `lai-debug-<run>` and `lai-release-<run>` APKs (debug retained 14 days, release 30 days) plus `lai-release-mapping-<run>`. Every push to `main` also builds both variants; pull requests stay debug-only.

## Create a release

```bash
git tag -a v0.1.0 -m "LAI 0.1.0"
git push origin v0.1.0
```

The workflow uses `GITHUB_RUN_NUMBER` as monotonically increasing `versionCode` and the tag without `v` as `versionName`.

## Production signing

Generate and back up a keystore on a trusted offline workstation; do not generate or store it in this repository. Base64-encode it and configure:

| Secret | Purpose |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | full keystore bytes encoded as base64 |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | signing alias |
| `ANDROID_KEY_PASSWORD` | key password |

The workflow decodes the keystore only to `$RUNNER_TEMP`, applies mode 0600, and passes values as process environment. If all four values are absent/incomplete, the release variant falls back to Android's debug signing solely to remain installable for device testing. `BuildConfig.PRODUCTION_SIGNED` records which path was selected.

Never publish a debug-signed APK as a production update. Android cannot update an installed app signed by a different key without uninstalling it.

## Future QAIRT/QNN CI

Qualcomm SDK packages and credentials must be GitHub secrets or downloaded from an access-controlled, license-compliant URL to runner-temporary storage. The planned job must:

1. pin and verify SDK package SHA-256;
2. never cache proprietary SDK payloads publicly;
3. convert/quantize models in a separate model-release workflow;
4. key context binaries by model hash, SoC/firmware, QAIRT version, and recipe;
5. publish only redistributable artifacts;
6. delete temporary SDK/model data at job completion.

QNN is not a build option of `runtime:llama`. When real integration begins, a dedicated Qualcomm runtime module and workflow path will acquire the licensed SDK, compile the actual adapter, and verify packaged libraries. Until then there is no QNN flag or placeholder artifact that could be mislabeled as accelerated.

## Repository footprint

Run only the lightweight policy check locally:

```bash
bash scripts/validate_repo.sh
```

All compilation remains in GitHub Actions. Generated build folders are ignored and not part of the persistent source snapshot.

## GPU enablement

**Default is CPU-only (`validated_accelerators` input default empty).** This is the stable
configuration: on the SM8735 / Adreno 825, the Vulkan driver both fails to compile the MMVQ
shader family (mitigated by LAI's `ggml-vulkan-skip-mmvq.patch`) AND then **crashes natively
during Vulkan compute** (device evidence 2026-08-19: process restart on message send, release
0.1.175). Per the project's rule, no acceleration is claimed until device-validated, so GPU is
**opt-in**: set `validated_accelerators=llama-vulkan` only on a device/driver combo that has
been qualified. CPU remains the reviewed, reliable backend.

GPU offload uses `LLAMA_LOAD_MODE_NONE` (mmap would force the integrated GPU's host-visible
buffers back to CPU — 0 layers offloaded). `VulkanBackend` sets `GGML_VK_DISABLE_COOPMAT/_2`
and `GGML_VK_DISABLE_MMVQ`, and `fetch_llama_cpp.sh` applies `ggml-vulkan-skip-mmvq.patch` so
the MMVQ family is not compiled when disabled (the env var alone only stopped the kernels being
used). `std::cerr` is routed to the `LAI-llama` logcat tag and the failing-pipeline name is
captured in-app; a Kotlin-level accelerator failure auto-falls back to CPU — but a native
driver crash cannot be caught by the app, which is why GPU defaults off on unqualified devices.

## Working rule: signed release only by default

**Default CI produces ONLY the signed release APK.** The debug APK is built only when it is
explicitly required for debugging:

- `workflow_dispatch` `build_type` default is `release` (options: `debug` / `release` / `both`).
- Push/tag builds → signed release APK only.
- Pull requests → debug APK only (CI validation; no release artifact from unmerged code).

### Why every release build is properly signed (and installs over the previous one)

GitHub Actions auto-creates a **fresh Android debug keystore on every runner**, so "release" APKs
signed with the debug key would carry a **different certificate on every build** — Android then
refuses to install a newer build over the previous one (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
That was the root cause of "signed app cannot be installed over the previous version"
(field reports 2026-08-19).

Fix: **every CI release APK is signed with a real keystore**:

| Build source | Keystore | `PRODUCTION_SIGNED` |
|---|---|---|
| `v*` tag | production keystore from `ANDROID_KEYSTORE_*` secrets | `true` |
| push / dispatch release | **deterministic test keystore** derived in CI from a committed seed (`scripts/ci/generate_test_keystore.py`) — the SAME certificate every run | `false` |

The test keystore is generated at build time into runner temp storage — **no key material is
committed**. Because the certificate is stable across runs and `versionCode` = GitHub run number
keeps increasing, every release APK installs over the previous one with `adb install -r` (or the
on-device installer). The test key is for device testing only; production releases always use the
secrets.

### GPU note (Adreno)

`VulkanBackend` sets `GGML_VK_DISABLE_COOPMAT`/`_2` and `GGML_VK_DISABLE_MMVQ` before ggml
Vulkan init, and `fetch_llama_cpp.sh` applies LAI's `ggml-vulkan-skip-mmvq.patch` so the MMVQ
shader family is NOT COMPILED when disabled (the env var alone only stopped the kernels being
used; the Adreno 825 driver still failed compiling `mul_mat_vec_q4_k_f32_f32` at init, confirmed
on-device 2026-08-19). Non-affected shaders run on the GPU; the CPU fallback protects against any
remaining driver failure. Retest GPU with release-175+.
