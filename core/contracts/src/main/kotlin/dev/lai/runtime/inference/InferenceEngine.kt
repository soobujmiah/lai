package dev.lai.runtime.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    val contextSize: Int

    suspend fun load(modelPath: String, backend: InferenceBackend = InferenceBackend.AUTO): Result<Unit>

    fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig = GenerationConfig(),
    ): Flow<InferenceEvent>

    fun generate(prompt: String, config: GenerationConfig = GenerationConfig()): Flow<InferenceEvent> =
        generate(listOf(ConversationMessage(ConversationRole.USER, prompt)), config)

    suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int>

    override fun close()
}
