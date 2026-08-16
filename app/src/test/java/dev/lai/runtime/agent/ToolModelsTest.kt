package dev.lai.runtime.agent

import dev.lai.runtime.core.LaiJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolModelsTest {
    @Test
    fun `tool call JSON round trips Bangla text`() {
        val source = """{"id":"1","name":"screen.type","arguments":{"text":"বাংলা লিখুন","viewId":"field"}}"""
        val parsed = LaiJson.decodeFromString<ToolCall>(source)
        assertEquals("screen.type", parsed.name)
        assertEquals("বাংলা লিখুন", parsed.arguments["text"].toString().trim('"'))
        assertEquals(parsed, LaiJson.decodeFromString<ToolCall>(LaiJson.encodeToString(ToolCall.serializer(), parsed)))
    }
}
