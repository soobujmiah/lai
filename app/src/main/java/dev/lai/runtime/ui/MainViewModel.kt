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
import dev.lai.runtime.diagnostics.AppDiagnostics
import dev.lai.runtime.diagnostics.AutomationDiagnostics
import dev.lai.runtime.diagnostics.DeviceDiagnostics
import dev.lai.runtime.diagnostics.DiagnosticsPrivacy
import dev.lai.runtime.diagnostics.DiagnosticsReportV1
import dev.lai.runtime.diagnostics.GenerationPerformanceDiagnostics
import dev.lai.runtime.diagnostics.ModelDiagnostics
import dev.lai.runtime.diagnostics.RuntimeDiagnostics
import dev.lai.runtime.diagnostics.ToolAuditDiagnostics
import dev.lai.runtime.automation.AccessibilityGateway
import dev.lai.runtime.history.ChatSessionSummary
import dev.lai.runtime.history.StoredChatMessage
import dev.lai.runtime.history.StoredChatSession
import dev.lai.runtime.inference.BackgroundDownloadState
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
                _state.update { it.copy(accessibilityConnected = accessibility, shizukuState = shizuku) }
            }
        }
        viewModelScope.launch {
            container.events.collect { event ->
                when (event) {
                    is AppRuntimeEvent.ModelUnloadedForMemory -> {
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
            workspace.state.collect { workspaceState ->
                _state.update { it.copy(workspace = workspaceState) }
            }
        }
        loadToolAudit()
        importBundledModels()
        refreshModels()
        loadCachedCatalog()
        refreshWorkspace()
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
            val notice = granted.fold(
                onSuccess = { "Workspace folder connected • $status" },
                onFailure = { error ->
                    "Workspace folder was not connected: " +
                        (error.message ?: "permission could not be kept")
                },
            )
            _state.update { it.copy(notice = notice) }
        }
    }

    /** Releases the persistable permission. Nothing inside the user's folder is deleted. */
    fun revokeWorkspace() {
        viewModelScope.launch {
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

    /** Registers workspace model metadata only; never copies weights and never auto-loads. */
    fun scanWorkspaceModels() {
        viewModelScope.launch {
            val reviewed = state.value.supportedModels.associate { it.sha256.lowercase() to it.id }
            val message = workspace.discoverModels(reviewed)
            _state.update { it.copy(notice = message) }
        }
    }

    fun setMode(mode: UiMode) = _state.update { it.copy(mode = mode, settingsVisible = false, notice = null) }
    fun setInput(value: String) = _state.update { it.copy(input = value) }
    fun toggleSettings() = _state.update { it.copy(settingsVisible = !it.settingsVisible) }
    fun setDeveloperMode(enabled: Boolean) = _state.update { it.copy(developerMode = enabled) }
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

    private fun importBundledModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dev.lai.runtime.inference.BundledModelImporter.importIfNeeded(getApplication())
                refreshModels()
            } catch (_: Exception) { }
        }
    }

    private fun loadToolAudit() {
        viewModelScope.launch {
            val snapshot = container.toolAuditRepository.snapshot()
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
            }
        }
    }

    /** Restores a usable chat state after a stopped generation, dropping the empty reply bubble. */
    private fun restoreAfterStoppedGeneration(message: String) {
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
    }

    /**
     * Records why an attempt produced nothing, so a support export can explain a silent failure.
     * The reason is truncated and LAI-authored; prompts and model output are never stored.
     */
    private fun recordGenerationFailure(reason: String) {
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

    fun loadModel(modelId: String) {
        if (state.value.busy) return
        val model = state.value.installedModels.firstOrNull { it.id == modelId } ?: return
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
                val capabilities = runtime.compiledBackends.map { descriptor ->
                    BackendCapability(
                        backend = descriptor.id,
                        computeClass = descriptor.computeClass,
                        supported = true,
                        evidence = evidence,
                        estimatedPeakBytes = estimate.estimatedPeakBytes,
                        supportedModelFormats = descriptor.supportedModelFormats,
                        supportedQuantizations = descriptor.supportedQuantizations,
                        preference = descriptor.defaultPriority,
                    )
                }
                val profile = container.runtimeEnvironment.profile(capabilities)
                val reviewedModel = state.value.supportedModels.firstOrNull { it.id == model.id }
                val requiredBytes = maxOf(estimate.estimatedPeakBytes, reviewedModel?.estimatedPeakBytes ?: 0L)
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
                val file = container.modelRepository.resolve(model)
                val loadStarted = SystemClock.elapsedRealtime()
                container.inferenceEngine.load(file.absolutePath, decision.selected).getOrThrow()
                ScheduledLoad(
                    backend = decision.selected,
                    reason = decision.reason,
                    estimatedPeakBytes = requiredBytes,
                    environment = profile.environment,
                    loadMs = SystemClock.elapsedRealtime() - loadStarted,
                )
            }
            result.onSuccess { load ->
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

    fun deleteModel(modelId: String) {
        if (state.value.busy) return
        if (state.value.activeModelId == modelId) {
            _state.update { it.copy(notice = "Unload the model before deleting it") }
            return
        }
        viewModelScope.launch {
            val removed = container.modelRepository.delete(modelId)
            _state.update {
                it.copy(notice = if (removed) "Model deleted from this device" else "Model was not found")
            }
            refreshModels()
        }
    }

    fun unloadModel() {
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
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Diagnostics JSON exported by explicit user action",
                        notice = "Privacy-filtered diagnostics exported",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        diagnosticsStatus = "Last export failed",
                        notice = error.message ?: "Diagnostics export failed",
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
        private const val MAX_FAILURE_REASON_CHARS = 200
        private const val MAX_TOOL_AUDIT_RECORDS = 50
        private val DIAGNOSTICS_JSON = Json(LaiJson) { prettyPrint = true }
    }
}
