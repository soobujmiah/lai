package dev.lai.runtime.ocr

import dev.lai.runtime.core.LaiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrContractsTest {
    @Test
    fun `Bangla OCR schema round trips geometry and language`() {
        val result = OcrResult(
            fullText = "বাংলা লেখা",
            blocks = listOf(
                OcrBlock(
                    text = "বাংলা লেখা",
                    language = "bn",
                    confidence = 0.98f,
                    polygon = listOf(OcrPoint(1, 2), OcrPoint(3, 4)),
                    handwritten = false,
                ),
            ),
            processingTimeMs = 10,
            engine = "test",
        )
        val decoded = LaiJson.decodeFromString(
            OcrResult.serializer(),
            LaiJson.encodeToString(OcrResult.serializer(), result),
        )
        assertEquals(result, decoded)
        assertEquals(listOf("bn", "en"), OcrRequest().languages)
        assertTrue(OcrRequest().includeConfidence)
    }
}
