package dev.lai.runtime.workspace

import dev.lai.runtime.settings.SettingsDocumentV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Bounded reader/writer for the non-secret `config/settings.json` inside the granted workspace.
 *
 * All heavy lifting (range/finite/schema validation, default merging, migration, encoding) is
 * delegated to [WorkspaceSettingsCodec] in pure core; this adapter owns only the SAF byte transfer
 * and the safe temp-write-then-replace strategy. Reads always return an effective document, falling
 * back to embedded defaults when the workspace is not granted, the file is absent, oversized, or
 * malformed. Writes verify the candidate is the exact v1 schema before persisting.
 */
class WorkspaceSettingsStore(
    private val repository: WorkspaceRepository,
    private val codec: WorkspaceSettingsCodec = WorkspaceSettingsCodec(),
    private val maxBytes: Int = MAX_BYTES,
) {
    /** Effective document plus provenance for UI/diagnostics. Never throws. */
    suspend fun load(): SettingsLoadOutcome = withContext(Dispatchers.IO) {
        val saf = repository.saf() ?: return@withContext defaults("workspace not granted")
        val configDocId = saf.childDocumentId(saf.rootDocumentId, WorkspaceLayout.CONFIG_DIRECTORY)
            ?: return@withContext defaults("config directory absent")
        val settingsDocId = saf.childDocumentId(configDocId, WorkspaceLayout.SETTINGS_FILE_NAME)
            ?: return@withContext defaults("settings file absent")
        val bytes = runCatching { readBounded(saf.openInput(settingsDocId)) }
            .getOrElse { return@withContext defaults("settings file could not be read: ${it.message}") }
        when (val outcome = codec.decode(bytes, maxBytes)) {
            is WorkspaceSettingsCodec.DecodeOutcome.Loaded -> SettingsLoadOutcome(
                document = outcome.document,
                fromFile = true,
                fellBackToDefaults = outcome.fellBackToDefaults,
                warnings = outcome.warnings,
            )
            is WorkspaceSettingsCodec.DecodeOutcome.Malformed ->
                defaults("malformed settings: ${outcome.reason}")
            WorkspaceSettingsCodec.DecodeOutcome.Oversized ->
                defaults("settings exceeded $maxBytes bytes")
            WorkspaceSettingsCodec.DecodeOutcome.Absent ->
                defaults("settings file empty")
        }
    }

    /** Verifies and persists [document] via temp-write-then-replace. Fails when not granted. */
    suspend fun save(document: SettingsDocumentV1): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val saf = repository.saf() ?: error("Workspace not granted")
            repository.ensureLayout().getOrThrow()
            val configDocId = saf.childDocumentId(saf.rootDocumentId, WorkspaceLayout.CONFIG_DIRECTORY)
                ?: error("config directory missing")
            val bytes = codec.encode(document)
            require(codec.verifyForStorage(bytes, maxBytes).accepted) {
                "refusing to store settings that fail schema verification"
            }

            val tempName = WorkspaceLayout.SETTINGS_FILE_NAME + TMP_SUFFIX
            saf.childDocumentId(configDocId, tempName)?.let { existingTemp -> runCatching { saf.delete(existingTemp) } }
            val tempDocId = saf.createDocument(configDocId, MIME_JSON, tempName)
                ?: error("could not create temp settings file")
            runCatching { saf.openOutput(tempDocId).use { it.write(bytes) } }
                .onFailure { runCatching { saf.delete(tempDocId) } }
                .getOrThrow()

            saf.childDocumentId(configDocId, WorkspaceLayout.SETTINGS_FILE_NAME)?.let { existing ->
                runCatching { saf.delete(existing) }
            }
            saf.rename(tempDocId, WorkspaceLayout.SETTINGS_FILE_NAME)
                ?: error("could not finalize settings file")
        }.map { }
    }

    private fun readBounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0
        input.use { stream ->
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > maxBytes) error("settings exceeded $maxBytes bytes")
                out.write(chunk, 0, read)
            }
        }
        return out.toByteArray()
    }

    private fun defaults(reason: String) = SettingsLoadOutcome(
        document = SettingsDocumentV1(),
        fromFile = false,
        fellBackToDefaults = true,
        warnings = listOf(reason),
    )

    companion object {
        const val MAX_BYTES = 32 * 1024
        private const val TMP_SUFFIX = ".tmp"
        private const val MIME_JSON = "application/json"
    }
}

/** Effective loaded settings and provenance. [document] is always safe to use. */
data class SettingsLoadOutcome(
    val document: SettingsDocumentV1,
    val fromFile: Boolean,
    val fellBackToDefaults: Boolean,
    val warnings: List<String>,
)
