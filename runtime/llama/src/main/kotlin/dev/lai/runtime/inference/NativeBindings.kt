package dev.lai.runtime.inference

/** JNI declarations implemented by runtime/llama/src/main/cpp/native_inference.cpp. */
internal object NativeBindings {
    private var loadFailure: String? = null

    init {
        try {
            System.loadLibrary("lai_llama")
        } catch (error: UnsatisfiedLinkError) {
            loadFailure = error.stackTraceToString()
        }
    }

    val loaded: Boolean
        get() = runCatching { nativeRuntimeInfo() }.isSuccess

    fun loadError(): String = loadFailure.orEmpty()

    fun lastError(): String = runCatching { nativeLastError() }.getOrDefault("")

    fun createSession(modelPath: String, backend: String): Long =
        nativeCreateSession(modelPath, backend)

    fun destroySession(session: Long) = nativeDestroySession(session)

    fun generate(
        session: Long,
        roles: Array<String>,
        contents: Array<String>,
        maxNewTokens: Int,
        temperature: Float,
        topP: Float,
        seed: Long,
        callback: TokenCallback,
    ) = nativeGenerate(session, roles, contents, maxNewTokens, temperature, topP, seed, callback)

    fun countTokens(session: Long, roles: Array<String>, contents: Array<String>): Int =
        nativeCountTokens(session, roles, contents)

    interface TokenCallback {
        fun onToken(text: String)
        fun onComplete(tokensGenerated: Int)
        fun onError(message: String)
    }

    @JvmStatic private external fun nativeRuntimeInfo(): String
    @JvmStatic private external fun nativeLastError(): String
    @JvmStatic private external fun nativeCreateSession(modelPath: String, backend: String): Long
    @JvmStatic private external fun nativeDestroySession(session: Long)
    @JvmStatic private external fun nativeGenerate(
        session: Long,
        roles: Array<String>,
        contents: Array<String>,
        maxNewTokens: Int,
        temperature: Float,
        topP: Float,
        seed: Long,
        callback: TokenCallback,
    )
    @JvmStatic private external fun nativeCountTokens(
        session: Long,
        roles: Array<String>,
        contents: Array<String>,
    ): Int
}
