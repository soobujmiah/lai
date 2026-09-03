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
    }
}

@Serializable
private data class NativeRuntimeInfo(val backends: List<String> = emptyList(), val detail: String = "")

class NativeInferenceEngine : InferenceEngine {
    @Volatile private var session: Long = 0

    /**
     * Installs the native signal handler (SIGSEGV/SIGABRT/SIGBUS/SIGILL/SIGFPE) so that GPU /
     * driver crashes — which are native and uncatchable by Kotlin — write a backtrace into the
     * diagnostic log file (see LaiLog) and logcat. Call once at startup after the log path is
     * known; safe no-op if the native library is not loaded.
     */
    fun installNativeCrashHandler(logFilePath: String?) {
        if (NativeBindings.loaded) NativeBindings.installNativeCrashHandler(logFilePath)
    }

    /**
     * Android OpenCL vendor discovery for the Adreno track. The statically linked Khronos
     * ICD loader only searches /system/vendor/Khronos/OpenCL/vendors by default, which most
     * Qualcomm devices do not populate; this native call synthesizes a vendor directory in
     * app-private storage (bare soname `libOpenCL.so` resolved through the public-library
     * namespace, plus absolute vendor paths) and points OCL_ICD_VENDORS at it. Must run
     * BEFORE any backend capability probe. Safe no-op when the native library is not loaded.
     */
    fun configureOpenCLVendors(baseDir: String?) {
        if (NativeBindings.loaded) NativeBindings.configureOpenCLVendors(baseDir)
    }

    /**
     * Thermal governor hook: records a decode-thread budget that the native decode loop applies
     * at its next safe point (between llama_decode calls). Safe to call from any thread at any
     * time; a no-op when no model is loaded.
     */
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
        if (handle == 0L) {
            Result.failure(IllegalStateException(NativeBindings.lastError()))
        } else {
            session = handle
            Result.success(Unit)
        }
    }

    override fun generate(
        conversation: List<ConversationMessage>,
        config: GenerationConfig,
    ): Flow<InferenceEvent> = callbackFlow {
        val handle = session
        if (handle == 0L) {
            trySend(InferenceEvent.Failed("No model is loaded"))
            close()
            return@callbackFlow
        }
        if (conversation.none { it.content.isNotBlank() }) {
            trySend(InferenceEvent.Failed("Conversation is empty"))
            close()
            return@callbackFlow
        }
        val roles = conversation.map { it.role.name.lowercase() }.toTypedArray()
        val contents = conversation.map { it.content }.toTypedArray()
        val cancelled = AtomicBoolean(false)
        val worker = launch(Dispatchers.Default) {
            val values = NativeBindings.generate(
                session = handle,
                roles = roles,
                contents = contents,
                maxNewTokens = config.maxNewTokens.coerceIn(1, 4096),
                temperature = config.temperature.coerceIn(0f, 2f),
                topP = config.topP.coerceIn(0.05f, 1f),
                seed = config.seed,
                callback = object : NativeTokenCallback {
                    override fun onToken(text: String) {
                        if (cancelled.get()) return
                        // MUST NOT be trySend: this flow is consumed by a collector that does real
                        // work per token (state copy + recomposition). On the default RENDEZVOUS
                        // channel trySend fails whenever the collector is mid-frame, and the token
                        // is silently discarded - which is why replies arrived empty on device.
                        // The buffered channel below plus a blocking put keeps every token and
                        // applies natural backpressure to the native decode loop instead.
                        // A failure here means the collector is gone (flow closed/cancelled);
                        // latch cancellation so the native decode loop stops promptly.
                        if (channel.trySendBlocking(InferenceEvent.Token(text)).isFailure) {
                            cancelled.set(true)
                        }
                    }

                    override fun isCancelled(): Boolean = cancelled.get()
                },
            )
            if (!cancelled.get()) {
                if (values != null && values.size >= METRIC_COUNT) {
                    val metrics = GenerationMetrics(
                        promptTokens = values[0].toInt(),
                        generatedTokens = values[1].toInt(),
                        promptEvaluationMs = values[2] / 1000,
                        timeToFirstTokenMs = values[3] / 1000,
                        decodeMs = values[4] / 1000,
                        totalMs = values[5] / 1000,
                        evaluatedPromptTokens = values[6].toInt(),
                    )
                    channel.trySendBlocking(InferenceEvent.Completed(metrics.generatedTokens, metrics))
                } else {
                    channel.trySendBlocking(
                        InferenceEvent.Failed(NativeBindings.lastError().ifBlank { "Native inference failed" }),
                    )
                }
            }
            close()
        }
        awaitClose {
            cancelled.set(true)
            worker.cancel()
        }
    }
        // Buffer so a brief UI stall never costs a token. Combined with the blocking send above
        // the native decode loop is throttled by backpressure instead of dropping output.
        .buffer(TOKEN_BUFFER_CAPACITY)

    override suspend fun countTokens(conversation: List<ConversationMessage>): Result<Int> = withContext(Dispatchers.Default) {
        val handle = session
        if (handle == 0L) return@withContext Result.failure(IllegalStateException("No model is loaded"))
        runCatching {
            val count = NativeBindings.countTokens(
                handle,
                conversation.map { it.role.name.lowercase() }.toTypedArray(),
                conversation.map { it.content }.toTypedArray(),
            )
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
        // Hexagon NPU track (docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md). ggml itself
        // classifies the HTP device as GGML_BACKEND_DEVICE_TYPE_GPU (upstream's own framing:
        // "Hexagon NPU behaves as a 'GPU' device when it comes to -ngl and other offload-related
        // options"), but LAI's own ComputeClass is a separate, more accurate vocabulary for the
        // scheduler/UI/evidence records -- this is genuinely an NPU, not a GPU.
        "hexagon" -> BackendDescriptor(
            id = BackendId("llama-hexagon"),
            computeClass = ComputeClass.NPU,
            supportedModelFormats = setOf("gguf"),
            defaultPriority = 300,
        )
        else -> null
    }

    private fun nativeName(id: BackendId): String = when (id.value) {
        "llama-cpu" -> "cpu"
        "llama-vulkan" -> "vulkan"
        "llama-opencl" -> "opencl"
        "llama-hexagon" -> "hexagon"
        else -> error("Unknown llama backend ${id.value}")
    }

    companion object {
        private const val DEFAULT_CONTEXT_SIZE = 4096
        private const val METRIC_COUNT = 7

        /** Bounded token backlog before the native decode loop is throttled by backpressure. */
        private const val TOKEN_BUFFER_CAPACITY = 256
    }
}
