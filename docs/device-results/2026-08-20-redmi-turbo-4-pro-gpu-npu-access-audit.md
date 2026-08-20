# Redmi Turbo 4 Pro — accelerator access audit & strategy handoff (2026-08-20)

**Audience: any future assistant resuming GPU/NPU work on this device.** This file is the
single source of truth for WHY each acceleration path is open/closed and WHAT to do next.
Companion evidence: [`2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md`](2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md).

## Device

Xiaomi 25053RT47C (Redmi Turbo 4 Pro), codename `onyx`, Android 16 (build
`BP2A.250605.031.A3`), kernel `6.6.77-android15-8`, HyperOS, QTI SM8735 (Snapdragon 8s
Gen 4), Adreno 825 GPU, Hexagon NPU, app targets SDK 36.

## The three doors to the GPU (why games "use GPU" but LAI can't use OpenCL)

| Door | Access on this device | Used by |
|---|---|---|
| Vulkan / OpenGL ES **graphics** (NDK `libvulkan.so`, `libGLESv*.so`) | **OPEN to every app** — Android's standard rendering APIs | Every game (PUBG etc.); LAI's Vulkan compute backend ALSO enters here |
| OpenCL (raw GPU compute) | **LOCKED for modern apps** — HyperOS publishes `libOpenCL.so` to no app namespace; only legacy targetSdk apps bypass the config | OpenCL-Z (2015 app) works; LAI refused by linker |
| Sanctioned AI channels (NNAPI HAL, bundled vendor runtimes like QNN, AICore/ML Kit) | Varies — crosses binder into privileged vendor processes, bypassing the app linker wall | Camera AI, system GenAI features |

Key insight: **LAI's Vulkan backend was never blocked by Xiaomi** — Vulkan is an open
door (proven: our build loaded, offloaded, and crashed INSIDE the Qualcomm driver, which
is a driver bug, not an access denial). OpenCL was the only locked door.

## Path status (evidence-backed)

### 1. Vulkan compute — BEST active lever (driver bug, fixable candidates exist)

- Crash: SIGSEGV at `vkCmdBindPipeline+0x4` in `vulkan.adreno.so` binding the MUL_MAT
  pipeline (release-183 addr2line). All compile-time mitigations exhausted
  (coopmat/2, MMVQ+compile-skip patch, f16, integer-dot, async, fusion, FA off,
  graphics-queue forcing). Upstream already disables large tiles on Qualcomm
  (`mul_mat_l=false` for `VK_VENDOR_ID_QUALCOMM`), so the crash is in the medium/small
  tile path.
- **NEW lever identified 2026-08-20 (not yet landed):** upstream issue
  ggml-org/llama.cpp#25734 + PR #25735 ("clamp l_/m_ warptile WM to <= BM, fix wrong
  matmul on subgroupSize > 64") — still OPEN upstream, but the diff **applies cleanly to
  LAI's pinned commit ad1de39 (dry-run verified, offset-only)**. If Adreno 825 reports
  subgroupSize=128, the medium warptile is degenerate (WM=128 > BM=64 → div-by-zero,
  shared-mem overrun in `mul_mm.comp`) — a plausible root cause for the bind/dispatch
  crash. If subgroupSize<=64 the clamp is a harmless no-op.
  - Action: land as LAI patch `scripts/ci/ggml-vulkan-clamp-warptile.patch` (same
    mechanism as `ggml-vulkan-skip-mmvq.patch`), wire into `fetch_llama_cpp.sh`, then ONE
    qualification build `validated_accelerators=llama-vulkan`.
  - Evidence to capture in that test: `logcat -s LAI-llama` init line
    `ggml_vulkan: 0 = Adreno (TM) 825 … subgroup size …` (records the device's real
    subgroup size either way) + crash-or-success outcome.
- Passive lever: Qualcomm driver updates (retest on every HyperOS OTA).

### 2. OpenCL — CLOSED (device-policy wall), backend dormant

- Full chain in the companion facts file. Summary: OpenCL 3.0 Adreno 825 stack is healthy
  (OpenCL-Z), but `cat /linkerconfig/ld.config.txt | grep -i opencl` is EMPTY and the
  linker trace refuses LAI's dlopen (`clns-10` permitted_paths = /data only).
