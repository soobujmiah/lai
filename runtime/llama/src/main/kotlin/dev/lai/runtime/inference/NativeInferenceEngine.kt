package dev.lai.runtime.inference

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class NativeInferenceEngine : InferenceEngine {
    @Volatile private var session: Long = 0

    override val contextSize: Int
        get() = DEFAULT_CONTEXT_SIZE

    override val capabilities: RuntimeCapabilities
        get() = RuntimeCapabilities(
            nativeLibraryLoaded = NativeBindings.loaded,
            compiledBackends = listOf("cpu", "vulkan", "opencl")
                .mapNotNull(::descriptorForNativeBackend)
                .toSet(),
            detail = NativeBindings.lastError(),
        )

    override suspend fun load(modelPath: String, backend: BackendId?): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            require(NativeBindings.loaded) { "Native inference library is not loaded" }
            val selected = backend ?: BackendId("llama-cpu")
            val nativeBackend = nativeName(selected)
            val old = session
            if (old != 0L) NativeBindings.destroySession(old)
            val created = NativeBindings.createSession(modelPath, nativeBackend)
            check(created != 0L) { NativeBindings.lastError().ifBlank { "Failed to load model" } }
            session = created
        }
    }

    override fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig,
    ): Flow<InferenceEvent> = channelFlow {
        val handle = session
        if (handle == 0L) {
            send(InferenceEvent.Failed("No model is loaded"))
            return@channelFlow
        }
        val cancelled = AtomicBoolean(false)
        val worker = CoroutineScope(Dispatchers.Default).launch {
            try {
                NativeBindings.generate(
                    handle,
                    conversation.map { it.role.name.lowercase() }.toTypedArray(),
                    conversation.map { it.content }.toTypedArray(),
                    config.maxNewTokens,
                    config.temperature,
                    config.topP,
                    config.seed,
                    object : NativeBindings.TokenCallback {
                        override fun onToken(text: String) {
                            if (!cancelled.get()) trySend(InferenceEvent.Token(text))
                        }

                        override fun onComplete(tokensGenerated: Int) {
                            if (!cancelled.get()) trySend(InferenceEvent.Completed(tokensGenerated))
                        }

                        override fun onError(message: String) {
                            if (!cancelled.get()) trySend(InferenceEvent.Failed(message))
                        }
                    },
                )
            } catch (e: CancellationException) {
                // Consumer cancellation is expected; do not turn it into a model error.
            } catch (e: Throwable) {
                if (!cancelled.get()) trySend(InferenceEvent.Failed(e.message ?: "Generation failed"))
            }
        }
        awaitClose {
            cancelled.set(true)
            worker.cancel()
        }
    }.buffer(TOKEN_BUFFER_CAPACITY)

    override suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int> = withContext(Dispatchers.Default) {
        val handle = session
        if (handle == 0L) return@withContext Result.failure(IllegalStateException("No model is loaded"))
        runCatching {
            val count = NativeBindings.countTokens(handle, conversation.map { it.role.name.lowercase() }.toTypedArray(), conversation.map { it.content }.toTypedArray())
            check(count >= 0) { NativeBindings.lastError().ifBlank { "Token counting failed" } }
            count
        }
    }

    override fun close() {
        val handle = session
        session = 0
        if (handle != 0L && NativeBindings.loaded) NativeBindings.destroySession(handle)
    }

    private fun descriptorForNativeBackend(name: String): BackendDescriptor? = when (name) {
        "cpu" -> BackendDescriptor(
            id = BackendId("llama-cpu"),
            computeClass = ComputeClass.CPU,
            supportedModelFormats = setOf("gguf"),
            defaultPriority = 100,
        )
        "vulkan" -> BackendDescriptor(
            id = BackendId("llama-vulkan"),
            computeClass = ComputeClass.GPU,
            supportedModelFormats = setOf("gguf"),
            defaultPriority = 200,
        )
        "opencl" -> BackendDescriptor(
            id = BackendId("llama-opencl"),
            computeClass = ComputeClass.GPU,
            supportedModelFormats = setOf("gguf"),
            defaultPriority = 200,
        )
        else -> null
    }

    private fun nativeName(id: BackendId): String = when (id.value) {
        "llama-cpu" -> "cpu"
        "llama-vulkan" -> "vulkan"
        "llama-opencl" -> "opencl"
        else -> error("Unknown llama backend ${id.value}")
    }

    companion object {
        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val METRIC_COUNT = 7
        private const val TOKEN_BUFFER_CAPACITY = 64
    }
}
