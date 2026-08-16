package dev.lai.runtime.workspace

import dev.lai.runtime.core.LaiJson
import dev.lai.runtime.settings.SettingsDocumentV1
import dev.lai.runtime.settings.SettingsPolicy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Pure workspace decision logic. No Android, network, or file I/O lives here.
 *
 * - [classify] turns SAF-observed [ModelCandidate] values into [DiscoveredModel] statuses:
 *   size/format/digest validation, deduplication by SHA-256, reviewed-catalog matching, and
 *   a registration count limit. `REVIEWED`/`LOCAL_UNREVIEWED` may be registered; `REJECTED`
 *   never is, and registration never allocates weights or auto-loads inference.
 * - [WorkspaceSettingsCodec] encodes/decodes the bounded, non-secret `settings.json` document,
 *   enforcing a strict maximum byte size, strict JSON, and schema validation. On read it falls
 *   back to embedded defaults; on write it verifies the candidate is exactly the v1 schema so the
 *   file can never become a prompt/document/credential dump.
 */
class WorkspacePolicy {

    /**
     * @param candidates raw SAF-observed files (any order; output is sorted by relative path).
     * @param reviewedBySha256 lowercased-hex SHA-256 → reviewed catalog model id.
     * @param limits bounded size/format/count policy.
     */
    fun classify(
        candidates: List<ModelCandidate>,
        reviewedBySha256: Map<String, String>,
        limits: DiscoveryLimits = DiscoveryLimits(),
    ): List<DiscoveredModel> {
        val reviewed = reviewedBySha256.mapKeys { it.key.lowercase() }
        val seenDigests = HashSet<String>()

        // Pass 1: per-candidate base status, tracking digests for dedup, in deterministic path order.
        val staged = candidates.sortedBy { it.relativePath }.map { candidate ->
            baseStatus(candidate, reviewed, limits, seenDigests)
        }

        // Pass 2: the registration count limit applies only to registerable (non-rejected) files.
        var registered = 0
        return staged.map { stage ->
            when {
                stage.status == ModelDiscoveryStatus.REJECTED -> stage.toDiscovered()
                registered >= limits.maxRegisteredFiles -> DiscoveredModel(
                    relativePath = stage.candidate.relativePath,
                    fileName = stage.candidate.fileName,
                    sizeBytes = stage.candidate.sizeBytes,
                    sha256 = stage.candidate.sha256,
                    modelFormat = stage.candidate.modelFormat,
                    status = ModelDiscoveryStatus.REJECTED,
                    reviewedCatalogId = null,
                    rejectionReason = "discovery_limit",
                )
                else -> { registered++; stage.toDiscovered() }
            }
        }
    }

    private fun baseStatus(
        candidate: ModelCandidate,
        reviewed: Map<String, String>,
        limits: DiscoveryLimits,
        seenDigests: HashSet<String>,
    ): Stage {
        if (candidate.sizeBytes > limits.maxFileBytes) {
            return Stage(candidate, ModelDiscoveryStatus.REJECTED, null, "oversized")
        }
        val format = candidate.modelFormat
        if (format == null || format !in limits.supportedFormats) {
            return Stage(candidate, ModelDiscoveryStatus.REJECTED, null, "unsupported_format")
        }
        val digest = candidate.sha256?.lowercase()
        if (digest == null) {
            return Stage(candidate, ModelDiscoveryStatus.REJECTED, null, "missing_digest")
        }
        if (!seenDigests.add(digest)) {
            return Stage(candidate, ModelDiscoveryStatus.REJECTED, null, "duplicate_digest")
        }
        val catalogId = reviewed[digest]
        return if (catalogId != null) {
            Stage(candidate, ModelDiscoveryStatus.REVIEWED, catalogId, null)
        } else {
            Stage(candidate, ModelDiscoveryStatus.LOCAL_UNREVIEWED, null, null)
        }
    }

