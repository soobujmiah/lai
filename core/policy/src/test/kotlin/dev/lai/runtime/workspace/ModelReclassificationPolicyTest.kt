package dev.lai.runtime.workspace

import dev.lai.runtime.inference.InstalledModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelReclassificationPolicyTest {

    private val policy = ModelReclassificationPolicy()

    private val qwenDigest = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
    private val otherDigest = "dcd819ff094852c38faba6873d8ff0c9d51eadb2844539e52042ae5d647bbfdb"
    private val reviewed = mapOf(qwenDigest to "qwen2.5-1.5b-instruct-q4-k-m")

    private fun model(
        id: String,
        sha256: String = qwenDigest,
        fileName: String = "$id.gguf",
        sourceUrl: String = "local-import",
        installedAtEpochMs: Long = 1_000L,
    ) = InstalledModel(
        id = id,
        displayName = "Qwen 2.5 1.5B Instruct",
        fileName = fileName,
        bytes = 1_117_320_736L,
        sha256 = sha256,
        sourceUrl = sourceUrl,
        installedAtEpochMs = installedAtEpochMs,
    )

    // ---------- stale filename-derived ids get reclassified ----------

    @Test
    fun `stale filename-derived id is reclassified to the canonical catalog id`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val plan = policy.reclassify(listOf(stale), reviewed)

        assertEquals(mapOf("qwen2.5-1.5b-instruct-q4_k_m" to "qwen2.5-1.5b-instruct-q4-k-m"), plan.idRemap)
        assertEquals("qwen2.5-1.5b-instruct-q4-k-m", plan.updatedRegistry.single().id)
    }

    @Test
    fun `reclassification never touches fileName, so the on-disk file is never renamed`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m", fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf")
        val plan = policy.reclassify(listOf(stale), reviewed)

        assertEquals("qwen2.5-1.5b-instruct-q4_k_m.gguf", plan.updatedRegistry.single().fileName)
    }

    @Test
    fun `reclassification preserves every field except id`() {
        val stale = model(
            id = "qwen2.5-1.5b-instruct-q4_k_m",
            sourceUrl = "local-import",
            installedAtEpochMs = 42_000L,
        )
        val result = policy.reclassify(listOf(stale), reviewed).updatedRegistry.single()

        assertEquals(stale.displayName, result.displayName)
        assertEquals(stale.fileName, result.fileName)
        assertEquals(stale.bytes, result.bytes)
        assertEquals(stale.sha256, result.sha256)
        assertEquals(stale.sourceUrl, result.sourceUrl)
        assertEquals(stale.installedAtEpochMs, result.installedAtEpochMs)
    }

    // ---------- already-canonical entries are left alone ----------

    @Test
    fun `already-canonical id is not touched and produces no remap entry`() {
        val canonical = model(id = "qwen2.5-1.5b-instruct-q4-k-m")
        val plan = policy.reclassify(listOf(canonical), reviewed)

        assertTrue(plan.idRemap.isEmpty())
        assertSame(canonical, plan.updatedRegistry.single())
    }

    @Test
    fun `unreviewed digest with no catalog match is left alone`() {
        val unmatched = model(id = "some-local-model", sha256 = "deadbeef".repeat(8))
        val plan = policy.reclassify(listOf(unmatched), reviewed)

        assertTrue(plan.idRemap.isEmpty())
        assertSame(unmatched, plan.updatedRegistry.single())
    }

    @Test
    fun `mixed registry only reclassifies the entry that needs it`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val other = model(id = "qwen2.5-1.5b-instruct-q4-0", sha256 = otherDigest)
        val plan = policy.reclassify(listOf(stale, other), reviewed)

        assertEquals(setOf("qwen2.5-1.5b-instruct-q4_k_m"), plan.idRemap.keys)
        assertEquals(
            listOf("qwen2.5-1.5b-instruct-q4-k-m", "qwen2.5-1.5b-instruct-q4-0"),
            plan.updatedRegistry.map { it.id },
        )
    }

    // ---------- idempotency ----------

    @Test
    fun `running reclassification twice in a row is a no-op the second time`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val first = policy.reclassify(listOf(stale), reviewed)
        val second = policy.reclassify(first.updatedRegistry, reviewed)

        assertTrue(second.idRemap.isEmpty())
        assertEquals(first.updatedRegistry, second.updatedRegistry)
    }

    @Test
    fun `repeated reclassification across many passes converges and stays stable`() {
        var registry = listOf(model(id = "qwen2.5-1.5b-instruct-q4_k_m"))
        repeat(5) { registry = policy.reclassify(registry, reviewed).updatedRegistry }

        assertEquals("qwen2.5-1.5b-instruct-q4-k-m", registry.single().id)
        assertTrue(policy.reclassify(registry, reviewed).idRemap.isEmpty())
    }

    // ---------- duplicate prevention ----------

    @Test
    fun `two entries matching the same canonical id never collide - only the first is renamed`() {
        val staleA = model(id = "qwen-legacy-a")
        val staleB = model(id = "qwen-legacy-b")
        val plan = policy.reclassify(listOf(staleA, staleB), reviewed)

        // Exactly one rename happened, in registry order.
        assertEquals(mapOf("qwen-legacy-a" to "qwen2.5-1.5b-instruct-q4-k-m"), plan.idRemap)
        val ids = plan.updatedRegistry.map { it.id }
        assertEquals(listOf("qwen2.5-1.5b-instruct-q4-k-m", "qwen-legacy-b"), ids)
        // No id collision: every id in the resulting registry is unique.
        assertEquals(ids.size, ids.toSet().size)
        // Neither entry was dropped -- two inputs, two outputs.
        assertEquals(2, plan.updatedRegistry.size)
    }

    @Test
    fun `an entry is never reclassified onto an id another entry already legitimately owns`() {
        // "qwen2.5-1.5b-instruct-q4-k-m" is already registered (e.g. a fresh, correctly-classified
        // import) alongside an older stale duplicate of the exact same file under a legacy id.
        val alreadyCanonical = model(id = "qwen2.5-1.5b-instruct-q4-k-m")
        val staleDuplicate = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val plan = policy.reclassify(listOf(alreadyCanonical, staleDuplicate), reviewed)

        assertTrue("must not rename onto an id that already exists", plan.idRemap.isEmpty())
        assertEquals(
            listOf("qwen2.5-1.5b-instruct-q4-k-m", "qwen2.5-1.5b-instruct-q4_k_m"),
            plan.updatedRegistry.map { it.id },
        )
    }

    // ---------- legacy references (the contract callers rely on) ----------

    @Test
    fun `idRemap lets a caller holding a reference to the old id resolve it to the live entry`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val plan = policy.reclassify(listOf(stale), reviewed)

        // Simulates MainViewModel's `activeModelId`/any other id-keyed live reference: apply the
        // remap, falling back to the original value when nothing changed.
        val legacyReference: String? = "qwen2.5-1.5b-instruct-q4_k_m"
        val resolved = legacyReference?.let { plan.idRemap[it] } ?: legacyReference

        assertEquals("qwen2.5-1.5b-instruct-q4-k-m", resolved)
        assertTrue(plan.updatedRegistry.any { it.id == resolved })
    }

    @Test
    fun `a reference to an id that was not reclassified is left exactly as-is`() {
        val canonical = model(id = "qwen2.5-1.5b-instruct-q4-k-m")
        val plan = policy.reclassify(listOf(canonical), reviewed)

        val reference: String? = "qwen2.5-1.5b-instruct-q4-k-m"
        val resolved = reference?.let { plan.idRemap[it] } ?: reference

        assertEquals(reference, resolved)
    }

    @Test
    fun `a null reference (no active model) is unaffected by reclassification`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val plan = policy.reclassify(listOf(stale), reviewed)

        val reference: String? = null
        val resolved = reference?.let { plan.idRemap[it] } ?: reference

        assertEquals(null, resolved)
    }

    // ---------- structural edge cases ----------

    @Test
    fun `empty registry reclassifies to an empty plan`() {
        val plan = policy.reclassify(emptyList(), reviewed)

        assertTrue(plan.idRemap.isEmpty())
        assertTrue(plan.updatedRegistry.isEmpty())
    }

    @Test
    fun `empty reviewed catalog reclassifies nothing`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m")
        val plan = policy.reclassify(listOf(stale), emptyMap())

        assertTrue(plan.idRemap.isEmpty())
        assertSame(stale, plan.updatedRegistry.single())
    }

    @Test
    fun `sha256 matching is case insensitive, matching WorkspacePolicy's own contract`() {
        val stale = model(id = "qwen2.5-1.5b-instruct-q4_k_m", sha256 = qwenDigest.uppercase())
        val plan = policy.reclassify(listOf(stale), reviewed)

        assertEquals("qwen2.5-1.5b-instruct-q4-k-m", plan.updatedRegistry.single().id)
    }
}
