# NPU/Hexagon Scoping (2026-09-03)

**Read this first.** This picks up where `docs/HANDOFF-2026-08-20-acceleration-sprint.md`
(verdict #3/#4) and `docs/HANDOFF-2026-09-02-build1-kleidiai-vulkan-clamp.md` left off. Both
GPU paths on this device are now closed for the moment: Vulkan crashes on the first decode
step even with the warptile clamp (`docs/device-results/2026-09-03-redmi-turbo-4-pro-vulkan-warptile-clamp-crash.md`),
and OpenCL is OS-walled by HyperOS with no code fix available (`libOpenCL.so` published to no
app namespace; re-checked today — `ro.build.display.id` is still `BP2A.250605.031.A3`, same
build as 2026-08-20, so no OTA has reopened it). **NPU is the correct next acceleration
priority**, not a fallback taken by default — the alternative GPU paths were checked first and
are genuinely exhausted, not skipped.

## 1. Device evidence: Hexagon HTP architecture is v73

The 2026-08-20 doc flagged SM8735's `dsp_arch` as "not publicly listed (between v75 and v79)".
Resolved today with direct on-device evidence — no guessing required:

```text
$ adb shell ls /vendor/lib/rfsa/adsp/
libQnnHtpV73.so
libQnnHtpV73QemuDriver.so
libQnnHtpV73Skel.so
libSnpeHtpV73Skel.so
...
$ adb shell getprop ro.soc.model
SM8735
```

The vendor partition ships V73-suffixed QNN/SNPE HTP skel libraries only — no V75/V79/V81
variants are present. **SM8735's Hexagon HTP is v73.** `libcdsprpc.so`/`libadsprpc.so` (fastRPC)
are present at `/vendor/lib64/`, consistent with the 08-20 finding that fastRPC works on this
HyperOS build (Local Dream's NPU-class Stable Diffusion generation already proved this
functionally).

## 2. Two possible integration paths — and why the cheaper one now exists

`docs/adr/0005-snapdragon-first-vendor-neutral-backends.md` (2026-08-16) and
`docs/ROADMAP.md`'s "Legacy backlog: Snapdragon specialization" both describe **one** path:
a full Qualcomm QAIRT/QNN SDK integration — model conversion to a QNN context binary/DLC,
HTP context manifest/cache, and a **dedicated `runtime:qnn` adapter living outside
`runtime:llama`**, publishing `qualcomm-qnn-htp`. That path still exists and is not wrong, but
it is not the only one anymore, and it is not the cheapest one available today.

**New finding this session:** LAI's already-pinned llama.cpp commit
(`ad1de39e0708e3ced9c71bb3c82d93a2c046a73f`, same pin used for CPU/Vulkan/OpenCL today) already
vendors upstream's own **`ggml-hexagon`** backend
(`ggml/src/ggml-hexagon/`, confirmed via the actual file at that commit — not assumed). This is
a native llama.cpp backend, in the same family as `ggml-vulkan` and `ggml-opencl` that LAI
already ships:

- It runs **GGUF directly** — no QNN context-binary conversion, no DLC, no calibration step.
- It builds against the **Hexagon SDK** (not the full QAIRT/QNN SDK) using the SDK's own
  `hexagon_fun.cmake` + toolchain, compiling four HTP "skel" shared libraries
  (`libggml-htp-v73.so`, `-v75`, `-v79`, `-v81`) **unconditionally** — the CMake
  (`ggml-hexagon/CMakeLists.txt`, fetched and read at the pinned commit) does not need a
  per-chipset build flag; all four ship and the runtime picks the matching one
  (confirmed against upstream's own `docs/backend/snapdragon/README.md` at the same commit,
  which shows the backend self-reporting e.g. `ggml-hex: Hexagon Arch version v79` on other
  devices and loading `libggml-htp-v79.so` accordingly — our device would self-report v73 and
  load `libggml-htp-v73.so`).
- On Android there is **no code-signing requirement** for the HTP skels — the CMakeLists' only
  signing branch is gated `if (CMAKE_SYSTEM_NAME MATCHES Windows AND GGML_HEXAGON_HTP_CERT)`.
  The skels install as ordinary `.so` files (`install(FILES ${HTP_SKELS} TYPE LIB)`), same as
  any other native library in the APK.
- Upstream's own reference numbers (other Snapdragon devices, v79/v81 chips, not ours — quoted
  as directional motivation only, not a claim about SM8735): a 1.24B Q4_0 model decoded at
  ~51.5 tok/s and prompt-processed at ~136 tok/s on a v79 HTP session. That is well above both
  LAI's current CPU baseline (8–15 tok/s) and the still-unshipped KleidiAI CPU number from
  Build 1 (~28 tok/s on this exact device). It is *not* evidence for v73 or for SM8735 — it is
  the reason this path is worth the qualification effort, nothing more, per this project's own
  evidence vocabulary.

