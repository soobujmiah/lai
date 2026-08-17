package dev.lai.runtime.history

import android.content.Context
import dev.lai.runtime.core.LaiJson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * App-private, no-backup chat history storage. One JSON file per session.
 *
 * Storage rules (the strictest in the app, because this is the only store holding conversation
 * content):
 * - Lives in `noBackupFilesDir/chats` — never in cloud backups, never behind SAF, never network.
 * - Bounded: at most [MAX_SESSIONS] sessions (oldest pruned on save) and [MAX_MESSAGES] messages
 *   per session (oldest dropped), so history can never grow without limit.
 * - Writes are atomic (temp file + rename): a crash mid-save can corrupt at most the temp file,
 *   never an existing session.
 * - Corrupt or foreign files are skipped by [list] and [load] instead of failing the whole
 *   feature; history is a convenience, not a ledger (the audit hash chain is `platform:audit`).
 */
class ChatHistoryRepository internal constructor(
    private val directory: File,
) {
    constructor(context: Context) : this(File(context.noBackupFilesDir, "chats"))

    private val mutex = Mutex()
    private val json = Json(LaiJson) { ignoreUnknownKeys = true }

    suspend fun list(): List<ChatSessionSummary> = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessionFiles().mapNotNull { file ->
                readSession(file)?.let { session ->
                    ChatSessionSummary(
                        id = session.id,
                        title = session.title,
                        updatedAtEpochMs = session.updatedAtEpochMs,
                        messageCount = session.messages.size,
                    )
                }
            }.sortedByDescending { it.updatedAtEpochMs }
        }
    }

    suspend fun load(id: String): StoredChatSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!ID.matches(id)) return@withLock null
            readSession(fileFor(id))
        }
    }

    suspend fun save(session: StoredChatSession): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                require(ID.matches(session.id)) { "Chat session id is not valid" }
                directory.mkdirs()
                val bounded = session.copy(
                    title = session.title.take(MAX_TITLE_LENGTH),
                    messages = session.messages
                        .takeLast(MAX_MESSAGES)
                        .map { it.copy(text = it.text.take(MAX_MESSAGE_LENGTH)) },
                )
                val temp = File(directory, "${bounded.id}.json.tmp")
                temp.writeText(json.encodeToString(StoredChatSession.serializer(), bounded))
                val target = fileFor(bounded.id)
                check(temp.renameTo(target) || (target.delete() && temp.renameTo(target))) {
                    "Chat session could not be committed to storage"
                }
                pruneBeyondLimit()
            }
        }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { ID.matches(id) && fileFor(id).delete() }
    }

    private fun sessionFiles(): List<File> =
        directory.listFiles { file -> file.isFile && file.name.startsWith("chat-") && file.name.endsWith(".json") }
            ?.toList() ?: emptyList()

    private fun readSession(file: File): StoredChatSession? = runCatching {
        json.decodeFromString(StoredChatSession.serializer(), file.readText())
            .takeIf { it.schemaVersion == 1 && ID.matches(it.id) }
    }.getOrNull()

    private fun pruneBeyondLimit() {
        val sessions = sessionFiles()
            .mapNotNull { file -> readSession(file)?.let { file to it.updatedAtEpochMs } }
            .sortedByDescending { it.second }
        sessions.drop(MAX_SESSIONS).forEach { (file, _) -> file.delete() }
    }

    private fun fileFor(id: String) = File(directory, "chat-$id.json")

    companion object {
        private const val MAX_SESSIONS = 100
        private const val MAX_MESSAGES = 512
        private const val MAX_MESSAGE_LENGTH = 32_768
        private const val MAX_TITLE_LENGTH = 80
        private val ID = Regex("^[a-f0-9-]{8,64}$")
    }
}
