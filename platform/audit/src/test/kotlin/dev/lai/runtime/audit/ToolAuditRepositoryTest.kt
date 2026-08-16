package dev.lai.runtime.audit

import dev.lai.runtime.agent.ToolAuditLedger
import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.agent.ToolRisk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ToolAuditRepositoryTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val call = ToolCall(
        id = "persistent-call-1",
        name = "app.launch",
        arguments = buildJsonObject { put("package", "com.private.example") },
    )

    @Test
    fun `audit survives repository recreation without storing call content`() = runBlocking {
        val file = File(temporary.root, "security/tool-audit-v1.jsonl")
        val first = ToolAuditRepository(file, ToolAuditLedger())
        assertTrue(first.snapshot().integrityValid)
        first.recordDecision(call, ToolRisk.INTERACTION, approved = false).getOrThrow()
        first.recordDecision(call, ToolRisk.INTERACTION, approved = true).getOrThrow()
        first.recordCompletion(call, ToolRisk.INTERACTION, success = true).getOrThrow()

        val reopened = ToolAuditRepository(file, ToolAuditLedger())
        val snapshot = reopened.snapshot()
        assertTrue(snapshot.integrityValid)
        assertTrue(snapshot.records.size == 3)
        val persisted = file.readText()
        assertFalse(persisted.contains("com.private.example"))
        assertFalse(persisted.contains("arguments"))
        assertTrue(persisted.contains("recordHash"))
    }

    @Test
    fun `exact approval replay is blocked across repository recreation`() = runBlocking {
        val file = File(temporary.root, "tool-audit-v1.jsonl")
        ToolAuditRepository(file, ToolAuditLedger())
            .recordDecision(call, ToolRisk.INTERACTION, approved = true)
            .getOrThrow()
        val replay = ToolAuditRepository(file, ToolAuditLedger())
            .recordDecision(call, ToolRisk.INTERACTION, approved = true)
        assertTrue(replay.isFailure)
        assertTrue(ToolAuditRepository(file, ToolAuditLedger()).snapshot().integrityValid)
    }

    @Test
    fun `partial or corrupt line disables further audit writes`() = runBlocking {
        val file = File(temporary.root, "tool-audit-v1.jsonl")
        val repository = ToolAuditRepository(file, ToolAuditLedger())
        repository.recordDecision(call, ToolRisk.INTERACTION, approved = false).getOrThrow()
        file.appendText("{broken\n")

        assertFalse(repository.snapshot().integrityValid)
        assertTrue(repository.recordDecision(call.copy(id = "persistent-call-2"), ToolRisk.INTERACTION, true).isFailure)
    }
}
