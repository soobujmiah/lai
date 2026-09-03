package dev.lai.runtime.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lai.runtime.BuildConfig
import dev.lai.runtime.LaiApplication
import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.agent.ToolCallParseResult
import dev.lai.runtime.agent.ToolInstructionGate
import dev.lai.runtime.settings.ContextWindowPolicy
import dev.lai.runtime.agent.ToolProposalCounters
import dev.lai.runtime.agent.ToolRisk
import dev.lai.runtime.audit.ToolAuditOutcome
import dev.lai.runtime.audit.ToolAuditRecordV1
import dev.lai.runtime.core.AppRuntimeEvent
import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.core.LaiLog
import dev.lai.runtime.diagnostics.AppDiagnostics
import dev.lai.runtime.diagnostics.AutomationDiagnostics
import dev.lai.runtime.diagnostics.DeviceDiagnostics
import dev.lai.runtime.diagnostics.DiagnosticsPrivacy
import dev.lai.runtime.diagnostics.DiagnosticsReportV1
import dev.lai.runtime.diagnostics.GenerationPerformanceDiagnostics
import dev.lai.runtime.diagnostics.LogEntryDiagnostics
import dev.lai.runtime.diagnostics.ModelDiagnostics
import dev.lai.runtime.diagnostics.RuntimeDiagnostics
import dev.lai.runtime.diagnostics.ToolAuditDiagnostics
import dev.lai.runtime.automation.AccessibilityGateway
import dev.lai.runtime.history.ChatSessionSummary
import dev.lai.runtime.history.StoredChatMessage
import dev.lai.runtime.history.StoredChatSession
import dev.lai.runtime.inference.BackgroundDownloadState
import dev.lai.runtime.inference.ComputeClass
import dev.lai.runtime.inference.ConversationMessage
import dev.lai.runtime.inference.ConversationRole
import dev.lai.runtime.inference.DownloadProgress
import dev.lai.runtime.inference.GenerationConfig
import dev.lai.runtime.inference.BackendId
import dev.lai.runtime.inference.GenerationMetrics
import dev.lai.runtime.inference.InferenceEvent
import dev.lai.runtime.inference.InstalledModel
import dev.lai.runtime.inference.ModelSpec
import dev.lai.runtime.model.CatalogSource
import dev.lai.runtime.model.ReviewedModel
import dev.lai.runtime.model.ReviewedModelCatalog
import dev.lai.runtime.scheduler.BackendCapability
import dev.lai.runtime.scheduler.CapabilityEvidence
import dev.lai.runtime.scheduler.InferenceWorkload
import dev.lai.runtime.scheduler.RuntimeEnvironment
import dev.lai.runtime.scheduler.ThermalGovernorPolicy
import dev.lai.runtime.scheduler.ThermalState
import dev.lai.runtime.settings.LlmSettings
import dev.lai.runtime.settings.SettingsDocumentV1
import dev.lai.runtime.shell.ShizukuState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

enum class UiMode { CHAT, SCREEN_READER, AUTOMATOR }

enum class RuntimeOperation {
    NO_MODEL,
    IDLE,
    DOWNLOADING,
    IMPORTING,
    EXPORTING,
    LOADING,
    READY,
    GENERATING,
    CANCELLING,
    READING_SCREEN,
    AUTOMATING,
    ERROR,
}

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val contextEligible: Boolean = true,
    /**
     * Stable identity for `LazyColumn`'s `key`.
     *
     * Without it Compose keys rows by index, so appending a streamed token invalidates every
     * bubble in the list and the whole conversation recomposes on each token - the main source of
     * chat jank on device. With a stable id only the row whose text changed is redrawn.
     */
    val id: Long = nextChatMessageId(),
)

private val chatMessageIds = java.util.concurrent.atomic.AtomicLong(0L)

private fun nextChatMessageId(): Long = chatMessageIds.incrementAndGet()

data class PendingToolProposal(
    val call: ToolCall,
    val summary: String,
    val risk: ToolRisk,
)

private data class ScheduledLoad(
    val backend: BackendId,
    val reason: String,
    val estimatedPeakBytes: Long,
    val environment: RuntimeEnvironment,
    val loadMs: Long,
)

private data class PreparedConversation(
    val messages: List<ConversationMessage>,
    val trimmedTurns: Int,
    /** Completed turns dropped by the user's rolling context window (not overflow trims). */
    val windowedTurns: Int,
)

private fun environmentSummary(environment: RuntimeEnvironment): String {
    val memory = environment.availableMemoryBytes?.let { "${it / 1_048_576} MB available" } ?: "memory unknown"
    val battery = environment.batteryPercent?.let { "$it% battery" } ?: "battery unknown"
    val charging = when (environment.charging) { true -> "charging"; false -> "not charging"; null -> "charge unknown" }
    return "$memory • $battery • $charging • thermal ${environment.thermalState.name}"
}

data class MainUiState(
    val mode: UiMode = UiMode.CHAT,
    val input: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            fromUser = false,
            text = "আসসালামু আলাইকুম — আমি LAI। একটি লোকাল মডেল যুক্ত হলে বাংলা বা English-এ সাহায্য করতে পারব।",
            contextEligible = false,
        ),
    ),
    val operation: RuntimeOperation = RuntimeOperation.NO_MODEL,
    val accessibilityConnected: Boolean = false,
    val shizukuState: ShizukuState = ShizukuState.Unavailable,
    val developerMode: Boolean = false,
    val settingsVisible: Boolean = false,
    val installedModels: List<InstalledModel> = emptyList(),
    val supportedModels: List<ReviewedModel> = ReviewedModelCatalog.all,
    val recommendedModel: ReviewedModel = ReviewedModelCatalog.recommendedCpuBaseline,
    val catalogStatus: String = "Embedded supported-model list available offline",
    val catalogRefreshing: Boolean = false,
    val activeModelId: String? = null,
    val modelName: String = "",
    val modelUrl: String = "",
    val modelSha: String = "",
    val downloadProgress: DownloadProgress? = null,
    val downloadingModelId: String? = null,
    val notice: String? = null,
    val runtimeDetail: String = "",
    val schedulerDetail: String = "No model has been scheduled",
    val environmentDetail: String = "",
    val estimatedPeakBytes: Long? = null,
    val lastModelLoadMs: Long? = null,
    val lastGenerationMetrics: GenerationMetrics? = null,
    val performanceHistory: List<GenerationMetrics> = emptyList(),
    val thermalGovernorDetail: String? = null,
    val chatSessions: List<ChatSessionSummary> = emptyList(),
    val chatHistoryVisible: Boolean = false,
    val trimmedConversationTurns: Int = 0,
    val windowedConversationTurns: Int = 0,
    val diagnosticsStatus: String = "No diagnostics exported",
    val toolProposalsEnabled: Boolean = false,
    val pendingToolProposal: PendingToolProposal? = null,
    val toolProposalCounters: ToolProposalCounters = ToolProposalCounters(),
    val toolAuditHistory: List<ToolAuditRecordV1> = emptyList(),
    val toolAuditStatus: String = "Persistent tool audit not loaded",
    val toolAuditIntegrityValid: Boolean = false,
    val workspace: WorkspaceUiState = WorkspaceUiState(),
    /** Short, LAI-authored reason the last attempt produced no tokens. Never model/prompt text. */
    val lastGenerationFailure: String? = null,
    val emptyGenerationCount: Int = 0,
) {
    val busy: Boolean
        get() = operation in setOf(
            RuntimeOperation.DOWNLOADING,
            RuntimeOperation.IMPORTING,
            RuntimeOperation.EXPORTING,
            RuntimeOperation.LOADING,
            RuntimeOperation.GENERATING,
            RuntimeOperation.CANCELLING,
            RuntimeOperation.READING_SCREEN,
            RuntimeOperation.AUTOMATING,
        )
}

