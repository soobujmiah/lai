package dev.lai.runtime.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceContractsTest {
    @Test
    fun `generation defaults are bounded and backend capability is explicit`() {
        val config = GenerationConfig()
        assertEquals(512, config.maxNewTokens)
        assertTrue(config.temperature in 0f..2f)
        val cpu = BackendDescriptor(
            id = BackendId("llama-cpu"),
            computeClass = ComputeClass.CPU,
            supportedModelFormats = setOf("gguf"),
        )
        val capability = RuntimeCapabilities(
            nativeLibraryLoaded = true,
            compiledBackends = setOf(cpu),
            detail = "cpu ready",
        )
        assertTrue(cpu in capability.compiledBackends)
        assertFalse(capability.compiledBackends.any { it.computeClass == ComputeClass.NPU })
    }

    @Test
    fun `backend ids are adapter owned rather than a vendor enum`() {
        assertEquals("vendor-a-npu", BackendId("vendor-a-npu").value)
        assertEquals("future-vendor-npu", BackendId("future-vendor-npu").value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid backend ids are rejected`() {
        BackendId("Vendor Backend With Spaces")
    }

    @Test
    fun `stream events retain Bangla text and completion count`() {
        val token = InferenceEvent.Token("বাংলা")
        val metrics = GenerationMetrics(12, 7, 100, 120, 350, 470)
        val complete = InferenceEvent.Completed(7, metrics)
        val failure = InferenceEvent.Failed("failed")
        val conversation = listOf(
            ConversationMessage(ConversationRole.USER, "বাংলা"),
            ConversationMessage(ConversationRole.ASSISTANT, "উত্তর"),
        )
        assertEquals("বাংলা", token.text)
        assertEquals(7, complete.tokensGenerated)
        assertEquals(20.0, complete.metrics?.decodeTokensPerSecond ?: 0.0, 0.001)
        assertEquals(ConversationRole.ASSISTANT, conversation.last().role)
        assertEquals("failed", failure.message)
    }

    @Test
    fun `prompt throughput divides by evaluated tokens when a KV prefix was reused`() {
        // 470-token prompt of which only 100 were evaluated (370 reused from the KV cache) in 1 s:
        // honest prefill speed is 100 tok/s, not an inflated 470 tok/s.
        val reused = GenerationMetrics(470, 26, 1000, 1100, 1653, 2753, evaluatedPromptTokens = 100)
        assertEquals(100.0, reused.promptTokensPerSecond, 0.001)
        // Without reuse the default keeps the original semantics: all prompt tokens evaluated.
        val noReuse = GenerationMetrics(12, 7, 100, 120, 350, 470)
        assertEquals(12, noReuse.evaluatedPromptTokens)
        assertEquals(120.0, noReuse.promptTokensPerSecond, 0.001)
    }
}
