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

@Serializable
enum class ConversationRole { SYSTEM, USER, ASSISTANT }

@Serializable
data class ConversationMessage(
    val role: ConversationRole,
    val content: String,
)

data class GenerationMetrics(
    val promptTokens: Int,
    val generatedTokens: Int,
    val promptEvaluationMs: Long,
    val timeToFirstTokenMs: Long,
    val decodeMs: Long,
    val totalMs: Long,
) {
    val promptTokensPerSecond: Double =
        if (promptEvaluationMs > 0) promptTokens * 1000.0 / promptEvaluationMs else 0.0
    val decodeTokensPerSecond: Double =
        if (decodeMs > 0) generatedTokens * 1000.0 / decodeMs else 0.0
}

sealed interface InferenceEvent {
    data class Token(val text: String) : InferenceEvent
    data class Completed(
        val tokensGenerated: Int,
        val metrics: GenerationMetrics? = null,
    ) : InferenceEvent
    data class Failed(val message: String) : InferenceEvent
}

data class RuntimeCapabilities(
    val nativeLibraryLoaded: Boolean,
    val compiledBackends: Set<InferenceBackend>,
    val detail: String,
)
