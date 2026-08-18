# Architecture

LAI is a **modular, layered, offline-first Android runtime** for local LLMs, Android automation, and future hybrid AI. The repository is the source of truth; the `16`-module graph is enforced by `scripts/check_architecture_boundaries.py`.

## Module Graph

```text
app (Compose shell + MainViewModel + ToolsDashboard + Xiaomi guide)
├── core:contracts (pure JVM: inference/ocr/agent/audit/automation/diagnostics/workspace/shell)
├── core:policy (pure: AgentPolicy, BuiltInToolCatalog, ToolAuditLedger, ToolInstructionGate, SettingsPolicy, ContextWindowPolicy)
├── core:scheduler (pure: InferenceScheduler, ModelMemoryEstimator, ThermalGovernorPolicy)
├── core:model (pure: ReviewedModelCatalog)
├── plugins:api (pure: LaiPlugin contract)
├── platform:download (ONLY network owner: ModelRepository, ModelDownloadWorker/Coordinator, RemoteModelCatalogRepository, WorkManager)
├── platform:audit (ONLY audit bytes: ToolAuditRepository hash chain)
├── platform:device (AndroidRuntimeEnvironmentProvider + thermalStates callback)
├── platform:history (ONLY content-bearing store: ChatHistoryRepository no-backup)
├── platform:accessibility (AccessibilityAutomationService, AccessibilityGateway, NodeSnapshotter)
├── platform:workspace (SAF grant, WorkspaceRepository/Saf/Discovery/Policy, WorkspaceSettingsStore)
├── platform:shizuku (ElevatedShell, PrivilegedUserService, ShizukuController)
├── runtime:llama (JNI/C++ llama.cpp CPU + Vulkan scaffold: native_inference, llama_cpu_backend batch 32, backend_registry)
├── runtime:ocr (BanglaOcrService placeholder)
└── runtime:orchestrator (AgentRuntime)
```

**Direction enforced:** `core` → no Android/network/JNI/vendor; `platform` → Android authority, never upward; `runtime` → replaceable adapters, no UI; `app` → only composition root. `network` only in `platform:download`; `audit bytes` only in `platform:audit`; `chat content` only in `platform:history`.

## Data Flow (simplified)

```text
Compose UI (LaiApp) → MainViewModel (StateFlow) → AppContainer
  → Inference: MainViewModel → InferenceScheduler (evidence/thermal/memory) → NativeInferenceEngine → JNI C++ (llama.cpp) → Adreno/QNN later
  → Tools: MainViewModel → AgentRuntime → AccessibilityGateway / Shizuku / BanglaOcrService → ToolAuditRepository (approval-before-authority, replay guard)
  → Models: ModelRepository (registry.json) ↔ platform:download (OkHttp, WorkManager, SHA-256, resume) ↔ storage/LAI/models (SAF auto-import)
  → Diagnostics: DiagnosticsReportV1 (privacy-safe, no prompts) → export via SAF
```

## Trust Boundaries

*   **Network:** Only `platform:download` has `INTERNET`; all other modules fail if they import `okhttp`/`java.net`.
*   **Audit:** Only `platform:audit` may write `tool_audit.jsonl` (hash-chained, fsync, full-chain verify).
*   **Content:** Only `platform:history` may persist `chat_history.json` (no-backup, ≤100 sessions, ≤512 msgs, atomic).
*   **Authority:** `AccessibilityGateway` (weak ref, `flagIncludeNotImportantViews`) + `ElevatedShell` (argv allowlist, no raw shell). Model text can never self-approve.
*   **Secrets:** PKCS12 `lai-release` key in `secrets.ANDROID_KEYSTORE_*` + offline copy — never in repo.

## Native Boundary

`runtime/llama` isolates JNI/C++: `runtimeInfo / createSession / countTokens / generate / setThreadLimit / destroySession / lastError` (7-slot metrics array). `BackendSession` (`count_tokens, generate, set_thread_limit`) — thread budget atomic between `llama_decode`. `kv_tokens_` mirrors cache token-for-token; `llama_memory_seq_rm(0, reused, -1)` keeps longest common prefix; batch `32`; adaptive `little 7 idle → big 0-3 burst`; `sched_setaffinity` best-effort.

## Future Additions (additive, no rewrite)

*   `core:tokenization` (SentencePiece unigram), `core:rag` (BM25 + Granite 107M 384-dim LiteRT embedder), `core:pipeline` (DAG), `core:agent` (split from `core:policy` for multi-step loop), `backend:litert`/`backend:rag-litert`, `features:rag/ocr` — each as new `core`/`backend`/`features` module, composed by `app`. Vulkan `GGML_VULKAN=ON` with `SPIRV-Headers` on CI, QNN isolated `runtime:qnn` with licensed QAIRT.

## Verification

`scripts/validate_repo.sh` checks `128 MB` cap, no `*.apk/*.so/*.gguf/*.jks`, docs links, catalog. `check_architecture_boundaries.py` checks direction. `validate_model_catalog.py` checks `models-v1.json` rev3. Tests `176` + JaCoCo ratchets (contracts .15 / policy .55 / scheduler .70 / model .50 / plugins .50) run on CI; device tests (Vulkan, QNN, OCR quality, thermal) are `DEVICE TEST REQUIRED`.
