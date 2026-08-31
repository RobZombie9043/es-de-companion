package com.esde.companion.data.gameguides

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** One picked file's raw bytes plus its original display name (for title/extension
 * derivation) - see [readBytesAndFileName]. */
internal data class ImportedGuideFile(
    val bytes: ByteArray,
    val fileName: String,
)

/**
 * Stream-based read for a single picked document [Uri] (ACTION_OPEN_DOCUMENT), used by
 * GameGuideImportPicker's Import flow - same [android.content.ContentResolver] stream
 * reasoning as [com.esde.companion.data.storage.ConfigBackupFileIo], just reading raw bytes
 * (a guide file may be binary - PDF/image) and the document's own display name instead of
 * text. Called directly from the Composable that owns the picker result, keeping raw [Uri]s
 * out of the ViewModel/domain layers.
 */
internal object GuideImportFileIo {
    fun readBytesAndFileName(
        context: Context,
        uri: Uri,
    ): Result<ImportedGuideFile> =
        runCatching {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to open input stream for $uri")
            val fileName = displayNameFor(context, uri) ?: uri.lastPathSegment ?: "guide"
            ImportedGuideFile(bytes, fileName)
        }

    private fun displayNameFor(
        context: Context,
        uri: Uri,
    ): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
