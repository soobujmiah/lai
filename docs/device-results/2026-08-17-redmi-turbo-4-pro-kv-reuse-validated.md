# Redmi Turbo 4 Pro — KV-prefix reuse validated (0.9.5)

Test date: 2026-08-17 (11:46 Asia/Dhaka)
Tested build: `0.9.5` (versionCode 111), **production-signed**
Result source: exported `lai-diagnostics-v1.json`, 14 completed generations

## Headline

Steady-state time-to-first-token dropped from **17 s and climbing** (0.9.0, full re-prefill
every request) to **~0.6 s flat** — a ~25× improvement, measured on device, on battery
(79 %, not charging), thermal `NOMINAL` throughout.

## Evidence

| Turn | promptTokens | evaluatedPromptTokens | TTFT | Note |
|---|---|---|---|---|
| 1 | 334 | 334 | 13.6 s | Cold prefill, paid once |
| 2 | 356 | 13 | 595 ms | Reuse begins |
| 3 | 400 | 11 | 536 ms | |
| 4–9 | 448–546 | ~12 each | 601–705 ms | **Flat while history grows** |
| 10 | 542 | 214 | 8.3 s | Prefix shift → bounded re-prefill |
| 11 | 517 | 1 | 118 ms | Maximal reuse |
| 12 | 500 | 172 | 7.2 s | Prefix shift |
| 13 | 334 | 6 | 298 ms | New chat: system-prompt prefix itself reused |
| 14 | 358 | 15 | 690 ms | |

- Decode throughput 12–17 tok/s (unchanged, as expected — decode was never the problem).
- `promptTokensPerSecond` now divides by evaluated tokens: honest 17–25 tok/s prefill readings.
- Model load 1 517 ms. `windowedConversationTurns: 0` — the memory slider was not exercised
  this run; the window path remains device-unverified.

## Observations to watch

1. **Several 3-token replies** (turns 5, 6, 8, 9). Could be legitimately terse answers, or the
   new brevity-biased system prompt + repetition penalty cutting replies too short. Needs a
   qualitative check of actual reply text (diagnostics exclude content by design).
2. Turn-10/12 prefix shifts are consistent with the tool-instruction gate toggling or window
   sliding — bounded cost, works as designed.
