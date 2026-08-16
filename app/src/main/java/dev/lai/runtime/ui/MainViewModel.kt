package dev.lai.runtime.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lai.runtime.LaiApplication
import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.core.AppRuntimeEvent
import dev.lai.runtime.automation.AccessibilityGateway
import dev.lai.runtime.inference.ConversationMessage
import dev.lai.runtime.inference.ConversationRole
import dev.lai.runtime.inference.DownloadProgress
import dev.lai.runtime.inference.GenerationConfig
import dev.lai.runtime.inference.GenerationMetrics
import dev.lai.runtime.inference.InferenceBackend
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
import dev.lai.runtime.shell.ShizukuState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

enum class UiMode { CHAT, SCREEN_READER, AUTOMATOR }

enum class RuntimeOperation {
    NO_MODEL,
    IDLE,
    DOWNLOADING,
    IMPORTING,
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
)

private data class ScheduledLoad(
    val backend: InferenceBackend,
    val reason: String,
    val estimatedPeakBytes: Long,
    val environment: RuntimeEnvironment,
    val loadMs: Long,
)

private data class PreparedConversation(
    val messages: List<ConversationMessage>,
    val trimmedTurns: Int,
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
    val notice: String? = null,
    val runtimeDetail: String = "",
    val schedulerDetail: String = "No model has been scheduled",
    val environmentDetail: String = "",
    val estimatedPeakBytes: Long? = null,
    val lastModelLoadMs: Long? = null,
    val lastGenerationMetrics: GenerationMetrics? = null,
    val trimmedConversationTurns: Int = 0,
) {
    val busy: Boolean
        get() = operation in setOf(
            RuntimeOperation.DOWNLOADING,
            RuntimeOperation.IMPORTING,
            RuntimeOperation.LOADING,
            RuntimeOperation.GENERATING,
            RuntimeOperation.CANCELLING,
            RuntimeOperation.READING_SCREEN,
            RuntimeOperation.AUTOMATING,
        )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaiApplication).container
    private var generationJob: Job? = null
    private val _state = MutableStateFlow(
        MainUiState(
            runtimeDetail = container.inferenceEngine.capabilities.detail,
            environmentDetail = environmentSummary(container.runtimeEnvironment.snapshot()),
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
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
        refreshModels()
        loadCachedCatalog()
    }

    fun setMode(mode: UiMode) = _state.update { it.copy(mode = mode, settingsVisible = false, notice = null) }
    fun setInput(value: String) = _state.update { it.copy(input = value) }
    fun toggleSettings() = _state.update { it.copy(settingsVisible = !it.settingsVisible) }
    fun setDeveloperMode(enabled: Boolean) = _state.update { it.copy(developerMode = enabled) }
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
        if (prompt.isEmpty() || state.value.busy) return
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
            return
        }

        _state.update {
            it.copy(
                input = "",
                messages = it.messages + ChatMessage(true, prompt) + ChatMessage(false, ""),
                operation = RuntimeOperation.GENERATING,
                lastGenerationMetrics = null,
                notice = null,
            )
        }
        generationJob = viewModelScope.launch {
            try {
                val prepared = prepareConversation(GENERATION_CONFIG)
                _state.update { it.copy(trimmedConversationTurns = prepared.trimmedTurns) }
                container.inferenceEngine.generate(prepared.messages, GENERATION_CONFIG).collect { event ->
                    when (event) {
                        is InferenceEvent.Token -> appendAssistantToken(event.text)
                        is InferenceEvent.Completed -> _state.update {
                            it.copy(
                                operation = RuntimeOperation.READY,
                                lastGenerationMetrics = event.metrics,
                                notice = "Generated ${event.tokensGenerated} tokens locally",
                            )
                        }
                        is InferenceEvent.Failed -> markGenerationFailed(event.message)
                    }
                }
            } catch (cancelled: CancellationException) {
                markLastAssistantContextIneligible()
                _state.update {
                    it.copy(
                        operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                        notice = if (it.activeModelId != null) "Generation stopped locally" else it.notice,
                    )
                }
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
        generationJob?.cancel(CancellationException("User stopped generation"))
    }

    fun clearConversation() {
        if (state.value.busy) return
        _state.update {
            it.copy(
                messages = it.messages.take(1),
                trimmedConversationTurns = 0,
                lastGenerationMetrics = null,
                notice = "Conversation cleared locally",
            )
        }
    }

    private suspend fun prepareConversation(config: GenerationConfig): PreparedConversation {
        val all = state.value.messages
            .filter { it.contextEligible && it.text.isNotBlank() }
            .map {
                ConversationMessage(
                    role = if (it.fromUser) ConversationRole.USER else ConversationRole.ASSISTANT,
                    content = it.text,
                )
            }
            .toMutableList()
        require(all.isNotEmpty()) { "Conversation is empty" }
        var trimmed = 0
        while (true) {
            val tokenCount = container.inferenceEngine.countTokens(all).getOrThrow()
            if (tokenCount + config.maxNewTokens <= container.inferenceEngine.contextSize) {
                return PreparedConversation(all.toList(), trimmed)
            }
            if (all.size <= 1) error("Current message is too large for the model context")
            all.removeAt(0)
            if (all.firstOrNull()?.role == ConversationRole.ASSISTANT) all.removeAt(0)
            trimmed += 1
        }
    }

    private fun markGenerationFailed(message: String) {
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
                val environment = container.runtimeEnvironment.snapshot()
                val runtime = container.inferenceEngine.capabilities
                val evidence = buildSet {
                    if (runtime.nativeLibraryLoaded) add(CapabilityEvidence.COMPILED)
                    if (runtime.nativeLibraryLoaded) add(CapabilityEvidence.RUNTIME_PROBED)
                }
                val capabilities = runtime.compiledBackends
                    .filter { it != InferenceBackend.AUTO }
                    .map { backend ->
                        BackendCapability(
                            backend = backend,
                            supported = true,
                            evidence = evidence,
                            estimatedPeakBytes = estimate.estimatedPeakBytes,
                        )
                    }
                val decision = container.inferenceScheduler.select(
                    workload = InferenceWorkload(estimate.estimatedPeakBytes),
                    environment = environment,
                    capabilities = capabilities,
                )
                val file = container.modelRepository.resolve(model)
                val loadStarted = SystemClock.elapsedRealtime()
                container.inferenceEngine.load(file.absolutePath, decision.selected).getOrThrow()
                ScheduledLoad(
                    backend = decision.selected,
                    reason = decision.reason,
                    estimatedPeakBytes = estimate.estimatedPeakBytes,
                    environment = environment,
                    loadMs = SystemClock.elapsedRealtime() - loadStarted,
                )
            }
            result.onSuccess { load ->
                _state.update {
                    it.copy(
                        operation = RuntimeOperation.READY,
                        activeModelId = model.id,
                        schedulerDetail = "${load.backend.name}: ${load.reason}",
                        environmentDetail = environmentSummary(load.environment),
                        estimatedPeakBytes = load.estimatedPeakBytes,
                        lastModelLoadMs = load.loadMs,
                        notice = "${model.displayName} loaded locally in ${load.loadMs} ms on ${load.backend.name}",
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
            )
        }
        viewModelScope.launch {
            val result = container.modelRepository.download(spec) { progress ->
                _state.update { it.copy(downloadProgress = progress) }
            }
            result.onSuccess { model ->
                _state.update {
                    it.copy(
                        operation = if (it.activeModelId != null) RuntimeOperation.READY else RuntimeOperation.IDLE,
                        notice = "Installed ${model.displayName}",
                        modelName = "",
                        modelUrl = "",
                        modelSha = "",
                        downloadProgress = null,
                    )
                }
                refreshModels()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        operation = RuntimeOperation.ERROR,
                        notice = error.message ?: "Download failed",
                        downloadProgress = null,
                    )
                }
            }
        }
    }

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
        private val GENERATION_CONFIG = GenerationConfig(maxNewTokens = 256)
    }
}
