# LAI UI Design System

## Goals

LAI's UI is a Material 3-based, responsive, performance-first interface with a coherent light/dark visual language. It must feel clean, calm and fast rather than visually busy.

## Principles

1. Preserve existing application/runtime architecture; UI is a presentation layer.
2. Prefer Material 3 components and tokens over one-off widgets.
3. Use stable state-driven rendering; never run inference/native work from composables.
4. Animations communicate state or hierarchy. They are not decoration.
5. Keep motion short and avoid continuous GPU-heavy effects.
6. Respect reduced-motion/accessibility preferences.
7. Design for phones first, then adapt to larger windows with navigation rail/pane layouts.
8. Light and dark themes use the same semantic tokens and hierarchy.

## Motion

- Fast feedback: ~120 ms
- Standard transitions: ~220 ms
- Emphasized transitions: ~320 ms
- No perpetual shimmer or animated background effects.
- Streaming text must not trigger whole-screen animation/recomposition.

## Structure

```text
MainViewModel / state
        |
        v
     App UI
        |
   Material 3 theme
        |
   Screens/components
```

Existing `MainViewModel`, runtime, scheduler, agent, Accessibility, Shizuku, workspace and diagnostics responsibilities remain outside the presentation components.

## Responsive behavior

- Compact: single navigation surface with bottom navigation or compact drawer.
- Medium: navigation rail + content.
- Expanded: navigation rail/pane + primary content + optional secondary inspector.

Breakpoints must be based on available window size, not device model names.

## Interaction states

Every interactive surface should account for idle, pressed, focused, disabled, loading, success and error states where relevant. Errors should explain the recovery action without exposing secrets or native crash details unnecessarily.

## Performance rules

- Stable `LazyColumn` keys for chat/history rows.
- Avoid unnecessary state reads high in the composition tree.
- Keep streaming updates scoped to the changing message row.
- Avoid expensive blur, parallax and continuous canvas effects.
- Prefer derived state and immutable UI models.
- Keep animations off the inference/runtime execution path.

## Accessibility

- Minimum touch targets follow Material accessibility guidance.
- Content descriptions are semantic, not decorative.
- Color is never the only state indicator.
- Dynamic font scaling must remain usable.
- Reduced motion must be respected.
