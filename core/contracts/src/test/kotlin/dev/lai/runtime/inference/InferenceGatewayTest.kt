package dev.lai.runtime.inference

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceGatewayTest {
    private val cpu = BackendDescriptor(
        id = BackendId("llama-cpu"),
        computeClass = ComputeClass.CPU,
        supportedModelFormats = setOf("gguf"),
        defaultPriority = 100,
    )

    private fun engine() = object : InferenceEngine {
        override val capabilities = RuntimeCapabilities(true, setOf(cpu), "cpu")
        override val contextSize = 4096
        var loadedBackend: BackendId? = null
        override suspend fun load(modelPath: String, backend: BackendId?): Result<Unit> {
            loadedBackend = backend
            return Result.success(Unit)
        }
        override fun generate(conversation: List<ConversationMessage>, config: GenerationConfig) =
            kotlinx.coroutines.flow.flowOf(InferenceEvent.Token("ok"), InferenceEvent.Completed(1))
        override suspend fun countTokens(conversation: List<ConversationMessage>) = Result.success(1)
        override fun close() = Unit
    }

    @Test
    fun `unqualified providers are invisible to capability discovery`() = runBlocking {
        val measuredEngine = engine()
        val gateway = InferenceGateway(listOf(
            InferenceGateway.ProviderRegistration(
                ProviderDescriptor("unqualified", setOf(cpu), ProviderEvidence.UNQUALIFIED),
                engine(),
            ),
            InferenceGateway.ProviderRegistration(
                ProviderDescriptor("cpu", setOf(cpu), ProviderEvidence.MEASURED),
                measuredEngine,
            ),
        ))

        assertEquals(setOf(cpu), gateway.capabilities.compiledBackends)
        assertTrue(gateway.load("model.gguf", BackendId("llama-cpu")).isSuccess)
        assertEquals(BackendId("llama-cpu"), measuredEngine.loadedBackend)
        assertEquals("cpu", gateway.lastProvenance?.providerId)
    }

    @Test
    fun `requested unavailable backend does not silently fall back`() = runBlocking {
        val gateway = InferenceGateway(listOf(
            InferenceGateway.ProviderRegistration(
                ProviderDescriptor("cpu", setOf(cpu), ProviderEvidence.MEASURED),
                engine(),
            ),
        ))

        val result = gateway.load("model.gguf", BackendId("llama-vulkan"))
        assertTrue(result.isFailure)
        assertEquals(null, gateway.lastProvenance)
    }

    @Test
    fun `streaming and cancellation remain flow based`() = runBlocking {
        val gateway = InferenceGateway(listOf(
            InferenceGateway.ProviderRegistration(
                ProviderDescriptor("cpu", setOf(cpu), ProviderEvidence.MEASURED),
                engine(),
            ),
        ))
        gateway.load("model.gguf", BackendId("llama-cpu"))
        assertEquals("ok", gateway.generate("hello").first().let { (it as InferenceEvent.Token).text })
    }
}
