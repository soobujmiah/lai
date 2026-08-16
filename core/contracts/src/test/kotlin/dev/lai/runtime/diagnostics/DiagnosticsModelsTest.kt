package dev.lai.runtime.diagnostics

import dev.lai.runtime.core.LaiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsModelsTest {
    @Test
    fun `diagnostics JSON is versioned and excludes content fields`() {
        val report = DiagnosticsReportV1(
            generatedAtEpochMs = 1,
            app = AppDiagnostics("1.0", 1, false, "READY", "cache"),
            device = DeviceDiagnostics("vendor", "model", 35, listOf("arm64-v8a"), 100, 80, false, "NOMINAL"),
            runtime = RuntimeDiagnostics(true, listOf("CPU"), "CPU", 4096, "model", 10, 200, true, "READY", 0),
            models = listOf(ModelDiagnostics("model", "Model", 100, "a".repeat(64), true)),
            performance = listOf(GenerationPerformanceDiagnostics(10, 2, 5, 6, 20, 26, 2000.0, 100.0)),
            privacy = DiagnosticsPrivacy(),
        )
        val json = LaiJson.encodeToString(DiagnosticsReportV1.serializer(), report)
        val decoded = LaiJson.decodeFromString(DiagnosticsReportV1.serializer(), json)
        assertEquals(report, decoded)
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertFalse(json.contains("promptText"))
        assertFalse(json.contains("generatedText"))
        assertTrue(decoded.privacy.localOnlyUntilUserExport)
    }
}
