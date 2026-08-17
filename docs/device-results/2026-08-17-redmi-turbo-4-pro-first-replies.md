# Redmi Turbo 4 Pro — first successful chat generations (P0 closed)

Test date: 2026-08-17 (08:28 Asia/Dhaka)
Tested build: `0.9.0` (versionCode 88), **production-signed** (`productionSigned: true`)
Result source: user screenshot + exported `lai-diagnostics-v1.json`

## Device

| Field | Value |
|---|---|
| Model | Xiaomi 25053RT47C (Redmi Turbo 4 Pro) |
| SoC | QTI SM8735 (Snapdragon 8s Gen 4), 8 cores |
| Android SDK | 36 |
| Free memory at export | 3.74 GB |
| Battery | 99 %, charging |
| Thermal at export | NOMINAL |

## Result — first replies in project history

Six generations completed, streamed, and rendered: English ("hi" → "Hello! How can I assist you today?", "what can you do" → capability reply) and Bangla ("হাই" → "হাই, আমি কি করতে পারি?", "কি করতে পারো?" → reply). Model load: **721 ms**.

| Turn | Prompt tokens | TTFT | Prefill tok/s | Decode tok/s | Generated |
|---|---|---|---|---|---|
| 1 | 159 | 6.2 s | 25.6 | 17.6 | 9 |
| 2 | 182 | 6.4 s | 28.5 | 19.3 | 27 |
| 3 | 222 | 7.1 s | 31.3 | 18.2 | 22 |
| 4 | 268 | 9.6 s | 28.1 | 15.7 | 165 |
| 5 | 444 | 14.6 s | 30.5 | 17.6 | 9 |
| 6 | 470 | 17.0 s | 27.6 | 15.7 | 26 |

## Analysis

1. **The watchdog hypothesis is confirmed.** Prefill measures 25–31 tok/s, so the pre-fix ~407-token prompt genuinely needed ~14 s — the former 4 s cancel watchdog aborted every healthy generation across five reports. The 45 s grace plus the prefill cut fixed it.
2. **`ToolInstructionGate` worked as designed.** "hi" cost 159 prompt tokens (no ~314-token instruction); `proposalResponsesExamined: 6`, all `NOT_TOOL_CALL`, none misparsed.
3. **New top cost: linear TTFT growth.** Every `generate()` clears the KV cache and re-prefills the whole conversation (159 → 470 tokens in six turns; 6.2 s → 17.0 s TTFT). KV-prefix reuse is the next performance priority.
4. **Bangla output quality is weak** — grammatical but partly incoherent (base Qwen 2.5 1.5B limitation), while English is clean. Tracked as a roadmap decision, not a runtime bug.

## New UI bug observed (fixed in the follow-up commit)

When the keyboard closes, the composer slides down and the bottom mode bar pops in only at the end of the animation, displacing the layout for a frame ("navigation tab pushed off screen, then comes back"). Cause: bar visibility keyed on the *current* IME inset (`isImeVisible`), which stays non-zero until the close animation ends. Fix: key on `WindowInsets.imeAnimationTarget`, which flips at animation start.
