# Session Handoff — GPU/NPU Acceleration Sprint (2026-08-20)

**Read this first.** Everything below was established with device evidence on the
Redmi Turbo 4 Pro (`25053RT47C`, `onyx`, SM8735 / Snapdragon 8s Gen 4, Adreno 825,
HyperOS 3.0.303.0, Android 16) on 2026-08-20. Full evidence lives in
`docs/device-results/2026-08-20-redmi-turbo-4-pro-gpu-npu-access-audit.md` and
`docs/device-results/2026-08-20-redmi-turbo-4-pro-opencl-device-facts.md`.

## Established verdicts (evidence-backed, do not re-litigate)

1. **Vulkan GPU — open door, Qualcomm driver bug, fix lever ready.**
   SIGSEGV at `vkCmdBindPipeline+0x4` in `vulkan.adreno.so` binding the MUL_MAT
   pipeline (release-183 addr2line). All compile-time mitigations exhausted
   (coopmat/2, MMVQ+skip-patch, f16, IDP, async, fusion, FA-off, graphics queue).
   Upstream already disables large tiles on Qualcomm → crash is in the medium/small
   tile path. **MLC Chat runs fast on this phone via TVM-Vulkan → the Adreno 825 is
   proven capable; the bug is ggml-kernel-specific.** Ready lever: upstream PR
   ggml-org/llama.cpp#25735 (clamp warptile WM≤BM for subgroupSize>64) —
   **dry-run-applies cleanly to LAI's pin ad1de39**; harmless no-op if subgroup≤64.
2. **OpenCL — locked by HyperOS for modern apps; backend dormant (by design).**
   Stack is healthy (OpenCL-Z: OpenCL 3.0 Adreno 825 FULL_PROFILE) but
   `libOpenCL.so` is published to NO app namespace (app-side linker trace + empty
   `ld.config.txt` grep; legacy 2015 apps bypass the config). LAI's `llama-opencl`
   backend ships dormant with vendor-dir synthesis; self-activates if a HyperOS OTA
   publishes the library. Do not spend more time here until an OTA.
3. **NPU door — OPEN (user-proven).** Local Dream generates Stable Diffusion images
   in seconds on this phone = NPU-class speed (CPU mode takes minutes) → fastRPC
   (`libcdsprpc`→Hexagon) works for real Play apps on this HyperOS build. Shell-side
   greps that read empty are NOT authoritative (wrong namespace view). No NNAPI HAL,
   no AICore service (both checked, empty).
4. **ggml-hexagon is ALREADY inside LAI's pinned llama.cpp ad1de39**
   (`ggml/src/ggml-hexagon`, dlopens `libcdsprpc.so`, needs Hexagon SDK at build time
   for DSP skels, optional HTP cert signing). Upstream scope: Hexagon v73/v75/v79/v81,
   Q4_0/Q8_0/MXFP4/FP32, core LLM ops. **SM8735's dsp_arch is NOT publicly listed**
   (between v75 and v79) — first scoping step is identifying it (QNN SDK chipset
   table or experiment). Local Dream's V68+ support list implies this chip's HTP is
   QNN-usable.
5. **CPU — 2–3× headroom found.** ChatterUI (newer llama.cpp, KleidiAI on) decoded
   the same Qwen Q4_K_M at ~28 tok/s on this device vs LAI's validated 8–15 tok/s.
   LAI's CMake forces `GGML_CPU_KLEIDIAI OFF`. The pinned llama.cpp supports KleidiAI
   natively: pinned archive v1.24.0, MD5 `2f02ebe29573d45813e671eb304f2a00`
   (policy-compatible). **One-line flip + one build.**

## The approved plan (status: AWAITING USER "GO")

**Build 1 — "CPU boost + Vulkan qualification" (single artifact):**
1. `runtime/llama/src/main/cpp/CMakeLists.txt`: `set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)`
   (verify the FetchContent download of the pinned archive works on the CI runner).
