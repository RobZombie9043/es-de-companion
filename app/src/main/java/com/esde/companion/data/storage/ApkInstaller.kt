package com.esde.companion.data.storage

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Builds the system package-installer intent for a downloaded update APK. */
object ApkInstaller {
    fun installIntent(
        context: Context,
        apkFile: File,
    ): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
