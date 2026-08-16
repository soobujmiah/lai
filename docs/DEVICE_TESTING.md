# Snapdragon physical-device testing

Build in GitHub Actions, install on the physical phone, collect only redacted evidence, and refine in a new commit. Do not send private screen dumps or tokens.

Recorded results:

- [2026-08-16 — Redmi Turbo 4 Pro, Android 16, Snapdragon 8s Gen 4](device-results/2026-08-16-redmi-turbo-4-pro.md)

## Device record

```text
LAI commit/tag:
APK artifact/run:
Production signed: yes/no
Device model:
SoC:
RAM:
Android/build fingerprint:
Security patch:
Shizuku version/mode/UID:
Battery level / plugged:
Ambient temperature:
```

## Phase 1 smoke test

1. Install debug APK with `adb install -r`.
2. Launch; verify three bottom modes and no telemetry on main screens.
3. Switch device language to বাংলা; verify labels render and layout remains usable.
4. Open Settings; enable Developer Mode; confirm native status says no concrete backend.
5. Enable LAI accessibility in Android Settings.
6. In another non-sensitive app, use **Inspect screen**; verify node count result.
7. Test a manually approved click/type/scroll through an instrumented harness when added; never start with banking, password, authenticator, or system-update screens.
8. Start Shizuku; return to LAI; approve permission; verify UID status.
9. Deny/revoke Shizuku and accessibility separately; verify operations fail cleanly.
10. Enter a small known GGUF Hugging Face URL and SHA-256; interrupt/resume download; confirm install metadata. Large models are unnecessary for Phase 1.
11. Tap Screen Reader; confirm the missing OCR model is clearly reported rather than fake text.
12. Background/foreground the app, rotate, lock/unlock, and kill/restart Shizuku.

## Logs

Capture narrow logs:

```bash
adb logcat -c
adb logcat --pid="$(adb shell pidof -s dev.lai.runtime.debug)" -v threadtime
```

For native crash evidence:

```bash
adb logcat -b crash -d
adb shell dumpsys meminfo dev.lai.runtime.debug
```

Redact package names if sensitive and never include accessibility node text from private apps.

## Phase 2/3 performance protocol

Use airplane mode, fixed brightness, a stable thermal starting condition, the same prompt/model/hash/context, and at least five runs. Record:

- model load latency;
- time to first token;
- prefill tokens/s;
- decode tokens/s;
- peak Java/native/PSS;
- battery power when available;
- thermal status and throttling time;
- output hash/quality comparison for CPU, Vulkan, QNN;
- fallback reason and runtime/driver versions.

A faster backend is not accepted if Bangla output, tool JSON, or numerical correctness regresses beyond an agreed threshold.

## Feedback template

```text
Expected:
Observed:
Reproduction steps:
Frequency:
Relevant redacted logs:
Screenshot (non-sensitive only):
Regression from artifact:
```
