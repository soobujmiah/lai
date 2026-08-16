package dev.lai.runtime.workspace

import dev.lai.runtime.settings.ImageGenerationSettings
import dev.lai.runtime.settings.LlmSettings
import dev.lai.runtime.settings.SearchSettings
import dev.lai.runtime.settings.SettingsDocumentV1
import dev.lai.runtime.settings.VoiceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePolicyTest {

    private val policy = WorkspacePolicy()
    private val codec = WorkspaceSettingsCodec()

    private val reviewedDigest = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
    private val reviewed = mapOf(reviewedDigest to "qwen2.5-1.5b-instruct-q4-k-m")

    private fun candidate(
        path: String,
        sha: String?,
        size: Long = 1_000L,
        format: String? = "gguf",
    ) = ModelCandidate(path, path.substringAfterLast('/'), size, sha, format)

    private fun statuses(models: List<DiscoveredModel>) = models.associate { it.relativePath to it.status }

    // ---------- classification ----------

    @Test
    fun `empty input classifies to empty output`() {
        assertTrue(policy.classify(emptyList(), reviewed).isEmpty())
    }

    @Test
    fun `reviewed digest is matched and catalog id attached`() {
        val models = policy.classify(listOf(candidate("models/qwen.gguf", reviewedDigest)), reviewed)
        assertEquals(ModelDiscoveryStatus.REVIEWED, models.single().status)
        assertEquals("qwen2.5-1.5b-instruct-q4-k-m", models.single().reviewedCatalogId)
        assertNull(models.single().rejectionReason)
    }

    @Test
    fun `unknown digest is local unreviewed`() {
        val models = policy.classify(listOf(candidate("models/local.gguf", "deadbeef")), reviewed)
        assertEquals(ModelDiscoveryStatus.LOCAL_UNREVIEWED, models.single().status)
        assertNull(models.single().reviewedCatalogId)
    }

    @Test
    fun `digest matching is case insensitive`() {
        val models = policy.classify(listOf(candidate("models/q.gguf", reviewedDigest.uppercase())), reviewed)
        assertEquals(ModelDiscoveryStatus.REVIEWED, models.single().status)
    }

    @Test
    fun `oversized files are rejected`() {
        val models = policy.classify(
            listOf(candidate("models/big.gguf", "aa", size = 5L * 1024 * 1024 * 1024)),
            reviewed,
        )
        assertEquals(ModelDiscoveryStatus.REJECTED, models.single().status)
        assertEquals("oversized", models.single().rejectionReason)
    }

    @Test
    fun `unsupported or unknown formats are rejected`() {
        val unknown = policy.classify(listOf(candidate("models/a.bin", "aa", format = "bin")), reviewed)
        assertEquals(ModelDiscoveryStatus.REJECTED, unknown.single().status)
        assertEquals("unsupported_format", unknown.single().rejectionReason)

        val nullFormat = policy.classify(listOf(candidate("models/b.gguf", "bb", format = null)), reviewed)
        assertEquals(ModelDiscoveryStatus.REJECTED, nullFormat.single().status)
        assertEquals("unsupported_format", nullFormat.single().rejectionReason)
    }

    @Test
    fun `files without a digest are rejected`() {
        val models = policy.classify(listOf(candidate("models/nohash.gguf", null)), reviewed)
        assertEquals(ModelDiscoveryStatus.REJECTED, models.single().status)
        assertEquals("missing_digest", models.single().rejectionReason)
    }

    @Test
    fun `duplicate digests keep the first and reject the rest`() {
        val models = policy.classify(
            listOf(
                candidate("models/zeta.gguf", "dup"),
                candidate("models/alpha.gguf", "dup"),
            ),
            reviewed,
        )
        // sorted by path: alpha first
        assertEquals(ModelDiscoveryStatus.LOCAL_UNREVIEWED, statuses(models)["models/alpha.gguf"])
        assertEquals(ModelDiscoveryStatus.REJECTED, statuses(models)["models/zeta.gguf"])
        assertEquals("duplicate_digest", models.first { it.relativePath == "models/zeta.gguf" }.rejectionReason)
    }

    @Test
    fun `registration count limit rejects extras in path order`() {
        val candidates = (1..5).map { candidate("models/m$it.gguf", "d$it") }
        val models = policy.classify(candidates, reviewed, DiscoveryLimits(maxRegisteredFiles = 2))
        val accepted = models.filter { it.status != ModelDiscoveryStatus.REJECTED }
        assertEquals(2, accepted.size)
        val limited = models.filter { it.rejectionReason == "discovery_limit" }
        assertEquals(3, limited.size)
    }

    @Test
    fun `output is sorted by relative path regardless of input order`() {
        val models = policy.classify(
            listOf(candidate("models/zebra.gguf", "1"), candidate("models/apple.gguf", "2")),
            reviewed,
        )
        assertEquals(listOf("models/apple.gguf", "models/zebra.gguf"), models.map { it.relativePath })
    }

    @Test
    fun `rejected oversized file does not consume a digest for its duplicates`() {
        // an oversized file is excluded before dedup, so a same-digest valid file still registers
        val models = policy.classify(
            listOf(
                candidate("models/big.gguf", "d1", size = 5L * 1024 * 1024 * 1024),
                candidate("models/small.gguf", "d1", size = 1_000L),
            ),
            reviewed,
        )
        assertEquals(ModelDiscoveryStatus.REJECTED, statuses(models)["models/big.gguf"])
        assertEquals(ModelDiscoveryStatus.LOCAL_UNREVIEWED, statuses(models)["models/small.gguf"])
    }

    // ---------- settings codec ----------

    @Test
    fun `encode then decode round trips a valid document`() {
        val original = SettingsDocumentV1(llm = LlmSettings(temperature = 0.3f))
        val outcome = codec.decode(codec.encode(original), maxBytes = 16 * 1024)
        assertTrue(outcome is WorkspaceSettingsCodec.DecodeOutcome.Loaded)
        val loaded = outcome as WorkspaceSettingsCodec.DecodeOutcome.Loaded
        assertEquals(original, loaded.document)
        assertFalse(loaded.fellBackToDefaults)
    }

    @Test
    fun `null and empty bytes are absent`() {
        assertTrue(codec.decode(null, maxBytes = 1024) is WorkspaceSettingsCodec.DecodeOutcome.Absent)
        assertTrue(codec.decode(ByteArray(0), maxBytes = 1024) is WorkspaceSettingsCodec.DecodeOutcome.Absent)
    }

    @Test
    fun `oversized bytes are rejected`() {
        val outcome = codec.decode(ByteArray(100), maxBytes = 16)
        assertTrue(outcome is WorkspaceSettingsCodec.DecodeOutcome.Oversized)
    }

    @Test
    fun `malformed json is reported`() {
        val outcome = codec.decode("{not json".toByteArray(), maxBytes = 1024)
        assertTrue(outcome is WorkspaceSettingsCodec.DecodeOutcome.Malformed)
    }

    @Test
    fun `out of range document falls back to defaults on read`() {
        val outcome = codec.decode("""{"llm":{"temperature":9.0}}""".toByteArray(), maxBytes = 1024)
        assertTrue(outcome is WorkspaceSettingsCodec.DecodeOutcome.Loaded)
        val loaded = outcome as WorkspaceSettingsCodec.DecodeOutcome.Loaded
        assertTrue(loaded.fellBackToDefaults)
        assertEquals(SettingsDocumentV1(), loaded.document)
    }

    @Test
    fun `verifyForStorage accepts a clean encoded document`() {
        val bytes = codec.encode(SettingsDocumentV1())
        val verification = codec.verifyForStorage(bytes, maxBytes = 16 * 1024)
        assertTrue(verification.accepted)
        assertTrue(verification.reasons.isEmpty())
        assertTrue(codec.isStorable(bytes, 16 * 1024))
    }

    @Test
    fun `verifyForStorage rejects empty oversized malformed out of range and unknown field documents`() {
        assertFalse(codec.verifyForStorage(ByteArray(0), 1024).accepted)
        assertFalse(codec.verifyForStorage(ByteArray(10), 4).accepted)
        assertFalse(codec.verifyForStorage("{bad".toByteArray(), 1024).accepted)
        assertFalse(codec.verifyForStorage("""{"llm":{"temperature":9.0}}""".toByteArray(), 1024).accepted)
        // unknown fields are rejected on write so the file stays the exact v1 schema
        assertFalse(codec.verifyForStorage("""{"llm":{"temperature":0.7},"prompt":"বাংলা"}""".toByteArray(), 1024).accepted)
    }

    @Test
    fun `a truncated write is malformed rather than fatal`() {
        // Simulates a partially written file (power loss / interrupted SAF write).
        val truncated = codec.encode(SettingsDocumentV1()).copyOfRange(0, 12)
        assertTrue(codec.decode(truncated, maxBytes = 16 * 1024) is WorkspaceSettingsCodec.DecodeOutcome.Malformed)
    }

    @Test
    fun `an unsupported schema version falls back to defaults on read`() {
        val outcome = codec.decode("""{"schemaVersion":9,"llm":{"temperature":0.5}}""".toByteArray(), maxBytes = 1024)
        val loaded = outcome as WorkspaceSettingsCodec.DecodeOutcome.Loaded
        assertTrue(loaded.fellBackToDefaults)
        assertEquals(SettingsDocumentV1(), loaded.document)
    }

    @Test
    fun `storage rejection names the offending field`() {
        val smuggled = """{"schemaVersion":1,"note":"a private message the user never meant to store"}"""
        val verification = codec.verifyForStorage(smuggled.toByteArray(), 16 * 1024)
        assertFalse(verification.accepted)
        assertTrue(verification.reasons.toString(), verification.reasons.any { it.contains("note") })
    }

    @Test
    fun `maxBytes boundary is inclusive`() {
        val bytes = codec.encode(SettingsDocumentV1())
        assertTrue(codec.decode(bytes, maxBytes = bytes.size) is WorkspaceSettingsCodec.DecodeOutcome.Loaded)
    }
}
