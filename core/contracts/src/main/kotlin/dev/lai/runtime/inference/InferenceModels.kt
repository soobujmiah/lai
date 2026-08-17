package dev.lai.runtime.inference

import kotlinx.serialization.Serializable

/**
 * Stable, implementation-owned identifier used by generic LAI code.
 *
 * IDs are namespaced by an adapter (for example `llama-cpu` or `vendor-a-npu`)
 * so adding an implementation does not require adding it to a core enum.
 */
@Serializable
@JvmInline
value class BackendId(val value: String) {
    init {
        require(value.matches(VALID_ID)) { "Invalid backend id: $value" }
    }

    override fun toString(): String = value

    private companion object {
        val VALID_ID = Regex("^[a-z0-9][a-z0-9._-]{1,63}$")
    }
}

@Serializable
enum class ComputeClass { CPU, GPU, NPU, DSP, OTHER }

/** Static facts published by the adapter that owns this backend. */
data class BackendDescriptor(
    val id: BackendId,
    val computeClass: ComputeClass,
    val supportedModelFormats: Set<String>,
    val supportedQuantizations: Set<String> = emptySet(),
    val defaultPriority: Int = 0,
)

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
    // Prompt tokens actually evaluated this request. With KV-prefix reuse this is usually far
    // smaller than promptTokens, and promptEvaluationMs measures only these — so throughput
    // must divide by this count or it overstates. Defaults to promptTokens (no reuse).
    val evaluatedPromptTokens: Int = promptTokens,
) {
    val promptTokensPerSecond: Double =
        if (promptEvaluationMs > 0) evaluatedPromptTokens * 1000.0 / promptEvaluationMs else 0.0
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
    val compiledBackends: Set<BackendDescriptor>,
    val detail: String,
)
