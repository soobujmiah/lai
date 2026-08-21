package dev.lai.runtime.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    val contextSize: Int

    /** A null backend lets this concrete engine choose among only the adapters it owns. */
    suspend fun load(modelPath: String, backend: BackendId? = null): Result<Unit>

    fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig = GenerationConfig(),
    ): Flow<InferenceEvent>

    fun generate(prompt: String, config: GenerationConfig = GenerationConfig()): Flow<InferenceEvent> =
        generate(listOf(ConversationMessage(ConversationRole.USER, prompt)), config)

    suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int>

    /** Optional native-runtime hooks; non-native engines keep the safe no-op defaults. */
    fun installNativeCrashHandler(logFilePath: String) {}
    fun configureOpenCLVendors(baseDir: String) {}
    fun setDecodeThreadLimit(decodeThreads: Int) {}

    override fun close()
}
