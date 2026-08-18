package dev.lai.runtime.workspace

/**
 * Name-based replace protocol for document stores that cannot overwrite on rename
 * (Android SAF / `DocumentsContract.renameDocument`).
 *
 * Invariant: a failed or interrupted replace must leave a readable last-known-good
 * document under [targetName] or [backupName]. The previous implementation deleted
 * [targetName] before rename completed, so a failed finalize destroyed the last good file.
 *
 * Sequence:
 * 1. Write new bytes only to [tempName].
 * 2. If [targetName] exists, move it to [backupName] (after dropping a stale backup).
 * 3. Rename [tempName] to [targetName].
 * 4. On success, drop [backupName]. On finalize failure, restore [backupName] to [targetName].
 */
class AtomicNamedDocumentReplace {

    data class Handle(val id: String)

    interface Store {
        fun find(name: String): Handle?
        fun create(name: String): Handle
        fun write(handle: Handle, bytes: ByteArray)
        fun delete(handle: Handle): Boolean
        /** Returns the new handle, or null if the store rejected the rename. */
        fun rename(handle: Handle, newName: String): Handle?
    }

    /**
     * Locate the readable settings document. If only a backup remains after a crash,
     * attempt to restore it to [targetName]; if rename fails the backup handle is still returned.
     */
    fun resolveReadable(store: Store, targetName: String, backupName: String): Handle? {
        store.find(targetName)?.let { return it }
        val backup = store.find(backupName) ?: return null
        return store.rename(backup, targetName) ?: backup
    }

    fun replace(
        store: Store,
        targetName: String,
        tempName: String,
        backupName: String,
        bytes: ByteArray,
    ) {
        require(bytes.isNotEmpty()) { "replacement document must not be empty" }
        writeTemp(store, tempName, bytes)
        val temp = store.find(tempName) ?: error("temp document missing after write")
        val existing = store.find(targetName)
        if (existing != null) {
            store.find(backupName)?.let { stale -> store.delete(stale) }
            if (store.rename(existing, backupName) == null) {
                store.delete(temp)
                error("could not park last-known-good settings as backup")
            }
        }
        if (store.rename(temp, targetName) == null) {
            store.find(backupName)?.let { parked -> store.rename(parked, targetName) }
            store.find(tempName)?.let { leftover -> store.delete(leftover) }
            error("could not finalize settings file")
        }
        store.find(backupName)?.let { done -> store.delete(done) }
    }

    private fun writeTemp(store: Store, tempName: String, bytes: ByteArray) {
        val handle = store.find(tempName) ?: store.create(tempName)
        runCatching { store.write(handle, bytes) }
            .onFailure {
                store.delete(handle)
                throw it
            }
    }
}