**Recommendation:** qualify `ggml-hexagon` as a fourth `runtime:llama` backend
(`llama-hexagon`, matching the existing `llama-cpu`/`llama-vulkan`/`llama-opencl` naming) before
attempting the heavier QAIRT/QNN adapter path from ADR 0005. Checked against the actual
enforced boundary rule (not just its prose): `scripts/check_architecture_boundaries.py`'s
`VENDOR_BACKEND_MARKER` regex (`qualcomm|snapdragon|hexagon|qnn|qairt|...`) only fires inside
`core/contracts/.../inference/` and `core/scheduler/src/main/` — it does **not** cover
`runtime/llama/` or `core/model` (the catalog), so a `llama-hexagon` backend ID and its adapter
code are compliant with the actual CI gate, the same way `vulkan_backend.cpp`/
`opencl_backend.cpp` already are. ADR 0005's "dedicated `runtime:qnn` adapter outside
`runtime:llama`" language should be treated as describing the QAIRT/QNN path specifically, not
as a blanket ban on any Hexagon-capable code inside `runtime:llama` — `ggml-hexagon` didn't
exist in upstream llama.cpp when that ADR was written on 2026-08-16, so it couldn't have been
considered. **This doc does not silently rewrite ADR 0005** — that requires a proper
superseding ADR entry if/when `llama-hexagon` is actually implemented; this is a scoping
finding, not a decision record.

The full QAIRT/QNN adapter path remains the right fallback if `ggml-hexagon` turns out to be
insufficient (e.g. missing an op LAI's models need, or worse performance than CPU on v73
specifically) — it is not being deleted from the roadmap, only deprioritized behind the cheaper
option.

## 3. What actually blocks CI automation (needs a human decision, not more scoping)

`ggml-hexagon`'s CMake requires `HEXAGON_SDK_ROOT` pointing at an installed Hexagon SDK, and
`HEXAGON_TOOLS_ROOT` (the DSP cross-compiler toolchain) — these are not fetchable by an
immutable-URL `curl` the way Vulkan-Headers/SPIRV-Headers are. Upstream's own documented build
path (`docs/backend/snapdragon/README.md`) uses a public Docker image,
`ghcr.io/snapdragon-toolchain/arm64-android:v0.7`, which bundles Android NDK + Hexagon SDK +
OpenCL SDK + CMake. Two open items before any CI wiring can start, both requiring Sobuj's
input, not further research:

1. ~~Confirm the Docker image is actually pullable anonymously from a GitHub Actions
   runner~~ — **checked directly, not assumed.** Requested an anonymous GHCR token
   (`ghcr.io/token?scope=repository:snapdragon-toolchain/arm64-android:pull`) and fetched the
   `v0.7` manifest with it: `HTTP 200`, valid OCI image index, no login/EULA gate on the pull
   itself. **This item is resolved — no auth blocker exists for the Docker path.**
2. **Runner cost/time budget (still open, needs Sobuj)**: measured the actual `linux/amd64`
   manifest (the platform GitHub-hosted runners use) — **11 layers, ~2.42 GB compressed.** That
   is a real per-run cost if pulled fresh every time. Needs a decision: accept it per-build,
   add a Docker layer cache step (GitHub Actions supports this), or restrict Hexagon builds to
   explicit `workflow_dispatch` only (matching the existing `validated_accelerators` opt-in
   pattern already used for Vulkan) so it's never pulled on ordinary pushes.

## 4. Proposed next steps (ordered)

1. **Sobuj decision**: the Docker-image path is now confirmed viable (item 3.1, resolved) and
   is the recommended default over a manual SDK secret upload — the open call is narrower now:
   accept the ~2.42 GB pull cost per Hexagon-flagged run, add caching, or gate it to explicit
   `workflow_dispatch` (item 3.2).
2. Add `HEXAGON_SDK_ROOT`/`HEXAGON_TOOLS_ROOT` wiring to
   `runtime/llama/src/main/cpp/CMakeLists.txt` and a new
   `scripts/ci/fetch_hexagon_sdk.sh` (or Docker-based CI job step) parallel to the existing
   `fetch_llama_cpp.sh`/Vulkan-Headers pattern.
3. Register `llama-hexagon` end to end, same shape as the other three backends: `backend_registry.cpp`
   entry, `NativeInferenceEngine` descriptor, `core/model` catalog compatibility entry
   (`compatibleBackendIds`), `InferenceScheduler` evidence gate (`DEVICE_VALIDATED` only, same
   as Vulkan/OpenCL today — no acceleration claim before device proof).
4. Dispatch a qualification-only build (`-Plai.validatedAccelerators=llama-hexagon`, opt-in,
   not default) — mirrors exactly how Vulkan was qualified in Build 1.
5. Device-test with the same discipline as today's Vulkan run: load model, complete a full
   generation (not just load), record `docs/device-results/<date>-redmi-turbo-4-pro-hexagon-v73.md`
   regardless of outcome. Watch for `ggml-hex: Hexagon Arch version v73` in logcat as the
   correctness signal that the right skel loaded.
6. Only after a completed, crash-free, measured generation: mark `llama-hexagon`
   `DEVICE_VALIDATED`, consider it the new catalog `preferredBackendId`, and update
   `docs/ROADMAP.md`'s Snapdragon-specialization backlog to reflect the path actually taken
   (this doc intentionally does not edit that backlog itself — it proposes, a later PR with
   device evidence decides).

## 5. What this doc does not claim

No code was written or built this session — no local Android/NDK toolchain exists in this
workflow (GitHub Actions is the only build environment; see `docs/HANDOFF-2026-08-20-acceleration-sprint.md`'s
operational notes). No Hexagon acceleration has been measured on this device. v73 is confirmed
architecture identification, not a performance claim. The upstream v79/v81 numbers in §2 are
explicitly a different chip, quoted only to justify spending qualification effort here rather
than elsewhere.
