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

## Phase 2 CPU model evidence

A later on-device report from the same Redmi Turbo 4 Pro confirmed:

- llama.cpp CPU backend reported ready;
- Qwen 2.5 1.5B Instruct Q4_K_M installed with expected 1,065 MiB display size and digest prefix `6a1a2eb6d156…`;
- scheduler selected CPU using evidence and deterministic fallback order;
- Android reported 3,077 MiB available memory;
- conservative estimated model peak was 1,833 MiB, so memory preflight allowed loading;
- battery was 79%, not charging;
- thermal state was `NOMINAL`;
- model session was loaded (Unload control active);
- one local generation completed with 15 tokens;
- no crash or freeze was reported.

The supported-model status remained `Embedded supported-model list available offline`; signed web refresh/cache is not yet physically validated. The generated response content, Bangla quality, load latency, TTFT, decode speed, cancellation and sustained thermals were not shown, so those claims remain pending.

The supplied screenshots were reviewed but not committed because status bars contain device/network metadata.

## Next gate

- refresh and cache the signed catalog, then restart offline;
- capture the actual Bangla response;
- measure load time, TTFT and decode speed with v0.7;
- cancel a long response and immediately generate again;
- run a 10-minute thermal test;
- provide crash/log evidence if any.
