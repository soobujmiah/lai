# 2026-11-02 Redmi Turbo 4 Pro CPU baseline and prompt-fix retest

## Candidate build 458 retest

- App: `dev.lai.runtime` `0.1.458` / versionCode `458`
- Source: `143fccb8471093bb91a74bb8709c49cda66724cc`
- GitHub Actions: [run 41447878524](https://github.com/soobujmiah/lai/actions/runs/41447878524), workflow-dispatch release-variant build; CI checks and APK build passed
- Install: `adb install -r`; existing app data retained, no uninstall
- Device: same Redmi Turbo 4 Pro, Android 15, arm64
- Settings: temperature `0.7`, top-P `0.9`, max reply `256`, default seed; developer mode was turned off after testing

The corrected system prompt passed English language adherence on both catalog
quantizations. Q4_K_M loaded on `llama-cpu` in 544 ms; arithmetic returned `42`
(2 tokens; prefill 20.27 tok/s, decode 12.99 tok/s, TTFT 3.46 s, total 3.61 s).
The same short English story request returned English text at the 256-token
limit (prefill 23.23 tok/s, decode 8.66 tok/s, TTFT 0.99 s, total 30.56 s).
Its response was cut off by the configured token cap and included inconsistent
story details, so this passes language selection only, not creative correctness.

Q4_0 loaded on `llama-cpu` in 1166 ms and returned an English response to the
same story prompt in 91 tokens (prefill 19.93 tok/s, decode 9.02 tok/s, TTFT
3.37 s, total 13.46 s). It completed coherently at a basic narrative level,
though some phrasing was strained. Q4_0 then streamed a longer reply visibly;
Stop produced `Generation stopped locally`, and the following arithmetic turn
again returned `42` (2 tokens; prefill 18.58 tok/s, decode 10.81 tok/s, TTFT
1.14 s, total 1.32 s). Stop-to-terminal latency was not independently timed.
The app process remained alive. Logs show the scheduler selected CPU: CPU KV
buffer 112.00 MiB, compute buffer 151.38 MiB, and output buffer 0.58 MiB; the
generation thread used big cores 0-3 and returned to idle little core 7. Vulkan
device enumeration appeared in logs, but no `Vulkan0` compute allocation was
observed in the captured CPU sessions.

This candidate retest confirms the prompt change fixes the observed language
drift for these two Qwen quantizations, but overall CPU quality and the CPU
acceptance gate remain open. Bangla input/response, broader factual checks,
context limits, memory pressure, thermal stability, repeatable benchmarking,
and bounded cancellation timing still need evidence. Prompts and raw outputs
are not retained.

## Build and device

- App: `dev.lai.runtime` `0.1.456` / versionCode `456`
- Source: `c6c20725aacc7855f9e5546717c5464d8e3aab41`
- GitHub Actions: [run 41434515200](https://github.com/soobujmiah/lai/actions/runs/41434515200), completed successfully
- Device: Xiaomi Redmi Turbo 4 Pro, model `25053RT47C`, codename `onyx`, Android 15, arm64
- Models: `qwen2.5-1.5b-instruct-q4-k-m` (reviewed Qwen 2.5 1.5B Instruct Q4_K_M baseline) and `qwen2.5-1.5b-instruct-q4-0` (Q4_0 alternate)
- Backend: `llama-cpu`; load completed in 470 ms (Q4_K_M) and 1433 ms (Q4_0)
- Input authority: assistant-operated LAI UI test under scoped owner decision `DEC-2026-11-02-001`
- Settings: app defaults (temperature 0.7, top-P 0.9, max reply 256, runtime-selected seed); one earlier malformed input attempt was excluded

## Prompt and sampler source trace

`MainViewModel` prepares the selected conversation window and maps the active
numeric settings into `GenerationConfig`. Native `apply_chat_template()` adds
the LAI system prompt, reads the model-provided GGUF `tokenizer.chat_template`,
and fails closed if the metadata is absent or cannot format the supported
roles. The default numeric settings are temperature `0.7`, top-P `0.9`, maximum
new tokens `256`, and seed `-1` (runtime-selected). At temperature zero, native
code selects greedy sampling; otherwise it applies repeat penalties, top-P,
temperature, and distribution sampling in that order. The test used app defaults. These source paths were traced, but the owner's earlier Llama sample
is unavailable, so there is no evidence to blame the template, sampler, model,
or CPU kernel for that separate report.

## Result

A privacy-safe English arithmetic/format scenario returned the expected concise
answer. Three linked Q4_K_M follow-ups correctly recalled the result and one derived
fact, providing a four-turn context smoke check. The Q4_0 model also returned
the expected answer to the same simple English arithmetic/format check. The
prompts and responses are intentionally omitted. Captured runtime evidence
showed CPU pinning, CPU backend selection, CPU buffers, one graph split, and no
`Vulkan0` allocation. The process remained alive and all reviewed generations
completed.

| Scenario | Result | Prefill | Decode | TTFT | Total |
|---|---|---:|---:|---:|---:|
| English arithmetic and format | Expected concise answer | 22.6 tok/s | 10.9 tok/s | 0.80 s | 0.98 s |
| Linked follow-up/context | Correctly retained prior result | 23.4 tok/s | 6.9 tok/s | 0.86 s | 1.15 s |
| Derived follow-up | Correctly calculated from retained result | 22.6 tok/s | 8.7 tok/s | 0.94 s | 1.16 s |
| Earlier-answer recall | Correctly recalled the first result | 22.1 tok/s | 7.4 tok/s | 1.13 s | 1.40 s |
| Q4_0 cold-start arithmetic/format | Expected concise answer | 19.5 tok/s | 12.0 tok/s | 5.39 s | 5.56 s |

The short follow-ups produced two-token answers; their decode rates are not a
stable benchmark. Q4_0 cold-start latency is not directly comparable to the
warmed Q4_K_M follow-ups. The first correct run used the standard
app defaults. Thermal state and memory peak were not captured. The first input
attempt contained literal percent escape text and is excluded from correctness
results. No prompt/output text is present in repository files or captured logs.

## English language-control scenario

A synthetic English creative-writing scenario exposed a new quality failure on
Q4_K_M. In the first run (after an arithmetic turn), the model returned a
premature refusal in 12 tokens. In a fresh-chat default-settings run, it
produced off-target Bengali text; at temperature 0, another run repeated
Bengali text unrelated to the English request. Both long replies were stopped
locally (219 and 231 tokens); the exact tap-to-stop latency was not measured.
The app stayed alive. The same prompt on Q4_0 at app defaults produced a
coherent English story and completed in 146 tokens (prefill 19.3 tok/s, decode
8.1 tok/s, TTFT 5.08 s, total 23.14 s). The tested prompts and raw replies are
not retained.

Source trace found conflicting language instructions in
`runtime/llama/src/main/cpp/llama_session.cpp`: the prompt said to use Bangla
when the user writes Bangla, then unconditionally instructed the model in
Bangla to answer in Bangla. The source prompt now instructs the model to use
the user's language and keeps simple Bangla conditional on Bangla input. This
focused prompt fix passed CI and the English language-control comparison on
both quantizations on build 458. This
supports the prompt conflict as the cause of the observed language drift in
that scenario; it does not prove all earlier inaccurate replies are resolved.

## Limits and next checks

This is smoke evidence, not CPU acceptance. It does not validate Bangla output
(ADB Gboard initially used Bangla layout; no Bengali prompt was submitted),
bounded stop latency, context limits, memory pressure,
thermal/thread-governor stability, or benchmark repeatability. Two Q4_K_M
unload/reload cycles completed in 486 and 490 ms, followed by a successful
arithmetic generation. A temperature-zero story run reproduced the English
language/output problem. A subsequent no-model request was rejected before
inference and Q4_K_M reloaded in 638 ms; the trigger is unknown. The owner previously reported
inaccurate replies from a separate non-catalog Llama 3.2 3B model; that report
is not resolved by this narrow Qwen check. Keep the CPU gate open and defer GPU
and NPU qualification.

Content-free local logs (Termux temp, not committed):
`lai-cpu-owner-test-20261102T120304Z.log` and
`lai-cpu-valid-20261102.log`, `lai-cpu-followup-20261102.log`, and
`lai-cpu-q4_0-20261102.log`; build-458 capture:
`lai-cpu-build458-20261102.log`.
