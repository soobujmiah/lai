package dev.lai.runtime.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

/**
 * Bounded discovery of model files in the granted workspace `models/` directory.
 *
 * Traversal is bounded by depth, a scanned-file cap, and per-file size; each file's container
 * format is detected from its magic bytes and its SHA-256 is streamed (only for recognized,
 * in-limit files). The resulting [ModelCandidate] list is classified by the pure
 * [WorkspacePolicy], which deduplicates by digest, matches the reviewed catalog, and applies the
 * registration count limit. Discovery registers metadata only; it never allocates weights or
 * auto-loads inference — loading still copies/verifies into app-private runtime storage.
 *
 * @param reviewedBySha256 lowercased-hex SHA-256 -> reviewed catalog model id.
 */
class WorkspaceDiscovery(
    private val repository: WorkspaceRepository,
    private val policy: WorkspacePolicy = WorkspacePolicy(),
) : ModelDiscoveryPort {
    override suspend fun discoverModels(
        reviewedBySha256: Map<String, String>,
        limits: DiscoveryLimits,
    ): Result<List<DiscoveredModel>> = withContext(Dispatchers.IO) {
        runCatching {
            val saf = repository.saf() ?: error("Workspace not granted")
            val modelsDocId = saf.childDocumentId(saf.rootDocumentId, WorkspaceLayout.MODELS_DIRECTORY)
                ?: return@runCatching emptyList<DiscoveredModel>()
            val candidates = ArrayList<ModelCandidate>()
            val scanned = intArrayOf(0)
            walk(saf, modelsDocId, WorkspaceLayout.MODELS_DIRECTORY, depth = 0, limits, candidates, scanned)
            policy.classify(candidates, reviewedBySha256, limits)
        }
    }

    private fun walk(
        saf: WorkspaceSaf,
        directoryDocId: String,
        relativePath: String,
        depth: Int,
        limits: DiscoveryLimits,
        out: MutableList<ModelCandidate>,
        scanned: IntArray,
    ) {
        if (depth > MAX_DEPTH || scanned[0] >= MAX_SCAN) return
        val entries = runCatching { saf.listChildren(directoryDocId) }.getOrDefault(emptyList())
        entries.sortedBy { it.name ?: "" }.forEach { entry ->
            if (scanned[0] >= MAX_SCAN) return@forEach
            val childRelativePath = "$relativePath/${entry.name}"
            if (entry.isDirectory) {
                walk(saf, entry.documentId, childRelativePath, depth + 1, limits, out, scanned)
            } else {
                scanned[0]++
                inspectFile(saf, entry, childRelativePath)?.let(out::add)
            }
        }
    }

    /**
     * Reads a bounded file once: detect the container format from the header, and if recognized
     * continue streaming the same stream into the SHA-256 digest. Returns null only when the file
     * cannot be opened at all; oversized/unsupported/short files become REJECTED candidates.
     */
    private fun inspectFile(saf: WorkspaceSaf, entry: SafEntry, relativePath: String): ModelCandidate? {
        val name = entry.name ?: return null
        val size = entry.size
        if (size > MAX_HARDCAP_BYTES) {
            return ModelCandidate(relativePath, name, size, sha256 = null, modelFormat = null)
        }
        return runCatching {
            saf.openInput(entry.documentId).use { stream ->
                val header = ByteArray(HEADER_BYTES)
                val headerRead = readFully(stream, header)
                if (headerRead < HEADER_BYTES) {
                    return@use ModelCandidate(relativePath, name, size, sha256 = null, modelFormat = null)
                }
                val format = ModelFormatDetector.detect(header)
                if (format == null) {
                    return@use ModelCandidate(relativePath, name, size, sha256 = null, modelFormat = null)
                }
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(header, 0, headerRead)
                val chunk = ByteArray(STREAM_CHUNK_BYTES)
                while (true) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    digest.update(chunk, 0, read)
                }
                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                ModelCandidate(relativePath, name, size, sha256 = sha, modelFormat = format)
            }
        }.getOrNull()
    }

    private fun readFully(stream: InputStream, buffer: ByteArray): Int {
        var read = 0
        while (read < buffer.size) {
            val n = stream.read(buffer, read, buffer.size - read)
            if (n <= 0) break
            read += n
        }
        return read
    }

    companion object {
        private const val MAX_DEPTH = 4
        private const val MAX_SCAN = 256
        private const val HEADER_BYTES = 4
        private const val STREAM_CHUNK_BYTES = 64 * 1024

        // Never even open files far beyond any plausible model size; the classifier enforces the
        // real per-file limit via DiscoveryLimits.
        private const val MAX_HARDCAP_BYTES: Long = 8L * 1024 * 1024 * 1024
    }
}
