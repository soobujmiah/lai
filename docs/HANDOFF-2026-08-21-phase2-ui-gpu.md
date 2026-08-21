# Handoff — 2026-08-21 Phase 2 UI + Adreno GPU qualification

## Branch / PR

- Branch: `phase2/functional-material3-integration`
- PR: #16 — Phase 2 functional Material 3 state integration
- Last substantive source commit before closeout: `e634849` (`fix(ui): simplify chat actions and progress`)
- GPU evidence commit: `569744d` (`docs(gpu): record failed adreno vulkan qualification`)

## What changed this session

### UI

- Continued from merged PR #15; did not recreate/revert it.
- Kept `MainViewModel` and `MainUiState` authoritative.
- Removed standalone Models, Workspace and Provider tabs from app navigation.
- Kept model management, workspace controls and provider/backend status inside Settings only.
- Made Chat conversation-only:
  - no model/runtime/workspace status in Chat;
  - no duplicate progress indicators;
  - one compact 2 dp progress indicator while generating/cancelling;
  - New and History moved to the top app bar;
  - quick settings remains available from Chat.

### GPU / Redmi Turbo 4 Pro

- Read SKB Redmi Turbo 4 Pro hardware and accelerator records.
- Integrated the only actionable Vulkan lever from SKB:
  - `scripts/ci/ggml-vulkan-adreno-warptile-clamp.patch`
  - wired into `scripts/ci/fetch_llama_cpp.sh`
  - based on upstream llama.cpp PR #25735.
- Refactored Adreno Vulkan environment setup so probe and open share identical workaround flags.
- Triggered and tested the explicit Vulkan qualification APK:
  - app `0.6.217-debug`
  - `validated_accelerators=llama-vulkan`
- Result: **Vulkan still failed on device**.
  - Model load selected `llama-vulkan` and completed.
  - Generation crashed/stalled before first token.
  - Native crash remained in Qualcomm proprietary `vulkan.adreno.so` at `vkCmdBindPipeline+0x4` during ggml graph execution.

## Current truth

- CPU path is the only device-validated reliable LAI path on Redmi Turbo 4 Pro.
- Vulkan is compiled but unqualified/experimental on this device.
- OpenCL stack exists on the device but is blocked for modern apps by HyperOS app namespace policy.
- Do **not** claim GPU support works on Redmi Turbo 4 Pro.
- Do **not** ship `validated_accelerators=llama-vulkan` as default.

## Validation completed

- `bash scripts/validate_repo.sh` passed after each source/doc slice.
- GitHub Actions passed for source/CI validation at several PR heads, including:
  - `0e24011` GPU patch build verification;
  - `e634849` Chat UI cleanup;
  - normal PR CI after UI changes.
- Physical device validation for Vulkan failed with the attached user diagnostics.

## Known follow-up

1. Keep CPU default.
2. Park ggml Vulkan on Adreno 825 until one of:
   - Qualcomm/Xiaomi GPU driver OTA;
   - major upstream ggml-vulkan Adreno fix beyond PR #25735;
   - replacement non-ggml Vulkan runtime path.
3. If more speed is needed soon, prioritize CPU KleidiAI or a QNN/HTP feasibility track rather than more Vulkan env toggles.
4. Keep GPU/backend/model/workspace UI inside Settings only unless product direction changes.
