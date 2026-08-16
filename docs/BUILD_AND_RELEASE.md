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
3. API 35, Build Tools 35.0.0, NDK 27.0.12077973, and CMake 3.22.1 install on the runner;
4. immutable-SHA llama.cpp fetch into runner-temporary storage;
5. Kotlin unit tests and Android lint;
6. Kotlin/Compose resources and arm64 C++/llama.cpp JNI compilation;
7. APK assembly and artifact upload;
8. release upload for a version tag.

## Trigger manually

GitHub → **Actions** → **Android build** → **Run workflow**. Debug artifacts are installable and retained for 14 days.

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

`-Plai.enableQnn=ON` currently fails deliberately so CI cannot accidentally label a placeholder build as QNN-enabled.

## Repository footprint

Run only the lightweight policy check locally:

```bash
bash scripts/validate_repo.sh
```

All compilation remains in GitHub Actions. Generated build folders are ignored and not part of the persistent source snapshot.
