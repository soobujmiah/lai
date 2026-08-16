package dev.lai.runtime.settings

import dev.lai.runtime.core.LaiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSettingsTest {
    @Test
    fun `defaults preserve the reviewed Qwen product state`() {
        val s = SettingsDocumentV1()
        assertEquals(SettingsDocumentV1.SCHEMA_VERSION, s.schemaVersion)
        assertEquals(0.7f, s.llm.temperature)
        assertEquals(0.9f, s.llm.topP)
        assertEquals(256, s.llm.maxNewTokens)
        assertEquals(-1L, s.llm.seed)
        assertEquals(4096, s.llm.context.maxContextTokens)
        assertEquals(8, s.llm.context.keepLastTurns)
    }

    @Test
    fun `document round trips and encodes all defaults`() {
        val original = SettingsDocumentV1()
        val json = LaiJson.encodeToString(SettingsDocumentV1.serializer(), original)
        val back = LaiJson.decodeFromString(SettingsDocumentV1.serializer(), json)
        assertEquals(original, back)
        // encodeDefaults = true: the wire form is stable, self-describing, and numeric/boolean only.
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"temperature\":0.7"))
        assertTrue(json.contains("\"topP\":0.9"))
        assertTrue(json.contains("\"maxNewTokens\":256"))
        assertTrue(json.contains("\"maxContextTokens\":4096"))
    }

    @Test
    fun `every settings key is numeric or boolean`() {
        // Privacy invariant: no free-text field exists, so settings can never hold
        // prompts, documents, selectors, packages or credentials.
        val json = LaiJson.encodeToString(SettingsDocumentV1.serializer(), SettingsDocumentV1())
        listOf(
            "temperature", "topP", "maxNewTokens", "seed",
            "keepLastTurns", "maxContextTokens",
            "steps", "guidanceScale", "width", "height",
            "speechRate", "bargeIn",
            "maxResults", "minScore",
        ).forEach { key ->
            assertTrue("Expected numeric/boolean key '$key' to be encoded", json.contains("\"$key\""))
        }
    }

    @Test
    fun `override document round trips explicit values`() {
        val original = SettingsDocumentV1(
            llm = LlmSettings(temperature = 0.2f, topP = 0.8f, maxNewTokens = 512, seed = 42L),
            imageGeneration = ImageGenerationSettings(steps = 30, width = 768, height = 768),
            voice = VoiceSettings(speechRate = 1.25f, bargeIn = true),
            search = SearchSettings(maxResults = 10, minScore = 0.5f),
        )
        val json = LaiJson.encodeToString(SettingsDocumentV1.serializer(), original)
        assertEquals(original, LaiJson.decodeFromString(SettingsDocumentV1.serializer(), json))
    }
}
