package dev.lai.runtime.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import dev.lai.runtime.core.LaiJson

internal class NativeBindings private constructor() {
    companion object {
        val loaded: Boolean = runCatching { System.loadLibrary("lai_runtime") }.isSuccess

        @JvmStatic external fun runtimeInfo(): String
        @JvmStatic external fun createSession(modelPath: String, backend: String, contextSize: Int): Long
        @JvmStatic external fun generate(session: Long, prompt: String, configJson: String): String
        @JvmStatic external fun destroySession(session: Long)
        @JvmStatic external fun lastError(): String
    }
}

@Serializable
private data class NativeRuntimeInfo(val backends: List<String> = emptyList(), val detail: String = "")

class NativeInferenceEngine : InferenceEngine {
    private var session: Long = 0

    override val capabilities: RuntimeCapabilities by lazy {
        if (!NativeBindings.loaded) {
            RuntimeCapabilities(false, emptySet(), "Native library could not be loaded")
        } else {
            runCatching {
                val info = LaiJson.decodeFromString<NativeRuntimeInfo>(NativeBindings.runtimeInfo())
                RuntimeCapabilities(
                    nativeLibraryLoaded = true,
                    compiledBackends = info.backends.mapNotNull {
                        runCatching { InferenceBackend.valueOf(it.uppercase()) }.getOrNull()
                    }.toSet(),
                    detail = info.detail,
                )
            }.getOrElse { RuntimeCapabilities(true, emptySet(), it.message ?: "Runtime query failed") }
        }
    }

    override suspend fun load(modelPath: String, backend: InferenceBackend): Result<Unit> = withContext(Dispatchers.IO) {
        if (!NativeBindings.loaded) return@withContext Result.failure(IllegalStateException(capabilities.detail))
        close()
        val handle = NativeBindings.createSession(modelPath, backend.name.lowercase(), DEFAULT_CONTEXT_SIZE)
        if (handle == 0L) {
            Result.failure(IllegalStateException(NativeBindings.lastError()))
        } else {
            session = handle
            Result.success(Unit)
        }
    }

    override fun generate(prompt: String, config: GenerationConfig): Flow<InferenceEvent> = flow {
        val handle = session
        if (handle == 0L) {
            emit(InferenceEvent.Failed("No model is loaded"))
            return@flow
        }
        val response = runCatching {
            NativeBindings.generate(handle, prompt, LaiJson.encodeToString(GenerationConfig.serializer(), config))
        }.getOrElse {
            emit(InferenceEvent.Failed(it.message ?: "Native inference failed"))
            return@flow
        }
        if (response.isBlank()) {
            emit(InferenceEvent.Failed(NativeBindings.lastError()))
        } else {
            // Phase 1 JNI returns a complete response. Concrete backends will expose token callbacks.
            emit(InferenceEvent.Token(response))
            emit(InferenceEvent.Completed(response.length))
        }
    }.flowOn(Dispatchers.Default)

    override fun close() {
        val handle = session
        session = 0
        if (handle != 0L && NativeBindings.loaded) NativeBindings.destroySession(handle)
    }

    companion object {
        private const val DEFAULT_CONTEXT_SIZE = 4096
    }
}
