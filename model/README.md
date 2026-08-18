# Workspace model folder — `storage/LAI/models`

This repo stays **source-only (<128 MB)** — `*.gguf` is gitignored.

Put your GGUF **in your phone's workspace, not in this repo:**

```
/sdcard/LAI/models/qwen2.5-1.5b-instruct-q4-k-m.gguf
/storage/emulated/0/LAI/models/qwen2.5-1.5b-instruct-q4-k-m.gguf
```

That is the SAF workspace folder you grant via **LAI → Settings → Workspace → Connect**. It survives `adb install -r` and uninstall (if you keep the `LAI/` folder).

**How the signed APK rebuild works now:**

1. Keep the model in `storage/LAI/models` on your device (once).
2. Build **signed APK without Release** (so you can `install -r` without losing data):
   * GitHub: Actions → Android build → Run workflow → `build_type: release` → artifact `lai-release-*.apk` (production key `lai-release`)
   * Or locally: `./gradlew :app:assembleRelease`
3. `adb install -r lai-release.apk` → LAI starts → **auto-discovers** `LAI/models/*.gguf` on launch and registers it as Installed (no manual Import, no download). The 1.1 GB blob never enters the Git repo.

If `storage/LAI/models` is empty, LAI behaves as before (download/import via Settings).
