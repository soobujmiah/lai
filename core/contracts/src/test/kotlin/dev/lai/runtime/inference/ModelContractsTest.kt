package dev.lai.runtime.inference

import dev.lai.runtime.core.LaiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelContractsTest {
    @Test
    fun `download progress exposes safe fraction`() {
        assertEquals(0.5f, DownloadProgress(50, 100).fraction)
        assertEquals(1.0f, DownloadProgress(120, 100).fraction)
        assertNull(DownloadProgress(1, null).fraction)
        assertNull(DownloadProgress(1, 0).fraction)
    }

    @Test
    fun `installed model metadata round trips`() {
        val model = InstalledModel(
            id = "qwen-test",
            displayName = "Qwen Test",
            fileName = "qwen-test.gguf",
            bytes = 1024,
            sha256 = "a".repeat(64),
            sourceUrl = "https://huggingface.co/test/model.gguf",
            installedAtEpochMs = 42,
        )
        val json = LaiJson.encodeToString(InstalledModel.serializer(), model)
        assertEquals(model, LaiJson.decodeFromString(InstalledModel.serializer(), json))
        assertNull(ModelSpec("id", "name", "https://huggingface.co/x").sha256)
        val importSpec = ModelImportSpec("qwen", "Qwen", "b".repeat(64), 1024)
        assertEquals(1024L, importSpec.expectedBytes)
        assertEquals(64, importSpec.sha256.length)
    }
}
