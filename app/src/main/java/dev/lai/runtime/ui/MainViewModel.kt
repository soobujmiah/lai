package dev.lai.runtime.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.lai.runtime.LaiApplication
import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.automation.AccessibilityGateway
import dev.lai.runtime.inference.DownloadProgress
import dev.lai.runtime.inference.InferenceBackend
import dev.lai.runtime.inference.InferenceEvent
import dev.lai.runtime.inference.InstalledModel
import dev.lai.runtime.inference.ModelSpec
import dev.lai.runtime.shell.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

enum class UiMode { CHAT, SCREEN_READER, AUTOMATOR }

data class ChatMessage(val fromUser: Boolean, val text: String)

data class MainUiState(
    val mode: UiMode = UiMode.CHAT,
    val input: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(false, "আসসালামু আলাইকুম — আমি LAI। একটি লোকাল মডেল যুক্ত হলে বাংলা বা English-এ সাহায্য করতে পারব।"),
    ),
    val busy: Boolean = false,
    val accessibilityConnected: Boolean = false,
    val shizukuState: ShizukuState = ShizukuState.Unavailable,
    val developerMode: Boolean = false,
    val settingsVisible: Boolean = false,
    val installedModels: List<InstalledModel> = emptyList(),
    val activeModelId: String? = null,
    val modelName: String = "",
    val modelUrl: String = "",
    val modelSha: String = "",
    val downloadProgress: DownloadProgress? = null,
    val notice: String? = null,
    val runtimeDetail: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaiApplication).container
    private val _state = MutableStateFlow(
        MainUiState(runtimeDetail = container.inferenceEngine.capabilities.detail),
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
        refreshModels()
    }

    fun setMode(mode: UiMode) = _state.update { it.copy(mode = mode, settingsVisible = false, notice = null) }
    fun setInput(value: String) = _state.update { it.copy(input = value) }
    fun toggleSettings() = _state.update { it.copy(settingsVisible = !it.settingsVisible) }
    fun setDeveloperMode(enabled: Boolean) = _state.update { it.copy(developerMode = enabled) }
    fun setModelName(value: String) = _state.update { it.copy(modelName = value) }
    fun setModelUrl(value: String) = _state.update { it.copy(modelUrl = value) }
    fun setModelSha(value: String) = _state.update { it.copy(modelSha = value) }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun sendMessage() {
        val prompt = state.value.input.trim()
        if (prompt.isEmpty() || state.value.busy) return
        val immediateMessage = when {
            state.value.installedModels.isEmpty() ->
                "লোকাল মডেল এখনো ইনস্টল করা নেই। Settings → Developer mode থেকে একটি GGUF মডেল যোগ করুন।"
            state.value.activeModelId == null ->
                "মডেল ইনস্টল করা আছে। Settings → Developer mode থেকে মডেলটি Load করুন।"
            else -> null
        }
        if (immediateMessage != null) {
            _state.update {
                it.copy(
                    input = "",
                    messages = it.messages + ChatMessage(true, prompt) + ChatMessage(false, immediateMessage),
                )
            }
            return
        }

        _state.update {
            it.copy(
                input = "",
                messages = it.messages + ChatMessage(true, prompt) + ChatMessage(false, ""),
                busy = true,
            )
        }
        viewModelScope.launch {
            container.inferenceEngine.generate(prompt).collect { event ->
                when (event) {
                    is InferenceEvent.Token -> appendAssistantToken(event.text)
                    is InferenceEvent.Completed -> _state.update {
                        it.copy(busy = false, notice = "Generated ${event.tokensGenerated} tokens locally")
                    }
                    is InferenceEvent.Failed -> _state.update {
                        val updated = it.messages.toMutableList()
                        if (updated.lastOrNull()?.fromUser == false && updated.last().text.isBlank()) {
                            updated[updated.lastIndex] = ChatMessage(false, "Inference failed: ${event.message}")
                        }
                        it.copy(messages = updated, busy = false, notice = event.message)
                    }
                }
            }
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
        _state.update { it.copy(busy = true, notice = null) }
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
            _state.update { it.copy(busy = false, notice = notice) }
        }
    }

    fun readCurrentScreen() {
        if (state.value.busy) return
        _state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val result = container.agentRuntime.execute(
                ToolCall(UUID.randomUUID().toString(), "ocr.current_screen", buildJsonObject { }),
            )
            _state.update {
                it.copy(
                    busy = false,
                    notice = if (result.success) result.output.toString()
                    else result.error?.message ?: "OCR failed",
                )
            }
        }
    }

    fun loadModel(modelId: String) {
        if (state.value.busy) return
        val model = state.value.installedModels.firstOrNull { it.id == modelId } ?: return
        _state.update { it.copy(busy = true, notice = "Loading ${model.displayName}…") }
        viewModelScope.launch {
            val result = runCatching { container.modelRepository.resolve(model) }
                .mapCatching { file ->
                    container.inferenceEngine.load(file.absolutePath, InferenceBackend.CPU).getOrThrow()
                }
            result.onSuccess {
                _state.update {
                    it.copy(
                        busy = false,
                        activeModelId = model.id,
                        notice = "${model.displayName} is ready for local chat",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = false,
                        activeModelId = null,
                        notice = error.message ?: "Model loading failed",
                    )
                }
            }
        }
    }

    fun unloadModel() {
        container.inferenceEngine.close()
        _state.update { it.copy(activeModelId = null, notice = "Model unloaded") }
    }

    fun downloadModel() {
        val current = state.value
        if (current.busy) return
        val id = current.modelName.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
        _state.update { it.copy(busy = true, notice = null, downloadProgress = DownloadProgress(0, null)) }
        viewModelScope.launch {
            val result = container.modelRepository.download(
                ModelSpec(
                    id = id,
                    displayName = current.modelName.trim(),
                    url = current.modelUrl.trim(),
                    sha256 = current.modelSha.trim().ifBlank { null },
                ),
            ) { progress -> _state.update { it.copy(downloadProgress = progress) } }
            result.onSuccess { model ->
                _state.update {
                    it.copy(
                        busy = false,
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
                    it.copy(busy = false, notice = error.message ?: "Download failed", downloadProgress = null)
                }
            }
        }
    }

    private fun refreshModels() {
        viewModelScope.launch {
            _state.update { it.copy(installedModels = container.modelRepository.list()) }
        }
    }

    override fun onCleared() {
        container.inferenceEngine.close()
        super.onCleared()
    }
}
