package dev.lai.runtime.audit

import dev.lai.runtime.agent.ToolRisk
import kotlinx.serialization.Serializable

@Serializable
enum class ToolAuditOutcome { USER_DENIED, USER_APPROVED, EXECUTION_SUCCEEDED, EXECUTION_FAILED }

/**
 * Content-free persistent security event. Fingerprints are one-way correlation values; arguments and outputs are absent.
 */
@Serializable
data class ToolAuditRecordV1(
    val schemaVersion: Int = 1,
    val sequence: Long,
    val eventId: String,
    val callFingerprint: String,
    val toolName: String,
    val risk: ToolRisk,
    val outcome: ToolAuditOutcome,
    val timestampEpochMs: Long,
    val previousRecordHash: String,
    val recordHash: String,
)

data class ToolAuditSnapshot(
    val records: List<ToolAuditRecordV1>,
    val integrityValid: Boolean,
    val detail: String,
)
