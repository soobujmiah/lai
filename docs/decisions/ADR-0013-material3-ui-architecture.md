# ADR-0013 — Material 3 UI architecture and redesign

## Status
Proposed

## Decision
Redesign the LAI presentation layer around a Material 3 design system while preserving the existing application, runtime, scheduler, agent, native inference, Shizuku, OCR and workspace module boundaries.

The redesign is additive: UI composition, theme, navigation and reusable components may change; domain/runtime contracts do not.

## Goals

- fast, restrained, professional Android experience;
- Material 3 semantics and accessibility;
- adaptive phone/tablet/large-screen layouts;
- consistent typography, spacing, shape and motion tokens;
- clear separation between presentation state and runtime/provider implementation;
- smooth streaming chat without blocking the UI thread;
- explicit loading, empty, error and unavailable-provider states;
- light/dark theme support and dynamic color where supported.

## Application boundary

```text
Composable UI
   ↓
Screen state / ViewModel
   ↓
Application services
   ↓
InferenceEngine / AI Gateway / existing services
   ↓
Runtime implementations
```

UI code must not call llama.cpp, Vulkan, QNN, OpenCL, Shizuku, shell or filesystem authority directly.

## Navigation model

The primary information architecture is organized around user tasks rather than backend technology:

- Home / workspace
- Chat
- Models
- Providers
- Tools / Agent
- Projects
- Settings

Provider/backend diagnostics may be exposed inside appropriate screens, but CPU/GPU/NPU implementation details must not dominate the primary navigation.

## Performance rules

- Prefer state-driven rendering with stable models.
- Keep expensive inference, file and network operations off the main thread.
- Do not rebuild large UI trees for individual token events unnecessarily.
- Use lazy lists for conversations and model/project collections.
- Keep animation short and purposeful; never animate continuously during inference merely for decoration.
- Avoid image-heavy decorative assets in the core navigation shell.
- Preserve process/runtime lifecycle behavior already owned by AppContainer and services.

## Responsive design

Use adaptive Material 3 layouts rather than separate product implementations. Phone remains the primary target; larger widths may promote navigation to a rail/sidebar and expose multi-pane workspace layouts.

## Theme and design tokens

Create a single theme/token layer for:

- color scheme;
- typography;
- spacing;
- shapes;
- elevation;
- component dimensions;
- motion durations/easing.

Screens must consume tokens rather than scattering literal dimensions/colors throughout composables.

## Accessibility

Interactive controls must expose meaningful semantics, adequate touch targets, readable contrast, content descriptions where needed, and predictable focus/navigation behavior. Accessibility must not depend on visual decoration.

## Migration

1. Inventory current screens and state flows.
2. Introduce theme/design tokens without changing business logic.
3. Introduce reusable navigation/app-shell components.
4. Redesign Home and Chat first.
5. Redesign Models, Providers, Tools/Agent, Projects and Settings.
6. Add adaptive layouts and accessibility QA.
7. Remove obsolete presentation-only code only after equivalent behavior is covered.

## Non-goals

- rewriting the entire application architecture;
- moving native inference into UI modules;
- changing model/runtime behavior solely for visual reasons;
- introducing cloud network access through the UI;
- copying another product's exact visual design.

## Consequences

The UI gains a coherent design system and can evolve independently from CPU/GPU/NPU/cloud provider implementations. The main cost is a staged migration and additional component/state tests.
