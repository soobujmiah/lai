package dev.lai.runtime.inference

import dev.lai.runtime.core.LaiJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.util.concurrent.atomic.AtomicBoolean

interface NativeTokenCallback {
    fun onToken(text: String)
    fun isCancelled(): Boolean
}

internal class NativeBindings private constructor() {
    companion object {
        val loaded: Boolean = runCatching { System.loadLibrary("lai_runtime") }.isSuccess

        @JvmStatic external fun runtimeInfo(): String
        @JvmStatic external fun createSession(modelPath: String, backend: String, contextSize: Int): Long
        @JvmStatic external fun generate(
            session: Long,
            roles: Array<String>,
            contents: Array<String>,
            maxNewTokens: Int,
            temperature: Float,
            topP: Float,
            seed: Long,
            callback: NativeTokenCallback,
        ): LongArray?
        @JvmStatic external fun countTokens(session: Long, roles: Array<String>, contents: Array<String>): Int
        @JvmStatic external fun setThreadLimit(session: Long, decodeThreads: Int)
        @JvmStatic external fun destroySession(session: Long)
        @JvmStatic external fun lastError(): String
        @JvmStatic external fun installNativeCrashHandler(logFilePath: String?)
        @JvmStatic external fun configureOpenCLVendors(baseDir: String?)
        @JvmStatic external fun qualifyOpenCL(): String
    }
}

@Serializable
private data class NativeRuntimeInfo(val backends: List<String> = emptyList(), val detail: String = "")

class NativeInferenceEngine : InferenceEngine {
    @Volatile private var session: Long = 0

    fun installNativeCrashHandler(logFilePath: String?) {
        if (NativeBindings.loaded) NativeBindings.installNativeCrashHandler(logFilePath)
    }

    fun configureOpenCLVendors(baseDir: String?) {
        if (NativeBindings.loaded) NativeBindings.configureOpenCLVendors(baseDir)
    }

    /** Runs the non-production Qualcomm OpenCL qualification probe and returns its JSON evidence. */
    fun qualifyOpenCL(): Result<String> = runCatching {
        check(NativeBindings.loaded) { "Native library could not be loaded" }
        NativeBindings.qualifyOpenCL()
    }

    fun setDecodeThreadLimit(decodeThreads: Int) {
        val handle = session
        if (handle != 0L && NativeBindings.loaded && decodeThreads > 0) {
            NativeBindings.setThreadLimit(handle, decodeThreads)
        }
    }

    override val contextSize: Int = DEFAULT_CONTEXT_SIZE

    override val capabilities: RuntimeCapabilities by lazy {
        if (!NativeBindings.loaded) {
            RuntimeCapabilities(false, emptySet(), "Native library could not be loaded")
        } else {
            runCatching {
                val info = LaiJson.decodeFromString<NativeRuntimeInfo>(NativeBindings.runtimeInfo())
                RuntimeCapabilities(
                    nativeLibraryLoaded = true,
                    compiledBackends = info.backends.mapNotNull(::descriptorForNativeBackend).toSet(),
                    detail = info.detail,
                )
            }.getOrElse { RuntimeCapabilities(true, emptySet(), it.message ?: "Runtime query failed") }
        }
    }

    override suspend fun load(modelPath: String, backend: BackendId?): Result<Unit> = withContext(Dispatchers.IO) {
        if (!NativeBindings.loaded) return@withContext Result.failure(IllegalStateException(capabilities.detail))
        val nativeBackend = when (backend) {
            null -> "auto"
            else -> capabilities.compiledBackends.firstOrNull { it.id == backend }
                ?.let { nativeName(it.id) }
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Backend ${backend.value} is not provided by the llama runtime"),
                )
        }
        close()
        val handle = NativeBindings.createSession(modelPath, nativeBackend, contextSize)
        if (handle == 0L) Result.failure(IllegalStateException(NativeBindings.lastError()))
        else { session = handle; Result.success(Unit) }
    }

    override fun generate(conversation: List<ConversationMessage>, config: GenerationConfig): Flow<InferenceEvent> = callbackFlow {
        val handle = session
        if (handle == 0L) { trySend(InferenceEvent.Failed("No model is loaded")); close(); return@callbackFlow }
        if (conversation.none { it.content.isNotBlank() }) { trySend(InferenceEvent.Failed("Conversation is empty")); close(); return@callbackFlow }
        val roles = conversation.map { it.role.name.lowercase() }.toTypedArray()
        val contents = conversation.map { it.content }.toTypedArray()
        val cancelled = AtomicBoolean(false)
        val worker = launch(Dispatchers.Default) {
            val values = NativeBindings.generate(handle, roles, contents, config.maxNewTokens.coerceIn(1, 4096), config.temperature.coerceIn(0f, 2f), config.topP.coerceIn(0.05f, 1f), config.seed,
                object : NativeTokenCallback {
                    override fun onToken(text: String) {
                        if (!cancelled.get() && channel.trySendBlocking(InferenceEvent.Token(text)).isFailure) cancelled.set(true)
                    }
                    override fun isCancelled(): Boolean = cancelled.get()
                })
            if (!cancelled.get()) {
                if (values != null && values.size >= METRIC_COUNT) {
                    val metrics = GenerationMetrics(values[0].toInt(), values[1].toInt(), values[2] / 1000, values[3] / 1000, values[4] / 1000, values[5] / 1000, values[6].toInt())
                    channel.trySendBlocking(InferenceEvent.Completed(metrics.generatedTokens, metrics))
                } else channel.trySendBlocking(InferenceEvent.Failed(NativeBindings.lastError().ifBlank { "Native inference failed" }))
            }
            close()
        }
        awaitClose { cancelled.set(true); worker.cancel() }
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
        "cpu" -> BackendDescriptor(BackendId("llama-cpu"), ComputeClass.CPU, setOf("gguf"), 100)
        "vulkan" -> BackendDescriptor(BackendId("llama-vulkan"), ComputeClass.GPU, setOf("gguf"), 200)
        "opencl" -> BackendDescriptor(BackendId("llama-opencl"), ComputeClass.GPU, setOf("gguf"), 200)
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
        private const val TOKEN_BUFFER_CAPACITY = 256
    }
}
