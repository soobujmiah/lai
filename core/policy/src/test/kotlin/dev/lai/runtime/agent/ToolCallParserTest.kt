package dev.lai.runtime.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParserTest {
    private val parser = ToolCallParser()

    @Test
    fun `natural assistant text is not treated as a tool call`() {
        assertTrue(parser.parse("আমি আপনাকে ধাপগুলো বুঝিয়ে দিচ্ছি।") is ToolCallParseResult.NotToolCall)
        assertTrue(parser.parse("{\"answer\":\"ordinary JSON\"}") is ToolCallParseResult.NotToolCall)
        assertTrue(parser.parse("{ordinary braces}") is ToolCallParseResult.NotToolCall)
    }

    @Test
    fun `strict nested type proposal preserves Bangla and requires trusted review`() {
        val result = parser.parse(
            """{"id":"call-1","name":"screen.type","arguments":{"selector":{"viewId":"com.example:id/input"},"text":"বাংলা লিখুন"}}""",
        ) as ToolCallParseResult.Accepted
        assertEquals("screen.type", result.call.name)
        assertEquals("বাংলা লিখুন", result.call.arguments["text"].toString().trim('"'))
        assertEquals(ToolRisk.SENSITIVE, result.definition.risk)
        assertTrue(result.definition.requiresConfirmation)
        assertTrue(result.confirmationSummary.contains("বাংলা লিখুন"))

        val escaped = parser.parse(
            """{"id":"call-2","name":"screen.type","arguments":{"selector":{"viewId":"field"},"text":"line 1\nline 2"}}""",
        ) as ToolCallParseResult.Accepted
        assertTrue(escaped.confirmationSummary.contains("\\n"))
        assertFalse(escaped.confirmationSummary.contains('\n'))
    }

    @Test
    fun `all built in envelope shapes have a validated path`() {
        val accepted = listOf(
            """{"id":"a1","name":"screen.snapshot","arguments":{}}""",
            """{"id":"a2","name":"screen.click","arguments":{"text":"Continue"}}""",
            """{"id":"a3","name":"screen.scroll","arguments":{"selector":{"path":[0,2,1]},"forward":false}}""",
            """{"id":"a4","name":"system.global_action","arguments":{"action":"back"}}""",
            """{"id":"a5","name":"app.launch","arguments":{"package":"com.example.app"}}""",
            """{"id":"a6","name":"ocr.current_screen","arguments":{}}""",
            """{"id":"a7","name":"shell.operation","arguments":{"operation":"settings.get","arguments":{"namespace":"system","key":"screen_brightness"}}}""",
        )
        accepted.forEach { source ->
            assertTrue(source, parser.parse(source) is ToolCallParseResult.Accepted)
        }
    }

    @Test
    fun `model cannot smuggle confirmation or sensitive input authority`() {
        val confirmed = parser.parse(
            """{"id":"x","name":"screen.click","arguments":{"text":"OK"},"confirmed":true}""",
        ) as ToolCallParseResult.Rejected
        assertTrue(confirmed.message.contains("Unknown field"))

        val sensitive = parser.parse(
            """{"id":"x","name":"screen.type","arguments":{"selector":{"viewId":"field"},"text":"secret","allowSensitiveInput":true}}""",
        ) as ToolCallParseResult.Rejected
        assertTrue(sensitive.message.contains("Unknown field"))
        assertFalse(BuiltInToolCatalog.modelInstruction.contains("confirmed:true"))
    }

    @Test
    fun `malformed and oversized tool intent fails closed`() {
        val malformed = parser.parse("""{"id":"x","name":"screen.click","arguments": }""")
        assertEquals("MALFORMED_TOOL_JSON", (malformed as ToolCallParseResult.Rejected).code)

        val oversized = "{\"name\":\"screen.click\",\"arguments\":{},\"padding\":\"" + "x".repeat(17_000) + "\"}"
        assertEquals("TOOL_CALL_TOO_LARGE", (parser.parse(oversized) as ToolCallParseResult.Rejected).code)
    }

    @Test
    fun `unknown tools selectors and shell injection are rejected`() {
        val rejected = listOf(
            """{"id":"x","name":"network.upload","arguments":{}}""",
            """{"id":"x","name":"screen.click","arguments":{}}""",
            """{"id":"x","name":"screen.click","arguments":{"path":[0,-1]}}""",
            """{"id":"x","name":"screen.click","arguments":{"path":["0"]}}""",
            """{"id":"x","name":"screen.scroll","arguments":{"forward":"true"}}""",
            """{"id":"x","name":"screen.type","arguments":{"viewId":"field","text":"old ambiguous shape"}}""",
            """{"id":"x","name":"system.global_action","arguments":{"action":"power"}}""", 
            """{"id":"x","name":"app.launch","arguments":{"package":"com.safe.app;id"}}""",
            """{"id":"x","name":"shell.operation","arguments":{"operation":"package.force_stop","arguments":{"package":"com.safe.app;id"}}}""",
            """{"id":"x","name":"shell.operation","arguments":{"operation":"device.info","arguments":{"extra":"id"}}}""",
        )
        rejected.forEach { source ->
            assertTrue(source, parser.parse(source) is ToolCallParseResult.Rejected)
        }
    }

    @Test
    fun `catalog and runtime expose the same unique tool names`() {
        assertEquals(8, BuiltInToolCatalog.definitions.size)
        assertEquals(
            BuiltInToolCatalog.definitions.size,
            BuiltInToolCatalog.definitions.map { it.name }.distinct().size,
        )
    }
}
