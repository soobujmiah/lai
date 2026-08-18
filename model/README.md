# Model prebundle folder — signed APK with offline model

This folder is **local-only and never committed** (`*.gguf` is gitignored and `validate_repo.sh` forbids it).

**How to build a signed APK that already contains the model (no import after install):**

1. Put your reviewed GGUF here **on your machine or in CI** (do NOT `git add` it):
   ```
   model/qwen2.5-1.5b-instruct-q4-k-m.gguf   # exact 1,117,320,736 bytes + SHA-256
   # or any GGUF you want prebundled — name must match an entry in catalog/models-v1.json
   # or will be imported as `local-` model.
   ```

2. Build signed APK **without creating a Release**:
   * **On GitHub:** Actions → Android build → **Run workflow** → `build_type: release`
     * The workflow checks this folder at build time and bundles everything from `model/` + `models/` into `assets/models/`.
     * Artifact is `lai-release-*.apk` (production key `lai-release`, `install -r` keeps data).
   * **Locally** (if you have the model here):
     ```sh
     ./gradlew :app:assembleRelease -Plai.versionCode=130 -Plai.versionName=0.6.130
     ```

3. Install as **update** (no uninstall, no data loss):
   ```sh
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   On first launch LAI copies `assets/models/*.gguf` → `noBackupFilesDir/models/` and registers it — the model appears as **Installed** immediately.

**Why this is source-only safe:**
* `*.gguf` stays gitignored → `scripts/validate_repo.sh` still PASS, repo stays <128 MB.
* The APK is built **only on CI** (or your machine) from a local `model/` — the Git repo never contains the 1.1 GB blob.
* If `model/` is empty, the APK is tiny and model is downloaded/imported normally — no behavior change.

**CI note:** You can also trigger a signed prebundled build via API:
```sh
curl -X POST -H "Authorization: token $PAT" \
  https://api.github.com/repos/soobujmiah/lai/actions/workflows/android_build.yml/dispatches \
  -d '{"ref":"main","inputs":{"build_type":"release"}}'
```
Put the GGUF in `model/` **before** that dispatch (e.g., via a prior workflow that `curl`s it from Hugging Face).

Tracked files in this folder: only this README and `.gitkeep`. The `*.gguf` are always ignored.
