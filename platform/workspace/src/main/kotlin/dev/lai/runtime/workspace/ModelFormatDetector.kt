package dev.lai.runtime.workspace

/**
 * Detects a model container format from its leading bytes. Pure so it can be unit-tested on the JVM
 * without SAF or a real file. Currently recognizes the GGUF magic (`"GGUF"` = 0x47 0x47 0x55 0x46);
 * future formats plug in here without changing the discovery traversal.
 */
internal object ModelFormatDetector {
    private const val MAGIC_LENGTH = 4

    // GGUF little-endian uint32 magic 0x46554747 -> bytes 'G','G','U','F'.
    private val GGUF_MAGIC = byteArrayOf(0x47, 0x47, 0x55, 0x46)

    /** Returns a supported format id (e.g. `"gguf"`) or null when the header is unrecognized/short. */
    fun detect(firstBytes: ByteArray): String? {
        if (firstBytes.size < MAGIC_LENGTH) return null
        for (i in 0 until MAGIC_LENGTH) {
            if (firstBytes[i] != GGUF_MAGIC[i]) return null
        }
        return "gguf"
    }
}