/** Where a generation attempt got to, used to explain a stall in diagnostics. */
private enum class GenerationStage { IDLE, COUNTING_TOKENS, AWAITING_FIRST_TOKEN, STREAMING, COMPLETED }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaiApplication).container
    private var generationJob: Job? = null
    private var cancelWatchdogJob: Job? = null
    private var downloadWatchJob: Job? = null
    private val thermalGovernor = ThermalGovernorPolicy()
    private var thermalDecision: ThermalGovernorPolicy.Decision? = null
    private var currentChatId: String = UUID.randomUUID().toString()
    private var currentChatCreatedAtEpochMs: Long = System.currentTimeMillis()
    @Volatile private var generationStage = GenerationStage.IDLE

    /**
     * Typed-settings session + SAF workspace status (Phase 2A). All decisions live in the pure
     * coordinator; the ViewModel only binds them to `MainUiState` and to Android authority calls.
     */
    private val workspace = WorkspaceSettingsCoordinator(
        grant = container.workspaceRepository,
        store = container.workspaceSettingsStore,
        discovery = container.workspaceDiscovery,
    )
    private val _state = MutableStateFlow(
        MainUiState(
            runtimeDetail = container.inferenceEngine.capabilities.detail,
            environmentDetail = environmentSummary(container.runtimeEnvironment.snapshot()),
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        adoptBackgroundDownloads()
        refreshChatSessions()
        // Closed-loop thermal governor. Previously heat only REFUSED new work at SEVERE while an
        // in-flight reply kept burning all four threads. Now every thermal status change maps to
        // a decode-thread budget (pure ThermalGovernorPolicy, hysteresis included) that the
        // native decode loop applies between llama_decode calls.
        viewModelScope.launch {
            container.runtimeEnvironment.thermalStates().collect { thermal ->
                val baseline = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
                val previous = thermalDecision
                val decision = thermalGovernor.decide(thermal, previous, baseline)
                thermalDecision = decision
                container.inferenceEngine.setDecodeThreadLimit(decision.decodeThreads)
                _state.update {
                    it.copy(
                        thermalGovernorDetail = decision.reason,
                        notice = if (decision.reason != null && decision.reason != previous?.reason) {
                            decision.reason
                        } else {
                            it.notice
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(AccessibilityGateway.connected, container.shizukuController.state) { accessibility, shizuku ->
                accessibility to shizuku
            }.collect { (accessibility, shizuku) ->
                val previous = _state.value
                if (previous.accessibilityConnected != accessibility) {
                    LaiLog.i("LAI-perms", "Accessibility authority connected=$accessibility")
                }
                if (previous.shizukuState != shizuku) {
                    LaiLog.i("LAI-perms", "Shizuku state=$shizuku")
                }
                _state.update { it.copy(accessibilityConnected = accessibility, shizukuState = shizuku) }
            }
        }
        viewModelScope.launch {
            container.events.collect { event ->
                when (event) {
                    is AppRuntimeEvent.ModelUnloadedForMemory -> {
                        LaiLog.w("LAI-mem", "Model unloaded under memory pressure")
                        generationJob?.cancel(CancellationException("Model released for memory pressure"))
                        _state.update {
                            it.copy(
                                operation = RuntimeOperation.IDLE,
                                activeModelId = null,
                                lastGenerationMetrics = null,
                                notice = "Model unloaded to protect the device under memory pressure",
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var previousGranted = workspace.state.value.granted
            workspace.state.collect { workspaceState ->
                val granted = workspaceState.granted
                if (granted && !previousGranted) {
                    // The user granted (or re-granted) the workspace after startup — import any
                    // .gguf already sitting in models/ now, not just at the next process start.
                    LaiLog.i("LAI-workspace", "Workspace grant became active; importing models/")
                    importWorkspaceModels()
                }
                previousGranted = granted
                _state.update { it.copy(workspace = workspaceState) }
            }
        }
        loadToolAudit()
        refreshModels()
        loadCachedCatalog()
        refreshWorkspace()
        autoImportWorkspaceModels()
    }

    // ---------------------------------------------------------------------------------------
    // Workspace grant + typed settings (Phase 2A items 6-7)
    // ---------------------------------------------------------------------------------------

    /** Re-reads the SAF grant state and reloads settings; never throws and never blocks start-up. */
    fun refreshWorkspace() {
        viewModelScope.launch { workspace.refresh() }
    }

    /** Call from the `ACTION_OPEN_DOCUMENT_TREE` result. Takes the persistable permission first. */
    fun grantWorkspace(treeUri: Uri) {
        viewModelScope.launch {
            val granted = container.workspaceRepository.grant(treeUri)
                .mapCatching { container.workspaceRepository.ensureLayout().getOrThrow() }
            val status = workspace.refresh()
            granted.fold(
                onSuccess = { LaiLog.i("LAI-workspace", "Workspace folder granted") },
                onFailure = { error ->
                    LaiLog.e("LAI-workspace", "Workspace folder grant failed: ${error.message}", error)
                },
            )
            val notice = granted.fold(
                onSuccess = { "Workspace folder connected • $status" },
                onFailure = { error ->
                    "Workspace folder was not connected: " +
                        (error.message ?: "permission could not be kept")
                },
            )
            _state.update { it.copy(notice = notice) }
            if (granted.isSuccess) {
                // Import right away so a freshly granted folder's models/ appears as Installed.
                importWorkspaceModels()
            }
        }
    }

    /** Releases the persistable permission. Nothing inside the user's folder is deleted. */
    fun revokeWorkspace() {
        viewModelScope.launch {
            LaiLog.i("LAI-workspace", "Workspace folder revoked")
            container.workspaceRepository.revoke()
            workspace.refresh()
            _state.update {
                it.copy(notice = "Workspace folder disconnected; built-in defaults are in use")
            }
        }
    }

    fun showQuickSettings() = workspace.setQuickSettingsVisible(true)
    fun hideQuickSettings() = workspace.setQuickSettingsVisible(false)

    /** Upper bound the quick sheet must respect for max new tokens on the loaded runtime. */
    fun maxNewTokensCeiling(): Int = workspace.maxNewTokensCeiling(container.inferenceEngine.contextSize)

    /** "Apply once": affects the next reply only and never mutates saved defaults. */
    fun applyQuickSettings(llm: LlmSettings) {
        val message = workspace.applyOnce(llm)
        _state.update { it.copy(notice = message) }
    }

    /** "Save default": validated, then persisted through the exact-schema store. */
    fun saveDefaultSettings(document: SettingsDocumentV1) {
        viewModelScope.launch {
            val message = workspace.saveDefaults(document)
            _state.update { it.copy(notice = message) }
        }
    }

    /** "Reset": returns to embedded defaults, persisting them when a workspace is connected. */
    fun resetSettings() {
        viewModelScope.launch {
            val message = workspace.resetDefaults()
            _state.update { it.copy(notice = message) }
        }
    }

    /**
     * Registers workspace model metadata only; never copies weights and never auto-loads. Also
     * triggers the import pass, so a manual "Scan for models" makes .gguf files appear as
     * Installed instead of only updating the coarse counts.
     */
    fun scanWorkspaceModels() {
        viewModelScope.launch {
            val reviewed = state.value.supportedModels.associate { it.sha256.lowercase() to it.id }
            val message = workspace.discoverModels(reviewed)
            _state.update { it.copy(notice = message) }
            importWorkspaceModels()
        }
    }

    fun setMode(mode: UiMode) = _state.update { it.copy(mode = mode, settingsVisible = false, notice = null) }
    fun setInput(value: String) = _state.update { it.copy(input = value) }
    fun toggleSettings() = _state.update { it.copy(settingsVisible = !it.settingsVisible) }
    fun setDeveloperMode(enabled: Boolean) {
        LaiLog.i("LAI-app", "Developer mode enabled=$enabled")
        _state.update { it.copy(developerMode = enabled) }
    }
    fun setToolProposalsEnabled(enabled: Boolean) {
        if (enabled && !state.value.toolAuditIntegrityValid) {
            _state.update { it.copy(notice = "Persistent tool audit is unavailable; action proposals remain disabled") }
            return
        }
        _state.update {
            it.copy(
                toolProposalsEnabled = enabled,
                pendingToolProposal = if (enabled) it.pendingToolProposal else null,
                notice = if (enabled) {
                    "Local action proposals enabled; every proposed tool still requires one-time review"
                } else {
                    "Local action proposals disabled"
                },
            )
        }
    }
    fun setModelName(value: String) = _state.update { it.copy(modelName = value) }
    fun setModelUrl(value: String) = _state.update { it.copy(modelUrl = value) }
    fun setModelSha(value: String) = _state.update { it.copy(modelSha = value) }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun refreshSupportedModels() {
        if (state.value.catalogRefreshing) return
        _state.update { it.copy(catalogRefreshing = true, catalogStatus = "Checking signed catalog…") }
        viewModelScope.launch {
            container.modelCatalogRepository.refresh()
                .onSuccess { snapshot ->
                    applyCatalog(snapshot.document.models, snapshot.source)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            catalogRefreshing = false,
                            catalogStatus = "Web catalog unavailable; verified offline list retained",
                            notice = error.message ?: "Catalog refresh failed",
                        )
                    }
                }
        }
    }

    private fun loadCachedCatalog() {
        viewModelScope.launch {
            val snapshot = container.modelCatalogRepository.cachedOrEmbedded()
            applyCatalog(snapshot.document.models, snapshot.source)
        }
    }

    private fun autoImportWorkspaceModels() {
        viewModelScope.launch {
            // Give workspace refresh a moment to load the persisted grant, then auto-import
            // storage/LAI/models/*.gguf into app-private models/ so it appears as Installed.
            // Runs again whenever the grant becomes active (see workspace.state.collect).
            kotlinx.coroutines.delay(1500)
            if (!workspace.state.first().granted) {
                LaiLog.d("LAI-workspace", "Auto-import: skipped at startup — no workspace grant yet")
                return@launch
            }
            importWorkspaceModels()
        }
    }

    /**
     * Imports GGUF files from the granted workspace models/ folder (storage/LAI/models) into
     * app-private storage so discovered files appear as Installed. Re-runs when the workspace
     * grant becomes active, after a manual grant, and after a manual scan. Import always requires
     * a verified SHA-256 (ModelRepository.validateIdentity); files without a digest are skipped
     * and logged rather than failing silently.
     */
    private var importInFlight = false

    private fun importWorkspaceModels() {
        // Multiple triggers (startup, grant-active transition, manual grant, manual scan) can
        // fire close together; only one import pass may run at a time, otherwise two passes copy
        // the same 1.1 GB file into the same .part and corrupt each other (device evidence
        // 2026-08-19: ENOENT + SHA-256 mismatch on the shared staging file).
        if (importInFlight) {
            LaiLog.d("LAI-workspace", "Auto-import already running; skipping duplicate pass")
            return
        }
        importInFlight = true
        viewModelScope.launch {
        try {
            if (!workspace.state.value.granted) return@launch
            val reviewedBySha = state.value.supportedModels.associate { it.sha256.lowercase() to it.id }
            val result = container.workspaceDiscovery.discoverModels(
                reviewedBySha,
                dev.lai.runtime.workspace.DiscoveryLimits(),
            )
            val discovered = result.getOrNull() ?: run {
                LaiLog.w("LAI-workspace", "Auto-import: discovery failed: ${result.exceptionOrNull()?.message}")
                return@launch
            }
            if (discovered.isEmpty()) {
                LaiLog.d("LAI-workspace", "Auto-import: no .gguf found in the granted folder's models/ subdirectory")
                return@launch
            }
            // Filter to gguf files that are not yet installed
            val installedNames = state.value.installedModels.map { it.fileName }.toSet()
            val toImport = discovered.filter { it.modelFormat == "gguf" && it.fileName !in installedNames }
            if (toImport.isEmpty()) {
                LaiLog.d("LAI-workspace", "Auto-import: ${discovered.size} model(s) found, none new")
                return@launch
            }
            LaiLog.i("LAI-workspace", "Auto-import: ${toImport.size} of ${discovered.size} model(s) to import")
            val saf = container.workspaceRepository.saf() ?: return@launch
            val app = getApplication<Application>()
            var imported = 0
            for (d in toImport) {
                try {
                    val sha = d.sha256
                    if (sha == null) {
                        LaiLog.w("LAI-workspace", "Auto-import: skipping ${d.fileName} (no SHA-256 available)")
                        continue
                    }
                    val docId = findWorkspaceDocId(saf, d.relativePath) ?: continue
                    val treeUri = container.workspaceRepository.grantedTreeUri ?: continue
                    val uri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    // Use the reviewed import spec if this is a reviewed model, otherwise generic
                    val spec = d.reviewedCatalogId?.let { id ->
                        state.value.supportedModels.firstOrNull { it.id == id }?.toImportSpec()
                    } ?: dev.lai.runtime.inference.ModelImportSpec(
                        id = d.fileName.removeSuffix(".gguf").lowercase().replace(Regex("[^a-z0-9._-]+"), "-"),
                        displayName = d.fileName.removeSuffix(".gguf"),
                        expectedBytes = d.sizeBytes,
                        sha256 = sha,
                    )
                    val outcome = container.modelRepository.importModel(spec, app.contentResolver, uri) { _ -> }
                    if (outcome.isSuccess) {
                        imported++
                        LaiLog.i("LAI-workspace", "Auto-import: ${d.fileName} installed")
                    } else {
                        LaiLog.w("LAI-workspace", "Auto-import: ${d.fileName} not installed: ${outcome.exceptionOrNull()?.message}")
                    }
                    // Small delay between imports to avoid I/O storm for ~1.1 GB files
                    kotlinx.coroutines.delay(200)
                } catch (error: Exception) {
                    LaiLog.w("LAI-workspace", "Auto-import: ${d.fileName} failed: ${error.message}")
                }
            }
            if (imported > 0) {
                refreshModels()
                _state.update {
                    it.copy(notice = "$imported model(s) imported from the workspace folder")
                }
            }
        } finally {
            importInFlight = false
        }
        }
    }

    private fun findWorkspaceDocId(saf: dev.lai.runtime.workspace.WorkspaceSaf, relativePath: String): String? {
        var docId = saf.rootDocumentId
        for (part in relativePath.split("/")) {
            if (part.isEmpty()) continue
            val children = saf.listChildren(docId)
            val next = children.firstOrNull { it.name == part } ?: return null
            docId = next.documentId
        }
        return docId
    }

    private fun loadToolAudit() {
        viewModelScope.launch {
            val snapshot = container.toolAuditRepository.snapshot()
            if (!snapshot.integrityValid) {
                LaiLog.w("LAI-audit", "Tool audit integrity invalid; action proposals disabled")
            }
            _state.update {
                it.copy(
                    toolAuditHistory = snapshot.records.takeLast(MAX_TOOL_AUDIT_RECORDS),
                    toolAuditStatus = snapshot.detail,
                    toolAuditIntegrityValid = snapshot.integrityValid,
                    toolProposalsEnabled = if (snapshot.integrityValid) it.toolProposalsEnabled else false,
                    notice = if (snapshot.integrityValid) it.notice else {
                        "Local action proposals disabled because the persistent audit failed verification"
                    },
                )
            }
        }
    }

    private fun applyCatalog(models: List<ReviewedModel>, source: CatalogSource) {
        val safeModels = models.ifEmpty { ReviewedModelCatalog.all }
        val sourceLabel = when (source) {
            CatalogSource.EMBEDDED -> "Embedded supported-model list available offline"
            CatalogSource.VERIFIED_CACHE -> "Verified cached model list available offline"
            CatalogSource.SIGNED_WEB -> "Signed web model list verified and cached"
        }
        _state.update {
            it.copy(
                supportedModels = safeModels,
                recommendedModel = safeModels.firstOrNull() ?: ReviewedModelCatalog.recommendedCpuBaseline,
                catalogStatus = sourceLabel,
                catalogRefreshing = false,
            )
        }
    }

    fun sendMessage() {
        val prompt = state.value.input.trim()
        if (prompt.isEmpty() || state.value.busy || state.value.pendingToolProposal != null) return
        val immediateMessage = when {
            state.value.installedModels.isEmpty() ->
                "লোকাল মডেল এখনো ইনস্টল করা নেই। Settings থেকে একটি supported model ডাউনলোড করুন।"
            state.value.activeModelId == null ->
                "মডেল ইনস্টল করা আছে। Settings থেকে মডেলটি Load করুন।"
            else -> null
        }
        if (immediateMessage != null) {
            _state.update {
                it.copy(
                    input = "",
                    messages = it.messages +
                        ChatMessage(true, prompt, contextEligible = false) +
                        ChatMessage(false, immediateMessage, contextEligible = false),
                )
            }
            persistChat()
            return
        }

        _state.update {
            it.copy(
                input = "",
                messages = it.messages + ChatMessage(true, prompt) + ChatMessage(false, ""),
                operation = RuntimeOperation.GENERATING,
                lastGenerationMetrics = null,
                // First token on a 1.5B CPU model can take several seconds while the prompt is
                // processed. Say so, instead of showing an empty bubble that looks broken.
                notice = "Thinking locally… the first words can take a few seconds.",
            )
        }
        // Thermal admission. The scheduler already gates model LOAD on heat, but a long chat can
        // heat the device after loading and nothing re-checked it. Field report: sustained
        // generation made the phone hot with no warning anywhere in the UI.
        val thermal = container.runtimeEnvironment.snapshot().thermalState
        if (thermal >= ThermalState.SEVERE) {
            _state.update {
                it.copy(
                    input = "",
                    messages = it.messages +
                        ChatMessage(true, prompt, contextEligible = false) +
                        ChatMessage(
                            fromUser = false,
                            text = "ফোন গরম হয়ে গেছে, তাই উত্তর তৈরি বন্ধ রাখা হলো। কিছুক্ষণ ঠান্ডা হতে দিন।\n\n" +
                                "Paused because the device is too hot. Let it cool for a moment and try again.",
                            contextEligible = false,
                        ),
                    notice = "Generation paused to protect the device (thermal ${thermal.name})",
                )
            }
            return
        }

        // Consume any armed "Apply once" override here, so it spends itself on exactly one reply.
        val requestSettings = workspace.consumeForRequest()
        val requestConfig = generationConfig(requestSettings)
        // Three device reports produced "no reply" with no indication of WHERE it stalled.
        // This records the last stage reached so a diagnostics export names the blocking call
        // (token counting, waiting for the first token, or mid-stream) instead of guessing.
        generationStage = GenerationStage.COUNTING_TOKENS
        LaiLog.i("LAI-llm", "Generation start model=${state.value.activeModelId}")
        generationJob = viewModelScope.launch {
            try {
                val prepared = prepareConversation(requestConfig, requestSettings.context.keepLastTurns)
                generationStage = GenerationStage.AWAITING_FIRST_TOKEN
                _state.update {
                    it.copy(
                        trimmedConversationTurns = prepared.trimmedTurns,
                        windowedConversationTurns = prepared.windowedTurns,
                    )
                }
                container.inferenceEngine.generate(prepared.messages, requestConfig).collect { event ->
                    when (event) {
                        is InferenceEvent.Token -> {
                            generationStage = GenerationStage.STREAMING
                            appendAssistantToken(event.text)
                        }
                        is InferenceEvent.Completed -> {
                            generationStage = GenerationStage.COMPLETED
                            completeGeneration(event)
                        }
                        is InferenceEvent.Failed -> markGenerationFailed(event.message)
                    }
                }
            } catch (cancelled: CancellationException) {
                restoreAfterStoppedGeneration(
                    if (state.value.activeModelId != null) "Generation stopped locally" else "Generation stopped",
                )
            } catch (error: Exception) {
                markGenerationFailed(error.message ?: "Generation failed")
            } finally {
                generationJob = null
            }
        }
    }

    fun cancelGeneration() {
        if (state.value.operation != RuntimeOperation.GENERATING) return
        LaiLog.i("LAI-llm", "Generation stop requested by user")
        _state.update { it.copy(operation = RuntimeOperation.CANCELLING, notice = "Stopping generation…") }
        val cancelled = generationJob
        cancelled?.cancel(CancellationException("User stopped generation"))

        // Field report (Redmi Turbo 4 Pro): Stop could stick on "Stopping…" forever.
        //
        // Job.cancel() only sets a flag. A coroutine blocked inside a non-suspending JNI call
        // (llama_decode, or countTokens during prompt preparation) does not observe it until
        // that call returns, so the catch block that restores the UI may never run. The user is
        // then stranded with a dead button and no way back.
        //
        // The watchdog guarantees the UI recovers: it waits for the job to finish, and if the
        // native side has not yielded within the grace period it hands the chat back to the user
        // regardless. The engine is released so a wedged session cannot keep burning CPU.
        cancelWatchdogJob?.cancel()
        cancelWatchdogJob = viewModelScope.launch {
            val exited = cancelled == null ||
                withTimeoutOrNull(CANCEL_GRACE_MS) { cancelled.join() } != null
            if (state.value.operation != RuntimeOperation.CANCELLING) return@launch
            if (exited) {
                restoreAfterStoppedGeneration("Generation stopped locally")
            } else {
                // The native call has not returned yet. Do NOT close the engine: unloading the
                // model here made every Stop cost a 2.6 s reload and left the next message
                // answered with "load the model from Settings" (field report, build 0.6.83).
                //
                // The cancellation flag is already latched, so the decode loop exits on its own
                // at the next token boundary. Return the UI to a usable state and keep the model.
                generationJob = null
                recordGenerationFailure(
                    "Stalled at ${generationStage.name} for over ${CANCEL_GRACE_MS} ms",
                )
                _state.update {
                    it.copy(
                        operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                        notice = "Generation is still finishing in the background; the model stays loaded.",
                    )
                }
                markLastAssistantContextIneligible()
                dropTrailingEmptyAssistantMessage()
                // A stalled generation on an accelerator is usually a wedged driver; closing the
                // engine releases it and the CPU reload keeps the app usable.
                fallbackToCpuAfterAcceleratorFailure(
                    "Stalled at ${generationStage.name} on accelerator for over ${CANCEL_GRACE_MS} ms",
                )
            }
        }
    }

    /** Restores a usable chat state after a stopped generation, dropping the empty reply bubble. */
    private fun restoreAfterStoppedGeneration(message: String) {
        LaiLog.i("LAI-llm", "Generation stopped: $message")
        markLastAssistantContextIneligible()
        dropTrailingEmptyAssistantMessage()
        _state.update {
            it.copy(
                operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                notice = message,
            )
        }
    }

    /**
     * Removes the placeholder assistant bubble when generation produced nothing.
     *
     * Without this a stopped or failed reply leaves an empty grey bubble on screen, which reads as
     * a broken app rather than as "nothing was generated".
     */
    private fun dropTrailingEmptyAssistantMessage() {
        _state.update {
            val last = it.messages.lastOrNull()
            if (last != null && !last.fromUser && last.text.isBlank()) {
                it.copy(messages = it.messages.dropLast(1))
            } else {
                it
            }
        }
    }

    private fun completeGeneration(event: InferenceEvent.Completed) {
        val current = state.value
        val modelOutput = current.messages.lastOrNull()?.takeIf { !it.fromUser }?.text.orEmpty()
        event.metrics?.let { metrics ->
            LaiLog.i(
                "LAI-llm",
                "Generation completed: ${metrics.generatedTokens} tokens, " +
                    "decode=${metrics.decodeTokensPerSecond} tok/s, " +
                    "prefill=${metrics.promptTokensPerSecond} tok/s, " +
                    "ttft=${metrics.timeToFirstTokenMs} ms, total=${metrics.totalMs} ms",
            )
        } ?: LaiLog.i("LAI-llm", "Generation completed: no metrics reported")
        val proposal = if (current.toolProposalsEnabled) {
            container.agentRuntime.parseToolProposal(modelOutput)
        } else {
            ToolCallParseResult.NotToolCall
        }
        _state.update { latest ->
            val performance = event.metrics?.let { metrics ->
                (latest.performanceHistory + metrics).takeLast(MAX_PERFORMANCE_SAMPLES)
            } ?: latest.performanceHistory
            val proposalCounters = if (current.toolProposalsEnabled) {
                latest.toolProposalCounters.record(proposal)
            } else {
                latest.toolProposalCounters
            }
            when (proposal) {
                ToolCallParseResult.NotToolCall -> latest.copy(
                    operation = RuntimeOperation.READY,
                    lastGenerationMetrics = event.metrics,
                    performanceHistory = performance,
                    toolProposalCounters = proposalCounters,
                    notice = "Generated ${event.tokensGenerated} tokens locally",
                )
                is ToolCallParseResult.Accepted -> latest.copy(
                    messages = replaceLastToolTurn(
                        latest.messages,
                        "Proposed local action: ${proposal.confirmationSummary}",
                    ),
                    operation = RuntimeOperation.READY,
                    lastGenerationMetrics = event.metrics,
                    performanceHistory = performance,
                    toolProposalCounters = proposalCounters,
                    pendingToolProposal = PendingToolProposal(
                        call = proposal.call,
                        summary = proposal.confirmationSummary,
                        risk = proposal.definition.risk,
                    ),
                    notice = "Nothing has run; review the model-proposed action",
                )
                is ToolCallParseResult.Rejected -> latest.copy(
                    messages = replaceLastToolTurn(
                        latest.messages,
                        "LAI rejected an invalid model-proposed action.",
                    ),
                    operation = RuntimeOperation.READY,
                    lastGenerationMetrics = event.metrics,
                    performanceHistory = performance,
                    toolProposalCounters = proposalCounters,
                    notice = proposal.message,
                )
            }
        }
        persistChat()
    }

    fun approvePendingTool() {
        val proposal = state.value.pendingToolProposal ?: return
        if (state.value.busy || !state.value.toolAuditIntegrityValid) return
        _state.update {
            it.copy(
                pendingToolProposal = null,
                operation = RuntimeOperation.AUTOMATING,
                notice = "Recording one-time approval before local execution…",
            )
        }
        viewModelScope.launch {
            val approval = container.toolAuditRepository.recordDecision(
                call = proposal.call,
                risk = proposal.risk,
                approved = true,
            )
            if (approval.isFailure) {
                val verified = container.toolAuditRepository.snapshot()
                val approvalError = approval.exceptionOrNull()
                if (approvalError != null) {
                    LaiLog.e("LAI-audit", "Tool approval audit failed: ${approvalError.message}", approvalError)
                } else {
                    LaiLog.e("LAI-audit", "Tool approval audit failed: unknown audit error")
                }
                val message = "Action not run because approval audit failed: " +
                    (approval.exceptionOrNull()?.message ?: "unknown audit error")
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(false, message, contextEligible = false),
                        operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                        notice = message,
                        toolProposalsEnabled = false,
                        toolAuditHistory = verified.records.takeLast(MAX_TOOL_AUDIT_RECORDS),
                        toolAuditIntegrityValid = verified.integrityValid,
                        toolAuditStatus = "$message • ${verified.detail}",
                    )
                }
                return@launch
            }
            val approvedSnapshot = approval.getOrThrow()
            _state.update {
                it.copy(
                    toolAuditHistory = approvedSnapshot.records.takeLast(MAX_TOOL_AUDIT_RECORDS),
                    toolAuditStatus = approvedSnapshot.detail,
                    notice = "Approval recorded; running the exact validated action once…",
                )
            }
            val result = container.agentRuntime.execute(proposal.call, userConfirmed = true)
            if (result.success) {
                LaiLog.i("LAI-agent", "Approved tool executed id=${proposal.call.name}")
            } else {
                LaiLog.w("LAI-agent", "Approved tool failed id=${proposal.call.name}: ${result.error?.message}")
            }
            val completion = container.toolAuditRepository.recordCompletion(
                call = proposal.call,
                risk = proposal.risk,
                success = result.success,
            )
            val completionSnapshot = completion.getOrNull()
            val resultText = if (result.success) {
                "Approved action completed locally: ${proposal.summary}"
            } else {
                "Approved action was not completed: ${result.error?.message ?: "tool failed"}"
            }
            val auditWarning = completion.exceptionOrNull()?.message
            if (auditWarning != null) {
                LaiLog.w("LAI-audit", "Tool audit completion failed: $auditWarning")
            }
            val verifiedAfterFailure = if (auditWarning != null) container.toolAuditRepository.snapshot() else null
            _state.update {
                it.copy(
                    messages = it.messages + ChatMessage(false, resultText, contextEligible = false),
                    operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                    notice = if (auditWarning == null) resultText else "$resultText • audit completion failed",
                    toolAuditHistory = (
                        completionSnapshot?.records ?: verifiedAfterFailure?.records ?: approvedSnapshot.records
                    ).takeLast(MAX_TOOL_AUDIT_RECORDS),
                    toolAuditStatus = completionSnapshot?.detail
                        ?: "Audit completion failed: $auditWarning • ${verifiedAfterFailure?.detail}",
                    toolProposalsEnabled = if (auditWarning == null) it.toolProposalsEnabled else false,
                    toolAuditIntegrityValid = completionSnapshot?.integrityValid
                        ?: verifiedAfterFailure?.integrityValid
                        ?: false,
                )
            }
        }
    }

    fun denyPendingTool() {
        val proposal = state.value.pendingToolProposal ?: return
        if (state.value.busy) return
        LaiLog.i("LAI-agent", "Tool proposal denied id=${proposal.call.name}")
        _state.update {
            it.copy(
                pendingToolProposal = null,
                messages = it.messages + ChatMessage(
                    fromUser = false,
                    text = "Proposed action was not run.",
                    contextEligible = false,
                ),
                notice = "Action denied; no Android authority was invoked",
            )
        }
        viewModelScope.launch {
            val denial = container.toolAuditRepository.recordDecision(
                call = proposal.call,
                risk = proposal.risk,
                approved = false,
            )
            val snapshot = denial.getOrNull()
            if (snapshot != null) {
                _state.update {
                    it.copy(
                        toolAuditHistory = snapshot.records.takeLast(MAX_TOOL_AUDIT_RECORDS),
                        toolAuditStatus = snapshot.detail,
                    )
                }
            } else {
                val error = denial.exceptionOrNull()
                val verified = container.toolAuditRepository.snapshot()
                _state.update {
                    it.copy(
                        toolProposalsEnabled = false,
                        toolAuditHistory = verified.records.takeLast(MAX_TOOL_AUDIT_RECORDS),
                        toolAuditIntegrityValid = verified.integrityValid,
                        toolAuditStatus = "Audit denial record failed: ${error?.message} • ${verified.detail}",
                        notice = "Action was denied and not run, but its audit record could not be stored",
                    )
                }
            }
        }
    }

    fun clearConversation() {
        if (state.value.busy || state.value.pendingToolProposal != null) return
        // An unspent "Apply once" override belongs to the conversation the user just discarded.
        workspace.discardOverride()
        // Every completed exchange was already persisted, so "New chat" only has to rotate the
        // session identity: the old conversation stays in history, the screen starts fresh.
        currentChatId = UUID.randomUUID().toString()
        currentChatCreatedAtEpochMs = System.currentTimeMillis()
        _state.update {
            it.copy(
                messages = it.messages.take(1),
                trimmedConversationTurns = 0,
                windowedConversationTurns = 0,
                lastGenerationMetrics = null,
                notice = "Conversation cleared locally",
            )
        }
    }

    /**
     * Persists the on-screen conversation into app-private no-backup history. Called after every
     * event that changes the transcript (reply completed, reply failed, canned notices), so a
     * process death never loses more than the message currently streaming.
     */
    private fun persistChat() {
        val current = state.value
        val stored = current.messages.drop(1) // the greeting is UI furniture, not conversation
            .filter { it.text.isNotBlank() }
            .map { StoredChatMessage(it.fromUser, it.text, System.currentTimeMillis()) }
        if (stored.none { it.fromUser }) return
        val session = StoredChatSession(
            id = currentChatId,
            title = stored.first { it.fromUser }.text.take(48),
            createdAtEpochMs = currentChatCreatedAtEpochMs,
            updatedAtEpochMs = System.currentTimeMillis(),
            messages = stored,
        )
        viewModelScope.launch {
            container.chatHistoryRepository.save(session)
            _state.update { it.copy(chatSessions = container.chatHistoryRepository.list()) }
        }
    }

    fun toggleChatHistory() {
        _state.update { it.copy(chatHistoryVisible = !it.chatHistoryVisible) }
        if (state.value.chatHistoryVisible) refreshChatSessions()
    }

    fun loadChatSession(id: String) {
        if (state.value.busy || state.value.pendingToolProposal != null) return
        viewModelScope.launch {
            val session = container.chatHistoryRepository.load(id)
            if (session == null) {
                _state.update { it.copy(notice = "Saved chat could not be opened", chatHistoryVisible = false) }
                return@launch
            }
            currentChatId = session.id
            currentChatCreatedAtEpochMs = session.createdAtEpochMs
            workspace.discardOverride()
            _state.update {
                it.copy(
                    messages = listOf(it.messages.first()) +
                        session.messages.map { message -> ChatMessage(message.fromUser, message.text) },
                    chatHistoryVisible = false,
                    trimmedConversationTurns = 0,
                    windowedConversationTurns = 0,
                    lastGenerationMetrics = null,
                    notice = "Chat restored — it continues locally from where it stopped",
                )
            }
        }
    }

    fun deleteChatSession(id: String) {
        viewModelScope.launch {
            container.chatHistoryRepository.delete(id)
            if (id == currentChatId) {
                // The on-screen transcript survives, but under a fresh identity so the deleted
                // session does not silently reappear at the next persisted reply.
                currentChatId = UUID.randomUUID().toString()
                currentChatCreatedAtEpochMs = System.currentTimeMillis()
            }
            _state.update { it.copy(chatSessions = container.chatHistoryRepository.list()) }
        }
    }

    private fun refreshChatSessions() {
        viewModelScope.launch {
            _state.update { it.copy(chatSessions = container.chatHistoryRepository.list()) }
        }
    }

    private suspend fun prepareConversation(config: GenerationConfig, keepLastTurns: Int): PreparedConversation {
        val current = state.value
        // Prefill is the dominant pre-first-token cost on the 4-thread CPU path, and the tool
        // instruction is its single biggest line item. It is only prepended when the latest user
        // message plausibly requests an Android action (ToolInstructionGate) — a plain "hi" now
        // carries no tool tokens at all. Authority is unchanged: proposals are still validated
        // twice and confirmed by the user before dispatch.
        val latestUserText = current.messages.lastOrNull { it.fromUser && it.contextEligible }?.text.orEmpty()
        val includeToolInstruction = current.toolProposalsEnabled &&
            ToolInstructionGate.shouldIncludeInstruction(latestUserText)
        // The user's rolling context window (⚙ "conversation memory") is applied to the history
        // BEFORE token counting: it is a deliberate bound the user chose, distinct from the
        // token-overflow trimming below, and both are reported separately in diagnostics.
        val history = current.messages
            .filter { it.contextEligible && it.text.isNotBlank() }
            .map {
                ConversationMessage(
                    role = if (it.fromUser) ConversationRole.USER else ConversationRole.ASSISTANT,
                    content = it.text,
                )
            }
        val windowed = ContextWindowPolicy.applyTurnWindow(history, keepLastTurns)
        val all = buildList {
            if (includeToolInstruction) {
                add(ConversationMessage(ConversationRole.SYSTEM, container.agentRuntime.modelToolInstruction))
            }
            addAll(windowed.messages)
        }.toMutableList()
        val protectedPrefix = if (includeToolInstruction) 1 else 0
        require(all.size > protectedPrefix) { "Conversation is empty" }
        var trimmed = 0
        while (true) {
            val tokenCount = container.inferenceEngine.countTokens(all).getOrThrow()
            if (tokenCount + config.maxNewTokens <= container.inferenceEngine.contextSize) {
                return PreparedConversation(all.toList(), trimmed, windowed.droppedTurns)
            }
            if (all.size <= protectedPrefix + 1) error("Current message is too large for the model context")
            all.removeAt(protectedPrefix)
            if (all.getOrNull(protectedPrefix)?.role == ConversationRole.ASSISTANT) {
                all.removeAt(protectedPrefix)
            }
            trimmed += 1
        }
    }

    /** Maps typed [LlmSettings] onto the runtime's [GenerationConfig], clamped to the live context. */
    private fun generationConfig(llm: LlmSettings): GenerationConfig {
        val ceiling = workspace.maxNewTokensCeiling(container.inferenceEngine.contextSize)
        return GenerationConfig(
            maxNewTokens = llm.maxNewTokens.coerceIn(1, ceiling),
            temperature = llm.temperature,
            topP = llm.topP,
            seed = llm.seed,
        )
    }

    private fun replaceLastToolTurn(messages: List<ChatMessage>, replacement: String): List<ChatMessage> {
        val updated = messages.toMutableList()
        val assistantIndex = updated.indexOfLast { !it.fromUser }
        if (assistantIndex >= 0) {
            updated[assistantIndex] = updated[assistantIndex].copy(text = replacement, contextEligible = false)
            val userIndex = assistantIndex - 1
            if (userIndex >= 0 && updated[userIndex].fromUser) {
                updated[userIndex] = updated[userIndex].copy(contextEligible = false)
            }
        }
        return updated
    }

    private fun markGenerationFailed(message: String) {
        LaiLog.e("LAI-llm", "Generation failed: $message")
        recordGenerationFailure(message)
        _state.update {
            val updated = it.messages.toMutableList()
            val last = updated.lastOrNull()
            if (last != null && !last.fromUser) {
                updated[updated.lastIndex] = last.copy(
                    text = last.text.ifBlank { "Inference failed: $message" },
                    contextEligible = false,
                )
                val userIndex = updated.lastIndex - 1
                if (userIndex >= 0 && updated[userIndex].fromUser) {
                    updated[userIndex] = updated[userIndex].copy(contextEligible = false)
                }
            }
            it.copy(messages = updated, operation = RuntimeOperation.ERROR, notice = message)
        }
        persistChat()
        // If an accelerator (GPU) was active and the native driver failed, reload the same model
        // on CPU so the next attempt works instead of failing on a broken driver. Device evidence
        // 2026-08-19: Adreno 825 -> vk::Device::createComputePipeline ErrorUnknown.
        fallbackToCpuAfterAcceleratorFailure(message)
    }

    /**
     * When generation fails on an accelerator backend (e.g. a Vulkan driver error), switch the
     * loaded model to the CPU backend so the app remains usable. Honest and fail-closed: no
     * acceleration is claimed until it is physically validated. Safe no-op on CPU.
     */
    private fun fallbackToCpuAfterAcceleratorFailure(message: String) {
        val scheduler = state.value.schedulerDetail.orEmpty()
        val acceleratorActive = scheduler.startsWith("LLAMA-VULKAN") || scheduler.contains("llama-vulkan") ||
            scheduler.startsWith("LLAMA-OPENCL") || scheduler.contains("llama-opencl")
        val backendFailure = message.contains("vk::", ignoreCase = true) ||
            message.contains("vulkan", ignoreCase = true) ||
            message.contains("opencl", ignoreCase = true) ||
            message.contains("pipeline", ignoreCase = true) ||
            message.contains("backend", ignoreCase = true) ||
            message.contains("device", ignoreCase = true) ||
            message.contains("accelerator", ignoreCase = true) ||
            message.contains("stall", ignoreCase = true)
        if (!acceleratorActive || !backendFailure) return
        val modelId = state.value.activeModelId ?: return
        val model = state.value.installedModels.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            LaiLog.w("LAI-llm", "Accelerator generation failed ($message); reloading $modelId on CPU")
            container.inferenceEngine.close()
            val file = container.modelRepository.resolve(model)
            val reloaded = container.inferenceEngine.load(file.absolutePath, BackendId("llama-cpu")).isSuccess
            _state.update {
                it.copy(
                    operation = if (reloaded) RuntimeOperation.READY else RuntimeOperation.ERROR,
                    activeModelId = if (reloaded) modelId else null,
                    schedulerDetail = if (reloaded) "LLAMA-CPU: switched after accelerator failure" else it.schedulerDetail,
                    notice = if (reloaded) {
                        "The GPU backend failed on this device; switched to CPU for the next attempt."
                    } else {
                        "The GPU backend failed and the CPU reload also failed."
                    },
                )
            }
            if (reloaded) {
                LaiLog.i("LAI-llm", "Accelerator -> CPU fallback complete for $modelId")
            }
        }
    }

    /**
     * Records why an attempt produced nothing, so a support export can explain a silent failure.
     * The reason is truncated and LAI-authored; prompts and model output are never stored.
     */
    private fun recordGenerationFailure(reason: String) {
        LaiLog.w("LAI-llm", "Generation recorded as failed: $reason")
        _state.update {
            it.copy(
                lastGenerationFailure = reason.take(MAX_FAILURE_REASON_CHARS),
                emptyGenerationCount = it.emptyGenerationCount + 1,
            )
        }
    }

    private fun markLastAssistantContextIneligible() {
        _state.update {
            val updated = it.messages.toMutableList()
            val last = updated.lastOrNull()
            if (last != null && !last.fromUser) {
                updated[updated.lastIndex] = last.copy(contextEligible = false)
                val userIndex = updated.lastIndex - 1
                if (userIndex >= 0 && updated[userIndex].fromUser) {
                    updated[userIndex] = updated[userIndex].copy(contextEligible = false)
                }
            }
            it.copy(messages = updated)
        }
    }

    private fun appendAssistantToken(token: String) {
        _state.update { current ->
            val messages = current.messages.toMutableList()
            val last = messages.lastOrNull()
            if (last != null && !last.fromUser) {
                messages[messages.lastIndex] = last.copy(text = last.text + token)
            }
            current.copy(messages = messages)
        }
    }

    fun openAccessibilitySettings() {
        getApplication<Application>().startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestShizuku() = container.shizukuController.requestPermission()

    fun inspectScreen() {
        if (state.value.busy) return
        _state.update { it.copy(operation = RuntimeOperation.AUTOMATING, notice = null) }
        viewModelScope.launch {
            val result = container.agentRuntime.execute(
                ToolCall(UUID.randomUUID().toString(), "screen.snapshot", buildJsonObject { }),
            )
            val notice = if (result.success) {
                val count = result.output["snapshot"]?.toString()?.length ?: 0
                "Screen structure captured safely ($count JSON characters)."
            } else {
                result.error?.message ?: "Screen inspection failed"
            }
            _state.update {
                it.copy(
                    operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                    notice = notice,
                )
            }
        }
    }

    fun readCurrentScreen() {
        if (state.value.busy) return
        _state.update { it.copy(operation = RuntimeOperation.READING_SCREEN, notice = null) }
        viewModelScope.launch {
            val result = container.agentRuntime.execute(
                ToolCall(UUID.randomUUID().toString(), "ocr.current_screen", buildJsonObject { }),
            )
            _state.update {
                it.copy(
                    operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                    notice = if (result.success) result.output.toString()
                    else result.error?.message ?: "OCR failed",
                )
            }
        }
    }

    fun loadModel(modelId: String, forcedBackend: BackendId? = null) {
        if (state.value.busy) return
        val model = state.value.installedModels.firstOrNull { it.id == modelId } ?: return
        LaiLog.i("LAI-model", "Loading model id=$modelId" + (forcedBackend?.let { " forcedBackend=${it.value}" } ?: ""))
        _state.update {
            it.copy(operation = RuntimeOperation.LOADING, notice = "Checking device resources for ${model.displayName}…")
        }
        viewModelScope.launch {
            val result = runCatching {
                val estimate = container.memoryEstimator.estimate(model.bytes, MODEL_CONTEXT_TOKENS)
                val runtime = container.inferenceEngine.capabilities
                val evidence = buildSet {
                    if (runtime.nativeLibraryLoaded) add(CapabilityEvidence.COMPILED)
                    if (runtime.nativeLibraryLoaded) add(CapabilityEvidence.RUNTIME_PROBED)
                }
                // DEVICE_VALIDATED is granted only to accelerators that have passed physical-device
                // qualification (see docs/device-results). The scheduler refuses every non-CPU
                // backend lacking this evidence, so GPU stays disabled until validated on hardware.
                // Enabled per build via -Plai.validatedAccelerators=llama-vulkan.
                val validatedAccelerators: Set<String> = BuildConfig.VALIDATED_ACCELERATORS
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toSet()
                val capabilities = runtime.compiledBackends.map { descriptor ->
                    val isAccelerator = descriptor.computeClass != ComputeClass.CPU
                    BackendCapability(
                        backend = descriptor.id,
                        computeClass = descriptor.computeClass,
                        supported = true,
                        evidence = if (isAccelerator && descriptor.id.value in validatedAccelerators) {
                            evidence + CapabilityEvidence.DEVICE_VALIDATED
                        } else {
                            evidence
                        },
                        estimatedPeakBytes = estimate.estimatedPeakBytes,
                        supportedModelFormats = descriptor.supportedModelFormats,
                        supportedQuantizations = descriptor.supportedQuantizations,
                        preference = descriptor.defaultPriority,
                    )
                }
                val profile = container.runtimeEnvironment.profile(capabilities)
                val reviewedModel = state.value.supportedModels.firstOrNull { it.id == model.id }
                val requiredBytes = maxOf(estimate.estimatedPeakBytes, reviewedModel?.estimatedPeakBytes ?: 0L)
                val file = container.modelRepository.resolve(model)
                val loadStarted = SystemClock.elapsedRealtime()
                if (forcedBackend != null) {
                    // Qualification-only path (see docs/TESTING.md "Backend qualification"):
                    // deliberately bypasses InferenceScheduler.select() and its CPU-first
                    // preference ranking so an accelerator can be device-tested before it is
                    // ever the catalog's preferred/fallback choice. Only reachable when the
                    // caller (MainActivity's qualification intent) already confirmed this
                    // backend is in BuildConfig.VALIDATED_ACCELERATORS for this build — normal
                    // signed releases ship with that set empty, so this path is inert there.
                    // load() itself fails loudly (Result.failure) if the backend is not
                    // actually compiled/available; it never silently substitutes CPU.
                    container.inferenceEngine.load(file.absolutePath, forcedBackend).getOrThrow()
                    ScheduledLoad(
                        backend = forcedBackend,
                        reason = "Forced backend override for accelerator qualification (scheduler bypassed)",
                        estimatedPeakBytes = requiredBytes,
                        environment = profile.environment,
                        loadMs = SystemClock.elapsedRealtime() - loadStarted,
                    )
                } else {
                    val decision = container.inferenceScheduler.select(
                        workload = InferenceWorkload(
                            estimatedRequiredBytes = requiredBytes,
                            modelFormat = reviewedModel?.modelFormat ?: "gguf",
                            quantization = reviewedModel?.quantization,
                            compatibleBackends = reviewedModel?.compatibleBackendIds
                                ?.map(::BackendId)
                                ?.toSet()
                                .orEmpty(),
                            backendPreference = reviewedModel?.let {
                                listOf(it.preferredBackendId) + it.fallbackBackendIds
                            }?.map(::BackendId).orEmpty(),
                            requiredAbis = reviewedModel?.requiredAbis?.toSet().orEmpty(),
                        ),
                        profile = profile,
                    )
                    container.inferenceEngine.load(file.absolutePath, decision.selected).getOrThrow()
                    ScheduledLoad(
                        backend = decision.selected,
                        reason = decision.reason,
                        estimatedPeakBytes = requiredBytes,
                        environment = profile.environment,
                        loadMs = SystemClock.elapsedRealtime() - loadStarted,
                    )
                }
            }
            result.onSuccess { load ->
                LaiLog.i(
                    "LAI-model",
                    "Model loaded id=$modelId backend=${load.backend.value} in ${load.loadMs} ms",
                )
                _state.update {
                    it.copy(
                        operation = RuntimeOperation.READY,
                        activeModelId = model.id,
                        schedulerDetail = "${load.backend.value.uppercase()}: ${load.reason}",
                        environmentDetail = environmentSummary(load.environment),
                        estimatedPeakBytes = load.estimatedPeakBytes,
                        lastModelLoadMs = load.loadMs,
                        notice = "${model.displayName} loaded locally in ${load.loadMs} ms on ${load.backend.value.uppercase()}",
                    )
                }
            }.onFailure { error ->
                LaiLog.e("LAI-model", "Model load failed id=$modelId: ${error.message}", error)
                _state.update {
                    it.copy(
                        operation = RuntimeOperation.ERROR,
                        activeModelId = null,
                        schedulerDetail = "Load rejected: ${error.message ?: "unknown reason"}",
                        notice = error.message ?: "Model loading failed",
                    )
                }
            }
        }
    }

    /**
     * Cheap, model-free diagnostic probe for the 2026-09-03 Hexagon/OpenCL shared-init hang
     * investigation (docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-v73.md). Reads
     * [InferenceEngine.capabilities] — which internally calls `available()` on every compiled
     * backend, including the one under test — and logs the result, without ever calling
     * [loadModel] or touching the native `createSession`/`open()` path. This isolates whether a
     * hang is in backend *enumeration* (this probe) or specifically in the heavier session/model
     * *load* path (see [runBackendQualification]), in well under a second if enumeration is
     * healthy, instead of waiting out a multi-minute qualification timeout to learn the same
     * thing. Triggered by `qualify_probe=true` alongside `qualify_backend` on the same
     * MainActivity intent extras qualification uses; independent of BuildConfig gating since it
     * never forces a load — reading capabilities is already always safe.
     */
    fun runBackendProbe(backendIdValue: String) {
        LaiLog.i("LAI-diag", "probe: ENTER backend=$backendIdValue")
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            val capabilities = container.inferenceEngine.capabilities
            val elapsedMs = SystemClock.elapsedRealtime() - start
            LaiLog.i(
                "LAI-diag",
                "probe: capabilities resolved in ${elapsedMs}ms nativeLibraryLoaded=" +
                    "${capabilities.nativeLibraryLoaded} detail=\"${capabilities.detail}\"",
            )
            if (capabilities.compiledBackends.isEmpty()) {
                LaiLog.i("LAI-diag", "probe: compiledBackends is EMPTY (no backend reported available)")
            }
            capabilities.compiledBackends.forEach { descriptor ->
                LaiLog.i(
                    "LAI-diag",
                    "probe: compiledBackends[] id=${descriptor.id.value} computeClass=${descriptor.computeClass} " +
                        "formats=${descriptor.supportedModelFormats} quantizations=${descriptor.supportedQuantizations}",
                )
            }
            val targetSeen = capabilities.compiledBackends.any { it.id.value == backendIdValue }
            LaiLog.i("LAI-diag", "probe: DONE backend=$backendIdValue reportedAvailable=$targetSeen totalMs=$elapsedMs")
        }
    }

    /**
     * ADB-first accelerator qualification entry point (docs/TESTING.md "Backend qualification").
     * Driven by an intent extra on the exported, always-present MainActivity launcher activity —
     * no UI navigation or coordinate taps required. Forces [modelId] onto [backendIdValue],
     * bypassing the scheduler's CPU-first preference, then runs one real generation with
     * [prompt] so a single `adb shell am start` produces observable, grep-able evidence
     * (`adb logcat -s LAI-qualify:* LAI-model:* LAI-llm:*`) of whether the backend actually
     * engaged, without ever touching production backend-selection behavior: this only executes
     * when [backendIdValue] is present in this build's BuildConfig.VALIDATED_ACCELERATORS,
     * which is empty on every ordinary signed release.
     */
    fun runBackendQualification(modelId: String, backendIdValue: String, prompt: String) {
        val validated = BuildConfig.VALIDATED_ACCELERATORS
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        if (backendIdValue !in validated) {
            LaiLog.w(
                "LAI-qualify",
                "DENIED model=$modelId backend=$backendIdValue — not in this build's " +
                    "VALIDATED_ACCELERATORS=$validated; rebuild with " +
                    "-Plai.validatedAccelerators=$backendIdValue to qualify it",
            )
            return
        }
        LaiLog.i("LAI-qualify", "STARTING model=$modelId backend=$backendIdValue")
        loadModel(modelId, forcedBackend = BackendId(backendIdValue))
        viewModelScope.launch {
            state.first { it.operation == RuntimeOperation.READY || it.operation == RuntimeOperation.ERROR }
            if (state.value.operation != RuntimeOperation.READY || state.value.activeModelId != modelId) {
                LaiLog.e(
                    "LAI-qualify",
                    "LOAD_FAILED model=$modelId backend=$backendIdValue detail=${state.value.schedulerDetail}",
                )
                return@launch
            }
            LaiLog.i("LAI-qualify", "LOAD_OK model=$modelId backend=$backendIdValue — sending qualification prompt")
            _state.update { it.copy(input = prompt) }
            sendMessage()
            state.first { it.operation == RuntimeOperation.READY || it.operation == RuntimeOperation.ERROR }
            LaiLog.i(
                "LAI-qualify",
                "DONE model=$modelId backend=$backendIdValue finalOperation=${state.value.operation} " +
                    "metrics=${state.value.lastGenerationMetrics}",
            )
        }
    }

    fun deleteModel(modelId: String) {
        if (state.value.busy) return
        if (state.value.activeModelId == modelId) {
            _state.update { it.copy(notice = "Unload the model before deleting it") }
            return
        }
        viewModelScope.launch {
            val removed = container.modelRepository.delete(modelId)
            LaiLog.i("LAI-model", "Model delete id=$modelId removed=$removed")
            _state.update {
                it.copy(notice = if (removed) "Model deleted from this device" else "Model was not found")
            }
            refreshModels()
        }
    }

    fun unloadModel() {
        LaiLog.i("LAI-model", "Model unload")
        container.inferenceEngine.close()
        _state.update {
            it.copy(
                operation = RuntimeOperation.IDLE,
                activeModelId = null,
                lastModelLoadMs = null,
                lastGenerationMetrics = null,
                notice = "Model unloaded",
            )
        }
    }

    fun installRecommendedModel() = installSupportedModel(state.value.recommendedModel.id)

    fun installSupportedModel(modelId: String) {
        val model = state.value.supportedModels.firstOrNull { it.id == modelId }
            ?: run {
                _state.update { it.copy(notice = "Supported model is no longer in the verified catalog") }
                return
            }
        if (state.value.installedModels.any { it.id == model.id }) {
            _state.update { it.copy(notice = "${model.displayName} is already installed") }
            return
        }
        startModelDownload(model.toModelSpec())
    }

    fun exportInstalledModel(modelId: String, destination: Uri) {
        if (state.value.busy) return
        val model = state.value.installedModels.firstOrNull { it.id == modelId }
            ?: run {
                _state.update { it.copy(notice = "Installed model was not found") }
                return
            }
        _state.update {
            it.copy(
                operation = RuntimeOperation.EXPORTING,
                notice = "Creating uninstall-safe model copy…",
                downloadProgress = DownloadProgress(0, model.bytes),
            )
        }
        viewModelScope.launch {
            container.modelRepository.exportModel(
                model = model,
                contentResolver = getApplication<Application>().contentResolver,
                destination = destination,
            ) { progress -> _state.update { it.copy(downloadProgress = progress) } }
                .onSuccess {
                    _state.update {
                        it.copy(
                            operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                            notice = "Retained GGUF copy verified. It will survive LAI uninstall.",
                            downloadProgress = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            operation = RuntimeOperation.ERROR,
                            notice = error.message ?: "Model export failed",
                            downloadProgress = null,
                        )
                    }
                }
        }
    }

    fun importRecommendedModel(uri: Uri) {
        if (state.value.busy) return
        val reviewed = state.value.recommendedModel
        _state.update {
            it.copy(
                operation = RuntimeOperation.IMPORTING,
                notice = "Verifying selected model locally…",
                downloadProgress = DownloadProgress(0, reviewed.bytes),
            )
        }
        viewModelScope.launch {
            val result = container.modelRepository.importModel(
                spec = reviewed.toImportSpec(),
                contentResolver = getApplication<Application>().contentResolver,
                uri = uri,
            ) { progress -> _state.update { it.copy(downloadProgress = progress) } }
            result.onSuccess { model ->
                _state.update {
                    it.copy(
                        operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                        notice = "Imported and verified ${model.displayName}",
                        downloadProgress = null,
                    )
                }
                refreshModels()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        operation = RuntimeOperation.ERROR,
                        notice = error.message ?: "Import failed",
                        downloadProgress = null,
                    )
                }
            }
        }
    }

    fun downloadModel() {
        val current = state.value
        val id = current.modelName.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
        startModelDownload(
            ModelSpec(
                id = id,
                displayName = current.modelName.trim(),
                url = current.modelUrl.trim(),
                sha256 = current.modelSha.trim().ifBlank { null },
            ),
        )
    }

    private fun startModelDownload(spec: ModelSpec) {
        if (state.value.busy) return
        _state.update {
            it.copy(
                operation = RuntimeOperation.DOWNLOADING,
                notice = null,
                downloadProgress = DownloadProgress(0, spec.expectedBytes),
                downloadingModelId = spec.id,
            )
        }
        // The download itself runs in WorkManager, so it survives app exit and process death;
        // interruptions resume from the last byte via HTTP Range on the .part staging file.
        // This ViewModel only OBSERVES the work and mirrors it into UI state.
        container.modelDownloadCoordinator.enqueue(spec)
        watchDownload(spec.id)
    }

    /** Pause keeps the resumable .part staging file; pressing Download again continues it. */
    fun pauseModelDownload() {
        val id = state.value.downloadingModelId ?: return
        container.modelDownloadCoordinator.stop(id)
        val resumedMb = (state.value.downloadProgress?.downloadedBytes ?: 0L) / 1_048_576
        _state.update {
            it.copy(notice = "Download paused — Download again resumes from $resumedMb MB")
        }
    }

    /** Cancel stops the work AND discards the partial bytes. */
    fun cancelModelDownload() {
        val id = state.value.downloadingModelId ?: return
        container.modelDownloadCoordinator.stop(id)
        viewModelScope.launch {
            container.modelRepository.discardPartial(id)
            _state.update { it.copy(notice = "Download cancelled and partial data removed") }
        }
    }

    /** Reattaches UI to any download that kept running after the app was last closed. */
    private fun adoptBackgroundDownloads() {
        viewModelScope.launch {
            val active = container.modelDownloadCoordinator.observeAll().first()
                .firstOrNull {
                    it.modelId.isNotBlank() &&
                        (it.state == BackgroundDownloadState.RUNNING || it.state == BackgroundDownloadState.ENQUEUED)
                } ?: return@launch
            _state.update {
                it.copy(
                    operation = RuntimeOperation.DOWNLOADING,
                    downloadProgress = active.progress ?: DownloadProgress(0, null),
                    downloadingModelId = active.modelId,
                    notice = "Resumed watching a download that continued in the background",
                )
            }
            watchDownload(active.modelId)
        }
    }

    private fun watchDownload(modelId: String) {
        downloadWatchJob?.cancel()
        downloadWatchJob = viewModelScope.launch {
            container.modelDownloadCoordinator.observe(modelId).collect { status ->
                when (status?.state) {
                    null -> Unit
                    BackgroundDownloadState.ENQUEUED -> _state.update {
                        it.copy(operation = RuntimeOperation.DOWNLOADING, downloadingModelId = modelId)
                    }
                    BackgroundDownloadState.RUNNING -> _state.update {
                        it.copy(
                            operation = RuntimeOperation.DOWNLOADING,
                            downloadingModelId = modelId,
                            downloadProgress = status.progress ?: it.downloadProgress,
                        )
                    }
                    BackgroundDownloadState.SUCCEEDED -> {
                        LaiLog.i("LAI-download", "Model download succeeded id=$modelId")
                        _state.update {
                            it.copy(
                                operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                                notice = "Model installed and verified",
                                modelName = "",
                                modelUrl = "",
                                modelSha = "",
                                downloadProgress = null,
                                downloadingModelId = null,
                            )
                        }
                        refreshModels()
                        downloadWatchJob?.cancel()
                    }
                    BackgroundDownloadState.FAILED -> {
                        LaiLog.e("LAI-download", "Model download failed id=$modelId: ${status.failureReason ?: "unknown reason"}")
                        _state.update {
                            it.copy(
                                operation = RuntimeOperation.ERROR,
                                notice = status.failureReason ?: "Download failed",
                                downloadProgress = null,
                                downloadingModelId = null,
                            )
                        }
                        downloadWatchJob?.cancel()
                    }
                    BackgroundDownloadState.CANCELLED -> {
                        // Paused or cancelled by explicit user action; that action already set
                        // the explanatory notice, so only the operational state is restored.
                        LaiLog.i("LAI-download", "Model download cancelled id=$modelId")
                        _state.update {
                            it.copy(
                                operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                                downloadProgress = null,
                                downloadingModelId = null,
                            )
                        }
                        downloadWatchJob?.cancel()
                    }
                }
            }
        }
    }

    fun exportDiagnostics(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val report = buildDiagnosticsReport()
                val json = DIAGNOSTICS_JSON.encodeToString(DiagnosticsReportV1.serializer(), report)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                        output.flush()
                    } ?: error("Android could not open the selected export destination")
                }
            }
            result.onSuccess {
                LaiLog.i("LAI-app", "Diagnostics JSON exported")
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Diagnostics JSON exported by explicit user action",
                        notice = "Privacy-filtered diagnostics exported",
                    )
                }
            }.onFailure { error ->
                LaiLog.e("LAI-app", "Diagnostics export failed: ${error.message}", error)
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Last export failed",
                        notice = error.message ?: "Diagnostics export failed",
                    )
                }
            }
        }
    }

    /**
     * Exports the centralized diagnostic log (redacted) to a user-chosen SAF destination, so a
     * signed release build can be debugged on a real device without adb/root access.
     */
    fun exportLogFile(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = LaiLog.exportText()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(text.toByteArray(Charsets.UTF_8))
                        output.flush()
                    } ?: error("Android could not open the selected export destination")
                }
            }
            result.onSuccess {
                LaiLog.i("LAI-app", "Diagnostic log exported")
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Diagnostic log exported by explicit user action",
                        notice = "Diagnostic log exported (secrets redacted)",
                    )
                }
            }.onFailure { error ->
                LaiLog.e("LAI-app", "Diagnostic log export failed: ${error.message}", error)
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Last log export failed",
                        notice = error.message ?: "Diagnostic log export failed",
                    )
                }
            }
        }
    }

    private fun buildDiagnosticsReport(): DiagnosticsReportV1 {
        val current = state.value
        val profile = container.runtimeEnvironment.profile(emptyList())
        val environment = profile.environment
        val capabilities = container.inferenceEngine.capabilities
        val shizuku = when (val value = current.shizukuState) {
            ShizukuState.Unavailable -> "UNAVAILABLE"
            ShizukuState.PermissionRequired -> "PERMISSION_REQUIRED"
            is ShizukuState.Ready -> "READY_UID_${value.uid}"
            is ShizukuState.Error -> "ERROR"
        }
        return DiagnosticsReportV1(
            generatedAtEpochMs = System.currentTimeMillis(),
            app = AppDiagnostics(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                productionSigned = BuildConfig.PRODUCTION_SIGNED,
                operation = current.operation.name,
                catalogStatus = current.catalogStatus,
            ),
            device = DeviceDiagnostics(
                manufacturer = profile.manufacturer,
                model = profile.model,
                androidSdk = profile.androidSdk,
                supportedAbis = profile.supportedAbis,
                availableMemoryBytes = environment.availableMemoryBytes,
                batteryPercent = environment.batteryPercent,
                charging = environment.charging,
                thermalState = environment.thermalState.name,
                socManufacturer = profile.socManufacturer,
                socModel = profile.socModel,
                cpuCoreCount = profile.cpuCoreCount,
            ),
            runtime = RuntimeDiagnostics(
                nativeLibraryLoaded = capabilities.nativeLibraryLoaded,
                compiledBackends = capabilities.compiledBackends.map { it.id.value }.sorted(),
                activeBackendDecision = current.schedulerDetail,
                contextSize = container.inferenceEngine.contextSize,
                activeModelId = current.activeModelId,
                modelLoadMs = current.lastModelLoadMs,
                estimatedPeakBytes = current.estimatedPeakBytes,
                accessibilityConnected = current.accessibilityConnected,
                shizukuState = shizuku,
                trimmedConversationTurns = current.trimmedConversationTurns,
                windowedConversationTurns = current.windowedConversationTurns,
                lastGenerationFailure = current.lastGenerationFailure,
                emptyGenerationCount = current.emptyGenerationCount,
            ),
            models = current.installedModels.map { model ->
                ModelDiagnostics(
                    id = model.id,
                    displayName = model.displayName,
                    bytes = model.bytes,
                    sha256 = model.sha256,
                    active = model.id == current.activeModelId,
                )
            },
            performance = current.performanceHistory.map { metrics -> metrics.toDiagnostics() },
            privacy = DiagnosticsPrivacy(),
            logs = LaiLog.recentEntries().map { entry ->
                LogEntryDiagnostics(
                    timestampEpochMs = entry.timestampEpochMs,
                    level = entry.level,
                    tag = entry.tag,
                    message = entry.message,
                )
            },
            automation = AutomationDiagnostics(
                toolProposalsEnabled = current.toolProposalsEnabled,
                proposalResponsesExamined = current.toolProposalCounters.responsesExamined,
                proposalAccepted = current.toolProposalCounters.accepted,
                proposalRejected = current.toolProposalCounters.rejected,
                proposalNotToolCall = current.toolProposalCounters.notToolCall,
                lastProposalOutcome = current.toolProposalCounters.lastOutcome,
                proposalRejectionCodes = current.toolProposalCounters.rejectionCodes,
                auditIntegrityValid = current.toolAuditIntegrityValid,
                records = current.toolAuditHistory.map { record ->
                    ToolAuditDiagnostics(
                        toolName = record.toolName,
                        risk = record.risk.name,
                        userApproved = record.outcome != ToolAuditOutcome.USER_DENIED,
                        success = when (record.outcome) {
                            ToolAuditOutcome.EXECUTION_SUCCEEDED -> true
                            ToolAuditOutcome.EXECUTION_FAILED -> false
                            ToolAuditOutcome.USER_APPROVED,
                            ToolAuditOutcome.USER_DENIED -> null
                        },
                        timestampEpochMs = record.timestampEpochMs,
                    )
                },
            ),
        )
    }

    private fun GenerationMetrics.toDiagnostics() = GenerationPerformanceDiagnostics(
        promptTokens = promptTokens,
        generatedTokens = generatedTokens,
        promptEvaluationMs = promptEvaluationMs,
        timeToFirstTokenMs = timeToFirstTokenMs,
        decodeMs = decodeMs,
        totalMs = totalMs,
        promptTokensPerSecond = promptTokensPerSecond,
        decodeTokensPerSecond = decodeTokensPerSecond,
        evaluatedPromptTokens = evaluatedPromptTokens,
    )

    private fun refreshModels() {
        viewModelScope.launch {
            val models = container.modelRepository.list()
            _state.update {
                it.copy(
                    installedModels = models,
                    operation = when {
                        it.activeModelId != null -> RuntimeOperation.READY
                        models.isEmpty() -> RuntimeOperation.NO_MODEL
                        it.operation == RuntimeOperation.ERROR -> RuntimeOperation.ERROR
                        else -> RuntimeOperation.IDLE
                    },
                )
            }
        }
    }

    override fun onCleared() {
        container.inferenceEngine.close()
        super.onCleared()
    }

    companion object {
        private const val MODEL_CONTEXT_TOKENS = 4096

        /**
         * How long Stop waits for the native call to yield before returning the UI to the user.
         *
         * This must exceed a realistic prompt-prefill time, not just a token gap. On this device
         * (SM8735, 4 worker threads, Qwen 1.5B Q4_K_M) a prompt of ~400 tokens - which is what a
         * short message becomes once the bilingual system prompt and, when enabled, the ~314-token
         * tool instruction are prepended - needs roughly 7-27 s of prefill before the first token
         * exists. The previous 4 s budget expired during normal prefill, so the watchdog reported a
         * stall and abandoned a perfectly healthy generation (device reports 0.6.83 / 0.6.84).
         */
        private const val CANCEL_GRACE_MS = 45_000L
        private const val MAX_PERFORMANCE_SAMPLES = 20
        private const val MAX_FAILURE_REASON_CHARS = 512
        private const val MAX_TOOL_AUDIT_RECORDS = 50
        private val DIAGNOSTICS_JSON = Json(LaiJson) { prettyPrint = true }
    }
}
