# Design Philosophy

LAI shall feel like **one coherent Personal AI Platform**, not a collection of utilities.

## Principles

*   **Offline-first where practical, cloud-capable where local is insufficient** — local `llama.cpp` CPU/Vulkan/QNN is the default; cloud/remote are explicit, user-configured providers.
*   **Privacy-first, local-first execution** — prompts, trees, captures, models stay on device; network only for signed catalog refresh and explicit download.
*   **Provider-agnostic, backend-agnostic** — UI never imports a provider SDK; `BackendId` is opaque, scheduler is evidence-based (`AVAILABLE/SUPPORTED/ACTIVE/MEASURED/UNKNOWN`).
*   **Evidence, not guesses** — unmeasured renders `N/A`; never claim without log.
*   **Fail-closed, explicit permissions, user-controlled automation** — every consequential tool requires one-time review; `ToolAuditLedger` is hash-chained, approval-before-authority, replay-blocked.
*   **Modular, progressive enhancement, graceful fallback** — CPU fallback truthful; `install -r` keeps `storage/LAI/models`; `little 7 idle → big 0-3 burst`.
*   **Resource-aware, device-aware** — memory/thermal/battery preflight, `ContextWindowPolicy`, hysteresis, `little` idle.
*   **Professional, accessible, maintainable** — clean, modern, Google-grade polish without copying.

## Hybrid AI

Local `CPU/GPU/NPU` + Cloud `OpenAI/Anthropic/Gemini/OpenRouter` + Remote `Ollama/LAN` — user configures multiple providers; system routes by capability/privacy/latency/cost/model/task/device/network/preference/battery/thermal, with fallback truthful and cost/privacy controls.

## System Feel

One system: Chat, Agent, Files, Developer, Terminal, Knowledge, Models, Automation share the same scheduler, audit, and diagnostics. Progressive disclosure and command palette keep the app simple despite many capabilities.
