# Snapdragon physical-device testing

Build in GitHub Actions, install on the physical phone, collect only redacted evidence, and refine in a new commit. Do not send private screen dumps or tokens.

Recorded results:

- [2026-08-16 — Redmi Turbo 4 Pro, Android 16, Snapdragon 8s Gen 4](device-results/2026-08-16-redmi-turbo-4-pro.md)
- *(this list is not kept current commit-by-commit — dozens of results from 2026-08-17 through 2026-09-03 exist under `device-results/` but aren't individually indexed here; treat the directory listing and `PROJECT_STATE.md`'s inline references as authoritative for what's been recorded, not this list alone)*
- [2026-09-04 — Hexagon HTP `llama-hexagon` qualify: first real PASS](device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md)
- [2026-09-04 — Hexagon HTP reproducibility confirmed; Q4_K quant limit found](device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-limit.md)
- [2026-09-04 — ChatterUI OpenCL reference investigation](device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md)
- [2026-09-04 — OpenCL main-thread hang: root cause (source trace, no device run)](device-results/2026-09-04-opencl-hang-main-thread-root-cause.md)
- [2026-09-04 — OpenCL revalidation: app hang fixed, backend itself still not viable](device-results/2026-09-04-redmi-turbo-4-pro-opencl-revalidation.md)
- [2026-09-04 — Vulkan instance-init diagnostic: Vulkan ruled out as the `llama_backend_init()` blocker](device-results/2026-09-04-redmi-turbo-4-pro-vulkan-instance-init-diagnostic.md)
- [2026-09-04 — OpenCL registration diagnostic: `clGetPlatformIDs()` is the first call without a matching EXIT](device-results/2026-09-04-redmi-turbo-4-pro-opencl-registration-diagnostic.md)

## Device record

```text
LAI commit/tag:
APK artifact/run:
Production signed: yes/no
Device model:
SoC / reported SoC manufacturer:
RAM:
Android/build fingerprint:
Security patch:
Shizuku version/mode/UID:
Battery level / plugged:
Ambient temperature:
```

## Online-acquisition / offline-operation gate

Open Model Setup and explicitly refresh the supported list; record that the status changes to a signed web catalog. Disable connectivity, restart LAI and verify the cached list remains. Download or import a reviewed model, then keep airplane mode enabled while loading, generating, cancelling, using OCR and running automation. Capture traffic separately to confirm only catalog/model acquisition endpoints are contacted and no user content is sent.

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

## One-shot tool proposal gate

1. Confirm Local action proposals is off by default and ordinary chat behavior is unchanged.
2. Enable it in Settings and request one harmless action against a non-sensitive test app.
3. Make one explicit, unambiguous Android action request. Immediately check Developer Mode: the parser must report `ACCEPTED`, `NOT_TOOL_CALL`, or a coarse `REJECTED_*` outcome without exposing response text.
4. Verify mixed prose plus JSON, markdown fences, unknown fields, `confirmed`, `allowSensitiveInput`, wrong types, invalid packages and non-allowlisted shell operations do not open an executable proposal and increment only content-free rejection counters.
5. For valid JSON, verify the dialog shows the exact action/risk and that nothing happens before approval.
6. Press **Do not run** and verify no Accessibility/Shizuku action occurs.
7. Propose again, press **Approve once**, verify exactly one action occurs, then verify the exact call cannot be approved again after rotation, backgrounding, Activity recreation, or process restart.
8. Restart LAI and verify Developer Mode reports a valid persistent chained audit with approval and completion events.
9. Verify password fields remain blocked and no argument, selector, typed text, fingerprint, record hash, package or result appears in diagnostics/logs.
10. Confirm tool result is not automatically sent back to the model and no second action runs.
11. If a test build deliberately corrupts/truncates the audit file, verify proposal mode disables and no approved action executes.

## Retained model copy

With an installed reviewed model, press **Keep copy**, save to Documents/Downloads, and wait for SHA verification. Uninstall LAI, confirm the GGUF remains in the chosen location, reinstall, choose **Import file**, and verify the model loads without network traffic. App-private and app-specific external directories do not satisfy this test because Android removes both on uninstall.

## Diagnostics JSON

After model load and several generations, use **Settings → Export diagnostics JSON**. Attach this standard file with feedback; inspect it first. It must contain runtime/performance metadata and must not contain prompt, response, screenshot, OCR, document, package-name or credential content. The schema is documented in [DIAGNOSTICS_EXPORT.md](DIAGNOSTICS_EXPORT.md).

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

- exact device, SoC, Android/firmware, and starting thermal state;
- stable backend ID, compute class, capability evidence, and runtime/SDK/driver version;
- model ID, artifact format, quantization/precision, digest, context, and prompt/input size;
- model load latency, time to first token, prefill tokens/s, decode tokens/s, and total latency;
- available memory plus peak Java/native/PSS;
- CPU/GPU/NPU utilization only when a trustworthy measurement method exists;
- battery power/energy when available, including the measurement method;
- sustained duration, thermal transitions, throttling time, cancellation/unload, and failures;
- output/quality comparison with the CPU reference;
- backend-failure result, fallback backend, and explicit fallback reason.

For QNN, additionally record QAIRT version, HTP architecture/runtime compatibility, converted-artifact identity, and graph/context-cache state. Never generalize one SoC result to all Snapdragon devices.

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
