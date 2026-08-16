package dev.lai.runtime.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSessionPolicyTest {

    private val policy = SettingsSessionPolicy()

    @Test
    fun `initial session is embedded defaults with no override`() {
        val session = policy.initial()
        assertEquals(SettingsDocumentV1(), session.saved)
        assertNull(session.pendingLlmOverride)
        assertFalse(session.hasPendingOverride)
        assertEquals(0.7f, session.effectiveLlm.temperature)
    }

    @Test
    fun `a valid loaded document restores typed values`() {
        val stored = SettingsDocumentV1(llm = LlmSettings(temperature = 0.2f, topP = 0.5f, maxNewTokens = 512))
        val session = policy.fromLoad(stored, fromFile = true, fellBackToDefaults = false)
        assertTrue(session.loadedFromFile)
        assertFalse(session.fellBackToDefaults)
        assertEquals(0.2f, session.saved.llm.temperature)
        assertEquals(512, session.saved.llm.maxNewTokens)
    }

    @Test
    fun `an out of range loaded document falls back to defaults without throwing`() {
        val hostile = SettingsDocumentV1(llm = LlmSettings(temperature = 99f, maxNewTokens = 1_000_000))
        val session = policy.fromLoad(hostile, fromFile = true, fellBackToDefaults = false)
        assertTrue(session.fellBackToDefaults)
        assertEquals(SettingsDocumentV1().llm, session.saved.llm)
        assertTrue(session.warnings.any { it.contains("temperature") })
    }

    @Test
    fun `apply once changes the effective settings but never the saved defaults`() {
        val session = policy.initial()
        val result = policy.applyOnce(session, session.saved.llm.copy(temperature = 1.4f))
        assertTrue(result is SettingsSessionResult.Applied)
        val updated = result.session
        assertEquals(1.4f, updated.effectiveLlm.temperature)
        // The persisted baseline is untouched: this is the core privacy/UX guarantee of the sheet.
        assertEquals(0.7f, updated.saved.llm.temperature)
        assertTrue(updated.hasPendingOverride)
    }

    @Test
    fun `an invalid override is rejected and leaves the session untouched`() {
        val session = policy.initial()
        val result = policy.applyOnce(session, session.saved.llm.copy(topP = 5f))
        assertTrue(result is SettingsSessionResult.Rejected)
        assertEquals(session, result.session)
        assertFalse(result.session.hasPendingOverride)
        assertEquals("top_p_range", (result as SettingsSessionResult.Rejected).issues.single().code)
    }

    @Test
    fun `resolving a request consumes the override exactly once`() {
        val armed = (policy.applyOnce(policy.initial(), LlmSettings(temperature = 1.1f)) as
            SettingsSessionResult.Applied).session

        val first = policy.resolveForRequest(armed)
        assertTrue(first.usedOverride)
        assertEquals(1.1f, first.llm.temperature)

        val second = policy.resolveForRequest(first.session)
        assertFalse(second.usedOverride)
        assertEquals(0.7f, second.llm.temperature)
    }

    @Test
    fun `prepare save accepts a valid document and clears the override`() {
        val armed = (policy.applyOnce(policy.initial(), LlmSettings(temperature = 1.1f)) as
            SettingsSessionResult.Applied).session
        val document = SettingsDocumentV1(llm = LlmSettings(temperature = 0.3f, maxNewTokens = 128))

        val result = policy.prepareSave(armed, document)

        assertTrue(result is SettingsSessionResult.Applied)
        assertEquals(0.3f, result.session.saved.llm.temperature)
        assertFalse(result.session.hasPendingOverride)
    }

    @Test
    fun `prepare save rejects a document that exceeds its own context budget`() {
        val document = SettingsDocumentV1(
            llm = LlmSettings(maxNewTokens = 4096, context = ContextPolicy(maxContextTokens = 1024)),
        )
        val result = policy.prepareSave(policy.initial(), document)
        assertTrue(result is SettingsSessionResult.Rejected)
        assertEquals(
            "max_new_tokens_exceeds_context",
            (result as SettingsSessionResult.Rejected).issues.single().code,
        )
    }

    @Test
    fun `reset returns defaults and drops any override`() {
        val armed = (policy.applyOnce(policy.initial(), LlmSettings(temperature = 1.9f)) as
            SettingsSessionResult.Applied).session
        val reset = policy.reset(armed)
        assertEquals(SettingsDocumentV1(), reset.saved)
        assertFalse(reset.hasPendingOverride)
    }

    @Test
    fun `discarding an override does not spend it on a request`() {
        val armed = (policy.applyOnce(policy.initial(), LlmSettings(temperature = 1.9f)) as
            SettingsSessionResult.Applied).session
        val cleared = policy.discardOverride(armed)
        assertFalse(cleared.hasPendingOverride)
        assertEquals(0.7f, cleared.effectiveLlm.temperature)
    }

    @Test
    fun `max new tokens ceiling always reserves context for the prompt`() {
        val session = policy.fromLoad(
            SettingsDocumentV1(llm = LlmSettings(context = ContextPolicy(maxContextTokens = 8192))),
            fromFile = true,
            fellBackToDefaults = false,
        )
        // Runtime context is the binding constraint, and only half of it may go to the reply.
        assertEquals(1024, policy.maxNewTokensCeiling(session, runtimeContextTokens = 2048))
        // The absolute policy maximum still caps very large contexts.
        assertEquals(4096, policy.maxNewTokensCeiling(session, runtimeContextTokens = 32768))
        // An unknown/zero runtime context falls back to the configured budget, never to zero.
        assertEquals(4096, policy.maxNewTokensCeiling(session, runtimeContextTokens = null))
        assertEquals(4096, policy.maxNewTokensCeiling(session, runtimeContextTokens = 0))
    }

    @Test
    fun `the ceiling can never consume the whole context`() {
        // Regression: a ceiling equal to the context size makes
        // promptTokens + maxNewTokens <= contextSize unsatisfiable, so every send trimmed the
        // whole conversation and then threw - the user just saw the app never reply.
        listOf(512, 1024, 2048, 4096, 8192, 32768).forEach { runtimeContext ->
            val session = policy.fromLoad(
                SettingsDocumentV1(llm = LlmSettings(context = ContextPolicy(maxContextTokens = runtimeContext))),
                fromFile = true,
                fellBackToDefaults = false,
            )
            val ceiling = policy.maxNewTokensCeiling(session, runtimeContext)
            assertTrue(
                "ceiling $ceiling must leave room for a prompt in context $runtimeContext",
                ceiling < runtimeContext,
            )
            assertTrue("ceiling must stay usable", ceiling >= 32)
        }
    }
}