    private data class Stage(
        val candidate: ModelCandidate,
        val status: ModelDiscoveryStatus,
        val reviewedCatalogId: String?,
        val rejectionReason: String?,
    ) {
        fun toDiscovered() = DiscoveredModel(
            relativePath = candidate.relativePath,
            fileName = candidate.fileName,
            sizeBytes = candidate.sizeBytes,
            sha256 = candidate.sha256,
            modelFormat = candidate.modelFormat,
            status = status,
            reviewedCatalogId = if (status == ModelDiscoveryStatus.REVIEWED) reviewedCatalogId else null,
            rejectionReason = if (status == ModelDiscoveryStatus.REJECTED) rejectionReason else null,
        )
    }
}

/**
 * Bounded encode/decode of the non-secret `settings.json` document. Pure: the Android store
 * supplies bytes and consumes [DecodeOutcome]; it owns the temp-write-then-replace strategy and
 * the persistable SAF permission.
 */
class WorkspaceSettingsCodec(private val policy: SettingsPolicy = SettingsPolicy()) {

    /** UTF-8 JSON encoding of [document] using LAI's stable config. */
    fun encode(document: SettingsDocumentV1): ByteArray =
        LaiJson.encodeToString(SettingsDocumentV1.serializer(), document).toByteArray(Charsets.UTF_8)

    sealed interface DecodeOutcome {
        /** A document was produced. [fellBackToDefaults] is true when the input was unsafe and defaults were substituted. */
        data class Loaded(
            val document: SettingsDocumentV1,
            val fellBackToDefaults: Boolean,
            val warnings: List<String>,
        ) : DecodeOutcome
        data class Malformed(val reason: String) : DecodeOutcome
        data object Oversized : DecodeOutcome
        data object Absent : DecodeOutcome
    }

    /**
     * Lenient read used when loading existing settings: an absent/empty file is [DecodeOutcome.Absent],
     * an oversized file is [DecodeOutcome.Oversized], invalid JSON is [DecodeOutcome.Malformed], and an
     * unsupported/out-of-range but parseable document falls back to safe defaults ([DecodeOutcome.Loaded]
     * with `fellBackToDefaults = true`) rather than crashing.
     */
    fun decode(bytes: ByteArray?, maxBytes: Int): DecodeOutcome {
        if (bytes == null || bytes.isEmpty()) return DecodeOutcome.Absent
        if (bytes.size > maxBytes) return DecodeOutcome.Oversized
        val raw = runCatching {
            LaiJson.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        }.getOrElse { return DecodeOutcome.Malformed(it.message ?: "not valid JSON") }
        val migration = policy.migrate(raw)
        return DecodeOutcome.Loaded(migration.document, migration.fellBackToDefaults, migration.warnings)
    }

    /**
     * Strict verification used before persisting a candidate document. The stored file must be exactly
     * the v1 schema: bounded size, valid JSON, no range/type errors, and **no unknown fields** — so
     * `settings.json` can never absorb prompts, documents, selectors, packages, or credentials.
     */
    fun verifyForStorage(bytes: ByteArray, maxBytes: Int): StorageVerification {
        if (bytes.isEmpty()) return StorageVerification(false, listOf("document is empty"))
        if (bytes.size > maxBytes) return StorageVerification(false, listOf("document exceeds ${maxBytes} bytes"))
        val raw = runCatching {
            LaiJson.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        }.getOrElse { return StorageVerification(false, listOf("document is not valid JSON")) }
        val issues = policy.validate(raw).issues
        return if (issues.isEmpty()) {
            StorageVerification(true, emptyList())
        } else {
            StorageVerification(false, issues.map { "${it.path}: ${it.message}" })
        }
    }

    fun isStorable(bytes: ByteArray, maxBytes: Int): Boolean = verifyForStorage(bytes, maxBytes).accepted
}

data class StorageVerification(val accepted: Boolean, val reasons: List<String>)
