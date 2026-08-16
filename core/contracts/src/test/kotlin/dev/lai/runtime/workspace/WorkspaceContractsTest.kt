package dev.lai.runtime.workspace

import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.settings.SettingsDocumentV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceContractsTest {
    @Test
    fun `layout constants are canonical and shared`() {
        assertEquals(listOf("config", "settings.json"), WorkspaceLayout.settingsRelativeSegments)
        assertEquals("settings.json", WorkspaceLayout.SETTINGS_FILE_NAME)
        assertTrue(WorkspaceLayout.MODELS_DIRECTORY in WorkspaceLayout.managedDirectories)
        assertTrue(WorkspaceLayout.CONFIG_DIRECTORY in WorkspaceLayout.managedDirectories)
        assertEquals(listOf("models", "tools", "config", "cache"), WorkspaceLayout.managedDirectories)
    }

    @Test
    fun `discovery limits defaults match the bounded policy`() {
        val limits = DiscoveryLimits()
        assertEquals(64, limits.maxRegisteredFiles)
        assertEquals(4L * 1024 * 1024 * 1024, limits.maxFileBytes)
        assertEquals(setOf("gguf"), limits.supportedFormats)
    }

    @Test
    fun `model candidate and discovered model round trip`() {
        val candidate = ModelCandidate(
            relativePath = "models/qwen.gguf",
            fileName = "qwen.gguf",
            sizeBytes = 1234L,
            sha256 = "abc",
            modelFormat = "gguf",
        )
        val discovered = DiscoveredModel(
            relativePath = candidate.relativePath,
            fileName = candidate.fileName,
            sizeBytes = candidate.sizeBytes,
            sha256 = candidate.sha256,
            modelFormat = candidate.modelFormat,
            status = ModelDiscoveryStatus.REVIEWED,
            reviewedCatalogId = "qwen2.5-1.5b-instruct-q4-k-m",
        )
        assertEquals(candidate, LaiJson.decodeFromString(ModelCandidate.serializer(), LaiJson.encodeToString(ModelCandidate.serializer(), candidate)))
        assertEquals(discovered, LaiJson.decodeFromString(DiscoveredModel.serializer(), LaiJson.encodeToString(DiscoveredModel.serializer(), discovered)))
    }

    @Test
    fun `a settings load outcome carries provenance alongside a usable document`() {
        val fallback = SettingsLoadOutcome(
            document = SettingsDocumentV1(),
            fromFile = false,
            fellBackToDefaults = true,
            warnings = listOf("workspace not granted"),
        )
        // The document is always usable, even when the read failed.
        assertEquals(SettingsDocumentV1(), fallback.document)
        assertEquals(listOf("workspace not granted"), fallback.warnings)
        assertEquals(fallback, fallback.copy())

        val loaded = fallback.copy(fromFile = true, fellBackToDefaults = false, warnings = emptyList())
        assertNotEquals(fallback, loaded)
    }

    @Test
    fun `grant state round trips`() {
        assertEquals(
            WorkspaceGrantState.GRANTED,
            LaiJson.decodeFromString(WorkspaceGrantState.serializer(), LaiJson.encodeToString(WorkspaceGrantState.serializer(), WorkspaceGrantState.GRANTED)),
        )
    }
}
