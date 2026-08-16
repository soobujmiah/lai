# LAI — Local AI & Android Automation Runtime

LAI is a source-only Android foundation for private, Bangla-first on-device AI and consent-driven Android automation. It targets modern arm64 Snapdragon devices, beginning with Snapdragon 8s Gen 4 (Hexagon NPU + Adreno GPU).

> **Current status:** Phase 1 is physically validated on Snapdragon 8s Gen 4. Phase 2 CPU inference is build-verified with a CI-fetched immutable llama.cpp revision; physical GGUF loading and Bangla generation are the next release gate. Bangla OCR remains a model-required plugin boundary. See [status](docs/STATUS.md).

## Product principles

- **Local first:** prompts, screen structures, captures, and models stay on the phone.
- **Bangla first:** UTF-8 end to end, Bangla UI resources, bilingual prompts, and a versioned Bangla OCR schema.
- **User controlled:** consequential UI and Shizuku operations require confirmation.
- **No arbitrary shell:** elevated actions compile from typed, validated operations into argument arrays.
- **Source-only Git:** no SDKs, models, APKs, native libraries, caches, or keystores in the repository.
- **Documentation with code:** behavior, limitations, safety implications, and device evidence are updated in the same change.

## One local-first application

LAI has one application ID and upgrade path. Internet is used only when the user refreshes the signed supported-model catalog or explicitly downloads a reviewed component. Installed models, verified catalog metadata and all user intelligence remain available offline. Prompts, screens, documents, generations, automation data and telemetry are never transmitted. Local file import is always available as an alternative.

## Current capabilities

| Area | Included now | Intentional boundary |
|---|---|---|
| Compose UX | Chat, Screen Reader, Automator, hidden Developer Mode, Bangla resources | Chat reports backend absence rather than fabricating output |
| Accessibility | Snapshot, selector, click, set text, scroll, global actions, app launch, Android 11+ screenshot | No autonomous consequential action without confirmation |
| Shizuku | Binder state, permission, UID, structured operation policy, timeout/output limits | No raw shell command API |
| Models | Explicit HTTPS download, mandatory SHA-256, resume, GGUF validation, app-private registry | No model is bundled; only reviewed artifact hosts are accepted |
| Native runtime | Build-verified arm64 llama.cpp CPU runtime with cancellable token streaming | Physical GGUF/Bangla gate pending; Vulkan and QNN remain Phase 2/3 |
| Bangla OCR | Screenshot path, plugin interface, versioned structured JSON | Recognition model/runtime is a clearly reported placeholder |
| Delivery | GitHub-only SDK/NDK/CMake/Gradle setup, tests, lint, APK artifacts, tag releases | Release signing requires repository secrets |

## Architecture at a glance

```mermaid
flowchart LR
    UI[Compose UI] --> VM[MainViewModel]
    VM --> AG[AgentRuntime / tool policy]
    VM --> MR[ModelRepository]
    AG --> AX[AccessibilityGateway]
    AX --> AS[AccessibilityService]
    AG --> OCR[BanglaOcrService]
    AS --> OCR
    AG --> SH[ElevatedShell]
    SH --> SZ[Shizuku / ADB UID]
    VM --> INF[InferenceEngine]
    INF --> JNI[JNI C++ runtime]
    JNI --> CPU[llama.cpp CPU]
    JNI --> VK[Adreno Vulkan]
    JNI --> QNN[QAIRT/QNN Hexagon]
    CPU --> JNI
    VK -. Phase 2 .-> JNI
    QNN -. Phase 3 .-> JNI
```

Detailed component, trust-boundary, thread, and data-flow diagrams are in [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

```text
lai/
├── app/                     # composition root and Compose product shell
├── core/
│   ├── contracts/           # pure tool/inference/OCR/model contracts
│   ├── policy/              # consent, shell and zero-egress decisions
│   ├── scheduler/           # evidence/thermal/memory backend routing
│   └── model/               # immutable reviewed model catalog
├── platform/
│   ├── download/            # only network permission and transport
│   ├── device/              # memory/battery/thermal environment
│   ├── accessibility/       # Android Accessibility authority
│   └── shizuku/             # ADB/root UserService authority
├── runtime/
│   ├── llama/               # isolated JNI/C++ llama.cpp adapter
│   ├── ocr/                 # replaceable OCR adapter seam
│   └── orchestrator/        # policy-gated tool dispatch
├── plugins/api/             # versioned local-only plugin contract
├── docs/                    # architecture, ADRs and device evidence
└── scripts/                 # source, privacy and boundary enforcement
```

## Remote build: fastest path

1. Open the repository on GitHub and select **Actions → Android build**.
2. Choose **Run workflow**, leave `debug`, and run it.
3. When the build finishes, download `lai-debug-<run>` from **Artifacts**.
4. Install on the Snapdragon test phone:

   ```bash
   adb install -r app-debug.apk
   ```

5. Enable **Settings → Accessibility → LAI automation** only after reviewing the description.
6. For elevated operations, install/start [Shizuku](https://shizuku.rikka.app/guide/setup/) and approve LAI.
7. Record test results using [DEVICE_TESTING.md](docs/DEVICE_TESTING.md).

No Android SDK, NDK, CMake, Gradle distribution, QNN SDK, or model is installed by the repository-generation environment.

## Release build and signing

Tagging a semantic version such as `v0.2.0` builds and publishes an APK. Without signing secrets the workflow uses the Android debug key so the artifact is installable for testing but **not production signed**.

Configure these GitHub Actions secrets for production signing:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

See [BUILD_AND_RELEASE.md](docs/BUILD_AND_RELEASE.md) for exact commands and key-rotation cautions.

## Model download

The signed web catalog provides supported models with one-tap explicit download. A verified cache and embedded fallback keep the list available offline. Advanced manual URLs still require a mandatory SHA-256; local file import verifies the same reviewed digest, exact size and GGUF signature without storing the source URI. `platform:download` is the sole network-owning module; no prompts, screen data, generations or telemetry have an outbound path.

The v0.2 CPU backend can load a compatible GGUF after explicit user selection. Backend requirements and the Qualcomm licensing boundary are in [MODELS_AND_BACKENDS.md](docs/MODELS_AND_BACKENDS.md).

## Repository initialization (new owner/fork)

```bash
git init -b main
git add .
git commit -m "feat: scaffold LAI phase 1 runtime"
git remote add origin https://github.com/OWNER/lai.git
git push -u origin main
```

Use a credential manager or short-lived token. Never place a token in a remote URL, source file, workflow, shell script, or commit. If a token appears in chat or logs, rotate it after use.

## Documentation index

- [Architecture](docs/ARCHITECTURE.md)
- [Module ownership](docs/MODULES.md)
- [NpuHub comparison](docs/ARCHITECTURE_COMPARISON_NPUHUB.md)
- [Local-first privacy invariants](docs/PRIVACY_INVARIANTS.md)
- [Implementation status](docs/STATUS.md)
- [Build and release](docs/BUILD_AND_RELEASE.md)
- [Models and native backends](docs/MODELS_AND_BACKENDS.md)
- [Bangla OCR](docs/BANGLA_OCR.md)
- [Automation tool contract](docs/AUTOMATION_TOOLS.md)
- [Security and safety](docs/SECURITY_AND_SAFETY.md)
- [Physical-device testing](docs/DEVICE_TESTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Contributing/documentation policy](CONTRIBUTING.md)

## License

Apache License 2.0. Third-party model weights, Qualcomm SDK components, Shizuku, and future inference engines retain their own licenses and must be reviewed independently.
