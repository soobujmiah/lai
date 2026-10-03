package dev.lai.runtime.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OcrLanguagePolicyTest {
    @Test fun `mixed language model order is stable`() {
        assertEquals(
            listOf("bn" to "ben", "en" to "eng"),
            OcrLanguagePolicy.select(OcrRequest(listOf("en", "bn", "en"))),
        )
    }

    @Test fun `unsupported language cannot silently use another model`() {
        assertThrows(IllegalArgumentException::class.java) {
            OcrLanguagePolicy.select(OcrRequest(listOf("hi")))
        }
    }

    @Test fun `empty language request fails closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            OcrLanguagePolicy.select(OcrRequest(emptyList()))
        }
    }
}
