# LAI UI & AI Gateway Migration Status

**Snapshot:** 2026-08-21  
**Branch:** `phase1/ui-material3-provider-foundation`  
**Purpose:** Handoff document for the next implementation session.

## Current state

The migration is intentionally **additive**. Existing runtime, scheduler, `MainViewModel`, accessibility, Shizuku, workspace, history, model-management, diagnostics, and native inference boundaries remain authoritative.

### Completed in this branch

- Material 3 theme foundation.
- Light/dark/system theme support.
- Android 12+ dynamic Material 3 color support with static light/dark fallback.
- Centralized motion tokens (fast/standard/emphasized).
- Initial spacing/design-token layer.
- Presentation-only adaptive app-shell foundation.
- Chat presentation surface.
- Model catalog presentation surface.
- Workspace presentation surface.
- Multi-provider AI contract foundation in `core/contracts`.
- Provider kind/evidence classification and explicit network policy.
- AI gateway selection is evidence-gated; unqualified providers are not treated as ready.
- Structure snapshot documenting the preservation rules.

## Preservation rules

1. Do not replace `LaiApp.kt` wholesale.
2. Do not rewrite `MainViewModel` merely for UI styling.
3. Do not move or delete existing runtime/platform modules to introduce the UI or provider layer.
4. Native controls such as decode-thread limits, crash handling, and accelerator-specific configuration stay behind the native/runtime boundary.
5. UI must never directly own inference, shell, Accessibility, Shizuku, filesystem, or network authority.
6. Provider existence is not provider qualification. GPU/NPU/cloud readiness must remain evidence-driven.
7. `LOCAL_ONLY` must never silently fall back to a remote provider.
8. Every structural/architectural change must update documentation.

## Existing UI integration status

The existing `LaiApp` already provides important production behavior: chat streaming/cancellation, history, quick settings, Settings, Screen Reader, Automator, tool approval, model management, workspace actions, diagnostics, and keyboard/IME handling. The redesign therefore proceeds by component migration and visual refinement rather than a wholesale replacement.

## Next implementation sequence

1. Extract/reuse shared Material 3 components and tokens.
2. Incrementally polish the existing `ChatScreen` while preserving streaming and cancellation behavior.
3. Migrate history and quick-settings surfaces to the new visual language.
4. Integrate the existing model-management state into `ModelsScreen`.
5. Integrate workspace state into `WorkspaceScreen`.
6. Add the provider configuration/status surface and connect it to the AI Gateway contracts.
7. Refine Screen Reader, Automator, tool approval, and diagnostics surfaces.
8. Add reduced-motion handling and responsive phone/tablet/large-screen layouts.
9. Run Gradle compile, unit tests, lint/static checks, architecture/documentation validators, and CI.
10. Fix all build/test issues before opening/merging the final PR.

## Validation status

**Not yet claimed:** full build/CI verification of the complete UI migration.  
The current branch contains foundation work and must be validated after integration changes are complete.

## Handoff rule

Resume from this branch and this document. Do not restart the UI from scratch and do not discard the existing LAI functional UI/runtime architecture.
