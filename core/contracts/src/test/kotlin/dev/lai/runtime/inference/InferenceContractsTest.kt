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
        val capability = RuntimeCapabilities(
            nativeLibraryLoaded = true,
            compiledBackends = setOf(InferenceBackend.CPU),
            detail = "cpu ready",
        )
        assertTrue(InferenceBackend.CPU in capability.compiledBackends)
        assertFalse(InferenceBackend.QNN in capability.compiledBackends)
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
}
