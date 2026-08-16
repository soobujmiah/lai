package dev.lai.runtime.agent

import dev.lai.runtime.audit.ToolAuditOutcome
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolAuditLedgerTest {
    private val ledger = ToolAuditLedger()
    private val call = ToolCall(
        id = "call-audit-1",
        name = "app.launch",
        arguments = buildJsonObject { put("package", "com.example.app") },
    )

    @Test
    fun `approval and completion form a valid content free hash chain`() {
        val approved = ledger.append(
            emptyList(),
            call,
            ToolRisk.INTERACTION,
            ToolAuditOutcome.USER_APPROVED,
            "event-0001",
            1,
        )
        val completed = ledger.append(
            listOf(approved),
            call,
            ToolRisk.INTERACTION,
            ToolAuditOutcome.EXECUTION_SUCCEEDED,
            "event-0002",
            2,
        )
        val snapshot = ledger.verify(listOf(approved, completed))
        assertTrue(snapshot.integrityValid)
        assertTrue(approved.recordHash.matches(Regex("^[a-f0-9]{64}$")))
        assertTrue(completed.previousRecordHash == approved.recordHash)
        val persistedText = approved.toString() + completed.toString()
        assertFalse(persistedText.contains("com.example.app"))
    }

    @Test
    fun `fingerprint canonicalizes object key order`() {
        val first = ToolCall(
            "call-order",
            "screen.click",
            buildJsonObject {
                put("viewId", "com.example:id/button")
                put("text", "Continue")
            },
        )
        val reordered = ToolCall(
            "call-order",
            "screen.click",
            buildJsonObject {
                put("text", "Continue")
                put("viewId", "com.example:id/button")
            },
        )
        assertTrue(ledger.fingerprint(first) == ledger.fingerprint(reordered))
    }

    @Test
    fun `denial is valid without execution authority`() {
        val denied = ledger.append(
            emptyList(),
            call,
            ToolRisk.INTERACTION,
            ToolAuditOutcome.USER_DENIED,
            "event-0001",
            1,
        )
        assertTrue(ledger.verify(listOf(denied)).integrityValid)
    }

    @Test
    fun `tampering breaks the chain`() {
        val approved = ledger.append(
            emptyList(), call, ToolRisk.INTERACTION, ToolAuditOutcome.USER_APPROVED, "event-0001", 1,
        )
        assertFalse(ledger.verify(listOf(approved.copy(toolName = "screen.click"))).integrityValid)
        assertFalse(ledger.verify(listOf(approved.copy(recordHash = "a".repeat(64)))).integrityValid)
    }

    @Test
    fun `duplicate approval and completion without approval fail closed`() {
        val approved = ledger.append(
            emptyList(), call, ToolRisk.INTERACTION, ToolAuditOutcome.USER_APPROVED, "event-0001", 1,
        )
        assertTrue(
            runCatching {
                ledger.append(
                    listOf(approved), call, ToolRisk.INTERACTION, ToolAuditOutcome.USER_APPROVED, "event-0002", 2,
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ledger.append(
                    listOf(approved), call, ToolRisk.INTERACTION, ToolAuditOutcome.USER_DENIED, "event-0001", 2,
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ledger.append(
                    emptyList(), call, ToolRisk.INTERACTION, ToolAuditOutcome.EXECUTION_FAILED, "event-0003", 3,
                )
            }.isFailure,
        )
    }

    @Test
    fun `invalid calls and mismatched risk cannot enter audit`() {
        val invalid = call.copy(arguments = buildJsonObject { put("package", "com.safe.app;id") })
        assertTrue(runCatching { ledger.fingerprint(invalid) }.isFailure)
        assertTrue(
            runCatching {
                ledger.append(
                    emptyList(), call, ToolRisk.ELEVATED, ToolAuditOutcome.USER_DENIED, "event-0001", 1,
                )
            }.isFailure,
        )
    }
}