- LAI's `llama-opencl` backend remains compiled with startup vendor-directory synthesis;
  it self-activates with zero code change if a future HyperOS build publishes the library.

### 3. NPU / QNN (Hexagon HTP) — USER'S MAIN TARGET; door currently appears locked, one check left

- 2026-08-20 probe: `cat /linkerconfig/ld.config.txt | grep -iE
  "cdsprpc|adsprpc|sdsprpc|snpe|qnn"` → **EMPTY**. The fastRPC bridges (`libcdsprpc.so`
  etc.) are listed in `/vendor/etc/public.libraries.txt` but, exactly like OpenCL, are
  dead letters in the effective linker config. A bundled QNN runtime's HTP stub must reach
  the vendor fastRPC library to talk to the Hexagon — so QNN-HTP likely faces the same
  wall on this build. **Do NOT spend build cycles on QNN until the check below passes.**
- Remaining check (run in rish, decides everything):
  ```sh
  ls /vendor/bin/hw | grep -iE "neural|aiengine|qnn"
  service list | grep -iE "neural|aicore"
  ```
  - If a `neuralnetworks` HAL / service exists → a sanctioned side door exists (NNAPI
    crosses binder into a privileged vendor process and ignores the app linker wall).
    Note: upstream llama.cpp has no NNAPI backend (deprecated upstream); an NPU adapter
    would be new LAI engineering (QNN-via-NNAPI or a custom isolated adapter), still
    gated by the repo's evidence rules.
  - If empty too → NPU is gated to system apps on this HyperOS build; NPU waits for an
    OTA, and Vulkan (§1) is the only acceleration lever LAI controls today.
- Context for QNN when/if the door opens: QAIRT/QNN SDK (free download, license
  click-through) is distributed for bundling INSIDE the APK; models need conversion +
  quantization to an SM8735 context binary (GGUF cannot run on HTP); ExecuTorch's
  Qualcomm backend runs Llama-class models on HTP and is the reference flow; roadmap
  already plans licensed-QAIRT CI + isolated adapter + device evidence.

### 4. OpenGL ES compute — NOT a path

llama.cpp has no OpenGL ES backend; GLES 3.1 compute shaders exist but writing an LLM
backend on them is months of work for a worse result than the already-integrated Vulkan.
Not pursued.

## Decision table (what to do when)

| Trigger | Action |
|---|---|
| Now (no user GO yet) | Nothing builds. CPU is the shipped, device-validated default and is enough for daily use |
| User says GO on Vulkan retry | Land warptile-clamp LAI patch (§1) → one `validated_accelerators=llama-vulkan` build → device test captures subgroup size + outcome |
| Crash persists after clamp | Vulkan path parks until driver OTA; keep the evidence in this file |
| NNAPI check (§3) shows a HAL/service | Scope the NPU adapter (roadmap QNN phase), starting with the QAIRT intake plan |
| HyperOS OTA installed | Rerun the three greps from the OpenCL facts file + §3 check; dormant OpenCL backend self-activates if published |

## Facts future sessions must not re-collect (from the user's device)

- `/vendor/lib64/libOpenCL.so` exists (95,800 bytes) and is in
  `/vendor/etc/public.libraries.txt` — but published to NO app namespace (dead letter).
- Same dead-letter status for `libcdsprpc.so`/`libadsprpc.so`/`libsdsprpc.so`/`libSNPE.so`
  (listed in public.libraries.txt, absent from linkerconfig).
- OpenCL-Z sees: platform `QUALCOMM Snapdragon(TM)`, OpenCL 3.0 `build: 0800.33`, Adreno
  825, FULL_PROFILE, 8 compute units, unified memory, subgroups + `cl_qcom_dot_product8`
  + bf16 extensions; loaded the **32-bit** `/system/vendor/lib/libOpenCL.so` via legacy
  namespace (OpenCL-Z targets ~SDK 22).
- LAI linker trace: `clns-10` namespace, `permitted_paths="/data:/mnt/expand:/data/user/0/dev.lai.runtime"`,
  refuses both `/vendor/lib64/libOpenCL.so` and `/system/vendor/lib64/libOpenCL.so`.
