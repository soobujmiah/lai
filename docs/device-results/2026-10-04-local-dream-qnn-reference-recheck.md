# Local Dream QNN/HTP reference recheck — 2026-10-04

**Device:** Redmi Turbo 4 Pro (`onyx`, SM8735, Adreno 825)
**Reference app:** `io.github.xororz.localdream`, installed version `2.8.1`
**Control:** I operated Local Dream; scoped logcat and installed APK/package
metadata were inspected without automated app interaction. Raw logs remain local
under `/tmp/localdream-npu-20261004-*.log` and are not committed.

## Verified hardware/runtime chain

Read-only APK inspection found `assets/qnnlibs/libQnnHtp.so`,
`libQnnSystem.so`, and HTP skel/stub pairs for v68/v69/v73/v75/v79/v81.
During the owner-run session, Local Dream listed these runtime files and the
vendor FastRPC loader successfully opened its app-private
`libQnnHtpV73Skel.so` (`error_code 0x0`, domain 3). This is the active v73
device path, consistent with the September evidence.

In the final PID-scoped retry window (04:58:16–04:58:31 local time), the app
created three QNN DSP devices (`status 0x0`) and restored three contexts from
binaries. Its own backend log reported **28 successful `QnnGraph_execute`
calls** (`status 0x0`) and UNET steps 0–13. Step 13 took 292 ms.
I then deliberately stopped generation. The log showed
`STOP_GENERATION` followed by backend process exit code 143. Thus current QNN graph execution and
HTP engagement are PASS, but **complete image output and the claimed 1–2 s
end-to-end time are UNKNOWN for this session**. The older completed 20-step
512×512, 5.7 s Local Dream test remains in
`2026-09-03-redmi-turbo-4-pro-hexagon-real-npu-path-found.md`.

Selected content-free log markers from the PID-scoped window:

```text
QnnDsp <I> QnnDevice_create done. device = 0x3. status 0x0
QnnDsp <I> QnnContext_createFromBinary done successfully. context = 0x3
QnnDsp <I> QnnGraph_execute done. status 0x0
UNET step 13 dur: 292ms
service command: io.github.xororz.localdream.STOP_GENERATION
```

## LAI mapping and boundary

Local Dream's image pipeline uses QNN context binaries and bundled QNN HTP
runtime libraries. LAI's existing `llama-hexagon` pipeline instead takes a
GGUF Qwen Q4_0 model through `ggml-hexagon` and packaged
`libggml-htp-v73.so`; two September LAI runs already reached generation with
FastRPC/HTP0 evidence. Local Dream proves the general QNN route on this device
but does not make its image context binary or proprietary runtime files
interchangeable with LAI's LLM model. No Local Dream library was copied into
the repository.

The open LAI NPU gate is a current-build Q4_0 run with captured reply sanity.
Do not mark that output PASS based on Local Dream's image execution.
