package dev.lai.runtime.agent

import dev.lai.runtime.audit.ToolAuditOutcome
import dev.lai.runtime.audit.ToolAuditRecordV1
import dev.lai.runtime.audit.ToolAuditSnapshot
import dev.lai.runtime.core.LaiJson
import java.security.MessageDigest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Pure-JVM hash-chain and transition policy; Android file ownership stays in platform:audit. */
class ToolAuditLedger(
    private val parser: ToolCallParser = ToolCallParser(),
) {
    fun fingerprint(call: ToolCall): String {
        require(parser.validate(call) is ToolCallParseResult.Accepted) { "Cannot fingerprint an invalid tool call" }
        val canonicalCall = call.copy(arguments = canonicalize(call.arguments) as JsonObject)
        val canonical = LaiJson.encodeToString(ToolCall.serializer(), canonicalCall)
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    fun append(
        records: List<ToolAuditRecordV1>,
        call: ToolCall,
        risk: ToolRisk,
        outcome: ToolAuditOutcome,
        eventId: String,
        timestampEpochMs: Long,
    ): ToolAuditRecordV1 {
        val verified = verify(records)
        require(verified.integrityValid) { verified.detail }
        require(EVENT_ID.matches(eventId)) { "Invalid audit event id" }
        require(records.none { it.eventId == eventId }) { "Duplicate audit event id" }
        require(timestampEpochMs > 0) { "Invalid audit timestamp" }
        val definition = BuiltInToolCatalog.definition(call.name) ?: error("Unknown audited tool")
        require(definition.risk == risk) { "Audit risk does not match the canonical tool definition" }
        val fingerprint = fingerprint(call)
        enforceTransition(records, fingerprint, outcome)
        val draft = ToolAuditRecordV1(
            sequence = (records.lastOrNull()?.sequence ?: 0L) + 1L,
            eventId = eventId,
            callFingerprint = fingerprint,
            toolName = call.name,
            risk = risk,
            outcome = outcome,
            timestampEpochMs = timestampEpochMs,
            previousRecordHash = records.lastOrNull()?.recordHash ?: GENESIS_HASH,
            recordHash = "",
        )
        return draft.copy(recordHash = recordHash(draft))
    }

    fun verify(records: List<ToolAuditRecordV1>): ToolAuditSnapshot {
        var previousHash = GENESIS_HASH
        val approved = mutableSetOf<String>()
        val completed = mutableSetOf<String>()
        val eventIds = mutableSetOf<String>()
        records.forEachIndexed { index, record ->
            val reason = validateRecord(record, index + 1L, previousHash, approved, completed, eventIds)
            if (reason != null) return ToolAuditSnapshot(emptyList(), false, reason)
            previousHash = record.recordHash
            eventIds += record.eventId
            when (record.outcome) {
                ToolAuditOutcome.USER_APPROVED -> approved += record.callFingerprint
                ToolAuditOutcome.EXECUTION_SUCCEEDED,
                ToolAuditOutcome.EXECUTION_FAILED -> completed += record.callFingerprint
                ToolAuditOutcome.USER_DENIED -> Unit
            }
        }
        return ToolAuditSnapshot(records.toList(), true, "Verified ${records.size} chained audit event(s)")
    }

    private fun validateRecord(
        record: ToolAuditRecordV1,
        expectedSequence: Long,
        expectedPreviousHash: String,
        approved: Set<String>,
        completed: Set<String>,
        eventIds: Set<String>,
    ): String? {
        if (record.schemaVersion != 1) return "Unsupported audit schema at sequence $expectedSequence"
        if (record.sequence != expectedSequence) return "Audit sequence discontinuity at $expectedSequence"
        if (!EVENT_ID.matches(record.eventId)) return "Invalid audit event id at $expectedSequence"
        if (record.eventId in eventIds) return "Duplicate audit event id at $expectedSequence"
        if (!SHA256.matches(record.callFingerprint)) return "Invalid call fingerprint at $expectedSequence"
        if (!SHA256.matches(record.recordHash)) return "Invalid record hash at $expectedSequence"
        if (record.previousRecordHash != expectedPreviousHash) return "Audit chain mismatch at $expectedSequence"
        if (record.timestampEpochMs <= 0) return "Invalid audit timestamp at $expectedSequence"
        val definition = BuiltInToolCatalog.definition(record.toolName)
            ?: return "Unknown audited tool at $expectedSequence"
        if (definition.risk != record.risk) return "Audit risk mismatch at $expectedSequence"
        if (recordHash(record.copy(recordHash = "")) != record.recordHash) {
            return "Audit record hash mismatch at $expectedSequence"
        }
        when (record.outcome) {
            ToolAuditOutcome.USER_APPROVED -> {
                if (record.callFingerprint in approved) return "Duplicate approval at $expectedSequence"
            }
            ToolAuditOutcome.EXECUTION_SUCCEEDED,
            ToolAuditOutcome.EXECUTION_FAILED -> {
                if (record.callFingerprint !in approved) return "Execution without approval at $expectedSequence"
                if (record.callFingerprint in completed) return "Duplicate completion at $expectedSequence"
            }
            ToolAuditOutcome.USER_DENIED -> Unit
        }
        return null
    }

    private fun enforceTransition(
        records: List<ToolAuditRecordV1>,
        fingerprint: String,
        outcome: ToolAuditOutcome,
    ) {
        val matching = records.filter { it.callFingerprint == fingerprint }
        when (outcome) {
            ToolAuditOutcome.USER_APPROVED -> require(matching.none { it.outcome == ToolAuditOutcome.USER_APPROVED }) {
                "This exact tool call was already approved and cannot be replayed"
            }
            ToolAuditOutcome.EXECUTION_SUCCEEDED,
            ToolAuditOutcome.EXECUTION_FAILED -> {
                require(matching.any { it.outcome == ToolAuditOutcome.USER_APPROVED }) {
                    "Tool completion has no recorded approval"
                }
                require(matching.none {
                    it.outcome == ToolAuditOutcome.EXECUTION_SUCCEEDED ||
                        it.outcome == ToolAuditOutcome.EXECUTION_FAILED
                }) { "Tool call already has a completion record" }
            }
            ToolAuditOutcome.USER_DENIED -> Unit
        }
    }

    private fun canonicalize(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.toSortedMap().mapValues { canonicalize(it.value) })
        is JsonArray -> JsonArray(element.map(::canonicalize))
        else -> element
    }

    private fun recordHash(record: ToolAuditRecordV1): String {
        val canonical = LaiJson.encodeToString(ToolAuditRecordV1.serializer(), record)
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    companion object {
        private val SHA256 = Regex("^[a-f0-9]{64}$")
        private val EVENT_ID = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{7,79}$")
        private const val GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000"
    }
}
