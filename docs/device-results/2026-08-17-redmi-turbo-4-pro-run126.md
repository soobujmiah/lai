# Device report — Redmi Turbo 4 Pro (run 126) — 2026-08-17

**Source:** `lai-diagnostics-v1.json` (schema v1, `generatedAtEpochMs` 1786980516963 → 2026-08-17T10:08 UTC)
**App:** `0.6.126-debug` (versionCode 126, debug-signed, operation `READY`)
**Catalog:** `Embedded supported-model list available offline`

## Device (target hardware ✅)
| Field | Value | Expected |
|---|---|---|
| `manufacturer` | Xiaomi | Xiaomi |
| `model` | 25053RT47C | 25053RT47C (Redmi Turbo 4 Pro) |
| `androidSdk` | 36 | 36 (matches PROJECT_STATE target SDK 36) |
| `supportedAbis` | arm64-v8a | arm64-v8a |
| `socManufacturer` | QTI | QTI |
| `socModel` | SM8735 | SM8735 (Snapdragon 8s Gen 4) |
| `cpuCoreCount` | 8 | 8 |
| `availableMemoryBytes` | 3,762,089,984 (~3.5 GiB) | ~3.0 GiB reported in v0.8.0 |
| `batteryPercent` | 39%, `charging` false, `thermalState` NOMINAL | OK — above low-battery admission threshold; thermal allows full threads |
| `accessibilityConnected` | true | ✅ required for screen tools |
| `shizukuState` | READY_UID_2000 | ✅ binder + UID 2000 validated |

## Runtime
| Field | Value | Assessment |
|---|---|---|
| `nativeLibraryLoaded` | true | JNI `liblai_runtime.so` loaded |
| `compiledBackends` | `["llama-cpu"]` | CPU only — Vulkan/QNN still planned, correct |
| `activeBackendDecision` | `LLAMA-CPU: Selected using evidence, compatibility…` | Scheduler evidence-based path, not fallback |
| `contextSize` | 4096 | Contract |
| `activeModelId` | `qwen2.5-1.5b-instruct-q4-k-m` | Reviewed baseline, `1117320736` bytes, sha `6a1a2e…9407e` (exact reviewed size) |
| `modelLoadMs` | 2581 ms | **Slow** — previous device evidence 514 ms / 0.7–1.5 s. Possible cold mmap + thermal throttling or prior stall. Needs trace. |
| `estimatedPeakBytes` | 1,933,521,832 (~1.8 GiB) | Within `availableMemoryBytes`; preflight passed |
| `trimmed/windowedConversationTurns` | 0 / 0 | No context window pressure — fresh install / history cleared |
| `performance[]` | `[]` (empty) | **No successful generation** to report |

## Generation result — ❌ STALLED
```
lastGenerationFailure: "Stalled at AWAITING_FIRST_TOKEN for over 45000 ms"
emptyGenerationCount: 1
```

* No `lastGenerationMetrics` (TTFT/prefill/decode/total) — the 45 s watchdog fired before first token.
* This is **not** the historical `~0.6 s` KV-reuse steady-state nor `12–19 tok/s` decode. The stall is before any token is emitted.

### Likely causes (ranked)
1. **Cold KV / template path** — `generatedAtEpochMs` far in future (year 2026) suggests device clock skew; could affect thermal governor baseline? Unlikely to stall.
2. **Native stall** — previous tracing added µs logs for `template, tokenize, per-chunk prefill, first token`. This run has no `performance` entries, so the stall is **before** `first_token`.
3. **Low battery (39%) + not charging** — `ThermalGovernorPolicy` should still admit (threshold is lower), but battery policy may have reduced threads and exposed a deadlock between `llama_decode` and thread-limit atomic (fixed in 0.9.7 to apply only *between* decodes — verify).
4. **Model load slower (2581 ms vs 514 ms)** — hints at memory pressure or background download still holding file lock.

### What to collect next (5 min)
```bash
adb logcat -s LAI-llama -s LAI:*  # look for µs trace: mutex wait, template, tokenize, prefill, thermal: decode threads X->Y
adb shell dumpsys meminfo dev.lai.runtime
# Re-test with: Settings → Developer Mode → export diagnostics again after a short “hi” in Bangla + English
```
Re-run with `adb` and share the 50 lines after `LAI-llama: generate start`.

## Automation / audit
| Field | Value | Assessment |
|---|---|---|
| `toolProposalsEnabled` | true | Opt-in gate working |
| `proposalResponsesExamined` | 0 | No chat yet — expected if you only exported diagnostics |
| `auditPersistence` | `APP_PRIVATE_HASH_CHAIN_V1` | Correct, hash-chained, no-backup |
| `auditIntegrityValid` | true | Full-chain verification passed |
| `records[]` | `[]` | No tool executed — correct |
| `privacy.excludedData` | 14 categories (prompts, screenshots, OCR, tool args…) | **Privacy invariants intact** — diagnostics are content-free |

## Verdict for PROJECT_STATE.md
* **Backends / env / Shizuku / accessibility:** ✅ **Device validated** — same as v0.9.7, still holds.
* **KV-prefix reuse / streaming / Bangla quality:** ⛔ **Not re-validated this run** — stall blocks any judgement. Do not claim.
* **Thermal governor:** `thermalState NOMINAL` but `battery 39%` — needs warm-device re-test to see `Reduced CPU threads…` notice.
* **Chat history / background download:** Not exercised (windowed 0, no downloads).

### Next actions (until device test needed)
1. **Investigate stall** — provide `logcat` (§ above). If reproducible, the fix is in `runtime:llama` tracing (µs logs added in 0.9.7 should pinpoint).
2. **Update PROJECT_STATE.md** — add this run as `v0.6.126-debug stall evidence` (not a regression of KV reuse, but a pre-TTFT watchdog).
3. **Re-test** — one short Bengali turn: `হ্যালো, কেমন আছো?` and one English `hi`, then export again — we expect `performance[0]` with `timeToFirstTokenMs`.

No PII leaked — diagnostics contain no prompts, replies, or file names.

---
*Generated by Lead Android & AI Systems Engineer audit — 2026-08-17*
