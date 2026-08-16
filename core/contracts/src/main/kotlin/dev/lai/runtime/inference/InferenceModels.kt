package dev.lai.runtime.inference

import kotlinx.serialization.Serializable

@Serializable
enum class InferenceBackend { AUTO, CPU, VULKAN, QNN }

@Serializable
data class GenerationConfig(
    val maxNewTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val seed: Long = -1,
)

sealed interface InferenceEvent {
    data class Token(val text: String) : InferenceEvent
    data class Completed(val tokensGenerated: Int) : InferenceEvent
    data class Failed(val message: String) : InferenceEvent
}

data class RuntimeCapabilities(
    val nativeLibraryLoaded: Boolean,
    val compiledBackends: Set<InferenceBackend>,
    val detail: String,
)