2. Land the warptile clamp as LAI patch `scripts/ci/ggml-vulkan-clamp-warptile.patch`
   (adapt PR #25735 diff; verified to apply to the pin with offset) and wire into
   `scripts/ci/fetch_llama_cpp.sh` alongside `ggml-vulkan-skip-mmvq.patch`.
3. Dispatch workflow with `validated_accelerators=llama-vulkan` (release).
4. User installs → loads Qwen → sends message. Expected logcat evidence lines:
   `ggml_vulkan:` init line with the device's subgroup size (capture it!),
   `device: pinned offload to 'Vulkan0'`, `offloaded 28/29 layers to GPU`, tok/s.
5. Outcomes: works → record in `docs/device-results/`, mark llama-vulkan
   DEVICE_VALIDATED, consider catalog preference change. Crashes → addr2line with the
   unstripped artifact; Vulkan parks until driver OTA; NPU track becomes priority.

**Track 2 — NPU scoping (after Build 1 settles):**
1. Identify SM8735 Hexagon dsp_arch (QNN/QAIRT SDK chipset table; Local Dream's
   working NPU on this phone is proof-of-viability).
2. Plan Hexagon SDK intake in CI (pinned download, license click-through, secrets),
   skel build + signing, `GGML_HEXAGON=ON` behind a new LAI backend `llama-hexagon`
   registered like the others (backend_registry + NativeInferenceEngine descriptor +
   catalog compat + DEVICE_VALIDATED gate). Model format: Q4_0/Q8_0 GGUF supported.
3. Reference implementations to study: alibaba/MNN `MnnLlmChat` QnnModule.kt
   (SOC-gate + on-demand lib download + fallback), xororz/local-dream,
   avisre/snapdragon-npu-llm (ExecuTorch QNN .pte, proved v69 works).

## Open questions for the user (ask when they return)

- [ ] **ChatterUI with GPU Layers=99**: works or crashes? (newer llama.cpp = tells us
      whether upstream fixed the Adreno bug → maybe a pin bump beats the patch)
- [ ] Local Dream backend indicator: NPU or GPU? (makes verdict #3 certain)
- [ ] MLC Chat: which model + tok/s observed? (GPU reference number)

## Operational notes for the assistant (hard-won, do not rediscover)

- **Workspace snapshots rewind `.git` refs and strip credentials between turns.**
  ALWAYS: `git fetch "https://soobujmiah:<TOKEN>@github.com/soobujmiah/lai.git" main`
  → `git reset --hard FETCH_HEAD` (or rebase) before committing/pushing; push via the
  token URL; docs-only pushes auto-trigger CI — cancel the run afterwards (the user
  controls when builds happen).
- GitHub-centric repo: ALL compilation happens in GitHub Actions; this VM plans/writes
  code/pushes; the user installs artifacts on the Redmi Turbo 4 Pro and reports back.
  No local Android builds.
- The user's "adb" is Shizuku `rish` (uid 2000): `/` is read-only there (no
  redirections to `/`); logcat/grep pipelines are fine.
- Device facts (never re-collect): see the two 2026-08-20 device-results files.
- Repo policy: source-only (no binaries), immutable pinned SHAs, evidence vocabulary
  (never claim acceleration without DEVICE_VALIDATED device evidence), docs-with-code
  (`scripts/validate_repo.sh` before every push), cancel auto-CI on docs commits.
- Token rotation still recommended (PAT was pasted in chat early in the project).

---

## SESSION CLOSE — 2026-08-20 ~19:45 (Asia/Dhaka)

### Current state (at close)

- **Builds:** none running, none queued; build freeze active (user controls dispatch).
  Latest green qualification artifact: `lai-release-192` (OpenCL-era; superseded by the
  upcoming Build 1). Catalog: rev 5 signed & published. Main @ `6e6eb00` (docs).
- **Code shipped today (all in main):** Adreno OpenCL track (`opencl_backend.cpp`,
  vendor-dir synthesis, catalog rev 5, scheduler gate + tests), Vulkan device pinning,
  full diagnostics chain (dlopen probe, per-device probe logs), restored CI workflow.
- **Device-verified today:** OpenCL stack healthy but HyperOS-walled (dormant backend);
  NPU fastRPC door OPEN (Local Dream NPU-class generation); Adreno 825 GPU capable
  (MLC TVM-Vulkan fast); CPU 2–3× headroom identified (KleidiAI off in LAI builds).
- **Knowledge persisted:** this file + `docs/device-results/2026-08-20-*` (LAI) and
  `sobuj-knowledge-base` → `devices/redmi-turbo-4-pro/` (README, problems-and-fixes
  ledger, access audit, evidence/, ASSISTANT_CONTEXT mission state).

### Next steps (ordered)

1. **User says GO** → Build 1: `GGML_CPU_KLEIDIAI ON` (one line) +
   `scripts/ci/ggml-vulkan-clamp-warptile.patch` (adapt upstream PR #25735; verified
   applicable) wired into `fetch_llama_cpp.sh` → dispatch with
   `validated_accelerators=llama-vulkan` → user installs/tests → record evidence.
2. **Collect user's pending observations:** ChatterUI GPU Layers=99 outcome (may
   replace the patch with a llama.cpp pin bump if newer upstream already fixed the
   Adreno bug), Local Dream backend indicator (NPU vs GPU), MLC model/tok/s.
3. **NPU scoping:** identify SM8735 dsp_arch (QNN chipset table) → plan Hexagon SDK
   intake in CI + `ggml-hexagon` enablement as `llama-hexagon` backend (contract:
   evidence-gated like all others).
4. **Standing watch:** on any HyperOS OTA → re-run the three greps
   (opencl / cdsprpc / neural-HAL); dormant OpenCL backend self-activates if published.

Session closed. Everything needed to resume is in this file and the KB.
