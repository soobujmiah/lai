package dev.lai.runtime.audit

import android.content.Context
import dev.lai.runtime.agent.ToolAuditLedger
import dev.lai.runtime.agent.ToolCall
import dev.lai.runtime.agent.ToolRisk
import dev.lai.runtime.core.LaiJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** App-private, no-backup, content-free, tamper-evident tool audit storage. */
class ToolAuditRepository internal constructor(
    private val auditFile: File,
    private val ledger: ToolAuditLedger,
) {
    constructor(context: Context) : this(
        File(context.noBackupFilesDir, "security/tool-audit-v1.jsonl"),
        ToolAuditLedger(),
    )

    private val mutex = Mutex()
    private val auditJson = Json(LaiJson) { ignoreUnknownKeys = false }

    suspend fun snapshot(): ToolAuditSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching { readVerified() }.getOrElse { error ->
                ToolAuditSnapshot(
                    records = emptyList(),
                    integrityValid = false,
                    detail = error.message ?: "Persistent tool audit could not be verified",
                )
            }
        }
    }

    suspend fun recordDecision(
        call: ToolCall,
        risk: ToolRisk,
        approved: Boolean,
    ): Result<ToolAuditSnapshot> = append(
        call = call,
        risk = risk,
        outcome = if (approved) ToolAuditOutcome.USER_APPROVED else ToolAuditOutcome.USER_DENIED,
    )

    suspend fun recordCompletion(
        call: ToolCall,
        risk: ToolRisk,
        success: Boolean,
    ): Result<ToolAuditSnapshot> = append(
        call = call,
        risk = risk,
        outcome = if (success) ToolAuditOutcome.EXECUTION_SUCCEEDED else ToolAuditOutcome.EXECUTION_FAILED,
    )

    private suspend fun append(
        call: ToolCall,
        risk: ToolRisk,
        outcome: ToolAuditOutcome,
    ): Result<ToolAuditSnapshot> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val current = readVerified().records
                val next = ledger.append(
                    records = current,
                    call = call,
                    risk = risk,
                    outcome = outcome,
                    eventId = UUID.randomUUID().toString(),
                    timestampEpochMs = System.currentTimeMillis(),
                )
                val updated = ledger.verify(current + next).also {
                    check(it.integrityValid) { it.detail }
                }
                val encoded = auditJson.encodeToString(ToolAuditRecordV1.serializer(), next) + "\n"
                val bytes = encoded.toByteArray(Charsets.UTF_8)
                require(bytes.size <= MAX_RECORD_BYTES) { "Audit event exceeds its size limit" }
                require(auditFile.length() + bytes.size <= MAX_AUDIT_BYTES) {
                    "Persistent tool audit reached its safety limit"
                }
                auditFile.parentFile?.mkdirs()
                FileOutputStream(auditFile, true).use { output ->
                    output.write(bytes)
                    output.fd.sync()
                }
                updated
            }
        }
    }

    private fun readVerified(): ToolAuditSnapshot {
        if (!auditFile.exists()) return ledger.verify(emptyList())
        require(auditFile.isFile) { "Tool audit path is not a regular file" }
        require(auditFile.length() <= MAX_AUDIT_BYTES) { "Persistent tool audit exceeds its safety limit" }
        val records = auditFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapIndexed { index, line ->
                require(line.isNotBlank()) { "Blank audit line at ${index + 1}" }
                require(line.length <= MAX_RECORD_CHARS) { "Audit line ${index + 1} exceeds its size limit" }
                auditJson.decodeFromString(ToolAuditRecordV1.serializer(), line)
            }.toList().also {
                require(it.size <= MAX_RECORDS) { "Persistent tool audit has too many events" }
            }
        }
        return ledger.verify(records).also { check(it.integrityValid) { it.detail } }
    }

    companion object {
        private const val MAX_AUDIT_BYTES = 2L * 1024L * 1024L
        private const val MAX_RECORD_BYTES = 4 * 1024
        private const val MAX_RECORD_CHARS = 4 * 1024
        private const val MAX_RECORDS = 4_000
    }
}
