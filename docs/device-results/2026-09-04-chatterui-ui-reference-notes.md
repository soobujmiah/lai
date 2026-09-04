# ChatterUI UI reference notes (for future LAI model-settings UI work)

**Date:** 2026-09-04
**Source:** live UI walkthrough of `com.Vali98.ChatterUI` v0.8.9-beta9b on the Redmi Turbo 4 Pro,
done alongside the OpenCL backend investigation
(`docs/device-results/2026-09-04-redmi-turbo-4-pro-chatterui-opencl-reference-investigation.md`).
Not a design decision, not a commitment to copy ChatterUI's UI — a factual record of how a
comparable local-LLM app exposes backend/model controls, kept for reference when LAI's own model
settings UI is next worked on.

## Navigation structure

- Bottom-line entry point is a **character list** (ChatterUI is chat-character-oriented; LAI has no
  equivalent concept, this is not suggested for LAI).
- A left-side **drawer** (hamburger icon, top-left) is the actual settings hub, with entries:
  `App Mode` (Local/Remote segmented toggle), `Sampler`, `Formatting`, `Models`, `TTS`, `Logs`,
  `About`, `Settings`.
- `Settings` (drawer, gear icon) is **app-wide chat/UI preferences** (theme, chat behavior toggles
  like "Load Chat On Startup", "Auto Scroll") — nothing backend/model-related lives here.
- `Models` (drawer) is a flat list of imported models, each shown as a card with: name, a row of
  info chips (param count, quantization, file size, architecture family, "External"/local-storage
  badge), context length, filename, and three per-card actions: **edit (pencil) = rename only**,
  **delete (trash) = remove the catalog entry** (with a confirm dialog; explicitly does not delete
  the underlying file), **play/eject (circle icon)** = load this model / unload the currently
  loaded one (a play icon becomes an X/eject icon on whichever model is currently loaded; only one
  model can be loaded at a time, and the play button on every other model is disabled while one is
  loaded — you must eject first). A banner at the top of this screen always shows
  `Model Loaded: <name>` or `Model Loaded: None`.
- **`Show Settings`** at the bottom of the Models screen (easy to confuse with the drawer's
  `Settings` — same label ambiguity likely worth avoiding in LAI's own copy) opens **`Model
  Settings`** — this is the actual backend/inference config screen, and it's per click-through, not
  obviously per-model from the entry point (in practice it edits whatever model was last
  interacted with).

## `Model Settings` screen — the actual backend/inference config surface

Sections, top to bottom:

1. **CPU Settings** (heading, but contains generic + GPU fields too — a naming inconsistency worth
   avoiding in LAI's own version):
   - `Max Context` — slider + numeric box, e.g. 4096.
   - `Threads` — slider + numeric box.
   - `Batch` — slider + numeric box.
   - `GPU Layers` — slider + numeric box, **0 by default**, this is the actual offload knob (see
     the OpenCL investigation doc — nothing gets offloaded to any backend below unless this is
     nonzero).
   - `Context Shift` — toggle.
2. **Backend Device** — a three-way segmented control: **OpenCL / Hexagon / CPU**. This is a
   separate control from `GPU Layers`; the two combine (device + how many layers to put on it).
   Only one device can be selected at a time; there's no "auto"/fallback-chain option visible in
   this UI — the user picks exactly one.
3. **Advanced Settings**:
   - `Show Model Name In Chat` — toggle.
   - `Automatically Load Model on Chat` — toggle.
   - `Save Local KV` — toggle, with inline explanatory text about the tradeoff (continuation across
     app restarts vs. battery/storage cost of a large KV cache file) — a good pattern (explaining
     the cost of a feature inline rather than just naming it) worth carrying into LAI's own copy if
     an equivalent setting is ever added.
4. `Back To Models` button pinned at the bottom.

## `Logs` screen (drawer)

A plain scrolling text view of the app's own internal event log (JS-side, not Android logcat —
confirmed during the investigation that entries here, like `GPU Layers: 0`, do not appear in
logcat at all). Notable entries seen: `MODEL LOAD` blocks (model name + the full parameter set
above, including the actually-applied `GPU Layers` value), per-turn `Prompt Timings` /
`Predicted Timings` blocks (tokens/sec, ms/token, elapsed time) after every generation, and
lifecycle markers (`Resetting state values for startup`, `Loading Tokenizer` /
`Tokenizer Loaded`). This in-app log was the only place the actually-applied `GPU Layers` value for
a given load could be confirmed — logcat alone was not sufficient for that specific fact.

## Observed hazards worth remembering if this UI shape is ever mirrored

- The `Play` icon on a model card being silently `disabled` (not hidden, not visibly greyed
  differently enough to notice at a glance) while another model is loaded is easy to miss — cost
  real time during this investigation (taps registered at the OS level, produced no app response,
  with no visible feedback that the control was disabled).
- Two different screens both call themselves "Settings" (drawer `Settings` = app/chat prefs, Models
  screen `Show Settings` = per-model backend/inference config) — genuinely ambiguous from memory
  alone; only distinguishable by which screen you're already on.
- The GPU Layers text field's tap target and the model card's trash-icon tap target can end up at
  visually similar Y-coordinates depending on which banner text is showing above them (`Model
  Loaded: None` vs `Model Loaded: <name>` are different string lengths but same line height, so this
  wasn't a layout-shift issue — the real lesson is to get exact bounds via `uiautomator dump`
  rather than eyeballing screenshot coordinates when automating a third-party app's UI, since a
  screen recall from memory or a prior screenshot is not reliable for precise tap targets and a
  single mistap can trigger a destructive-looking (though ultimately cancelable) confirmation
  dialog).

## Explicitly not evaluated

Visual styling, color/theme, animation, accessibility, or React Native vs. LAI's Compose-based UI
framework tradeoffs — this note is about *what controls exist and how they're organized*, not about
visual design, since LAI's own UI direction is a separate decision for later.
