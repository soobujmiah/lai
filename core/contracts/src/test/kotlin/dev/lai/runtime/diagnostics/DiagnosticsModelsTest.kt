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
            automation = AutomationDiagnostics(
                toolProposalsEnabled = true,
                proposalResponsesExamined = 2,
                proposalAccepted = 0,
                proposalRejected = 1,
                proposalNotToolCall = 1,
                lastProposalOutcome = "REJECTED_INVALID_TOOL_CALL",
                proposalRejectionCodes = mapOf("INVALID_TOOL_CALL" to 1),
                records = listOf(ToolAuditDiagnostics("screen.click", "INTERACTION", true, true, 2)),
            ),
        )
        val json = LaiJson.encodeToString(DiagnosticsReportV1.serializer(), report)
        val decoded = LaiJson.decodeFromString(DiagnosticsReportV1.serializer(), json)
        assertEquals(report, decoded)
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertFalse(json.contains("promptText"))
        assertFalse(json.contains("generatedText"))
        assertFalse(json.contains("toolArguments"))
        assertEquals("screen.click", decoded.automation.records.single().toolName)
        assertEquals(2, decoded.automation.proposalResponsesExamined)
        assertEquals("REJECTED_INVALID_TOOL_CALL", decoded.automation.lastProposalOutcome)
        assertEquals(1, decoded.automation.proposalRejectionCodes["INVALID_TOOL_CALL"])
        assertEquals("APP_PRIVATE_HASH_CHAIN_V1", decoded.automation.auditPersistence)
        assertTrue(decoded.automation.auditIntegrityValid)
        assertTrue("tool_call_fingerprints" in decoded.privacy.excludedData)
        assertTrue("tool_audit_hashes" in decoded.privacy.excludedData)
        assertTrue(decoded.privacy.localOnlyUntilUserExport)
    }
}
