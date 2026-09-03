# Redmi Turbo 4 Pro — `llama-hexagon` qualification: first real PASS

**Date:** 2026-09-04
**Package:** `dev.lai.runtime` (release), installed from CI run `33812084740` (`lai-release-358`,
sha256 `b1a62a9d7e46b2b9619a826ba0a5992d59bc204befea58cf46b2a572112d591d`) — built from HEAD
`9ef2edd` (`-Plai.validatedAccelerators=llama-hexagon`).
**Command:** `scripts/device/lai_adb.sh qualify qwen2.5-1.5b-instruct-q4-0 llama-hexagon "Say hello in one short sentence." 180`
**Result:** `DONE` — script exit 0.

## Summary

This is the first time `llama-hexagon` has completed a full forced load + real generation on this
device. Every prior attempt this project cycle failed earlier in the chain (Kotlin-level startup
race, `ADSP_LIBRARY_PATH`/namespace bridge, HTP skel `.so`s never built, then built-but-unpackaged
into the APK — see the other 2026-09-03 device-results docs in this directory). With the CMake
3.31.6 pin (`a8954cb`/`21791ac`) and the HTP skel packaging fix (`9ef2edd`), this is the first APK
that actually ships `libggml-htp-v73/75/79/81.so`, and the first `qualify` run to reach a terminal
`LOAD_OK` → generation → `DONE` state for this backend.

## Evidence

`probe llama-hexagon` (before the full qualify run) already showed a real, non-null HTP device
handle where every prior CPU-only build showed `0x0`:
```
hexagon: available() find_htp_device() returned 0xb4000075c402c850 after 4 us
probe: compiledBackends[] id=llama-hexagon computeClass=NPU formats=[gguf] quantizations=[]
probe: DONE backend=llama-hexagon reportedAvailable=true totalMs=0
```

`qualify` terminal state (`LAI-qualify`/`LAI-model`/`LAI-llm` tags):
```
LAI-model : Model loaded id=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon in 1703 ms
LAI-qualify: LOAD_OK model=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon — sending qualification prompt
LAI-llm   : Generation completed: 256 tokens, decode=5.359797332663359 tok/s, prefill=33.35579514824798 tok/s, ttft=2984 ms, total=50736 ms
LAI-qualify: DONE model=qwen2.5-1.5b-instruct-q4-0 backend=llama-hexagon finalOperation=READY metrics=GenerationMetrics(promptTokens=99, generatedTokens=256, promptEvaluationMs=2968, timeToFirstTokenMs=2984, decodeMs=47763, totalMs=50736, evaluatedPromptTokens=99)
```

**Real DSP hardware engagement, not just a session handshake** — pulled the full (unfiltered,
pid-scoped) logcat, not just the `LAI-*` tag filter the script normally shows, and found genuine
Qualcomm vendor FastRPC/adsprpc driver traces (`vendor/qcom/proprietary/adsprpc/...`, unfakeable
from app code) confirming the skel this session's packaging fix put into the APK was actually
loaded onto the DSP:
```
apps_std_fopen_fd done for .../lib/arm64/./libggml-htp-v73.so ... fd 0x96 error_code 0x0
Successfully opened file .../lib/arm64/./libggml-htp-v73.so
remote_handle64_open: opened handle 0xb4000076740e5e50 ... file:///libggml-htp-v73.so?htp_iface_skel_handle_invoke ... domain 3
ggml-hex: Hexagon Arch version v73
ggml-hex: HTP0 allocating new session
ggml-hex: HTP0 new session : session-id 0 domain-id 3 ...
ggml-hex: HTP0 hwinfo: threads 4, hvx 4, hmx 1, vtcm 8 MB
ggml-hex: HTP0 op batching: n-bufs 16 n-tensors 7168 n-ops 1024 vmem 3145728000
```
`HTP0 hwinfo` (thread/HVX/HMX/VTCM counts) is queried live from the DSP over FastRPC — not
something that can be faked without a genuine hardware round-trip. `HTP0 op batching` shows the
backend configuring itself for real compute-graph op dispatch (1024 ops), not a no-op handshake.

Non-fatal errors observed during DSP session setup (did not block the overall PASS, noted for the
record): `Error 0x80000414: remote_handle64_invoke failed ... libdspqueue_rpc_skel.so method 3`
and `Error 0xffffffff: fastrpc_enable_kernel_optimizations failed` — both appear to be optional
diagnostics/optimization paths, not required for the model to load and generate successfully.

## What this does NOT establish

- **Throughput comparison is inconclusive, not a red flag.** Decode (5.36 tok/s) sits inside the
  already-recorded CPU range (2.5–15 tok/s decode); prefill (33.36 tok/s) is above the recorded
  CPU range (10–20 tok/s). Given the strong hardware-engagement evidence above, the most likely
  read is a genuinely different (HTP-engaged) execution path with per-token FastRPC dispatch
  overhead affecting decode more than batched prefill — but this has not been confirmed with a
  per-op backend-assignment trace, and should not be oversold as a clean performance win.
- **No formal op-by-op backend-assignment trace was captured.** The evidence above is strong
  (real DSP session, real skel load, real hwinfo query, real op-batching config) but is not the
  same as instrumenting ggml's scheduler to confirm every matmul actually executed on HTP rather
  than falling back to CPU per-node within the same session.
- This is a single run, one model, one quantization (`qwen2.5-1.5b-instruct-q4-0`, q4_0). Not yet
  repeated, not yet tried against `qwen2.5-1.5b-instruct-q4-k-m`, not yet a stability/thermal/
  battery pass.

## Next steps (not yet done)

- Repeat the run to confirm reproducibility (this was a single pass).
- Try the second catalog model (`qwen2.5-1.5b-instruct-q4-k-m`) on `llama-hexagon`.
- If precise NPU-vs-CPU compute attribution matters for a "device validated" claim in
  `PROJECT_STATE.md`'s stricter sense, add instrumentation to log ggml's per-op backend
  assignment rather than inferring it from vendor driver traces alone.
- Decide whether `llama-hexagon` should move from empty-by-default to catalog-preferred/fallback
  in `core/model` now that it has one real passing run — that's a product decision, not made here.
