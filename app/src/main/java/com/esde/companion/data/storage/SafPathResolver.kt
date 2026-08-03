package com.esde.companion.data.storage

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * Converts a tree [Uri] returned by ACTION_OPEN_DOCUMENT_TREE into an absolute filesystem
 * path, so the rest of the app (validation, EsdeLogFileRepository, etc.) can keep working
 * with plain [java.io.File] paths - the SAF picker here is used purely as a ready-made
 * folder-browsing UI, not as the actual storage access mechanism (this app already holds
 * MANAGE_EXTERNAL_STORAGE - see CLAUDE.md / conversation history for why SAF's own
 * persistable-uri-grant model isn't used for reads).
 *
 * Reliable for the primary shared storage volume. SD card resolution relies on the
 * `<volume-id>:<relative-path>` document-id convention most OEMs follow, but this is not
 * guaranteed by the platform - returns null if the id can't be parsed, and callers should
 * treat that as "ask the user to try again" rather than silently falling back to a guess.
 */
object SafPathResolver {

    fun resolvePath(treeUri: Uri): String? {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        return resolveFromDocumentId(docId)
    }

    /**
     * Same idea as [resolvePath] but for a single-document [Uri] from
     * ACTION_OPEN_DOCUMENT (e.g. a user-picked image file) rather than a document *tree*
     * from ACTION_OPEN_DOCUMENT_TREE - the document-id convention (`<volume-id>:<relative-
     * path>`) is identical, only the id-extraction call differs.
     */
    fun resolveDocumentPath(documentUri: Uri): String? {
        val docId = runCatching { DocumentsContract.getDocumentId(documentUri) }.getOrNull() ?: return null
        return resolveFromDocumentId(docId)
    }

    private fun resolveFromDocumentId(docId: String): String? {
        val separatorIndex = docId.indexOf(':')
        if (separatorIndex == -1) return null

        val volumeId = docId.substring(0, separatorIndex)
        val relativePath = docId.substring(separatorIndex + 1)

        val root = if (volumeId.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            "/storage/$volumeId"
        }

        return if (relativePath.isEmpty()) root else "$root/$relativePath"
    }
}