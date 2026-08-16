package dev.lai.runtime.workspace

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Owns the user-granted SAF tree for the LAI workspace.
 *
 * Grant is obtained from the ActivityResult of `ACTION_OPEN_DOCUMENT_TREE`; the repository takes a
 * persistable read/write permission and remembers the tree URI in app-private preferences. It never
 * translates a `content://` URI into a raw path and never requests `MANAGE_EXTERNAL_STORAGE`.
 */
class WorkspaceRepository(private val context: Context) : WorkspaceGrantPort {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Tree URI of the currently granted workspace, or null when none is remembered. */
    val grantedTreeUri: Uri?
        get() = prefs.getString(KEY_TREE_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    /** Current grant state derived from remembered URI + held persistable permissions. */
    override val state: WorkspaceGrantState
        get() {
            val uri = grantedTreeUri ?: return WorkspaceGrantState.NOT_GRANTED
            val held = context.contentResolver.persistedUriPermissions.any { it.uri == uri }
            return if (held) WorkspaceGrantState.GRANTED else WorkspaceGrantState.REVOKED
        }

    /**
     * SAF handle for the granted tree, or null when not granted / permission revoked. Internal so only
     * workspace adapters ([WorkspaceSettingsStore], [WorkspaceDiscovery]) touch SAF directly.
     */
    internal fun saf(): WorkspaceSaf? {
        val tree = grantedTreeUri ?: return null
        return if (context.contentResolver.persistedUriPermissions.any { it.uri == tree }) {
            WorkspaceSaf(context.contentResolver, tree)
        } else null
    }

    /** Call from the `ACTION_OPEN_DOCUMENT_TREE` result. Persists read/write permission. */
    fun grant(treeUri: Uri): Result<Unit> = runCatching {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
    }

    /** Releases the persistable permission (best-effort) and forgets the tree URI. */
    fun revoke() {
        grantedTreeUri?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.edit().remove(KEY_TREE_URI).apply()
    }

    /** Idempotently ensures the canonical managed directories exist directly under the granted tree. */
    fun ensureLayout(): Result<Unit> = runCatching {
        val saf = saf() ?: error("Workspace not granted")
        WorkspaceLayout.managedDirectories.forEach { directory ->
            saf.createDirectory(saf.rootDocumentId, directory) ?: error("Could not create directory $directory")
        }
    }

    companion object {
        private const val PREFS_NAME = "lai_workspace"
        private const val KEY_TREE_URI = "tree_uri"
    }
}
