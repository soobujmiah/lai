# Design System

LAI shall use a **clean, modern, accessible** system — professional, not decorative. Quality bar: polished productivity software, without copying.

This design system is an original LAI product asset. It must not imitate proprietary UI chrome, icon sets, illustrations, or motion from another application. Third-party visual assets require intake under [`legal/THIRD_PARTY_INTAKE.md`](legal/THIRD_PARTY_INTAKE.md). Brand/mark questions are covered by [`legal/TRADEMARK_POLICY.md`](legal/TRADEMARK_POLICY.md).

## Foundations

*   **Typography:** `Material3` typescale, `headlineMedium` for screen titles, `titleLarge` for cards, `bodyLarge` for bubbles, `labelSmall` for metadata. Bangla and English share the same scale; line-height 1.4 for Bengali readability.
*   **Spacing:** `4, 8, 12, 14, 16, 20, 24 dp` — `LazyColumn` `14–16 dp` gaps, card `14–16 dp` padding, `20 dp` screen padding. No double `imePadding`.
*   **Colors:** `Material3` dynamic `light/dark/system` — `primaryContainer` for user bubbles, `surfaceVariant` for assistant, `secondaryContainer` for Xiaomi tip, `error` for `SENSITIVE/ELEVATED` risk badges. Contrast meets WCAG AA, touch targets ≥48 dp.
*   **Surfaces & Elevation:** `Card` `0–1 dp` for content, `ModalBottomSheet` for history/workspace, `AlertDialog` for tool approval. No excessive borders/animations.
*   **Radius:** `RoundedCornerShape 18 dp` for bubbles, `12 dp` for cards, `28 dp` for sheets.
*   **Iconography:** Text glyphs (`● ◉ ◆`) over `material-icons-extended` (~9 MB) to keep debug APK honest. No decoration without purpose.

## Components

Buttons (`Button` primary, `OutlinedButton` secondary, `TextButton` tertiary), `Switch` for proposals/developer mode, `OutlinedTextField` (4 lines max, `ImeAction.Send`), `LinearProgressIndicator` for downloads, `CircularProgressIndicator` centered, `NavigationBar` (Chat/Screen Reader/Automator) keyed on `imeAnimationTarget` (never current inset), `TopAppBar` (`safeDrawing` only H+Top), `Scaffold` `imePadding()` once.

## States

Loading (centered spinner), progress (determinate/indeterminate), empty (e.g. `chat_history_empty`), permission prompt (Xiaomi lock guide), agent progress (`Thinking locally…`), errors (`Stalled at AWAITING_FIRST_TOKEN`), success (`Generated 9 tokens locally`), warnings (`Reduced CPU threads…`).

## Themes

Light / Dark / System — `Theme.kt` `dynamicColor` aware, `high-refresh-rate` opt-in. No custom theme picker that duplicates system.

## Consistency

Every screen uses the same `StatusCard` (title + `onSurfaceVariant` detail), `FeatureScreen` (`title + subtitle + 12 dp column + notice`), `MessageBubble` (`0.86f` width). Lists use stable `id` keys so streaming recomposes only the changed row.
