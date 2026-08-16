package dev.lai.runtime.inference

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedStreamCopyTest {
    @Test
    fun `copies a stream exactly at the inclusive limit`() {
        val inputBytes = ByteArray(32) { it.toByte() }
        val output = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()

        val transferred = copyBounded(
            input = ByteArrayInputStream(inputBytes),
            output = output,
            base = 0,
            maxTotalBytes = inputBytes.size.toLong(),
            progressStepBytes = 8,
            onProgress = progress::add,
        )

        assertEquals(32L, transferred)
        assertArrayEquals(inputBytes, output.toByteArray())
        assertEquals(32L, progress.last())
    }

    @Test
    fun `rejects an unknown-length stream before writing beyond the limit`() {
        val output = ByteArrayOutputStream()

        assertThrows(StreamLimitExceededException::class.java) {
            copyBounded(
                input = ByteArrayInputStream(ByteArray(33) { 7 }),
                output = output,
                base = 0,
                maxTotalBytes = 32,
            )
        }

        // The source fits in one read, so the pre-write check rejects the whole chunk.
        assertEquals(0, output.size())
    }

    @Test
    fun `resume base counts against the final artifact limit`() {
        val output = ByteArrayOutputStream()

        assertThrows(StreamLimitExceededException::class.java) {
            copyBounded(
                input = ByteArrayInputStream(ByteArray(11)),
                output = output,
                base = 90,
                maxTotalBytes = 100,
            )
        }

        assertEquals(0, output.size())
    }

    @Test
    fun `rejects invalid limits before reading`() {
        val input = ByteArrayInputStream(ByteArray(1))
        val output = ByteArrayOutputStream()

        assertThrows(IllegalArgumentException::class.java) {
            copyBounded(input, output, base = -1, maxTotalBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            copyBounded(input, output, base = 2, maxTotalBytes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            copyBounded(input, output, base = 0, maxTotalBytes = 1, progressStepBytes = 0)
        }
    }
}
