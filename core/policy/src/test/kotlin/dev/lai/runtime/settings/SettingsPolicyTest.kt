package dev.lai.runtime.settings

import dev.lai.runtime.core.LaiJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPolicyTest {

    private val policy = SettingsPolicy()

    private fun json(source: String): JsonObject =
        LaiJson.parseToJsonElement(source).jsonObject

    private fun invalidCodes(raw: JsonObject): Set<String> =
        policy.validate(raw).errors.map { it.code }.toSet()

    private fun warningPaths(raw: JsonObject): Set<String> =
        policy.validate(raw).warnings.map { it.path }.toSet()

    @Test
    fun `defaults validate as valid and match Qwen state`() {
        val validation = policy.validate(policy.defaults())
        assertTrue(validation.toString(), validation.isValid)
        assertEquals(0.7f, policy.defaults().llm.temperature)
        assertEquals(0.9f, policy.defaults().llm.topP)
        assertEquals(256, policy.defaults().llm.maxNewTokens)
    }

    @Test
    fun `empty raw document is treated as schema v1 with defaults`() {
        val doc = policy.sanitize(json("{}"))
        assertEquals(policy.defaults(), doc)
        assertTrue(policy.validate(json("{}")).isValid)
    }

    @Test
    fun `valid raw document sanitizes to an equal typed document`() {
        val raw = json(
            """
            {"schemaVersion":1,"llm":{"temperature":0.4,"topP":0.85,"maxNewTokens":128,"seed":7},
             "imageGeneration":{"steps":25,"width":768,"height":512},"voice":{"speechRate":1.1},
             "search":{"maxResults":8,"minScore":0.4}}
            """.trimIndent(),
        )
        assertEquals(
            SettingsDocumentV1(
                llm = LlmSettings(temperature = 0.4f, topP = 0.85f, maxNewTokens = 128, seed = 7L),
                imageGeneration = ImageGenerationSettings(steps = 25, width = 768, height = 512),
                voice = VoiceSettings(speechRate = 1.1f),
                search = SearchSettings(maxResults = 8, minScore = 0.4f),
            ),
            policy.sanitize(raw),
        )
    }

    @Test
    fun `temperature boundaries are inclusive`() {
        assertTrue(policy.validate(json("""{"llm":{"temperature":0.0}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"temperature":2.0}}""")).isValid)
    }

    @Test
    fun `temperature out of range is rejected`() {
        assertEquals(setOf("temperature_range"), invalidCodes(json("""{"llm":{"temperature":-0.1}}""")))
        assertEquals(setOf("temperature_range"), invalidCodes(json("""{"llm":{"temperature":2.5}}""")))
    }

    @Test
    fun `non finite floats are rejected`() {
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"llm":{"temperature":NaN}}""")))
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"llm":{"temperature":Infinity}}""")))
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"llm":{"topP":Infinity}}""")))
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"voice":{"speechRate":NaN}}""")))
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"search":{"minScore":Infinity}}""")))
        assertEquals(setOf("non_finite_number"), invalidCodes(json("""{"imageGeneration":{"guidanceScale":-Infinity}}""")))
    }

    @Test
    fun `topP range is enforced`() {
        assertTrue(policy.validate(json("""{"llm":{"topP":0.0}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"topP":1.0}}""")).isValid)
        assertEquals(setOf("top_p_range"), invalidCodes(json("""{"llm":{"topP":1.5}}""")))
    }

    @Test
    fun `maxNewTokens range is enforced`() {
        assertTrue(policy.validate(json("""{"llm":{"maxNewTokens":1}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"maxNewTokens":4096}}""")).isValid)
        assertEquals(setOf("max_new_tokens_range"), invalidCodes(json("""{"llm":{"maxNewTokens":0}}""")))
        assertEquals(setOf("max_new_tokens_range"), invalidCodes(json("""{"llm":{"maxNewTokens":4097}}""")))
    }

    @Test
    fun `maxContextTokens range is enforced`() {
        assertTrue(policy.validate(json("""{"llm":{"context":{"maxContextTokens":512}}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"context":{"maxContextTokens":32768}}}""")).isValid)
        assertEquals(setOf("max_context_range"), invalidCodes(json("""{"llm":{"context":{"maxContextTokens":511}}}""")))
        assertEquals(setOf("max_context_range"), invalidCodes(json("""{"llm":{"context":{"maxContextTokens":32769}}}""")))
    }

    @Test
    fun `maxNewTokens cannot exceed the context window`() {
        val raw = json("""{"llm":{"maxNewTokens":3000,"context":{"maxContextTokens":2048}}}""")
        assertEquals(setOf("max_new_tokens_exceeds_context"), invalidCodes(raw))
        // falls back to defaults when sanitized
        assertEquals(policy.defaults(), policy.sanitize(raw))
    }

    @Test
    fun `keepLastTurns range is enforced`() {
        assertTrue(policy.validate(json("""{"llm":{"context":{"keepLastTurns":1}}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"context":{"keepLastTurns":64}}}""")).isValid)
        assertEquals(setOf("keep_last_turns_range"), invalidCodes(json("""{"llm":{"context":{"keepLastTurns":0}}}""")))
        assertEquals(setOf("keep_last_turns_range"), invalidCodes(json("""{"llm":{"context":{"keepLastTurns":65}}}""")))
    }

    @Test
    fun `seed accepts random sentinel and non-negative values only`() {
        assertTrue(policy.validate(json("""{"llm":{"seed":-1}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"seed":0}}""")).isValid)
        assertTrue(policy.validate(json("""{"llm":{"seed":9223372036854775807}}""")).isValid)
        assertEquals(setOf("seed_range"), invalidCodes(json("""{"llm":{"seed":-2}}""")))
    }

    @Test
    fun `image ranges are enforced`() {
        assertTrue(policy.validate(json("""{"imageGeneration":{"steps":1,"width":64,"height":1024}}""")).isValid)
        assertTrue(policy.validate(json("""{"imageGeneration":{"steps":100,"width":1024,"guidanceScale":30.0}}""")).isValid)
        assertEquals(setOf("image_steps_range"), invalidCodes(json("""{"imageGeneration":{"steps":0}}""")))
        assertEquals(setOf("image_steps_range"), invalidCodes(json("""{"imageGeneration":{"steps":101}}""")))
        assertEquals(setOf("guidance_range"), invalidCodes(json("""{"imageGeneration":{"guidanceScale":30.5}}""")))
        assertEquals(setOf("image_dimension"), invalidCodes(json("""{"imageGeneration":{"width":100}}""")))
        assertEquals(setOf("image_dimension"), invalidCodes(json("""{"imageGeneration":{"height":1088}}""")))
    }

    @Test
    fun `voice and search ranges are enforced`() {
        assertTrue(policy.validate(json("""{"voice":{"speechRate":0.5},"search":{"maxResults":1,"minScore":0.0}}""")).isValid)
        assertTrue(policy.validate(json("""{"voice":{"speechRate":2.0},"search":{"maxResults":50,"minScore":1.0}}""")).isValid)
        assertEquals(setOf("speech_rate_range"), invalidCodes(json("""{"voice":{"speechRate":0.4}}""")))
        assertEquals(setOf("speech_rate_range"), invalidCodes(json("""{"voice":{"speechRate":2.1}}""")))
        assertEquals(setOf("max_results_range"), invalidCodes(json("""{"search":{"maxResults":0}}""")))
        assertEquals(setOf("max_results_range"), invalidCodes(json("""{"search":{"maxResults":51}}""")))
        assertEquals(setOf("min_score_range"), invalidCodes(json("""{"search":{"minScore":-0.1}}""")))
    }

    @Test
    fun `unknown top-level and section fields are warnings not errors`() {
        val raw = json("""{"futureSection":{},"llm":{"futureField":1}}""")
        val validation = policy.validate(raw)
        assertTrue(validation.isValid)
        assertTrue("$.futureSection" in warningPaths(raw))
        assertTrue("llm.futureField" in warningPaths(raw))
    }

    @Test
    fun `unknown fields do not block sanitization`() {
        val raw = json("""{"extra":1,"llm":{"temperature":0.3,"extra2":2}}""")
        val doc = policy.sanitize(raw)
        assertEquals(0.3f, doc.llm.temperature)
        // other fields fall back to defaults
        assertEquals(0.9f, doc.llm.topP)
    }

    @Test
    fun `malformed typed values are errors and fall back to defaults`() {
        val raw = json("""{"llm":{"temperature":"hot"}}""")
        assertEquals(setOf("malformed_schema"), invalidCodes(raw))
        assertEquals(policy.defaults(), policy.sanitize(raw))
    }

    @Test
    fun `non-object section is a malformed error`() {
        val raw = json("""{"llm":5}""")
        assertFalse(policy.validate(raw).isValid)
        assertEquals(policy.defaults(), policy.sanitize(raw))
    }

    @Test
    fun `string schemaVersion is rejected`() {
        val raw = json("""{"schemaVersion":"1"}""")
        assertEquals(setOf("schema_version_type"), invalidCodes(raw))
        assertEquals(policy.defaults(), policy.sanitize(raw))
    }

    @Test
    fun `future schema version is unsupported`() {
        val raw = json("""{"schemaVersion":2}""")
        assertEquals(setOf("unsupported_schema_version"), invalidCodes(raw))
        assertEquals(policy.defaults(), policy.sanitize(raw))
    }

    @Test
    fun `migrate accepts a valid document without falling back`() {
        val migration = policy.migrate(
            json("""{"schemaVersion":1,"llm":{"temperature":0.6}}"""),
        )
        assertFalse(migration.fellBackToDefaults)
        assertEquals(0.6f, migration.document.llm.temperature)
        assertEquals(256, migration.document.llm.maxNewTokens)
    }

    @Test
    fun `migrate falls back for future version and malformed input`() {
        assertTrue(policy.migrate(json("""{"schemaVersion":3}""")).fellBackToDefaults)
        assertTrue(policy.migrate(json("""{"schemaVersion":"x"}""")).fellBackToDefaults)
        assertTrue(policy.migrate(json("""{"llm":{"temperature":"bad"}}""")).fellBackToDefaults)
        assertTrue(policy.migrate(json("""{"llm":{"temperature":-5.0}}""")).fellBackToDefaults)
    }

    @Test
    fun `migrate reports unknown-field warnings`() {
        val migration = policy.migrate(json("""{"unknownTop":1}"""))
        assertFalse(migration.fellBackToDefaults)
        assertTrue(migration.warnings.any { it.contains("unknownTop") })
    }

    @Test
    fun `bangla unicode survives parse and is reported intact`() {
        // No text field is accepted, but the parser must not corrupt Unicode anywhere.
        val raw = json("""{"নাম":1,"llm":{"temperature":0.7,"বর্ণনা":2}}""")
        val validation = policy.validate(raw)
        assertTrue(validation.isValid)
        assertTrue(warningPaths(raw).any { it.contains("নাম") })
        assertTrue(warningPaths(raw).any { it.contains("বর্ণনা") })
    }

    @Test
    fun `explicit value takes precedence over default and reset returns baseline`() {
        // precedence: explicit temperature overrides default; other fields keep defaults
        val doc = policy.sanitize(json("""{"llm":{"temperature":0.2}}"""))
        assertEquals(0.2f, doc.llm.temperature)
        assertEquals(0.9f, doc.llm.topP)
        // reset baseline
        assertEquals(policy.defaults(), policy.sanitize(json("{}")))
        assertEquals(-1L, doc.llm.seed)
    }

    @Test
    fun `typed validate flags a programmatically built bad document`() {
        val bad = SettingsDocumentV1(
            llm = LlmSettings(
                temperature = Float.NaN,
                maxNewTokens = 5000,
                context = ContextPolicy(maxContextTokens = 100),
            ),
        )
        val codes = policy.validate(bad).errors.map { it.code }.toSet()
        assertTrue("non_finite_number" in codes)
        assertTrue("max_new_tokens_range" in codes)
        assertTrue("max_context_range" in codes)
    }
}
