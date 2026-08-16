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

## v0.7.0 runtime reliability evidence

A later v0.7.0 test supplied text-visible device evidence (screenshots reviewed but not committed):

| Measurement | Result |
|---|---:|
| Backend | llama.cpp CPU |
| Scheduler | CPU, evidence/fallback policy |
| Available memory | 3,589 MiB |
| Estimated peak | 1,833 MiB |
| Model load | 578 ms |
| Battery | 72%, not charging |
| Thermal | NOMINAL |
| Prompt tokens | 207 |
| Output tokens | 182 |
| Time to first token | 4,533 ms |
| Prefill | 45.76 tok/s |
| Decode | 20.35 tok/s |
| Total generation | 13,473 ms |

The conversation showed several user/assistant turns and the final request received a coherent Bangla response, providing basic evidence that history reached the formatted prompt and Bangla output rendered correctly. This is not a broad Bangla quality evaluation. The model remained loaded and no crash/freeze was reported.

The supported-model status still showed the embedded offline list; signed web refresh/cache was not tested.

## v0.7.1 Diagnostics JSON evidence

The user exported `lai-diagnostics-v1.json`; the file itself remains outside the repository. Text-only observations:

- schema version 1, app v0.7.1 build 46, `productionSigned=false`;
- operation `READY`, native CPU backend loaded, 4,096-token context;
- model load 585 ms;
- 3,176,853,504 bytes available (~3,029.7 MiB) versus 1,922,627,104-byte estimate (~1,833.6 MiB);
- battery 64%, not charging, thermal `NOMINAL`;
- four local performance samples with prompt growth 159 → 183 → 220 → 295 tokens, providing stronger multi-turn-history evidence;
- average TTFT 5,575 ms (range 4,422–7,825);
- average prefill 38.60 tok/s (range 32.95–42.21);
- average decode 20.43 tok/s (range 19.47–21.72);
- average total 7,156 ms (range 5,283–10,598);
- no context turns trimmed yet;
- Accessibility disconnected and Shizuku permission required during this chat-only test;
- the export contained runtime/model/performance metadata and its privacy exclusion declaration, with no prompts, generated text, screenshots, OCR text, Accessibility trees, documents, credentials, package names, or network identifiers.

## v0.8.0 retained-model and tool-proposal evidence

The user supplied a v0.8.0 `lai-diagnostics-v1.json` export and answered the behavioral test checklist. The JSON attachment remains outside the repository; only these text observations are retained.

| Measurement | Result |
|---|---:|
| App/build | v0.8.0 / versionCode 55 / temporary signing |
| Report time | 2026-08-16 21:37:53 +06:00 |
| Android / SoC report | API 36 / QTI SM8735 / 8 CPU cores / arm64-v8a |
| Backend | `llama-cpu` |
| Available memory | 4,313,878,528 bytes (~4,114.0 MiB) |
| Estimated model peak | 1,933,521,832 bytes (~1,844.0 MiB) |
| Model load | 514 ms |
| Battery / thermal at export | 84%, not charging / `NOMINAL` |
| Accessibility / Shizuku | connected / UID 2000 |
| Prompt-token range | 382 → 406 → 442 → 510 |
| Output tokens | 9, 23, 38, 32 |
| Average TTFT | 9,174 ms (range 8,616–10,402) |
| Average prefill | 47.36 tok/s (range 44.38–49.07) |
| Average decode | 20.70 tok/s (range 19.49–21.44) |
| Average total | 10,421 ms (range 9,035–12,043) |
| Context trimming | 0 turns |

The four samples provide another basic multi-turn growth result. Their larger prompts and enabled tool instruction make direct latency comparison with v0.7.1 inappropriate; decode remained around the previously observed 20 tok/s.

The retained-model acceptance gate passed: **Keep copy** created the reviewed GGUF, the file survived LAI uninstall, v0.8.0 was reinstalled, and **Import file** restored and loaded the model without a network download. The active imported artifact reported the exact reviewed 1,117,320,736-byte size and full SHA-256 `6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e`.

Local action proposals were enabled, but no valid model proposal appeared and the v0.8.0 in-memory audit contained zero records. Therefore parser-dialog approval/denial and Android action execution did **not** pass physical validation. This negative result requires privacy-safe proposal outcome diagnostics and/or prompt-format refinement before another model-compliance test.

Only ordinary multi-turn generation was exercised. Stop/recovery, New chat reset, forced context trimming, and sustained thermal behavior were not tested. `NOMINAL` is one export-time observation, not sustained thermal evidence.

The export contained no prompts, generated text, screenshots, OCR text, Accessibility trees, foreground packages, documents, tool arguments/results, typed automation text, credentials, or network identifiers. Catalog status remained the embedded offline fallback; signed web refresh/cache remains unvalidated.

## Remaining gate

- refresh and cache the signed catalog, then restart offline;
- diagnose model tool-proposal formatting without exporting user/model content, then validate rejection, denial and one-time approval on v0.8.1 or later;
- verify the v0.8.1 persistent audit survives process restart and blocks exact-call replay;
- press Stop during a long response and immediately generate again;
- verify New chat forgets prior turns;
- force enough turns to exercise context trimming;
- run a 10-minute thermal test;
- provide crash/log evidence if any.
