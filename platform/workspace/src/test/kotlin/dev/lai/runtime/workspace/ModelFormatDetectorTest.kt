package dev.lai.runtime.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelFormatDetectorTest {
    @Test
    fun `recognizes the GGUF magic`() {
        assertEquals("gguf", ModelFormatDetector.detect("GGUF".toByteArray()))
        assertEquals("gguf", ModelFormatDetector.detect(byteArrayOf(0x47, 0x47, 0x55, 0x46)))
        // trailing bytes must not affect detection
        assertEquals("gguf", ModelFormatDetector.detect(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x00, 0x01, 0x02)))
    }

    @Test
    fun `rejects non gguf headers`() {
        assertNull(ModelFormatDetector.detect(byteArrayOf(0x00, 0x00, 0x00, 0x00)))
        assertNull(ModelFormatDetector.detect("gguf".toByteArray())) // lowercase is not the magic
        assertNull(ModelFormatDetector.detect("PK".toByteArray())) // zip/apk, unsupported
    }

    @Test
    fun `rejects short or empty buffers`() {
        assertNull(ModelFormatDetector.detect(ByteArray(0)))
        assertNull(ModelFormatDetector.detect(ByteArray(3)))
        assertNull(ModelFormatDetector.detect(byteArrayOf(0x47, 0x47, 0x55))) // one byte short
    }
}
