package dev.lai.runtime.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine : AutoCloseable {
    val capabilities: RuntimeCapabilities
    suspend fun load(modelPath: String, backend: InferenceBackend = InferenceBackend.AUTO): Result<Unit>
    fun generate(prompt: String, config: GenerationConfig = GenerationConfig()): Flow<InferenceEvent>
    override fun close()
}
