package dev.lai.runtime.workspace

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.InputStream
import java.io.OutputStream

/** A child entry returned by [WorkspaceSaf.listChildren]. */
data class SafEntry(
    val documentId: String,
    val name: String?,
    val mimeType: String?,
    val size: Long,
) {
    val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}

/**
 * Thin wrapper over [DocumentsContract] for a single granted SAF tree. Everything is resolved
 * through document IDs relative to the tree; a `content://` URI is never translated into a raw
 * filesystem path, and no `MANAGE_EXTERNAL_STORAGE` permission is ever requested.
 */
class WorkspaceSaf(
    private val resolver: ContentResolver,
    private val treeUri: Uri,
) {
    val rootDocumentId: String = DocumentsContract.getTreeDocumentId(treeUri)

    private fun documentUri(documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    /** First child document id with the given display name under [parentDocumentId], or null. */
    fun childDocumentId(parentDocumentId: String, name: String): String? =
        listChildren(parentDocumentId).firstOrNull { it.name == name }?.documentId

    fun listChildren(parentDocumentId: String): List<SafEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val entries = ArrayList<SafEntry>()
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            if (idIndex < 0) return@use
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex) ?: continue
                entries += SafEntry(
                    documentId = id,
                    name = if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null,
                    size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                )
            }
        }
        return entries
    }

    /** Idempotent: returns the existing directory's id or creates it. */
    fun createDirectory(parentDocumentId: String, name: String): String? {
        childDocumentId(parentDocumentId, name)?.let { return it }
        return createDocument(parentDocumentId, DocumentsContract.Document.MIME_TYPE_DIR, name)
    }

    /** Creates a document and returns its document id, or null on failure. */
    fun createDocument(parentDocumentId: String, mimeType: String, name: String): String? {
        val uri = DocumentsContract.createDocument(resolver, documentUri(parentDocumentId), mimeType, name)
            ?: return null
        return DocumentsContract.getDocumentId(uri)
    }

    fun delete(documentId: String): Boolean =
        DocumentsContract.deleteDocument(resolver, documentUri(documentId))

    /** Renames a document and returns its new document id, or null on failure. */
    fun rename(documentId: String, newName: String): String? {
        val uri = DocumentsContract.renameDocument(resolver, documentUri(documentId), newName)
            ?: return null
        return DocumentsContract.getDocumentId(uri)
    }

    fun openInput(documentId: String): InputStream =
        resolver.openInputStream(documentUri(documentId))
            ?: error("Could not open document for reading")

    fun openOutput(documentId: String): OutputStream =
        resolver.openOutputStream(documentUri(documentId))
            ?: error("Could not open document for writing")
}
