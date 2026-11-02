# CPU backend qualification plan

**Status:** CPU gate open. GPU and NPU qualification must wait until this gate
passes. This plan covers the current LAI CPU backend and its shipped model
catalog; it does not claim that every arbitrary GGUF is supported.

## Current evidence

Build `0.1.456` / versionCode `456` (source
`c6c20725aacc7855f9e5546717c5464d8e3aab41`, Actions run `41434515200`) was
exercised on the Redmi Turbo 4 Pro. Both catalog CPU quantizations of Qwen 2.5
1.5B Instruct loaded on `llama-cpu` (Q4_K_M 470 ms; Q4_0 1433 ms), with CPU
placement and no captured `Vulkan0` allocation. Q4_K_M returned an expected
English arithmetic/format answer and retained it through four linked turns;
Q4_0 returned the expected answer in one cold-start turn. This is narrow smoke
evidence only. Bangla, cancellation, streaming, memory/thermal behavior, and
repeatable benchmark behavior have not passed qualification. See the
[2026-11-02 device result](../device-results/2026-11-02-redmi-turbo-4-pro-cpu-baseline.md)
and `PROJECT_STATE.md`.

The owner separately reported partial inaccuracies in three earlier
build-456 generations using `llama-3.2-3b-instruct-q4_k_m`, a model outside the
reviewed catalog. The prompts, settings, and outputs were not retained, so the
specific issue cannot be reproduced or attributed. That report remains open;
the Qwen smoke result does not resolve it.

## Acceptance matrix

| Area | Required evidence | Current status |
|---|---|---|
| Supported model/quantization load | Load each catalog CPU-compatible model/quantization on the named device; record model ID, quant, context, load outcome/time and memory | Partial: recommended Qwen Q4_K_M loaded in 470 ms and Q4_0 in 1433 ms on build 456; other catalog combinations remain untested |
| CPU-only placement | Explicit CPU pin; CPU context/compute buffers; no accelerator compute allocation; actual scheduler decision matches CPU | Pass for Qwen Q4_K_M and Q4_0 build 456 sessions only; see device result |
| Single-turn correctness | Fixed, privacy-safe prompt set with expected factual/format properties; owner reviews actual responses; record model/settings and outcome without committing private response text | Arithmetic/format answer passed on both Qwen quantizations; Q4_K_M passed four linked turns. A harmless English creative prompt produced off-target Bengali/refusal output on Q4_K_M at default and greedy settings, while Q4_0 produced a coherent English answer. A source-level language-instruction conflict is being corrected; candidate build/device retest pending |
| Bangla/English quality | Owner-reviewed Bangla and English checks on the recommended catalog model; verify instruction following, factual answer and concise response | English arithmetic and Q4_0 story checks passed; Q4_K_M failed English-language adherence on a synthetic prompt. Bangla prompt not tested because ADB did not inject Bengali text reliably. Catalog flag remains `banglaQualityValidated = false` |
| Multi-turn/context | At least three linked turns; verify prior user facts are retained and unrelated facts are not invented | Four linked English turns retained the answer and one derived fact correctly; longer context/unrelated-fact checks remain open |
| Streaming | Tokens arrive incrementally, final completion is correct, no dropped/corrupted UTF-8 | Partial: long output appeared incrementally in LAI and Bangla Unicode rendered; the Q4_K_M content was incoherent. Q4_0 coherent story completed. Need a controlled UTF-8/streaming pass after the prompt fix |
| Stop/cancel and recovery | Stop during prefill and decode; verify bounded stop, usable session, and successful next generation | Decode stop returned `Generation stopped locally` at 219 and 231 tokens; process/model stayed usable. Tap-to-stop latency was not instrumented. After two unload/reload cycles, a new arithmetic generation passed. The inactive-model request/reload observation remains unexplained |
| Repeated load/unload | Repeated cycles without stale handles, leaked session state, crash, or wrong model identity | Two Q4_K_M unload/reload cycles passed in one clean MainActivity session (reloads 486 and 490 ms); post-cycle generation passed. Longer/memory-stress cycles remain open |
| Context limits | Near-capacity prompt succeeds when within budget; over-budget prompt fails clearly without corrupting the next turn | Not tested |
| Memory pressure | Record peak memory and behavior under realistic competing memory use; low-memory failure must be clean and recoverable | Not tested |
| Thermal/thread governor | Warm repeated run; capture temperature/thermal state, thread changes, latency and process health; confirm no deadlock or unsafe thread mutation | Not tested |
| Benchmark consistency | Fixed model, prompt, context, generation settings and thermal starting condition; repeat runs and compare prefill/decode/TTFT and memory | Three generations exist, but prompts and controlled conditions are unknown; not a benchmark pass |

## Execution and evidence rules

1. Trace model selection, conversation preparation, system prompt, GGUF chat
   template, sampling configuration and JNI option mapping before changing
   output behavior. Do not infer that a CPU kernel or sampler caused an answer
   issue from CPU placement alone.
2. Use a fixed, small, privacy-safe prompt set for reproducibility. For a
   diagnostic comparison, first run with temperature 0 (greedy decoding), then
   with the normal settings; hold model, prompt, context and all other settings
   constant. This distinguishes repeatability from sampling variation without
   presuming sampling is the defect. Include Bangla and English checks. Do not
   log prompts, model output, or private chat. Keep detailed content local and
   ephemeral if the owner elects to share it; repository records contain only
   scenario IDs and reviewed outcomes.
3. For this goal, the assistant may interact with LAI under owner decision
   `DEC-2026-11-02-001`. Use synthetic prompts, arm scoped app/native log
   capture before each scenario, then inspect only relevant tags, redact, and
   record content-free outcomes. Do not save prompt or response text.
4. For every failure, preserve the exact commit/build, device, model/quant,
   settings, thermal context, expected property, observed outcome, relevant
   content-free logs, diagnosis confidence, fix and retest result.
5. Run relevant CI/unit/build checks for each source fix. Provide a GitHub build
   for owner testing and record device evidence. A compile, load, or completed
   generation alone does not satisfy CPU readiness.

## Gate close criteria

CPU can be marked release-ready only after every matrix row has passing,
reviewable evidence for the supported catalog models on the target device,
including owner-reviewed Bangla and English quality;
response correctness has owner-reviewed outcomes; failure and recovery paths
are clean; benchmark results are repeatable and bounded; relevant CI/build
checks pass; and `PROJECT_STATE.md` records the exact artifacts and limitations.
The first required device run is the reviewed Qwen 2.5 1.5B Instruct Q4_K_M CPU baseline; Q4_0 is the catalog alternate and should also be checked for load/runtime stability. The non-catalog Llama 3.2 3B result remains useful supplementary evidence only. Until then CPU stays the active stage and GPU/NPU work remains deferred.
