# Redmi Turbo 4 Pro — Phase 1 physical-device result

Test date: 2026-08-16  
Tested release: `v0.1.0` / commit `fa7981e`  
Result source: user-observed UI screenshots and device report

## Device

| Field | Value |
|---|---|
| Device | Redmi Turbo 4 Pro |
| Physical RAM | 12 GB |
| Extended/virtual memory | 6 GB reported by device |
| Android | 16 |
| SoC | Qualcomm Snapdragon 8s Gen 4 |
| GPU | Adreno 825 |
| NPU | Qualcomm Hexagon |
| Shizuku identity | UID 2000 (ADB shell) |

Virtual memory is recorded as device configuration, not treated as equivalent to physical RAM for model-allocation decisions.

## Passed observations

- Application launched and rendered correctly in dark mode.
- Chat, Screen Reader, Automator, and Settings surfaces were reachable.
- Bangla text rendered correctly in onboarding and chat responses.
- Bangla/English input was accepted by the chat field.
- Accessibility service reported connected.
- Accessibility screen inspection completed and returned an 8,704-character JSON structure.
- Accessibility screenshot capability reported ready on Android 16.
- Shizuku connected successfully and reported shell UID 2000.
- Developer Mode correctly reported the Phase 1 JNI boundary and absence of a concrete inference backend.
- No crash was reported during these checks.

## Not yet validated

- OCR recognition, because the Phase 1 OCR engine intentionally has no model.
- Accessibility click/type/scroll against a dedicated harmless test harness.
- Shizuku allowlisted command execution result, beyond binder/permission readiness.
- Model download interruption/resume and hash mismatch behavior.
- LLM model loading, token generation, cancellation, memory pressure, and thermals.
- Vulkan/Adreno or QNN/Hexagon acceleration.

## Privacy handling

The supplied screenshots were reviewed but are not committed. Their status bars contain network/device metadata, and textual evidence is sufficient for this gate.

## Next gate

Install the first Phase 2 CPU build, download a small compatible instruct GGUF, load it explicitly, and record:

- model name and SHA-256;
- load duration and peak memory;
- first Bangla token correctness;
- time to first token and decode rate;
- cancellation behavior;
- 10-minute thermal behavior;
- crash/log evidence, if any.
