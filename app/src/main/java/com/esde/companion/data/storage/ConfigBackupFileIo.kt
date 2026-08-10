package com.esde.companion.data.storage

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Stream-based read/write for a single SAF document [Uri] (ACTION_CREATE_DOCUMENT /
 * ACTION_OPEN_DOCUMENT), used by the Backup & Restore feature (Settings > Setup). Unlike
 * [SafPathResolver], this doesn't assume the [Uri] resolves to a plain filesystem path - a
 * "Save As" destination may be a genuine SAF/cloud document provider rather than local
 * storage, so writes/reads go through [android.content.ContentResolver]'s stream API
 * instead.
 *
 * Called directly from the Composable that owns the picker result, same as
 * [SafPathResolver] - keeps raw [Uri]s out of the ViewModel/domain layers.
 */
object ConfigBackupFileIo {
    fun writeText(
        context: Context,
        uri: Uri,
        text: String,
    ): Result<Unit> =
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri) ?: error("Unable to open output stream for $uri")
            stream.use { OutputStreamWriter(it).use { writer -> writer.write(text) } }
        }

    fun readText(
        context: Context,
        uri: Uri,
    ): Result<String> =
        runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: error("Unable to open input stream for $uri")
            stream.use { BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() } }
        }
}