- rish = Shizuku interactive shell (uid 2000) is the user's adb substitute; `/` is
  read-only there — never redirect files to `/`; use `/data/local/tmp` or no redirection.

## Addendum — NPU ecosystem map and the ggml-hexagon route (2026-08-20)

- Every shipping NPU solution converges on one door: `libcdsprpc.so` fastRPC → Hexagon.
  MLC Chat (TVM→Hexagon, 8 Elite-only), MNN Chat (downloaded QNN libs → QNN HTP), Local
  Dream (bundled QNN), avisre/snapdragon-npu-llm (ExecuTorch QNN .pte, proved working on
  v69/8 Gen 1 at 31 tok/s for Qwen3-0.6B).
- **llama.cpp already ships an experimental `ggml-hexagon` backend, present in LAI's
  pinned commit ad1de39** (`ggml/src/ggml-hexagon`, dlopens `libcdsprpc.so`, requires
  Hexagon SDK at build time for the DSP skels + optional HTP cert signing). Upstream
  support: Hexagon v73/v75/v79/v81 (8 Gen 3 / 8 Elite / 8 Elite Gen 5); Q4_0/Q8_0/MXFP4/
  FP32; core LLM ops (MUL_MAT/MUL_MAT_ID, RMS_NORM, ROPE, SWIGLU, SOFTMAX...).
- SM8735 (8s Gen 4): Hexagon ~60 TOPS INT4/INT8, advertised LLM support, but the
  dsp_arch version is not publicly listed (between v75 and v79). Determine via QNN SDK
  chipset table or experiment before scoping ggml-hexagon for LAI.
- MLC Chat on this phone ran TVM-Vulkan/CPU, NOT NPU (Hexagon path is 8 Elite-only).
  If TVM-Vulkan performed well here, it is evidence the Adreno 825 executes LLM compute
  under a different Vulkan kernel stack — i.e. LAI's crash is ggml-kernel-specific.
- Decisive door test (unchanged): Local Dream or MNN Chat on this Redmi — does NPU engage
  on HyperOS? YES → scope ggml-hexagon integration (Hexagon SDK pinned in CI, skel build,
  backend registered like the others, evidence-gated). NO → NPU waits for an OTA.

## BREAKTHROUGH — decisive device observations (user, 2026-08-20 evening)

Two independent observations overturn the pessimistic reading of the shell-side greps:

1. **MLC Chat runs "so fast" on the Redmi Turbo 4 Pro.** MLC's Hexagon path is
   8 Elite-only, so this was MLC's **TVM-Vulkan** (or CPU) path. "So fast" on this
   device means the **Adreno 825 executes LLM compute successfully under TVM's Vulkan
   kernel stack** — the GPU is NOT incapable; LAI's SIGSEGV is specific to ggml's
   Vulkan kernels/driver interaction. This strongly motivates the ggml warptile-clamp
   patch experiment (§1): the hardware is proven innocent.
2. **Local Dream generates Stable Diffusion images "in seconds" on the same phone.**
   Local Dream's fast path is **QNN on the Hexagon NPU** (SD1.5 supported on Hexagon
   V68+); CPU mode takes minutes. Seconds-per-image is NPU-class performance →
   **the fastRPC door (libcdsprpc → Hexagon) is OPEN to real Play apps on this HyperOS
   build**, despite `ld.config.txt` greps from a shell reading empty. The shell-side
   linkerconfig view is NOT the app-namespace view — treat those greps as inconclusive,
   the device observations as authoritative.

Status changes:

- **NPU door test: PROBABLY PASSED** — confirm which backend Local Dream's UI shows
  (NPU vs GPU vs CPU) to make it certain.
- SM8735 Hexagon is evidently usable by QNN (V68+ scope includes this chip's NPU per
  Local Dream's support list) — determine the exact dsp_arch (QNN SDK chipset table) as
  the first scoping step for ggml-hexagon.
- Updated priority: (1) Vulkan warptile-clamp qualification build — GPU now proven
  viable by MLC; (2) scope ggml-hexagon integration (backend already present in LAI's
  pinned llama.cpp ad1de39) now that the door is demonstrably open; (3) OpenCL remains
  a separate locked wall (linker trace is app-side evidence), dormant.
